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

package org.featurehouse.mcmod.speedrun.alphabeta.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.featurehouse.mcmod.speedrun.alphabeta.AlphabetSpeedrunMod;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrunEvents;

@Mod("alphabet_speedrun")
public class AlphabetSpeedrunForge {
    public AlphabetSpeedrunForge() {
        EventBuses.registerModEventBus("alphabet_speedrun", FMLJavaModLoadingContext.get().getModEventBus());
        ItemSpeedrunEvents.LOGGER.info("Initializing AlphabetSpeedrun Item Module (Forge)");   // DO NOT REMOVE THIS LINE
        AlphabetSpeedrunMod.init();
    }
}
