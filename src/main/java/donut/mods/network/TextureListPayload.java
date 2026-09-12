package donut.mods.network;

import donut.mods.Donuts_Particles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client. Sent on join and whenever an admin rescans the texture
 * directory, so every client always has an up-to-date list of texture IDs to choose
 * from in the GUI - without ever transferring the images themselves until needed.
 */
public record TextureListPayload(List<String> textureIds) implements CustomPayload {

    public static final CustomPayload.Id<TextureListPayload> ID =
            new CustomPayload.Id<>(Donuts_Particles.id("texture_list"));

    public static final PacketCodec<RegistryByteBuf, TextureListPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.textureIds().size());
                for (String id : payload.textureIds()) {
                    buf.writeString(id, 64);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<String> ids = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ids.add(buf.readString(64));
                }
                return new TextureListPayload(ids);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}