package donut.mods.network;

import donut.mods.Donuts_Particles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client -> Server: "please add this image to the shared texture directory." */
public record TextureUploadPayload(String desiredId, byte[] data) implements CustomPayload {

	public static final int MAX_BYTES = 512 * 1024; // matches ParticleTextureDirectory's own cap

	public static final CustomPayload.Id<TextureUploadPayload> ID =
			new CustomPayload.Id<>(Donuts_Particles.id("texture_upload"));

	public static final PacketCodec<RegistryByteBuf, TextureUploadPayload> CODEC = PacketCodec.of(
			(payload, buf) -> {
				buf.writeString(payload.desiredId(), 64);
				buf.writeByteArray(payload.data());
			},
			buf -> new TextureUploadPayload(buf.readString(64), buf.readByteArray(MAX_BYTES))
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}