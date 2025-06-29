package org.featurehouse.mcmod.speedrun.alphabeta.util.pack.neoforge;

import dev.architectury.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.featurehouse.mcmod.speedrun.alphabeta.util.pack.BuiltinABSPackProvider;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
@EventBusSubscriber
public class BuiltinABSPackProviderImpl {
    public static Path readMeta() {
        return Platform.getMod("alphabet_speedrun").findResource("abs_builtin_packs.json").orElse(null);
    }

    public static void registerPacks() {}

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        for (String s : BuiltinABSPackProvider.SUB_PATHS.get()) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath("alphabet_speedrun", "resourcepacks/" + s),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("[ABS] " + s),
                    PackSource.BUILT_IN,
                    false,
                    Pack.Position.TOP
            );
        }
    }
}
