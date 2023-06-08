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
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

public final class InventoryListener implements ScreenHandlerListener {
    private final ServerPlayerEntity serverPlayer;

    public InventoryListener(ServerPlayerEntity serverPlayer) {
        this.serverPlayer = serverPlayer;
    }

    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        Slot slot = handler.getSlot(slotId);
        if (slot.inventory == serverPlayer.getInventory()) {
            ItemSpeedrunEvents.onItemPickup(serverPlayer, stack);
        }
    }

    @Override
    public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
}
