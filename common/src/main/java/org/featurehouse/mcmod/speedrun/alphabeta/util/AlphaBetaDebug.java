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

import com.google.common.base.Suppliers;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

@ApiStatus.Internal
public class AlphaBetaDebug {
    private AlphaBetaDebug() {}
    private static final Supplier<Logger> LOG = Suppliers.memoize(() -> LoggerFactory.getLogger(AlphaBetaDebug.class));
    private static final int debugEnabled = Integer.getInteger("speedrun.alphabet.debugK", 0);

    public static void log(int i, Consumer<Logger> consumer) {
        if (debugEnabled == 0)
            return;
        if (((1 << i) & debugEnabled) != 0)
            consumer.accept(LOG.get());
    }
}
