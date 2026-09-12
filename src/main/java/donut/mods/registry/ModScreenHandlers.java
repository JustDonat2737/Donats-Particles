package donut.mods.registry;

import donut.mods.Donuts_Particles;
import donut.mods.screen.ParticleEmitterScreenHandler;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {

    public static final ScreenHandlerType<ParticleEmitterScreenHandler> PARTICLE_EMITTER =
            new ExtendedScreenHandlerType<>(ParticleEmitterScreenHandler::new, BlockPos.PACKET_CODEC);

    public static void register() {
        Registry.register(Registries.SCREEN_HANDLER, Donuts_Particles.id("particle_emitter"), PARTICLE_EMITTER);
    }
}
