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

package org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty;

import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.components.ABSItemDataComponents;
import org.featurehouse.mcmod.speedrun.alphabeta.item.components.FireworkElytraUtils;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemRecordAccess;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum DefaultItemSpeedrunDifficulty implements ItemSpeedrunDifficulty {
    NN("empty", GivenItemState.NONE, GivenItemState.NONE),
    NC("firework", GivenItemState.NONE, GivenItemState.COMMON),
    NU("inf_firework", GivenItemState.NONE, GivenItemState.UNBREAKABLE),
    CN("elytra", GivenItemState.COMMON, GivenItemState.NONE),
    CC("elytra_firework", GivenItemState.COMMON, GivenItemState.COMMON),
    CU("elytra_inf_firework", GivenItemState.COMMON, GivenItemState.UNBREAKABLE),
    UN("inf_elytra", GivenItemState.UNBREAKABLE, GivenItemState.NONE),
    UC("inf_elytra_firework", GivenItemState.UNBREAKABLE, GivenItemState.COMMON),
    UU("inf_elytra_inf_firework", GivenItemState.UNBREAKABLE, GivenItemState.UNBREAKABLE)
    ;
    private final GivenItemState elytraState, fireworkState;
    private final ResourceLocation id;
    private final String translationKey;

    DefaultItemSpeedrunDifficulty(String rawId, GivenItemState elytraState, GivenItemState fireworkState) {
        this.elytraState = elytraState;
        this.fireworkState = fireworkState;
        id = ResourceLocation.fromNamespaceAndPath("speedabc", rawId);
        translationKey = "speedrun.alphabet.item.difficulty.speedabc." + rawId;
    }

    private static final Map<ResourceLocation, ItemSpeedrunDifficulty> ID_TO_OBJ =
            Arrays.stream(values()).collect(Collectors.toMap(DefaultItemSpeedrunDifficulty::getId, Function.identity()));

    @ApiStatus.Internal
    public static Map<ResourceLocation, ItemSpeedrunDifficulty> getIdToObjMap() { return Collections.unmodifiableMap(ID_TO_OBJ); }

    @NotNull
    public static ItemSpeedrunDifficulty getDifficulty(ResourceLocation id) { return ID_TO_OBJ.getOrDefault(id, NN); }

    @ApiStatus.Internal
    public static void registerDifficulty(ResourceLocation id, @NotNull ItemSpeedrunDifficulty difficulty) {
        ID_TO_OBJ.put(id, Objects.requireNonNull(difficulty));
    }

    @Override
    public void onStart(ServerPlayer player) {
        ItemStack stack1 = elytraState.createItemStack(Items.ELYTRA, 1);
        final ItemRecordAccess record = player.alphabetSpeedrun$getItemRecordAccess();
        // Detect if the player already has the Elytra that suits the difficulty
        if (stack1 != null && !player.getInventory().hasAnyMatching(itemStack -> {
            if (!itemStack.is(Items.ELYTRA)) return false;
            if (AlphabetSpeedrunConfigData.getInstance().isItemsOnlyAvailableWhenRunning() &&
                    !FireworkElytraUtils.stampsRecord(itemStack, record))
                return false;
            if (FireworkElytraUtils.bypassesItemCheck(itemStack)) {
                if (elytraState == GivenItemState.COMMON) return true;
                return itemStack.get(DataComponents.UNBREAKABLE) != null;
            }
            return false;
        })) {   // The player does not have the Elytra that suits the difficulty
            if (record != null)
                FireworkElytraUtils.putRecordStamp(stack1, record);
            if (!player.addItem(stack1)) {
                player.drop(stack1, true);
            }
        }
        stack1 = fireworkState.createItemStack(Items.FIREWORK_ROCKET, 64);
        // Detect if the player already has the Firework Rocket that suits the difficulty
        if (stack1 != null && !player.getInventory().hasAnyMatching(itemStack -> {
            if (!itemStack.is(Items.FIREWORK_ROCKET)) return false;
            if (AlphabetSpeedrunConfigData.getInstance().isItemsOnlyAvailableWhenRunning() &&
                    !FireworkElytraUtils.stampsRecord(itemStack, record))
                return false;
            if (FireworkElytraUtils.bypassesItemCheck(itemStack)) {
                if (elytraState == GivenItemState.COMMON) return true;
                return itemStack.get(ABSItemDataComponents.NO_SHRINKING.get()) != null;
            }
            return false;
        })) {   // // The player does not have the Elytra that suits the difficulty
            if (record != null)
                FireworkElytraUtils.putRecordStamp(stack1, record);
            if (!player.addItem(stack1)) {
                player.drop(stack1, true);
            }
        }
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public Component asText() {
        return Component.translatable(translationKey)
                .withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY))));
    }

    private enum GivenItemState {
        NONE((item, cnt) -> null),
        COMMON((item, cnt) -> FireworkElytraUtils.mapBypassing(new ItemStack(item, cnt))),
        UNBREAKABLE((item, cnt) -> FireworkElytraUtils.mapInfinite(FireworkElytraUtils.mapBypassing(new ItemStack(item, cnt)))),
        ;
        
        private final BiFunction<Item, Integer, @Nullable ItemStack> itemStackFunction;

        GivenItemState(BiFunction<Item, Integer, @Nullable ItemStack> itemStackFunction) {
            this.itemStackFunction = itemStackFunction;
        }
        
        @Nullable
        public ItemStack createItemStack(Item item, int count) {
            return itemStackFunction.apply(item, count);
        }
    }
}
