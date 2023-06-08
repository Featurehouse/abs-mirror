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

import com.google.gson.JsonObject;
import net.minecraft.obfuscate.DontObfuscate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public interface InternalItemCollector extends ItemCollector {
    default ItemRecordAccess alphabetSpeedrun$getItemRecordAccess() {throw new AssertionError();}
    default void alphabetSpeedrun$setItemRecordAccess(@Nullable ItemRecordAccess record) {throw new AssertionError();}
    default boolean alphabetSpeedrun$moveRecordToHistory(){throw new AssertionError();}
    default boolean alphabetSpeedrun$resumeLocalHistory(){throw new AssertionError();}
    default void alphabetSpeedrun$clearItemHistory(){throw new AssertionError();}
    default ItemSpeedrunRecord alphabetSpeedrun$getHistory(){throw new AssertionError();}
    @ApiStatus.Internal
    @DontObfuscate
    default JsonObject alphabetSpeedrun$internal$getHistoryRaw() { throw new AssertionError(); }

    @Override
    @Deprecated
    default ItemSpeedrunRecord alphabetSpeedrun$getItemRecord() {
        return alphabetSpeedrun$getItemRecordAccess() instanceof ItemSpeedrunRecord r ? r : null;
    }

    @Override
    @Deprecated
    default void alphabetSpeedrun$setItemRecord(ItemSpeedrunRecord record) {
        alphabetSpeedrun$setItemRecordAccess(record);
    }
}
