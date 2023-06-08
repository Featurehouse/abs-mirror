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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ConcurrentUtils {
    private static final AtomicInteger TID = new AtomicInteger();

    public static void run(CompletableFuture<Void> completableFuture, Consumer<Exception> onFailure) {
        new Thread(() -> {
            try {
                completableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                onFailure.accept(e);
            }
        }, "ABS-Concurrent-" + TID.getAndIncrement()).start();
    }
}
