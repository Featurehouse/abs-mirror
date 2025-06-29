package org.featurehouse.mcmod.speedrun.alphabeta.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.featurehouse.mcmod.speedrun.alphabeta.AlphabetSpeedrunMod;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.neoforge.ItemSpeedrunDifficultyRegistryEvent;

import java.util.Collection;

@EventBusSubscriber
public class AlphabetSpeedrunModImpl {
    public static Collection<? extends ItemSpeedrunDifficulty> fireDifficultyRegistry() {
        var event = new ItemSpeedrunDifficultyRegistryEvent();
        AlphabetSpeedrunNeoForge.eventBus.post(event);
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
