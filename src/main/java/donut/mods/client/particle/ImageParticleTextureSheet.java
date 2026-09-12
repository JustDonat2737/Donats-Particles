package donut.mods.client.particle;

import donut.mods.Donuts_Particles;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImageParticleTextureSheet implements ParticleTextureSheet {

    /**
     * Every sheet instance ever created. Vanilla 1.21.1's ParticleManager only renders
     * a hardcoded ImmutableList of sheet singletons, so any custom sheet is bucketed in
     * the particle map but never drawn. The ParticleManagerMixin extends that static list
     * with the sheets registered here (identity-matched against the particle map keys).
     */
    private static final List<ParticleTextureSheet> REGISTERED_SHEETS = new CopyOnWriteArrayList<>();

    private final Identifier textureId;

    public ImageParticleTextureSheet(Identifier textureId) {
        this.textureId = textureId;
        REGISTERED_SHEETS.add(this);
    }

    public static List<ParticleTextureSheet> getRegisteredSheets() {
        return REGISTERED_SHEETS;
    }

    @Nullable
    @Override
    public BufferBuilder begin(Tessellator tessellator, TextureManager textureManager) {
        Donuts_Particles.LOGGER.info("[TEMP-DIAG] ImageParticleTextureSheet.begin() binding texture {}", textureId); // TEMP-DIAG
        RenderSystem.setShader(GameRenderer::getParticleProgram);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        return tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
    }
}