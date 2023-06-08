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

package org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty;

import java.util.Collection;

/**
 * <p>This is the {@linkplain FunctionalInterface} that enables {@link ItemSpeedrunDifficulty}
 * registration.</p>
 * <p>For Fabric/Quilt: this is the entrypoint class of entrypoint
 * {@link #FABRIC_ENTRYPOINT_NAME "alphabet-speedrun:item-run-difficulty-registry"}. You can
 * implement it with either a class, a Kotlin object or a static method without arguments that
 * returns the {@link Collection}.</p>
 * <p>For Forge: This can be registered to the corresponding event (if you want to reuse the
 * lambda / method), or you can simply register the collection itself to the event, which are
 * equal in result.
 * </p>
 */
@FunctionalInterface
public interface DifficultiesFactory {
    String FABRIC_ENTRYPOINT_NAME = "alphabet-speedrun:item-run-difficulty-registry";

    Collection<? extends ItemSpeedrunDifficulty> registerDifficulties();
}
