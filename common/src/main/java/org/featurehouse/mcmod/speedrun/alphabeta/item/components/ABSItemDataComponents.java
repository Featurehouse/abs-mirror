package org.featurehouse.mcmod.speedrun.alphabeta.item.components;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Unit;
import java.util.UUID;

public final class ABSItemDataComponents {
    private ABSItemDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> REGISTRY = DeferredRegister.create("alphabet_speedrun", Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Unit>> NO_SHRINKING = REGISTRY.register("no_shrinking", ABSItemDataComponents::unit);
    public static final RegistrySupplier<DataComponentType<Unit>> BYPASSES_ITEM_CHECK = REGISTRY.register("bypasses_item_check", ABSItemDataComponents::unit);
    public static final RegistrySupplier<DataComponentType<UUID>> RECORD_STAMP = REGISTRY.register(
            "record_stamp",
            () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build()
    );

    private static DataComponentType<Unit> unit() {
        return DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build();
    }
}
