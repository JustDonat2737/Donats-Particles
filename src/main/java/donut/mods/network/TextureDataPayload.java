package donut.mods.network;

import donut.mods.Donuts_Particles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server -> Client: the actual PNG bytes for a requested texture id, or found=false
 * if the id no longer exists (e.g. the admin removed the file after the client
 * cached the ID list).
 */
public record TextureDataPayload(String textureId, boolean found, byte[] data) implements CustomPayload {

    public static final int MAX_BYTES = 512 * 1024;

    public static final CustomPayload.Id<TextureDataPayload> ID =
            new CustomPayload.Id<>(Donuts_Particles.id("texture_data"));

    public static final PacketCodec<RegistryByteBuf, TextureDataPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeString(payload.textureId(), 64);
                buf.writeBoolean(payload.found());
                buf.writeByteArray(payload.data());
            },
            buf -> {
                String id = buf.readString(64);
                boolean found = buf.readBoolean();
                byte[] data = buf.readByteArray(MAX_BYTES);
                return new TextureDataPayload(id, found, data);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}