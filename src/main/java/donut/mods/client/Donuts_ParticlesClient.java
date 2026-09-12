package donut.mods.client;

import donut.mods.client.gui.ParticleEmitterScreen;
import donut.mods.client.render.ClientTextureDirectoryCache;
import donut.mods.client.render.DynamicParticleTextureManager;
import donut.mods.network.TextureDataPayload;
import donut.mods.network.TextureListPayload;
import donut.mods.registry.ModScreenHandlers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class Donuts_ParticlesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.PARTICLE_EMITTER, ParticleEmitterScreen::new);

        // Server sends this on join and whenever an admin rescans the texture
        // directory - keeps the GUI's texture picker always up to date.
        ClientPlayNetworking.registerGlobalReceiver(TextureListPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientTextureDirectoryCache.update(payload.textureIds())));

        // Response to a request we sent - decode and register on the render thread.
        ClientPlayNetworking.registerGlobalReceiver(TextureDataPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.found()) {
                        DynamicParticleTextureManager.registerFromBytes(payload.textureId(), payload.data());
                    } else {
                        DynamicParticleTextureManager.markFailed(payload.textureId());
                    }
                }));
    }
}