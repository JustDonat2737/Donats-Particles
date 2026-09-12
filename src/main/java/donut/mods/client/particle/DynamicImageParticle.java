package donut.mods.client.particle;

import donut.mods.Donuts_Particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicImageParticle extends Particle {

    private static final Map<Identifier, ParticleTextureSheet> SHEET_CACHE = new ConcurrentHashMap<>();

    private final ParticleTextureSheet sheet;
    private final float size;

    public DynamicImageParticle(ClientWorld world, double x, double y, double z,
                                double velX, double velY, double velZ,
                                Identifier textureId, float size, int maxAgeTicks) {
        super(world, x, y, z);
        Donuts_Particles.LOGGER.info("[TEMP-DIAG] DynamicImageParticle constructed: tex={} pos=({}, {}, {}) size={} maxAge={}", textureId, x, y, z, size, maxAgeTicks); // TEMP-DIAG
        this.setVelocity(velX, velY, velZ);
        this.size = size;
        this.maxAge = maxAgeTicks;
        this.sheet = SHEET_CACHE.computeIfAbsent(textureId, ImageParticleTextureSheet::new);
    }

    @Override
    public ParticleTextureSheet getType() {
        return sheet;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= 0.98;
        this.velocityY *= 0.98;
        this.velocityZ *= 0.98;

        float lifeRatio = (float) this.age / this.maxAge;
        this.alpha = MathHelper.clamp(1.0f - lifeRatio, 0.0f, 1.0f);
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vector3f camPos = camera.getPos().toVector3f();
        float px = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - camPos.x);
        float py = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - camPos.y);
        float pz = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - camPos.z);

        Quaternionf rotation = new Quaternionf(camera.getRotation());
        Vector3f[] corners = {
                new Vector3f(-1.0f, -1.0f, 0.0f),
                new Vector3f(-1.0f, 1.0f, 0.0f),
                new Vector3f(1.0f, 1.0f, 0.0f),
                new Vector3f(1.0f, -1.0f, 0.0f)
        };

        float half = this.getSize(tickDelta) / 2f;
        for (Vector3f corner : corners) {
            corner.rotate(rotation);
            corner.mul(half);
            corner.add(px, py, pz);
        }

        int light = this.getBrightness(tickDelta);

        vertexConsumer.vertex(corners[0].x, corners[0].y, corners[0].z).texture(0, 1)
                .color(1f, 1f, 1f, this.alpha).light(light);
        vertexConsumer.vertex(corners[1].x, corners[1].y, corners[1].z).texture(0, 0)
                .color(1f, 1f, 1f, this.alpha).light(light);
        vertexConsumer.vertex(corners[2].x, corners[2].y, corners[2].z).texture(1, 0)
                .color(1f, 1f, 1f, this.alpha).light(light);
        vertexConsumer.vertex(corners[3].x, corners[3].y, corners[3].z).texture(1, 1)
                .color(1f, 1f, 1f, this.alpha).light(light);
    }


    public float getSize(float tickDelta) {
        return size;
    }
}