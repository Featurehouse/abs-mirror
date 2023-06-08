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

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.FireworkElytraUtils;
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
    private final Identifier id;
    private final String translationKey;

    DefaultItemSpeedrunDifficulty(String rawId, GivenItemState elytraState, GivenItemState fireworkState) {
        this.elytraState = elytraState;
        this.fireworkState = fireworkState;
        id = new Identifier("speedabc", rawId);
        translationKey = "speedrun.alphabet.item.difficulty.speedabc." + rawId;
    }

    private static final Map<Identifier, ItemSpeedrunDifficulty> ID_TO_OBJ =
            Arrays.stream(values()).collect(Collectors.toMap(DefaultItemSpeedrunDifficulty::getId, Function.identity()));

    @ApiStatus.Internal
    public static Map<Identifier, ItemSpeedrunDifficulty> getIdToObjMap() { return Collections.unmodifiableMap(ID_TO_OBJ); }

    @NotNull
    public static ItemSpeedrunDifficulty getDifficulty(Identifier id) { return ID_TO_OBJ.getOrDefault(id, NN); }

    @ApiStatus.Internal
    public static void registerDifficulty(Identifier id, @NotNull ItemSpeedrunDifficulty difficulty) {
        ID_TO_OBJ.put(id, Objects.requireNonNull(difficulty));
    }

    @Override
    public void onStart(ServerPlayerEntity player) {
        ItemStack stack1 = elytraState.createItemStack(Items.ELYTRA, 1);
        final ItemRecordAccess record = player.alphabetSpeedrun$getItemRecordAccess();
        if (stack1 != null && !player.getInventory().containsAny(itemStack -> {
            if (!itemStack.isOf(Items.ELYTRA)) return false;
            if (AlphabetSpeedrunConfigData.getInstance().isItemsOnlyAvailableWhenRunning() &&
                    !FireworkElytraUtils.stampsRecord(itemStack, record))
                return false;
            if (FireworkElytraUtils.bypassesItemCheck(itemStack)) {
                if (elytraState == GivenItemState.COMMON) return true;
                NbtCompound nbt = itemStack.getNbt();
                return nbt != null && nbt.getBoolean("Unbreakable");
            }
            return false;
        })) {
            if (record != null)
                FireworkElytraUtils.putRecordStamp(stack1, record);
            if (!player.giveItemStack(stack1)) {
                player.dropItem(stack1, true);
            }
        }
        stack1 = fireworkState.createItemStack(Items.FIREWORK_ROCKET, 64);
        if (stack1 != null && !player.getInventory().containsAny(itemStack -> {
            if (!itemStack.isOf(Items.FIREWORK_ROCKET)) return false;
            if (AlphabetSpeedrunConfigData.getInstance().isItemsOnlyAvailableWhenRunning() &&
                    !FireworkElytraUtils.stampsRecord(itemStack, record))
                return false;
            if (FireworkElytraUtils.bypassesItemCheck(itemStack)) {
                if (elytraState == GivenItemState.COMMON) return true;
                NbtCompound nbt = itemStack.getNbt();
                return nbt != null && nbt.getBoolean(FireworkElytraUtils.NO_SHRINKING);
            }
            return false;
        })) {
            if (record != null)
                FireworkElytraUtils.putRecordStamp(stack1, record);
            if (!player.giveItemStack(stack1)) {
                player.dropItem(stack1, true);
            }
        }
    }

    public Identifier getId() {
        return id;
    }

    @Override
    public Text asText() {
        return Text.translatable(translationKey)
                .styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal(id.toString()).formatted(Formatting.GRAY))));
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
