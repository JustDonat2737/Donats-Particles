package donut.mods.client.render;

import donut.mods.client.gui.ParticleEmitterScreen;
import donut.mods.registry.ModScreenHandlers;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class Donuts_ParticlesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.PARTICLE_EMITTER, ParticleEmitterScreen::new);
    }
}