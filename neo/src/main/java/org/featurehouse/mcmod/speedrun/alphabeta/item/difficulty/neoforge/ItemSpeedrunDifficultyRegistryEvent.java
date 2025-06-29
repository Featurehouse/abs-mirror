package org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.neoforge;

import com.google.common.collect.Lists;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.DifficultiesFactory;
import org.featurehouse.mcmod.speedrun.alphabeta.item.difficulty.ItemSpeedrunDifficulty;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
