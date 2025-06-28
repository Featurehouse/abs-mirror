package org.featurehouse.mcmod.speedrun.alphabeta.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class PacketUtil {
    private PacketUtil() {}

    public static void writeItemStack(ByteBuf buf, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(new RegistryFriendlyByteBuf(buf, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)), stack);
    }

    public static ItemStack readItemStack(ByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buf, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
    }
}
