package org.featurehouse.mcmod.speedrun.alphabeta.item.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class OpenItemListPayload implements CustomPacketPayload {
    public static final Type<OpenItemListPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("alphabet_speedrun", "item_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenItemListPayload> PACKET_CODEC = CustomPacketPayload.codec(
            (payload, buf) -> {},
            buf -> new OpenItemListPayload()
    );

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
