package donut.mods.particle;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;

public enum ParticleOption {
    FLAME(ParticleTypes.FLAME),
    SMOKE(ParticleTypes.SMOKE),
    LARGE_SMOKE(ParticleTypes.LARGE_SMOKE),
    END_ROD(ParticleTypes.END_ROD),
    HEART(ParticleTypes.HEART),
    LAVA(ParticleTypes.LAVA),
    NOTE(ParticleTypes.NOTE),
    CLOUD(ParticleTypes.CLOUD),
    DRIPPING_WATER(ParticleTypes.DRIPPING_WATER),
    PORTAL(ParticleTypes.PORTAL),
    CUSTOM_IMAGE(null);

    public final SimpleParticleType type;

    ParticleOption(SimpleParticleType type) {
        this.type = type;
    }

    public boolean isCustomImage() {
        return this == CUSTOM_IMAGE;
    }

    public ParticleOption next() {
        ParticleOption[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static ParticleOption fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return FLAME;
        }
    }
}