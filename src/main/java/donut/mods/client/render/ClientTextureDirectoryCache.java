package donut.mods.client.render;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Holds the most recent texture-id list received from the server. */
public final class ClientTextureDirectoryCache {

	private static final AtomicReference<List<String>> AVAILABLE = new AtomicReference<>(List.of());

	private ClientTextureDirectoryCache() {}

	public static void update(List<String> ids) {
		AVAILABLE.set(List.copyOf(ids));
	}

	public static List<String> getAvailableIds() {
		return AVAILABLE.get();
	}
}