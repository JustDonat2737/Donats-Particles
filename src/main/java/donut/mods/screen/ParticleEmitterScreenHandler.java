package donut.mods.screen;

import donut.mods.particle.ParticleEmitterBlockEntity;
import donut.mods.registry.ModScreenHandlers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side screen handler for the Particle Emitter block's GUI.
 * Carries the targeted {@link ParticleEmitterBlockEntity} so the client screen can
 * read the current values (particle, frequency, velocity, spread, count, texture)
 * and report the block position back when the player saves new settings.
 */
public class ParticleEmitterScreenHandler extends ScreenHandler {

    @Nullable
    private final ParticleEmitterBlockEntity blockEntity;

    public ParticleEmitterScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        super(ModScreenHandlers.PARTICLE_EMITTER, syncId);
        World world = playerInventory.player.getWorld();
        this.blockEntity = world.getBlockEntity(pos) instanceof ParticleEmitterBlockEntity emitter ? emitter : null;
    }

    /**
     * @return the emitter block entity this screen is bound to, or {@code null} if
     *         the block was removed/unloaded while the screen is open.
     */
    @Nullable
    public ParticleEmitterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * @return the world position of the bound emitter block (used to build the
     *         settings packet that the client sends back to the server).
     */
    public BlockPos getPos() {
        return blockEntity != null ? blockEntity.getPos() : BlockPos.ORIGIN;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return blockEntity != null
                && player.squaredDistanceTo(
                        blockEntity.getPos().getX() + 0.5,
                        blockEntity.getPos().getY() + 0.5,
                        blockEntity.getPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // This screen exposes no player-inventory slots, so there is nothing to move.
        return ItemStack.EMPTY;
    }
}
