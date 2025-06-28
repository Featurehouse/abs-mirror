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

package org.featurehouse.mcmod.speedrun.alphabeta.item.components;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.featurehouse.mcmod.speedrun.alphabeta.config.AlphabetSpeedrunConfigData;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemRecordAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FireworkElytraUtils {
    @Deprecated public static final String NO_SHRINKING = "AlphabetSpeedrunNoFireworkShrinking";    // type=unit
    @Deprecated public static final String BYPASSES_ITEM_CHECK = "AlphabetSpeedrunItemBypasses";    // type=unit
    @Deprecated public static final String RECORD_STAMP = "AphabetSpeedrunItemRecordStamp";         // type=string

    public static void handleFireworkDecrement(ItemStack stack, int dec) {
        if (stack.get(ABSItemDataComponents.NO_SHRINKING.get()) != null)
            stack.shrink(dec);
    }

    public static ItemStack mapBypassing(ItemStack stack) {
        if (stack.is(Items.FIREWORK_ROCKET)) {
            final int stackCount = stack.getCount();
            stack = Items.FIREWORK_ROCKET.getDefaultInstance();
            stack.setCount(stackCount);
        } else {
            stack = stack.copy();
        }
        stack.set(ABSItemDataComponents.BYPASSES_ITEM_CHECK.get(), Unit.INSTANCE);
        return stack;
    }

    public static ItemStack mapInfinite(ItemStack stack) {
        stack = stack.copy();
        if (stack.is(Items.ELYTRA)) {
            stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        } else if (stack.is(Items.FIREWORK_ROCKET)) {
            stack.set(ABSItemDataComponents.NO_SHRINKING.get(), Unit.INSTANCE);
        }
        return stack;
    }

    @Deprecated
    public static ItemStack newInfElytra() {
        final ItemStack stack = new ItemStack(Items.ELYTRA, 1);
        stack.set(ABSItemDataComponents.BYPASSES_ITEM_CHECK.get(), Unit.INSTANCE);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        return stack;
    }

    @Deprecated
    public static ItemStack newInfFireworkStack() {
        final ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET, 64);
        stack.set(ABSItemDataComponents.NO_SHRINKING.get(), Unit.INSTANCE);
        stack.set(ABSItemDataComponents.BYPASSES_ITEM_CHECK.get(), Unit.INSTANCE);
        return stack;
    }

    public static boolean bypassesItemCheck(ItemStack stack) {
        return stack.get(ABSItemDataComponents.BYPASSES_ITEM_CHECK.get()) != null;
    }

    /**
     * <p>Returns if the record stamp on the stack matches the record.</p>
     * <p>This decides, for example, whether the stack should be removed when inappropriate.</p>
     *
     * <p>If the record is null, then returning whether the {@link ABSItemDataComponents#RECORD_STAMP}
     * field is absent.</p>
     * <p>If the record isn't null, then the stamp must be either absent or
     * matched.</p>
     *
     * @see AlphabetSpeedrunConfigData#isItemsOnlyAvailableWhenRunning()
     */
    public static boolean stampsRecord(ItemStack stack, @Nullable ItemRecordAccess record) {
        if (stack.isEmpty()) return false;
        UUID stamp = stack.get(ABSItemDataComponents.RECORD_STAMP.get());
        if (stamp == null) {
            // if the stack isn't stamped, then a true should be returned.
            return true;
        }
        if (record != null) {
            return stamp.equals(record.recordId());
        }
        // stamp != null && record == null
        // The player is not running a record, whereas there is a stamp on the stack
        return false;
    }

    public static void putRecordStamp(ItemStack stack, @NotNull ItemRecordAccess record) {
        stack.set(ABSItemDataComponents.RECORD_STAMP.get(), record.recordId());
    }
}
