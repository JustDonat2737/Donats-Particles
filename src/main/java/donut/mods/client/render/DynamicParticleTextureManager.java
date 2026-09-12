package donut.mods.client.render;

import donut.mods.Donuts_Particles;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of custom particle textures downloaded FROM THE SERVER (never
 * from arbitrary local paths or URLs anymore - see ClientTextureRequester for how
 * bytes arrive). Registers each one as a real GPU texture under a synthetic
 * Identifier once decoded, keyed by the server's texture id.
 */
public final class DynamicParticleTextureManager {

    private static final Map<String, Identifier> REGISTERED = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private DynamicParticleTextureManager() {}

    public static Identifier getCached(String textureId) {
        return REGISTERED.get(textureId);
    }

    public static boolean isPending(String textureId) {
        return PENDING.contains(textureId);
    }

    public static boolean hasFailed(String textureId) {
        return FAILED.contains(textureId);
    }

    public static void markPending(String textureId) {
        PENDING.add(textureId);
    }

    public static void registerFromBytes(String textureId, byte[] data) {
        PENDING.remove(textureId);
        try (var stream = new ByteArrayInputStream(data)) {
            NativeImage image = NativeImage.read(stream);
            Identifier id = Donuts_Particles.id("dynamic/" + textureId);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
            REGISTERED.put(textureId, id);
            FAILED.remove(textureId);
            Donuts_Particles.LOGGER.info("[TEMP-DIAG] registerFromBytes SUCCESS: '{}' -> {} ({}x{})", textureId, id, image.getWidth(), image.getHeight()); // TEMP-DIAG
        } catch (IOException e) {
            Donuts_Particles.LOGGER.warn("Failed to decode particle texture '{}': {}", textureId, e.getMessage());
            FAILED.add(textureId);
        }
    }

    public static void markFailed(String textureId) {
        PENDING.remove(textureId);
        FAILED.add(textureId);
    }

    public static void clearCache() {
        REGISTERED.clear();
        PENDING.clear();
        FAILED.clear();
    }
}