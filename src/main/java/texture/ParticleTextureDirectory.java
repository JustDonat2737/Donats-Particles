package donut.mods.texture;

import donut.mods.Donuts_Particles;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Server-side registry of custom particle textures. Scans a fixed directory in the
 * server's config folder for PNG files and exposes them as small, stable, sanitized
 * IDs. Only files an admin places here are ever exposed - players never supply
 * arbitrary paths or URLs, which is what makes this safe for multiplayer (no SSRF,
 * no arbitrary local-disk reads, no per-client desync from missing files).
 */
public final class ParticleTextureDirectory {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png");
    private static final long MAX_FILE_BYTES = 512L * 1024; // 512 KB per texture

    private static Path directory;
    private static volatile Map<String, Path> textures = new LinkedHashMap<>();

    private ParticleTextureDirectory() {}

    public static void init() {
        directory = FabricLoader.getInstance().getConfigDir().resolve("donuts_particles").resolve("particle_textures");
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            Donuts_Particles.LOGGER.error("Could not create particle texture directory", e);
        }
        rescan();
    }

    public static synchronized void rescan() {
        Map<String, Path> found = new LinkedHashMap<>();
        if (directory == null || !Files.isDirectory(directory)) {
            textures = found;
            return;
        }
        try (var stream = Files.list(directory)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(path)) continue;
                String fileName = path.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                if (dot <= 0) continue;
                String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!ALLOWED_EXTENSIONS.contains(ext)) continue;

                String id = sanitizeId(fileName.substring(0, dot));
                if (id.isEmpty()) continue;

                try {
                    if (Files.size(path) > MAX_FILE_BYTES) {
                        Donuts_Particles.LOGGER.warn("Skipping particle texture '{}': exceeds {} bytes", fileName, MAX_FILE_BYTES);
                        continue;
                    }
                } catch (IOException e) {
                    continue;
                }

                found.put(id, path);
            }
        } catch (IOException e) {
            Donuts_Particles.LOGGER.error("Failed to scan particle texture directory", e);
        }
        textures = found;
        Donuts_Particles.LOGGER.info("Loaded {} custom particle texture(s)", textures.size());
    }

    private static String sanitizeId(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    public static List<String> getAvailableIds() {
        return List.copyOf(textures.keySet());
    }

    public static byte[] readTextureBytes(String id) throws IOException {
        Path path = textures.get(id);
        if (path == null) {
            throw new IOException("Unknown texture id: " + id);
        }
        return Files.readAllBytes(path);
    }

    public static boolean exists(String id) {
        return textures.containsKey(id);
    }
    // Made public so the upload receiver can reuse the same sanitization rules the
    // directory scanner already applies to filenames.
    public static String sanitizeIdPublic(String raw) {
        return sanitizeId(raw);
    }

    /**
     * Writes a new texture file into the directory. Caller is responsible for
     * validating the bytes are actually a PNG BEFORE calling this - this method
     * only handles the filesystem write.
     */
    public static synchronized void writeTextureFile(String id, byte[] data) throws IOException {
        if (directory == null) {
            throw new IOException("Texture directory not initialized");
        }
        Path target = directory.resolve(id + ".png");
        Files.write(target, data);
    }
}