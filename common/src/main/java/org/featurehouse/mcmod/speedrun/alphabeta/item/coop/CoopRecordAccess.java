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

package org.featurehouse.mcmod.speedrun.alphabeta.item.coop;

import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemRecordAccess;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public interface CoopRecordAccess extends ItemRecordAccess {
    List<UUID> getPlayers();
    List<UUID> getOperators();

    default boolean isOp(ServerPlayer player) {
        return getOperators().contains(player.getUUID());
    }

    @Override
    default CoopRecordAccess asCoop() throws IllegalStateException {
        return this;
    }

    @Override
    default boolean isCoop() {
        return true;
    }
}
