package donut.mods.client.particle;

import donut.mods.Donuts_Particles;
import donut.mods.client.render.ClientTextureRequester;
import donut.mods.particle.ParticleEmitterBlockEntity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ClientParticleSpawner {

    private ClientParticleSpawner() {}

    public static void spawnCustomImageParticles(ParticleEmitterBlockEntity be, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.world instanceof ClientWorld world)) return;

        Identifier textureId = ClientTextureRequester.resolve(be.getCustomTextureId());
        if (textureId == null) {
            Donuts_Particles.LOGGER.info("[TEMP-DIAG] spawnCustomImageParticles: resolve('{}') returned null - skipping this cycle", be.getCustomTextureId()); // TEMP-DIAG
            return; // still downloading/registering, or failed - retried next cycle
        }

        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;

        for (int i = 0; i < Math.max(1, be.getCount()); i++) {
            double px = cx + (world.random.nextDouble() * 2.0 - 1.0) * be.getSpreadX();
            double py = cy + (world.random.nextDouble() * 2.0 - 1.0) * be.getSpreadY();
            double pz = cz + (world.random.nextDouble() * 2.0 - 1.0) * be.getSpreadZ();

            DynamicImageParticle particle = new DynamicImageParticle(
                    world, px, py, pz,
                    be.getVelX(), be.getVelY(), be.getVelZ(),
                    textureId, 0.5f, 40
            );
            client.particleManager.addParticle(particle);
        }
    }
}