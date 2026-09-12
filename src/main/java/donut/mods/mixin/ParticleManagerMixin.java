package donut.mods.mixin;

import donut.mods.client.particle.ImageParticleTextureSheet;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla 1.21.1 ParticleManager#renderParticles iterates a hardcoded
 * PARTICLE_TEXTURE_SHEETS list (TERRAIN, OPAQUE, LIT, TRANSLUCENT, CUSTOM) and only
 * ever calls ParticleTextureSheet#begin for sheet instances contained in that list.
 * Custom sheets are still stored in the particle map (keyed by identity), so their
 * particles exist but are never drawn.
 *
 * This mixin captures the vanilla sheet list once and extends the render iteration
 * with every ImageParticleTextureSheet registered by the mod, preserving the original
 * sheet order so opaque/translucent sorting is unchanged.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Shadow
    @Final
    private static List<ParticleTextureSheet> PARTICLE_TEXTURE_SHEETS;

    private static List<ParticleTextureSheet> baseSheets;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void donuts_particles$recordBaseSheets(CallbackInfo ci) {
        baseSheets = List.copyOf(PARTICLE_TEXTURE_SHEETS);
    }

    @Redirect(
            method = "renderParticles",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/client/particle/ParticleManager;PARTICLE_TEXTURE_SHEETS:Ljava/util/List;"
            )
    )
    private List<ParticleTextureSheet> donuts_particles$includeCustomSheets() {
        List<ParticleTextureSheet> customSheets = ImageParticleTextureSheet.getRegisteredSheets();
        if (customSheets.isEmpty()) {
            return PARTICLE_TEXTURE_SHEETS;
        }
        List<ParticleTextureSheet> all = new ArrayList<>();
        all.addAll(baseSheets != null ? baseSheets : PARTICLE_TEXTURE_SHEETS);
        all.addAll(customSheets);
        return all;
    }
}