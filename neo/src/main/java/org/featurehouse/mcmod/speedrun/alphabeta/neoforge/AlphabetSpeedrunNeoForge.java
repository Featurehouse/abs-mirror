package org.featurehouse.mcmod.speedrun.alphabeta.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.featurehouse.mcmod.speedrun.alphabeta.AlphabetSpeedrunMod;
import org.featurehouse.mcmod.speedrun.alphabeta.item.ItemSpeedrunEvents;

@Mod("alphabet_speedrun")
public class AlphabetSpeedrunNeoForge {
    static IEventBus eventBus;

    public AlphabetSpeedrunNeoForge(IEventBus bus) {
        eventBus = bus;
        ItemSpeedrunEvents.LOGGER.info("Initializing AlphabetSpeedrun Item Module (NeoForge)"); // DO NOT REMOVE
        AlphabetSpeedrunMod.init();
    }
}
