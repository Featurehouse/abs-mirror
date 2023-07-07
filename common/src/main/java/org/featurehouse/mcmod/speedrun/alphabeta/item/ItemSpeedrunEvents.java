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

package org.featurehouse.mcmod.speedrun.alphabeta.item;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import net.minecraft.advancement.Advancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.DraftManager;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.ItemSpeedrunCommandHandle;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.ItemSpeedrunCommands;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.ItemListViewMenu;
import org.featurehouse.mcmod.speedrun.alphabeta.util.hooks.MultiverseHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ItemSpeedrunEvents {
    @FunctionalInterface
    public interface CollectedOne {
        EventResult onCollect(Either<ItemStack, Advancement> obj, ItemStack icon, ServerPlayerEntity player, ItemRecordAccess record);
    }

    public static final Event<CollectedOne> COLLECTED_ONE_EVENT = EventFactory.createEventResult();

    @FunctionalInterface
    public interface StartRunning {
        byte START_COOP = -101;
        byte JOIN_COOP = -102;
        byte START = -1, FROM_LOCAL = 0, FROM_DISK = 1;
        void onStartRunning(ServerPlayerEntity player, ItemRecordAccess record, byte resumeFrom);
    }
    public static final Event<StartRunning> START_RUNNING_EVENT = EventFactory.createLoop();

    @FunctionalInterface
    public interface StopRunning {
        void onStopRunning(ServerPlayerEntity player, ItemRecordAccess record);
    }
    public static final Event<StopRunning> STOP_RUNNING_EVENT_PRE = EventFactory.createLoop();

    @FunctionalInterface
    public interface FinishRecord {
        void onRecordFinish(ServerPlayerEntity player, ItemRecordAccess record, long gameTime);
    }

    public static final Event<FinishRecord> FINISH_RECORD_EVENT = EventFactory.createLoop();

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> ItemSpeedrunCommands.registerCommands(dispatcher));
        //PlayerEvent.PICKUP_ITEM_POST.register((player, itemEntity, stack) -> onItemPickup(player, stack));
        ReloadListenerRegistry.register(ResourceType.SERVER_DATA, new ItemSpeedrun.DataLoader());
        START_RUNNING_EVENT.register((player, record, resumeFrom) -> {
            ItemSpeedrunCommandHandle.tryResumeInventory(player);
            final long currentTime = player.server.getOverworld().getTime();
            ItemSpeedrunEvents.tryFinishRecord(record, currentTime, player);
            if (AlphabetSpeedrunConfigData.getInstance().isItemsOnlyAvailableWhenRunning()) {
                if (resumeFrom >= 0 /*resume, not start*/ && record.difficulty() instanceof DefaultItemSpeedrunDifficulty) {
                    // TODO multiplayer support
                    record.onStart(player);
                }
            }
            final long lastQuitTime = record.lastQuitTime();    // this is a timestamp
            // if last quit wasn't set, ignore
            if (lastQuitTime >= 0) {
                //record.vacantTime().add(lastQuitTime);
                record.setVacantTime(record.vacantTime() - lastQuitTime + currentTime);
                record.setLastQuitTime(-1);
            }
        });
        NetworkManager.registerReceiver(NetworkManager.c2s(), new Identifier("alphabet_speedrun", "item_list"),
                (buf, context) -> context.queue(() ->
                        ItemSpeedrunCommandHandle.viewCurrentRecord(
                                text -> context.getPlayer().sendMessage(text.copy().formatted(Formatting.RED)),
                                ((ServerPlayerEntity) context.getPlayer())
                        )));

        // Register ItemOnlyAvailableWhenRunning events
        TickEvent.PLAYER_POST.register(player -> {
            if (player.getWorld().isClient()) return;
            if (AlphabetSpeedrunConfigData.getInstance().isItemsOnlyAvailableWhenRunning()) {
                boolean dirty = false;
                final ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                final PlayerInventory inv = player.getInventory();
                for (int i = inv.size(); i >= 0; i--) {
                    final ItemStack stack = inv.getStack(i);
                    // Item should be discarded either:
                    // i. Running {abc}, while something is {def};
                    // ii. Not running, while something is {abc}.
                    if (FireworkElytraUtils.stampsRecord(stack, serverPlayer.alphabetSpeedrun$getItemRecordAccess()))
                        return;
                    inv.removeStack(i);
                    dirty = true;
                }
                if (dirty)
                    inv.markDirty();
            }
        });

        // Register StopOnQuit events
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (AlphabetSpeedrunConfigData.getInstance().isStopOnQuit()) {
                if (player.alphabetSpeedrun$getItemRecordAccess() != null) {
                    MutableBoolean hasError = new MutableBoolean();
                    ItemSpeedrunCommandHandle.quit(err -> hasError.setTrue(), player, false);
                    if (hasError.isFalse()) {
                        LOGGER.info("Speedrun Lifecycle: stopping record for {} on quit", player.getGameProfile().getName());
                    } else {
                        LOGGER.warn("Failed to stop record for {} on quit", player.getGameProfile().getName());
                    }
                }
            }
        });

        // Register TimerPausesWhenVacant events
        STOP_RUNNING_EVENT_PRE.register((player, record) -> {
            if (!record.isCoop() && AlphabetSpeedrunConfigData.getInstance().isTimerPausesWhenVacant()) {
                record.setLastQuitTime(player.server.getOverworld().getTime());
            }
        });

        PlayerEvent.PLAYER_ADVANCEMENT.register((player, advancement) -> {
            ItemRecordAccess rec = player.alphabetSpeedrun$getItemRecordAccess();
            if (rec != null) {
                List<SingleSpeedrunPredicate> predicates = rec.predicates();
                for (int i = 0; i < predicates.size(); i++) {
                    SingleSpeedrunPredicate predicate = predicates.get(i);
                    if (predicate.fitsAdvancementGet(advancement)) {
                        if (!COLLECTED_ONE_EVENT.invoker().onCollect(Either.right(advancement), predicate.getIcon(), player, rec).isFalse()) {
                            long time = player.server.getOverworld().getTime();
                            PlayerManager playerManager = player.server.getPlayerManager();
                            setAndAnnounceCollectedOne(player, rec, predicate.getIcon(), null, i, time, playerManager);
                        }
                    }
                }
            }
        });

        TickEvent.SERVER_PRE.register(server -> {
            DraftManager.get().tick();
            MultiplayerRecords.tickInvitations();
        });

        COLLECTED_ONE_EVENT.register((obj, icon, player, record) -> COLLECTED_ITEM_EVENT.invoker().onCollect(obj, player, record));

        FINISH_RECORD_EVENT.register((player, record, gameTime) -> {
            // TODO change broadcast to partial (players not involved will not receive broadcasts)
            var mgr = player.server.getPlayerManager();
            mgr.broadcast(ItemRecordMessages.itemCompleted(player, record, gameTime), false);
            if (!record.isCoop()) {
                player.alphabetSpeedrun$moveRecordToHistory();
                ItemRecordMessages.sendWinSound(player, mgr);
            }
            else {
                Collection<? extends ServerPlayerEntity> players;
                players = record.asCoop().getPlayers().stream()
                        .map(mgr::getPlayer)
                        .filter(Objects::nonNull)
                        .toList();
                players.forEach(p -> {
                    ItemRecordMessages.sendWinSound(p, mgr);
                    p.alphabetSpeedrun$setItemRecordAccess(null);
                });
            }
        });

        try {
            Class.forName(org.objectweb.asm.Type.getObjectType("org/featurehouse/mcmod/speedrun/alphabeta/item/ItemSpeedrunEvents").getClassName());
        } catch (ClassNotFoundException e) {
            if (Platform.getEnvironment() == Env.SERVER) {
                throw new UnsupportedOperationException("ABS Demo Mod doesn't support dedicated server.\n" +
                        "Get the full version by mailing to featurehouse@outlook.com");
            }
            for (String modId : Arrays.asList("minihud", "xaeros-minimap", "xaerosminimap")) {
                if (Platform.isModLoaded(modId)) {
                    throw new UnsupportedOperationException("ABS Demo Mod is not compatible with various mods.\n" +
                            "Get the full version by mailing to featurehouse@outlook.com");
                }
            }

            ClientGuiEvent.RENDER_HUD.register((drawContext, tickDelta) ->
                    drawContext.drawText(MinecraftClient.getInstance().textRenderer, Text.translatable("demo.speedrun.alphabet"), 10, 10, 0xfffff, true));
            TickEvent.SERVER_POST.register(server -> {
                var t = server.getOverworld().getTime();
                if (t >= 5400) {
                    if (t > 6000) {
                        GlfwUtil.makeJvmCrash();
                    } else if (t % 20 == 0) {
                        MinecraftClient.getInstance().getMessageHandler().onGameMessage(Text.translatable("demo.speedrun.alphabet.cd", (6000 - t) / 20), false);
                    }
                }
            });
            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
                if (!MinecraftClient.getInstance().isConnectedToLocalServer()) {
                    player.networkHandler.getConnection().disconnect(Text.translatable("demo.speedrun.alphabet.mp"));
                }
            });
        }
    }

    public static void onItemPickup(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return;
        final MinecraftServer server = player.server;
        final ItemRecordAccess record = player.alphabetSpeedrun$getItemRecordAccess();
        if (record != null) {
            //final Identifier id = Registry.ITEM.getId(stack.getItem());
            if (FireworkElytraUtils.bypassesItemCheck(stack)) return;
            //List<ItemPredicate> requirements = record.requirements();
            List<SingleSpeedrunPredicate> predicates = record.predicates();
            for (int i = 0; i < predicates.size(); i++) {
                if (record.isRequirementPassed(i)) continue;
                SingleSpeedrunPredicate requirement = predicates.get(i);
                if (requirement.testItemStack(stack)) {
                    if (!COLLECTED_ONE_EVENT.invoker().onCollect(Either.left(stack), requirement.getIcon(), player, record).isFalse()) {
                        final long time = server.getOverworld().getTime();
                        final PlayerManager mgr = server.getPlayerManager();
                        setAndAnnounceCollectedOne(player, record, requirement.getIcon(), stack, i, time, mgr);
                        tryFinishRecord(record, time, player);
                    }
                }
            }
        }
    }

    static void setAndAnnounceCollectedOne(ServerPlayerEntity player, ItemRecordAccess record,
                                                  ItemStack displayedStack,
                                                  @Nullable ItemStack actualStack,
                                                  int index,
                                                  long time, PlayerManager mgr) {
        record.setRequirementPassedTime(index, time);
        mgr.broadcast(ItemRecordMessages.itemCollected(player, displayedStack, record, time, actualStack), false);
        ItemRecordMessages.sendSound(mgr, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    static void tryFinishRecord(ItemRecordAccess record, long time, ServerPlayerEntity player) {
        if (record.tryMarkDone(time)) {
            FINISH_RECORD_EVENT.invoker().onRecordFinish(player, record, time);
        }
    }

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<ScreenHandlerType<?>> MENU_REG = DeferredRegister.create("alphabet_speedrun", MultiverseHooks.menuKey());
    public static final RegistrySupplier<ScreenHandlerType<ItemListViewMenu>> MENU_TYPE_R = MENU_REG.register("item_list", () ->
            MenuRegistry.ofExtended((id, $, buf) -> new ItemListViewMenu(id, buf.readVarInt(), buf.readUuid(),
                    size -> IntStream.range(0, size).mapToObj($$ -> buf.readItemStack()).collect(Collectors.toList()))));

    @FunctionalInterface
    @Deprecated
    public interface CollectedItem {
        EventResult onCollect(Either<ItemStack, Advancement> obj, ServerPlayerEntity player, ItemRecordAccess record);
    }

    @Deprecated
    @SuppressWarnings("all")
    public static final Event<CollectedItem> COLLECTED_ITEM_EVENT = EventFactory.createEventResult();

}
