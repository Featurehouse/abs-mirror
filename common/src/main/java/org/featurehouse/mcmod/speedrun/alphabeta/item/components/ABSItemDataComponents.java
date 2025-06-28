package org.featurehouse.mcmod.speedrun.alphabeta.item.components;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Unit;
import net.minecraft.util.Uuids;

import java.util.UUID;

public final class ABSItemDataComponents {
    private ABSItemDataComponents() {}

    public static final DeferredRegister<ComponentType<?>> REGISTRY = DeferredRegister.create("alphabet_speedrun", RegistryKeys.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<ComponentType<Unit>> NO_SHRINKING = REGISTRY.register("no_shrinking", ABSItemDataComponents::unit);
    public static final RegistrySupplier<ComponentType<Unit>> BYPASSES_ITEM_CHECK = REGISTRY.register("bypasses_item_check", ABSItemDataComponents::unit);
    public static final RegistrySupplier<ComponentType<UUID>> RECORD_STAMP = REGISTRY.register(
            "record_stamp",
            () -> ComponentType.<UUID>builder().codec(Uuids.INT_STREAM_CODEC).packetCodec(Uuids.PACKET_CODEC).build()
    );

    private static ComponentType<Unit> unit() {
        return ComponentType.<Unit>builder().codec(Unit.CODEC).packetCodec(Unit.PACKET_CODEC).build();
    }
}
