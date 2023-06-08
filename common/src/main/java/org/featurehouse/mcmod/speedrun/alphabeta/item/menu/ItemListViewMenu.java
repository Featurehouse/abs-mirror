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

package org.featurehouse.mcmod.speedrun.alphabeta.item.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrunEvents;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

public class ItemListViewMenu extends ScreenHandler {
    private final UUID uuid;
    private final List<ItemStack> iconList;
    private final Inventory fakeInv;
    @VisibleForTesting //private
    final ItemListMenuSync sync;
    private int page;   // offset = page * 63
    private final boolean isServer;

    @Deprecated(forRemoval = true) boolean isServer() { return isServer; }

    public ItemListViewMenu(int syncId, List<ItemStack> iconList, boolean isRemote,
                            ItemListMenuSync sync, UUID uuid) {
        super(ItemSpeedrunEvents.MENU_TYPE_R.get(), syncId);
        this.isServer = isRemote;
        this.uuid = uuid;
        this.sync = sync;
        this.addProperties(sync);
        this.iconList = iconList;
        fakeInv = new SimpleInventory(63);
        for (int i = 0; i < 63; i++)
            this.addSlot(new ReadOnlySlot(fakeInv, i, 8 + 18 * (i % 9), 19 + 18 * (i / 9)));
        if (isServer) this.setSlots();
        this.addProperty(new Property() {
            @Override
            public int get() {
                return getPage();
            }

            @Override
            public void set(int value) {
                setPage(value);
            }
        });
    }

    @Deprecated
    public UUID getUuid() {
        return uuid;
    }

    public int getPage() {
        return page;
    }

    // @param idx: the index in the page, not in the inventory!
    public @Nullable Boolean isSlotCompleted(int idx) {
        int realIndex = idx + getPage() * 63;
        if (realIndex >= sync.getListSize()) return null;
        return sync.getBit(realIndex);
    }

    private void setSlots() {
        final int mx = slotCountInPage();
        for (int i = 62; i >= mx; i--)
            this.fakeInv.setStack(i, ItemStack.EMPTY);
        for (int i = 0; i < mx; i++)
            this.fakeInv.setStack(i, iconList.get(i + page * 63));
        this.sendContentUpdates();
    }

    protected int pageCount() {
        return MathHelper.ceilDiv(iconList.size(), 63);
    }

    private int slotCountInPage() {
        return Math.min(iconList.size() - page * 63, 63);
    }

    public void setPageServer(int page) {
        if (page != this.page) {
            setPage(page);
            this.setSlots();
        }
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean hasNextPage() {
        return (getPage() + 1) * 63 <= iconList.size();
    }

    public boolean hasPrevPage() {
        return getPage() > 0;
    }

    public ItemListViewMenu(int syncId, int listSize, UUID uuid, IntFunction<List<ItemStack>> itemStacks) {
        this(syncId, itemStacks.apply(listSize), false, new ItemListMenuSync.ArrImpl(listSize), uuid);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // Transferring slots is disallowed
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id < 0 || id > pageCount()) return super.onButtonClick(player, id);
        setPageServer(id);
        return true;
    }

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
