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

package org.featurehouse.mcmod.speedrun.alphabeta.config;

import net.minecraft.util.Identifier;
import org.featurehouse.mcmod.speedrun.alphabeta.AlphabetSpeedrunMod;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.util.qj5.JsonWriter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public interface ItemRunDifficultyRuleFactory {
    Collection<ItemSpeedrunDifficulty> findDifficultDifficulties(Map<Identifier, ItemSpeedrunDifficulty> map);

    List<Identifier> asIdList();
    boolean isInverted();

    class Impl implements ItemRunDifficultyRuleFactory {
        private final Collection<Identifier> ids;
        private final boolean inverted;

        /**
         * @param ids matching ids. If null it means ALL, or technically, invert {@code inverted}.
         */
        Impl(@Nullable Collection<Identifier> ids, boolean inverted) {
            if (ids != null) {
                this.ids = ids;
                this.inverted = inverted;
            } else {
                this.ids = Collections.emptyList();
                this.inverted = !inverted;
            }
        }

        @Override
        public List<Identifier> asIdList() {
            return ids instanceof List ? (List<Identifier>) ids : ids.stream().sorted().toList();
        }

        @Override
        public boolean isInverted() {
            return inverted;
        }

        @Override
        public Collection<ItemSpeedrunDifficulty> findDifficultDifficulties(Map<Identifier, ItemSpeedrunDifficulty> map) {
            if (referringToAll(this)) return map.values();
            if (!isInverted()) {
                if (ids.isEmpty()) return Collections.emptySet();
                return ids.stream().map(id -> Optional.ofNullable(map.get(id))
                        .orElseThrow(() -> new IllegalArgumentException("Difficulty " + id + " not found")))
                        .collect(Collectors.toSet());
            } else {
                return map.keySet().stream().filter(id -> !ids.contains(id)).map(map::get).collect(Collectors.toSet());
            }
        }

        /**
         * @param writer just begin its {@code object}, {@code array}, {@code null} or something, not
         *               caring what the key is.
         */
        static void serializeList(ItemRunDifficultyRuleFactory factory, JsonWriter writer) throws IOException {
            List<Identifier> list = factory.asIdList();
            switch (list.size()) {
                case 0 -> writer.value("NONE");
                case 1 -> writer.value(list.get(0).toString());
                default -> {
                    writer.beginArray();
                    for (Identifier id : list)
                        writer.value(id.toString());
                    writer.endArray();
                }
            }
        }

        /**
         * Indicating whether the {@code factory} refers to {@code ALL}. If (and only if) a {@code factory} has an empty
         * ID list and is inverted, then it refers to {@code ALL}.
         */
        static boolean referringToAll(ItemRunDifficultyRuleFactory factory) {
            return factory.isInverted() && factory.asIdList().isEmpty();
        }

        private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        private static volatile boolean initialized;
        @ApiStatus.Internal
        public static void setInitialized() {
            if (!initialized) {
                synchronized (Impl.class) {
                    if (!initialized) {
                        if (STACK_WALKER.getCallerClass() == AlphabetSpeedrunMod.class) {
                            initialized = true;
                            AlphabetSpeedrunConfigData cfg = AlphabetSpeedrunConfigData.getInstance();
                            cfg.setDifficultiesWithOp(cfg.getDifficultiesWithOp());   // recalculate difficulties cache
                        } else {
                            throw new IllegalCallerException();
                        }
                    }
                }
            }
        }

        static boolean isInitialized() {
            if (!initialized) {
                synchronized (Impl.class) {
                    return initialized;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Impl that)) return false;
            return this.asIdList().equals(that.asIdList()) && this.isInverted() == that.isInverted();
        }

        @Override
        public int hashCode() {
            return asIdList().hashCode() << 1 + (isInverted() ? 1 : 0);
        }
    }
}
