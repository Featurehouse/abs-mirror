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

public abstract sealed class ItemListMenuSync implements net.minecraft.screen.PropertyDelegate {
    protected final int listSize;

    protected ItemListMenuSync(int listSize) {
        this.listSize = listSize;
    }

    @Override
    public int size() {
        return arrSize(listSize);
    }

    protected static int arrSize(int s) {
        return (s & 0b111) == 0 ? (s >> 3) : (s >> 3) + 1;
    }

    public boolean getBit(int idx) {
        return ((this.get(idx >> 3) >> (idx & 0b111)) & 1) != 0;
    }

    @SuppressWarnings("unused")
    public void setBit(int idx, boolean bit) {
        int bi;
        int val = this.get(bi = idx >> 3);
        if (bit) set(bi, val | (1 << (idx & 0b111)));
        else set(bi, val & ~(1 << (idx & 0b111)));
    }

    @Override
    public abstract int get(int index);

    @Override
    public abstract void set(int index, int value);

    public int getListSize() {
        return listSize;
    }

    public static final class ArrImpl extends ItemListMenuSync {
        private final int[] arr;

        public ArrImpl(int listSize) {
            super(listSize);
            this.arr = new int[arrSize(listSize)];
        }

        @Override
        public int get(int index) {
            return arr[index];
        }

        @Override
        public void set(int index, int value) {
            arr[index] = value;
        }
    }

    public static abstract non-sealed class BitImpl extends ItemListMenuSync {
        protected BitImpl(int listSize) {
            super(listSize);
        }

        @Override
        public int get(int index) {
            int bi = index << 3, r = 0;
            int sz = Math.min(this.getListSize() - bi, 8);
            for (int i = 0; i < sz; i++)
                if (this.getBit(bi + i)) r |= (1 << i);
            return r;
        }

        @Override
        public void set(int index, int value) {
            int bi = index << 3;
            boolean t;
            int sz = Math.min(this.getListSize() - bi, 8);
            for (int i = 0; i < sz; i++)
                if (this.getBit(bi + i) != (t = ((value >> i) & 1) != 0))
                    setBit(bi + i, t);
        }

        @Override
        public abstract boolean getBit(int idx);

        @Override
        public abstract void setBit(int idx, boolean bit);
    }
}
