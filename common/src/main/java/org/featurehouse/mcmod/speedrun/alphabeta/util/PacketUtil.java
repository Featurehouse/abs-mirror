package org.featurehouse.mcmod.speedrun.alphabeta.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;

public class PacketUtil {
    private PacketUtil() {}

    public static void writeItemStack(ByteBuf buf, ItemStack stack) {
        ItemStack.OPTIONAL_PACKET_CODEC.encode(new RegistryByteBuf(buf, DynamicRegistryManager.of(Registries.REGISTRIES)), stack);
    }

    public static ItemStack readItemStack(ByteBuf buf) {
        return ItemStack.OPTIONAL_PACKET_CODEC.decode(new RegistryByteBuf(buf, DynamicRegistryManager.of(Registries.REGISTRIES)));
    }
}
