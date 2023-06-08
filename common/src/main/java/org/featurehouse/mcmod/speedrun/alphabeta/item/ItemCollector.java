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

import org.featurehouse.mcmod.speedrun.alphabeta.util.MixinSensitive;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckForNull;

@ApiStatus.NonExtendable
public interface ItemCollector {
    @CheckForNull
    @MixinSensitive
    ItemRecordAccess alphabetSpeedrun$getItemRecordAccess();
    @MixinSensitive
    void alphabetSpeedrun$setItemRecordAccess(@Nullable ItemRecordAccess record);

    @CheckForNull
    @Deprecated
    @MixinSensitive
    ItemSpeedrunRecord alphabetSpeedrun$getItemRecord();
    @Deprecated
    @MixinSensitive
    void alphabetSpeedrun$setItemRecord(@Nullable ItemSpeedrunRecord record);

    @MixinSensitive
    boolean alphabetSpeedrun$moveRecordToHistory();
    @MixinSensitive
    boolean alphabetSpeedrun$resumeLocalHistory();
    @MixinSensitive
    void alphabetSpeedrun$clearItemHistory();

    @CheckForNull
    @MixinSensitive
    ItemSpeedrunRecord alphabetSpeedrun$getHistory();
}
