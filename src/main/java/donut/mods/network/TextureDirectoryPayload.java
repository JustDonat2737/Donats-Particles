package donut.mods.network;

import donut.mods.Donuts_Particles;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -&gt; client payload that synchronizes the entire custom particle texture
 * directory. The client replaces its local copy whenever it receives one of
 * these, so every client always has exactly the textures the server has - no
 * per-client image files, no missing-texture desyncs.
 *
 * <p>Sent on {@code ServerPlayConnectionEvents.JOIN} and again after every
 * {@code rescan()} (the {@code /donuts_particles reloadtextures} command).</p>
 */
public record TextureDirectoryPayload(List<TextureEntry> textures) implements CustomPayload {

	/** One registered texture: its stable id and the raw PNG file content. */
	public record TextureEntry(String id, byte[] pngBytes) {}

	public static final int MAX_TEXTURES = 256;
	/** Must match {@link texture.ParticleTextureDirectory#MAX_FILE_BYTES}. */
	public static final int MAX_TEXTURE_BYTES = 512 * 1024;
	public static final int MAX_TEXTURE_ID_LENGTH = 64;

	public static final CustomPayload.Id<TextureDirectoryPayload> ID =
			new CustomPayload.Id<>(Donuts_Particles.id("texture_directory"));

	public static final PacketCodec<RegistryByteBuf, TextureDirectoryPayload> CODEC = PacketCodec.of(
			TextureDirectoryPayload::write,
			TextureDirectoryPayload::read
	);

	private void write(RegistryByteBuf buf) {
		buf.writeVarInt(textures.size());
		for (TextureEntry entry : textures) {
			buf.writeString(entry.id(), MAX_TEXTURE_ID_LENGTH);
			buf.writeByteArray(entry.pngBytes());
		}
	}

	private static TextureDirectoryPayload read(RegistryByteBuf buf) {
		int count = buf.readVarInt();
		if (count > MAX_TEXTURES) {
			throw new IllegalArgumentException("Texture directory too large: " + count);
		}
		List<TextureEntry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			String id = buf.readString(MAX_TEXTURE_ID_LENGTH);
			byte[] png = buf.readByteArray(MAX_TEXTURE_BYTES);
			entries.add(new TextureEntry(id, png));
		}
		return new TextureDirectoryPayload(List.copyOf(entries));
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}