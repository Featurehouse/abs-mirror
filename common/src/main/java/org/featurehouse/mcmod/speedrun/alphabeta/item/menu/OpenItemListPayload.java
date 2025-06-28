package org.featurehouse.mcmod.speedrun.alphabeta.item.menu;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class OpenItemListPayload implements CustomPayload {
    public static final Id<OpenItemListPayload> ID = new Id<>(Identifier.of("alphabet_speedrun", "item_list"));
    public static final PacketCodec<RegistryByteBuf, OpenItemListPayload> PACKET_CODEC = CustomPayload.codecOf(
            (payload, buf) -> {},
            buf -> new OpenItemListPayload()
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
