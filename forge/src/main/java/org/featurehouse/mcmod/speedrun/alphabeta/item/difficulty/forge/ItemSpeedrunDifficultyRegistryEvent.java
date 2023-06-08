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

package org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.forge;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.apache.commons.compress.utils.Lists;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DifficultiesFactory;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This is the event to register {@link ItemSpeedrunDifficulty}.
 * @see DifficultiesFactory
 * @see ItemSpeedrunDifficulty
 */
@SuppressWarnings("unused")
public class ItemSpeedrunDifficultyRegistryEvent extends Event implements IModBusEvent {
    private final List<ItemSpeedrunDifficulty> fallback = Lists.newArrayList();

    public void register(ItemSpeedrunDifficulty difficulty) {
        fallback.add(difficulty);
    }

    public void register(Collection<? extends ItemSpeedrunDifficulty> c) {
        fallback.addAll(c);
    }

    public void register(DifficultiesFactory factory) {
        register(factory.registerDifficulties());
    }

    @ApiStatus.Internal
    public List<ItemSpeedrunDifficulty> fallbackView() {
        return Collections.unmodifiableList(fallback);
    }
}
