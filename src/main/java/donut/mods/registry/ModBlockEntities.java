package donut.mods.registry;

import donut.mods.Donuts_Particles;
import donut.mods.particle.ParticleEmitterBlockEntity;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlockEntities {

    public static final BlockEntityType<ParticleEmitterBlockEntity> PARTICLE_EMITTER =
            BlockEntityType.Builder.create(ParticleEmitterBlockEntity::new, ModBlocks.PARTICLE_EMITTER)
                    .build();

    public static void register() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Donuts_Particles.id("particle_emitter"), PARTICLE_EMITTER);
    }
}