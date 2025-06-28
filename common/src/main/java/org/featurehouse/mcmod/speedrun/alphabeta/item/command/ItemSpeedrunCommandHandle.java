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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.*;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecord;
import org.featurehouse.mcmod.speedrun.alphabeta.item.coop.CoopRecordAccess;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.ItemListMenuSync;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.ItemListViewMenu;
import org.featurehouse.mcmod.speedrun.alphabeta.util.PacketUtil;

import java.util.*;
import java.util.function.Consumer;

public final class ItemSpeedrunCommandHandle {
    public static int startFromDraft(Consumer<? super Component> errorConsumer, ServerPlayer player, Draft draft) {
        final ResourceLocation goal = draft.getGoal();
        if (goal == null) {
            errorConsumer.accept(Component.translatable("command.speedrun.alphabet.draft.not_found"));
        }
        ItemSpeedrun speedrun = ItemSpeedrun.get(goal);
        if (speedrun == null) {
            errorConsumer.accept(Component.translatable("command.speedrun.alphabet.start.not_found", goal));
            return 0;
        }
        ItemRecordAccess record0;
        final long igt = Objects.requireNonNull(player.getServer()).overworld().getGameTime();
        if ((record0 = player.alphabetSpeedrun$getItemRecordAccess()) != null) {
            errorConsumer.accept(Component.translatable("command.speedrun.alphabet.start.started",
                    player.getDisplayName(), RecordSnapshot.fromRecord(record0, igt).asText()));
            return 0;
        }

        ItemSpeedrunRecord record = createSPRecord(speedrun, Objects.requireNonNull(player.getServer()), draft.getDifficulty());
        final List<UUID> players = draft.getPlayers();

        final PlayerList playerManager = Objects.requireNonNull(player.getServer()).getPlayerList();
        if (draft.getPlayType() == PlayType.COOP) {
            CoopRecord coopRecord = new CoopRecord(record, /*operators=*/draft.getOperators(), /*players=*/players);
            coopRecord.getMates(playerManager, null).forEach(p -> {
                // Everyone
                RecordSnapshot record1 = RecordSnapshot.fromRecord(coopRecord, igt);
                player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.start",
                        record1.asText()));
                ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, coopRecord, ItemSpeedrunEvents.StartRunning.START_COOP);
                coopRecord.onStart(player);
            });
        } else {    // PVP
            // owner
            RecordSnapshot record1 = RecordSnapshot.fromRecord(record, igt);
            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.start", record1.asText()));
            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.START);
            record.onStart(player);
            // invite
            players.forEach(p0 -> {
                ServerPlayer player1 = playerManager.getPlayer(p0);
                if (player1 == null) return;
                ItemSpeedrunRecord subRecord = createSPRecord(speedrun, playerManager.getServer(), draft.getDifficulty());
                RecordSnapshot recordSnapshot = RecordSnapshot.fromRecord(subRecord, igt);
                player1.sendSystemMessage(Component.translatable("command.speedrun.alphabet.start", recordSnapshot.asText()));
                ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player1, subRecord, ItemSpeedrunEvents.StartRunning.START);
                subRecord.onStart(player1);
                //subRecord.sudoJoin(p0, Collections.singleton(player));
                record.mates().put(p0, subRecord.recordId());
                subRecord.mates().put(player.getUUID(), record.recordId());
            });
        }
        return 1;
    }

    static int start(CommandSourceStack sender, ResourceLocation id, Collection<? extends ServerPlayer> players, ItemSpeedrunDifficulty difficulty) {
        final ItemSpeedrun speedrun = ItemSpeedrun.get(id);
        if (speedrun == null) {
            sender.sendFailure(Component.translatable("command.speedrun.alphabet.start.not_found", id));
            return 0;
        }
        if (players.isEmpty()) {
            sender.sendFailure(Component.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }
        for (ServerPlayer player : players) {
            final long time = Objects.requireNonNull(player.getServer()).overworld().getGameTime();
            ItemRecordAccess record;
            if ((record = player.alphabetSpeedrun$getItemRecordAccess()) != null) {
                sender.sendFailure(Component.translatable("command.speedrun.alphabet.start.started",
                        player.getDisplayName(), RecordSnapshot.fromRecord(record, time).asText()));
                continue;
            }
            record = createSPRecord(speedrun, sender.getServer(), difficulty);
            player.alphabetSpeedrun$setItemRecordAccess(record);
            // Start
            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.start",
                    RecordSnapshot.fromRecord(record, time).asText()));
            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.START);
            difficulty.onStart(player);
        }
        return 1;
    }

    static int start(CommandSourceStack sender, ResourceLocation id, Collection<? extends ServerPlayer> players) {
        return start(sender, id, players, DefaultItemSpeedrunDifficulty.UU);
    }

    public static void tryResumeInventory(ServerPlayer player) {
        final Inventory inv = player.getInventory();
        final int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemSpeedrunEvents.onItemPickup(player, inv.getItem(i));
        }
    }

    static int stop(CommandSourceStack sender, Collection<? extends ServerPlayer> players) {
        return stop(sender::sendFailure, players, true);
    }

    public static int quit(Consumer<? super Component> errorParser, ServerPlayer player, boolean checkPlayer) {
        final ItemRecordAccess rec = player.alphabetSpeedrun$getItemRecordAccess();
        if (rec == null) {
            errorParser.accept(Component.translatable("command.speedrun.alphabet.quit.nil"));
            return 0;
        }
        if (!rec.isCoop()) {
            // check permission
            if (!player.hasPermissions(AlphabetSpeedrunConfigData.getInstance().getPermissions().getStop())) {
                errorParser.accept(Component.translatable("command.speedrun.alphabet.no_permission"));
                return 0;
            }
            return stop(errorParser, Collections.singleton(player), checkPlayer);
        } else {
            return quitCoop(player, rec.asCoop(), checkPlayer);
        }
    }

    private static int quitCoop(ServerPlayer player, CoopRecordAccess coopRecord, boolean sendMsgToPlayer) {
        coopRecord.getPlayers().remove(player.getUUID());
        if (sendMsgToPlayer) {
            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.quit",
                    RecordSnapshot.fromRecord(coopRecord, Objects.requireNonNull(player.getServer()).overworld().getGameTime()).asText()));
        }
        return 1;
    }

    public static int stop(Consumer<? super Component> errorParser, Collection<? extends ServerPlayer> players, boolean sendMsgToPlayer) {
        if (players.isEmpty()) {
            errorParser.accept(Component.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }
        final int stopOthers = AlphabetSpeedrunConfigData.getInstance().getPermissions().getStopOthers();

        for (ServerPlayer player : players) {
            final ItemRecordAccess oldRecord = player.alphabetSpeedrun$getItemRecordAccess();
            ItemSpeedrunEvents.STOP_RUNNING_EVENT_PRE.invoker().onStopRunning(player, oldRecord);

            if (oldRecord != null) {
                if (oldRecord.isCoop()) {
                    // Check access
                    final CoopRecordAccess coop = oldRecord.asCoop();
                    boolean stop = coop.getOperators().contains(player.getUUID());
                    if (!stop && player.hasPermissions(stopOthers))
                        stop = true;

                    if (stop) {
                        final Component text = RecordSnapshot.fromRecord(oldRecord, Objects.requireNonNull(player.getServer()).overworld().getGameTime()).asText();

                        for (UUID coopPlayer : coop.getPlayers()) {
                            final ServerPlayer p0 = Objects.requireNonNull(player.getServer()).getPlayerList().getPlayer(coopPlayer);
                            if (p0 == null) continue;
                            p0.alphabetSpeedrun$setItemRecordAccess(null);
                            if (sendMsgToPlayer) {
                                p0.sendSystemMessage(Component.translatable("command.speedrun.alphabet.stop.coop", text));
                            }
                        }
                    }
                } else {
                    // Just stop yourself
                    if (!player.alphabetSpeedrun$moveRecordToHistory()) {
                        errorParser.accept(Component.translatable("command.speedrun.alphabet.stop.not_found",
                                player.getDisplayName()));
                        continue;
                    }
                    if (sendMsgToPlayer) {
                        player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.stop",
                                Component.translatable("command.speedrun.alphabet.stop.resume_tips")));
                    }
                }
            }
        }
        return 1;
    }

    static int resumeLocal(CommandSourceStack sender, Collection<? extends ServerPlayer> players) {
        if (players.isEmpty()) {
            sender.sendFailure(Component.translatable("command.speedrun.alphabet.players_empty"));
            return 0;
        }
        for (ServerPlayer player : players) {
            ItemRecordAccess record;
            final long time = Objects.requireNonNull(player.getServer()).overworld().getGameTime();
            if ((record = player.alphabetSpeedrun$getItemRecordAccess()) != null) {
                sender.sendFailure(Component.translatable("command.speedrun.alphabet.start.started",
                        player.getDisplayName(), RecordSnapshot.fromRecord(record, time).asText()));
                continue;
            }
            if (!player.alphabetSpeedrun$resumeLocalHistory()) {
                sender.sendFailure(Component.translatable("command.speedrun.alphabet.resume.not_found",
                        player.getDisplayName()));
                continue;
            }
            record = player.alphabetSpeedrun$getItemRecordAccess();
            Objects.requireNonNull(record);//.setLastQuitTime(-1);
            player.sendSystemMessage(Component.translatable("command.speedrun.alphabet.resume",
                    RecordSnapshot.fromRecord(record, time).asText()));
            ItemSpeedrunEvents.START_RUNNING_EVENT.invoker().onStartRunning(player, record, ItemSpeedrunEvents.StartRunning.FROM_LOCAL);
        }
        return 1;
    }

    public static int viewCurrentRecord(Consumer<? super Component> errorConsumer, ServerPlayer player) {
        final ItemRecordAccess record = player.alphabetSpeedrun$getItemRecordAccess();
        if (record == null || record.isFinished()) {
            errorConsumer.accept(Component.translatable("command.speedrun.alphabet.view.none"));
            return 0;
        }
        final ItemSpeedrun goal = ItemSpeedrun.get(record.goalId());
        if (goal == null) {
            errorConsumer.accept(Component.translatable("command.speedrun.alphabet.start.not_found"));
            return 0;
        }

        final int size = record.predicates().size();

        final List<ItemStack> iconList = record.displayedStacks();
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider((int syncId, Inventory ignore0, Player ignore1) -> {
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
            buf.writeUUID(record.recordId());
            iconList.forEach(itemStack -> PacketUtil.writeItemStack(buf, itemStack));
        });
        return 1;
    }

    static ItemSpeedrunRecord createSPRecord(ItemSpeedrun goal, MinecraftServer server, ItemSpeedrunDifficulty difficulty) {
        final List<ItemPredicateProvider> key = goal.items();
        final List<SingleSpeedrunPredicate> requirements0 = key.stream().flatMap(ItemPredicateProvider::flatMaps).toList();

        long startTime = server.overworld().getGameTime();
        UUID recordId = UUID.randomUUID();
        return new ItemSpeedrunRecord(goal.id(), recordId, requirements0,
                startTime, difficulty);
    }

}
