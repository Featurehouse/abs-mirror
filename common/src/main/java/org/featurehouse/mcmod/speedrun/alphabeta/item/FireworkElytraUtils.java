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

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FireworkElytraUtils {
    public static final String NO_SHRINKING = "AlphabetSpeedrunNoFireworkShrinking";
    public static final String BYPASSES_ITEM_CHECK = "AlphabetSpeedrunItemBypasses";
    public static final String RECORD_STAMP = "AphabetSpeedrunItemRecordStamp";

    public static void handleFireworkDecrement(ItemStack stack, int dec) {
        final NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.getBoolean(NO_SHRINKING)) {
            stack.decrement(dec);
        }
    }

    @Deprecated
    public static ItemStack newInfFireworkStack() {
        final ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET, 64);
        final NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(NO_SHRINKING, true);
        nbt.putBoolean(BYPASSES_ITEM_CHECK, true);
        return stack;
    }

    public static ItemStack mapBypassing(ItemStack stack) {
        if (stack.isOf(Items.FIREWORK_ROCKET)) {
            ItemStack s = stack;
            stack = Items.FIREWORK_ROCKET.getDefaultStack();
            stack.setCount(s.getCount());
        }  else stack = stack.copy();
        stack.getOrCreateNbt().putBoolean(BYPASSES_ITEM_CHECK, true);
        return stack;
    }

    public static ItemStack mapInfinite(ItemStack stack) {
        stack = stack.copy();
        if (stack.isOf(Items.ELYTRA))
            stack.getOrCreateNbt().putBoolean("Unbreakable", true);
        else if (stack.isOf(Items.FIREWORK_ROCKET))
            stack.getOrCreateNbt().putBoolean(NO_SHRINKING, true);
        return stack;
    }

    @Deprecated
    public static ItemStack newInfElytra() {
        final ItemStack stack = new ItemStack(Items.ELYTRA, 1);
        final NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(BYPASSES_ITEM_CHECK, true);
        nbt.putBoolean("Unbreakable", true);
        return stack;
    }

    public static boolean bypassesItemCheck(ItemStack stack) {
        final NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(BYPASSES_ITEM_CHECK);
    }

    /**
     * <p>Returns if the record stamp on the stack matches the record.</p>
     *
     * <p>If the record is null, then returning whether the {@link #RECORD_STAMP}
     * field is absent.</p>
     * <p>If the record isn't null, then the stamp must be either absent or
     * matched.</p>
     *
     * @see AlphabetSpeedrunConfigData#isItemsOnlyAvailableWhenRunning()
     */
    public static boolean stampsRecord(ItemStack stack, @Nullable ItemRecordAccess record) {
        if (stack.isEmpty()) return false;
        final NbtCompound nbt = stack.getNbt();
        // if the stack isn't stamped, then a true should be returned.
        if (nbt == null || !nbt.contains(RECORD_STAMP, NbtElement.STRING_TYPE)) {
            return true;
        }

        if (record != null) {
            return Objects.equals(nbt.getString(RECORD_STAMP), record.recordId().toString());
        }

        return false;
    }

    public static void putRecordStamp(ItemStack stack, @NotNull ItemRecordAccess record) {
        stack.getOrCreateNbt().putString(RECORD_STAMP, record.recordId().toString());
    }
}
