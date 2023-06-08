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

package org.featurehouse.mcmod.speedrun.alphabeta.util;

import org.slf4j.LoggerFactory;

import java.io.IOException;

public interface SavableResource {
    void load() throws IOException;

    void save() throws IOException;

    default void safeLoad() {
        try {
            load();
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Failed to load", e);
        }
    }

    default void safeSave() {
        try {
            save();
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Failed to save", e);
        }
    }
}
