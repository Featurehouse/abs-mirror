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
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.DraftManager;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.ItemSpeedrunCommandHandle;
import org.featurehouse.mcmod.speedrun.alphabeta.item.command.ItemSpeedrunCommands;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DefaultItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.ItemListViewMenu;
import org.featurehouse.mcmod.speedrun.alphabeta.item.components.FireworkElytraUtils;
import org.featurehouse.mcmod.speedrun.alphabeta.item.menu.OpenItemListPayload;
import org.featurehouse.mcmod.speedrun.alphabeta.util.PacketUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ItemSpeedrunEvents {
    @FunctionalInterface
    public interface CollectedOne {
        EventResult onCollect(Either<ItemStack, AdvancementHolder> obj, ItemStack icon, ServerPlayer player, ItemRecordAccess record);
    }

    public static final Event<CollectedOne> COLLECTED_ONE_EVENT = EventFactory.createEventResult();

    @FunctionalInterface
    public interface StartRunning {
        byte START_COOP = -101;
        byte JOIN_COOP = -102;
        byte START = -1, FROM_LOCAL = 0, FROM_DISK = 1;
        void onStartRunning(ServerPlayer player, ItemRecordAccess record, byte resumeFrom);
    }
    public static final Event<StartRunning> START_RUNNING_EVENT = EventFactory.createLoop();

    @FunctionalInterface
    public interface StopRunning {
        void onStopRunning(ServerPlayer player, ItemRecordAccess record);
    }
    public static final Event<StopRunning> STOP_RUNNING_EVENT_PRE = EventFactory.createLoop();

    @FunctionalInterface
    public interface FinishRecord {
        void onRecordFinish(ServerPlayer player, ItemRecordAccess record, long gameTime);
    }

    public static final Event<FinishRecord> FINISH_RECORD_EVENT = EventFactory.createLoop();

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> ItemSpeedrunCommands.registerCommands(dispatcher));
        //PlayerEvent.PICKUP_ITEM_POST.register((player, itemEntity, stack) -> onItemPickup(player, stack));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new ItemSpeedrun.DataLoader(), ResourceLocation.fromNamespaceAndPath("alphabet_speedrun", "goals"));
        START_RUNNING_EVENT.register((player, record, resumeFrom) -> {
            ItemSpeedrunCommandHandle.tryResumeInventory(player);
            final long currentTime = player.getServer().overworld().getGameTime();
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
        NetworkManager.registerReceiver(NetworkManager.c2s(), OpenItemListPayload.ID, OpenItemListPayload.PACKET_CODEC, (payload, context) -> context.queue(() ->
                ItemSpeedrunCommandHandle.viewCurrentRecord(
                        text -> context.getPlayer().displayClientMessage(text.copy().withStyle(ChatFormatting.RED), false),
                        ((ServerPlayer) context.getPlayer())
                )));

        // Register ItemOnlyAvailableWhenRunning events
        TickEvent.PLAYER_POST.register(player -> {
            if (player.level().isClientSide()) return;
            if (AlphabetSpeedrunConfigData.getInstance().isItemsOnlyAvailableWhenRunning()) {
                boolean dirty = false;
                final ServerPlayer serverPlayer = (ServerPlayer) player;
                final Inventory inv = player.getInventory();
                for (int i = inv.getContainerSize(); i >= 0; i--) {
                    final ItemStack stack = inv.getItem(i);
                    // Item should be discarded either:
                    // i. Running {abc}, while something is {def};
                    // ii. Not running, while something is {abc}.
                    if (FireworkElytraUtils.stampsRecord(stack, serverPlayer.alphabetSpeedrun$getItemRecordAccess()))
                        return;
                    inv.removeItemNoUpdate(i);
                    dirty = true;
                }
                if (dirty)
                    inv.setChanged();
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
                record.setLastQuitTime(player.getServer().overworld().getGameTime());
            }
        });

        PlayerEvent.PLAYER_ADVANCEMENT.register((player, advancement) -> {
            ItemRecordAccess rec = player.alphabetSpeedrun$getItemRecordAccess();
            if (rec != null) {
                List<SingleSpeedrunPredicate> predicates = rec.predicates();
                for (int i = 0; i < predicates.size(); i++) {
                    SingleSpeedrunPredicate predicate = predicates.get(i);
                    if (predicate.fitsAdvancementGet(advancement)) {
                        if (!COLLECTED_ONE_EVENT.invoker().onCollect(Either.right(advancement), predicate.icon(), player, rec).isFalse()) {
                            long time = player.getServer().overworld().getGameTime();
                            PlayerList playerManager = player.getServer().getPlayerList();
                            setAndAnnounceCollectedOne(player, rec, predicate.icon(), null, i, time, playerManager);
                        }
                    }
                }
            }
        });

        TickEvent.SERVER_PRE.register(server -> {
            DraftManager.get().tick();
            MultiplayerRecords.tickInvitations();
        });

        FINISH_RECORD_EVENT.register((player, record, gameTime) -> {
            // TODO change broadcast to partial (players not involved will not receive broadcasts)
            var mgr = player.getServer().getPlayerList();
            mgr.broadcastSystemMessage(ItemRecordMessages.itemCompleted(player, record, gameTime), false);
            if (!record.isCoop()) {
                player.alphabetSpeedrun$moveRecordToHistory();
                ItemRecordMessages.sendWinSound(player, mgr);
            }
            else {
                Collection<? extends ServerPlayer> players;
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
    }

    public static void onItemPickup(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        final MinecraftServer server = player.getServer();
        Objects.requireNonNull(server);
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
                    if (!COLLECTED_ONE_EVENT.invoker().onCollect(Either.left(stack), requirement.icon(), player, record).isFalse()) {
                        final long time = server.overworld().getGameTime();
                        final PlayerList mgr = server.getPlayerList();
                        setAndAnnounceCollectedOne(player, record, requirement.icon(), stack, i, time, mgr);
                        tryFinishRecord(record, time, player);
                    }
                }
            }
        }
    }

    static void setAndAnnounceCollectedOne(ServerPlayer player, ItemRecordAccess record,
                                                  ItemStack displayedStack,
                                                  @Nullable ItemStack actualStack,
                                                  int index,
                                                  long time, PlayerList mgr) {
        record.setRequirementPassedTime(index, time);
        mgr.broadcastSystemMessage(ItemRecordMessages.itemCollected(player, displayedStack, record, time, actualStack), false);
        ItemRecordMessages.sendSound(mgr, SoundEvents.EXPERIENCE_ORB_PICKUP);
    }

    static void tryFinishRecord(ItemRecordAccess record, long time, ServerPlayer player) {
        if (record.tryMarkDone(time)) {
            FINISH_RECORD_EVENT.invoker().onRecordFinish(player, record, time);
        }
    }

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENU_REG = DeferredRegister.create("alphabet_speedrun", Registries.MENU);
    public static final RegistrySupplier<MenuType<ItemListViewMenu>> MENU_TYPE_R = MENU_REG.register(
            "item_list",
            () -> MenuRegistry.ofExtended(
                    (id, $, buf) -> new ItemListViewMenu(
                            id, buf.readVarInt(), buf.readUUID(),
                            size -> IntStream.range(0, size).mapToObj($$ -> PacketUtil.readItemStack(buf)).collect(Collectors.toList()))
            )
    );

}
