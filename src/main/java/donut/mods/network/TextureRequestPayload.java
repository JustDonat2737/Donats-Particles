package donut.mods.network;

import donut.mods.Donuts_Particles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client -> Server: "send me the raw bytes for this texture id." */
public record TextureRequestPayload(String textureId) implements CustomPayload {

    public static final CustomPayload.Id<TextureRequestPayload> ID =
            new CustomPayload.Id<>(Donuts_Particles.id("texture_request"));

    public static final PacketCodec<RegistryByteBuf, TextureRequestPayload> CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeString(payload.textureId(), 64),
            buf -> new TextureRequestPayload(buf.readString(64))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}