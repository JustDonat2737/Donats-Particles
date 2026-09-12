package donut.mods.client.render;

import donut.mods.network.TextureRequestPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

/**
 * Client-side entry point for obtaining a texture Identifier for a given server
 * texture id. If not yet cached, kicks off a network request and returns null for
 * this call (and every call until the response arrives) - callers should treat null
 * as "not ready yet, try again next tick."
 */
public final class ClientTextureRequester {

	private ClientTextureRequester() {}

	public static Identifier resolve(String textureId) {
		if (textureId == null || textureId.isBlank()) return null;

		Identifier cached = DynamicParticleTextureManager.getCached(textureId);
		if (cached != null) return cached;

		if (DynamicParticleTextureManager.hasFailed(textureId) || DynamicParticleTextureManager.isPending(textureId)) {
			return null;
		}

		DynamicParticleTextureManager.markPending(textureId);
		ClientPlayNetworking.send(new TextureRequestPayload(textureId));
		return null;
	}
}