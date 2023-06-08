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

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.featurehouse.mcmod.speedrun.alphabeta.AlphabetSpeedrunMod;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.forge.ItemSpeedrunDifficultyRegistryEvent;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = "alphabet_speedrun", bus = Mod.EventBusSubscriber.Bus.MOD)
public class AlphabetSpeedrunModImpl {
    public static Collection<? extends ItemSpeedrunDifficulty> fireDifficultyRegistry() {
        ItemSpeedrunDifficultyRegistryEvent event = new ItemSpeedrunDifficultyRegistryEvent();
        FMLJavaModLoadingContext.get().getModEventBus().post(event);
        return event.fallbackView();
    }

    @SubscribeEvent
    public static void initClient(FMLClientSetupEvent event) {
        event.enqueueWork(AlphabetSpeedrunMod::initClient);
    }

    @SubscribeEvent
    public static void initCommon(FMLCommonSetupEvent event) {
        event.enqueueWork(AlphabetSpeedrunMod::initConfig);
    }
}
