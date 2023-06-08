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

package org.featurehouse.mcmod.speedrun.alphabeta.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.minecraft.obfuscate.DontObfuscate;
import org.featurehouse.mcmod.speedrun.alphabeta.AlphabetSpeedrunMod;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrunEvents;

@DontObfuscate
public class AlphabetSpeedrunFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemSpeedrunEvents.LOGGER.info("Initializing AlphabetSpeedrun Item Module (Fabric)");   // DO NOT REMOVE THIS LINE
        AlphabetSpeedrunMod.init();
        AlphabetSpeedrunMod.initConfig();
    }

    @Environment(EnvType.CLIENT)
    @DontObfuscate
    public static void initClient() {
        AlphabetSpeedrunMod.initClient();
    }
}
