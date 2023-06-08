/*
 * This file is part of αβspeedrun.
 * Copyright (C) 2022 Pigeonia Featurehouse
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.featurehouse.mcmod.speedrun.alphabeta.item.command;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.*;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecord;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.ItemListMenuSync;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.ItemListViewMenu;

import java.util.*;
import java.util.function.Consumer;

public final class ItemSpeedrunCommandHandle {
    public static int startFromDraft(Consumer<? super Text> errorConsumer, ServerPlayerEntity player, Draft draft) {
        final Identifier goal = draft.getGoal();
        if (goal == null) {
            errorConsumer.accept(Text.translatable("command.speedrun.alphabet.draft.not_found"));
        }
        ItemSpeedrun speedrun = ItemSpeedrun.get(goal);
        if (speedrun == null) {
            errorConsumer.accept(Text.translatable("command.speedrun.alphabet.start.not_found", goal));
            return 0;
        }
        ItemRecordAccess record0;
        final long igt = player.server.getOverworld().getTime();
        if ((record0 = player.alphabetSpeedrun$getItemRecordAccess()) != null) {
            errorConsumer.accept(Text.translatable("command.speedrun.alphabet.start.started",
                    player.getDisplayName(), RecordSnapshot.fromRecord(record0, igt).asText()));
            return 0;
        }

        ItemSpeedrunRecord record = createSPRecord(speedrun, player.server, draft.getDifficulty());
        final List<UUID> players = draft.getPlayers();

        final PlayerManager playerManager = player.server.getPlayerManager();
        if (draft.getPlayType() == PlayType.COOP) {
            CoopRecord coopRecord = new CoopRecord(record, /*operators=*/draft.getOperators(), /*players=*/players);
            coopRecord.getMates(playerManager, null).forEach(p -> {
                // Everyone
                RecordSnapshot record1 = RecordSnapshot.fromRecord(coopRecord, igt);
                player.sendMessage(Text.translatable("command.speedrun.alphabet.start",
                        record1.asText()));
                ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, coopRecord, ItemSpeedrunEvents.StartRunning.START_COOP);
                coopRecord.onStart(player);
            });
        } else {    // PVP
            // owner
            RecordSnapshot record1 = RecordSnapshot.fromRecord(record, igt);
            player.sendMessage(Text.translatable("command.speedrun.alphabet.start", record1.asText()));
            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.START);
            record.onStart(player);
            // invite
            players.forEach(p0 -> {
                ServerPlayerEntity player1 = playerManager.getPlayer(p0);
                if (player1 == null) return;
                ItemSpeedrunRecord subRecord = createSPRecord(speedrun, playerManager.getServer(), draft.getDifficulty());
                RecordSnapshot recordSnapshot = RecordSnapshot.fromRecord(subRecord, igt);
                player1.sendMessage(Text.translatable("command.speedrun.alphabet.start", recordSnapshot.asText()));
                ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player1, subRecord, ItemSpeedrunEvents.StartRunning.START);
                subRecord.onStart(player1);
                //subRecord.sudoJoin(p0, Collections.singleton(player));
                record.mates().put(p0, subRecord.recordId());
                subRecord.mates().put(player.getUuid(), record.recordId());
            });
        }
        return 1;
    }

    static int start(ServerCommandSource sender, Identifier id, Collection<? extends ServerPlayerEntity> players, ItemSpeedrunDifficulty difficulty) {
        final ItemSpeedrun speedrun = ItemSpeedrun.get(id);
        if (speedrun == null) {
            sender.sendError(Text.translatable("command.speedrun.alphabet.start.not_found", id));
            return 0;
        }
        if (players.isEmpty()) {
            sender.sendError(Text.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }
        for (ServerPlayerEntity player : players) {
            final long time = player.server.getOverworld().getTime();
            ItemRecordAccess record;
            if ((record = player.alphabetSpeedrun$getItemRecordAccess()) != null) {
                sender.sendError(Text.translatable("command.speedrun.alphabet.start.started",
                        player.getDisplayName(), RecordSnapshot.fromRecord(record, time).asText()));
                continue;
            }
            record = createSPRecord(speedrun, sender.getServer(), difficulty);
            player.alphabetSpeedrun$setItemRecordAccess(record);
            // Start
            player.sendMessage(Text.translatable("command.speedrun.alphabet.start",
                    RecordSnapshot.fromRecord(record, time).asText()));
            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.START);
            difficulty.onStart(player);
        }
        return 1;
    }

    static int start(ServerCommandSource sender, Identifier id, Collection<? extends ServerPlayerEntity> players) {
        return start(sender, id, players, DefaultItemSpeedrunDifficulty.UU);
    }

    public static void tryResumeInventory(ServerPlayerEntity player) {
        final PlayerInventory inv = player.getInventory();
        final int size = inv.size();
        for (int i = 0; i < size; i++) {
            ItemSpeedrunEvents.onItemPickup(player, inv.getStack(i));
        }
    }

    static int stop(ServerCommandSource sender, Collection<? extends ServerPlayerEntity> players) {
        return stop(sender::sendError, players, true);
    }

    public static int quit(Consumer<? super Text> errorParser, ServerPlayerEntity player, boolean checkPlayer) {
        final ItemRecordAccess rec = player.alphabetSpeedrun$getItemRecordAccess();
        if (rec == null) {
            errorParser.accept(Text.translatable("command.speedrun.alphabet.quit.nil"));
            return 0;
        }
        if (!rec.isCoop()) {
            // check permission
            if (!player.hasPermissionLevel(AlphabetSpeedrunConfigData.getInstance().getPermissions().getStop())) {
                errorParser.accept(Text.translatable("command.speedrun.alphabet.no_permission"));
                return 0;
            }
            return stop(errorParser, Collections.singleton(player), checkPlayer);
        } else {
            return quitCoop(player, rec.asCoop(), checkPlayer);
        }
    }

    private static int quitCoop(ServerPlayerEntity player, CoopRecordAccess coopRecord, boolean sendMsgToPlayer) {
        coopRecord.getPlayers().remove(player.getUuid());
        if (sendMsgToPlayer) {
            player.sendMessage(Text.translatable("command.speedrun.alphabet.quit",
                    RecordSnapshot.fromRecord(coopRecord, player.server.getOverworld().getTime()).asText()));
        }
        return 1;
    }

    public static int stop(Consumer<? super Text> errorParser, Collection<? extends ServerPlayerEntity> players, boolean sendMsgToPlayer) {
        if (players.isEmpty()) {
            errorParser.accept(Text.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }
        final int stopOthers = AlphabetSpeedrunConfigData.getInstance().getPermissions().getStopOthers();

        for (ServerPlayerEntity player : players) {
            final ItemRecordAccess oldRecord = player.alphabetSpeedrun$getItemRecordAccess();
            ItemSpeedrunEvents.STOP_RUNNING_EVENT_PRE.invoker().onStopRunning(player, oldRecord);

            if (oldRecord != null) {
                if (oldRecord.isCoop()) {
                    // Check access
                    final CoopRecordAccess coop = oldRecord.asCoop();
                    boolean stop = coop.getOperators().contains(player.getUuid());
                    if (!stop && player.hasPermissionLevel(stopOthers))
                        stop = true;

                    if (stop) {
                        final Text text = RecordSnapshot.fromRecord(oldRecord, player.server.getOverworld().getTime()).asText();

                        for (UUID coopPlayer : coop.getPlayers()) {
                            final ServerPlayerEntity p0 = player.server.getPlayerManager().getPlayer(coopPlayer);
                            if (p0 == null) continue;
                            p0.alphabetSpeedrun$setItemRecordAccess(null);
                            if (sendMsgToPlayer) {
                                p0.sendMessage(Text.translatable("command.speedrun.alphabet.stop.coop", text));
                            }
                        }
                    }
                } else {
                    // Just stop yourself
                    if (!player.alphabetSpeedrun$moveRecordToHistory()) {
                        errorParser.accept(Text.translatable("command.speedrun.alphabet.stop.not_found",
                                player.getDisplayName()));
                        continue;
                    }
                    if (sendMsgToPlayer) {
                        player.sendMessage(Text.translatable("command.speedrun.alphabet.stop",
                                Text.translatable("command.speedrun.alphabet.stop.resume_tips")));
                    }
                }
            }
        }
        return 1;
    }

    static int resumeLocal(ServerCommandSource sender, Collection<? extends ServerPlayerEntity> players) {
        if (players.isEmpty()) {
            sender.sendError(Text.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }
        for (ServerPlayerEntity player : players) {
            ItemRecordAccess record;
            final long time = player.server.getOverworld().getTime();
            if ((record = player.alphabetSpeedrun$getItemRecordAccess()) != null) {
                sender.sendError(Text.translatable("command.speedrun.alphabet.start.started",
                        player.getDisplayName(), RecordSnapshot.fromRecord(record, time).asText()));
                continue;
            }
            if (!player.alphabetSpeedrun$resumeLocalHistory()) {
                sender.sendError(Text.translatable("command.speedrun.alphabet.resume.not_found",
                        player.getDisplayName()));
                continue;
            }
            record = player.alphabetSpeedrun$getItemRecordAccess();
            Objects.requireNonNull(record);//.setLastQuitTime(-1);
            player.sendMessage(Text.translatable("command.speedrun.alphabet.resume",
                    RecordSnapshot.fromRecord(record, time).asText()));
            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.FROM_LOCAL);
        }
        return 1;
    }

    public static int viewCurrentRecord(Consumer<? super Text> errorConsumer, ServerPlayerEntity player) {
        final ItemRecordAccess record = player.alphabetSpeedrun$getItemRecordAccess();
        if (record == null || record.isFinished()) {
            errorConsumer.accept(Text.translatable("command.speedrun.alphabet.view.none"));
            return 0;
        }
        final ItemSpeedrun goal = ItemSpeedrun.get(record.goalId());
        if (goal == null) {
            errorConsumer.accept(Text.translatable("command.speedrun.alphabet.start.not_found"));
            return 0;
        }

        final int size = record.predicates().size();

        final List<ItemStack> iconList = record.displayedStacks();
        MenuRegistry.openExtendedMenu(player, new SimpleNamedScreenHandlerFactory((int syncId, PlayerInventory ignore0, PlayerEntity ignore1) -> {
            ItemListMenuSync sync = new ItemListMenuSync.BitImpl(size) {
                @Override
                public boolean getBit(int idx) {
                    return record.isRequirementPassed(idx);
                }

                @Override
                public void setBit(int idx, boolean bit) {
                    // Not allowed
                }
            };
            //org.featurehouse.mcmod.speedrun.alphabeta.util.AlphaBetaDebug.log(logger -> logger.info(java.util.Arrays.toString(java.util.stream.IntStream.range(0, sync.getListSize()).mapToObj(sync::getBit).toArray())));
            return new ItemListViewMenu(syncId, iconList, true, sync, record.recordId());
        }, goal.display()), buf -> {
            buf.writeVarInt(size);
            buf.writeUuid(record.recordId());
            iconList.forEach(buf::writeItemStack);
        });
        return 1;
    }

    static ItemSpeedrunRecord createSPRecord(ItemSpeedrun goal, MinecraftServer server, ItemSpeedrunDifficulty difficulty) {
        final List<ItemPredicateProvider> key = goal.items();
        final List<SingleSpeedrunPredicate> requirements0 = key.stream().flatMap(ItemPredicateProvider::flatMaps).toList();

        long startTime = server.getOverworld().getTime();
        UUID recordId = UUID.randomUUID();
        return new ItemSpeedrunRecord(goal.id(), recordId, requirements0,
                startTime, difficulty);
    }
}
