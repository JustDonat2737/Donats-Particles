package donut.mods.particle;

import donut.mods.Donuts_Particles;
import donut.mods.client.particle.ClientParticleSpawner;
import donut.mods.network.EmitterSettingsPayload;
import donut.mods.registry.ModBlockEntities;
import donut.mods.screen.ParticleEmitterScreenHandler;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ParticleEmitterBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {

    private ParticleOption particle = ParticleOption.FLAME;
    private int frequency = 20;
    private float velX = 0f, velY = 0.05f, velZ = 0f;
    private float spreadX = 0.2f, spreadY = 0.2f, spreadZ = 0.2f;
    private int count = 1;
    private String customTextureId = ""; // "" means unset

    private int tickCounter = 0;

    public ParticleEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARTICLE_EMITTER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ParticleEmitterBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < Math.max(1, be.frequency)) {
            return;
        }
        be.tickCounter = 0;

        if (world.isClient) {
            if (be.particle.isCustomImage() && !be.customTextureId.isBlank()) {
                Donuts_Particles.LOGGER.info("[TEMP-DIAG] tick() client custom path: particle={} customTextureId='{}' freq={} count={}", be.particle.name(), be.customTextureId, be.frequency, be.count); // TEMP-DIAG
                ClientParticleSpawner.spawnCustomImageParticles(be, pos);
            }
        } else if (!be.particle.isCustomImage() && world instanceof ServerWorld serverWorld) {
            be.emitVanilla(serverWorld, pos);
        }
    }

    private void emitVanilla(ServerWorld world, BlockPos pos) {
        ParticleEffect effect = particle.type;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;

        for (int i = 0; i < Math.max(1, count); i++) {
            double px = cx + (world.random.nextDouble() * 2.0 - 1.0) * spreadX;
            double py = cy + (world.random.nextDouble() * 2.0 - 1.0) * spreadY;
            double pz = cz + (world.random.nextDouble() * 2.0 - 1.0) * spreadZ;
            world.spawnParticles(effect, px, py, pz, 0, velX, velY, velZ, 1.0);
        }
    }

    public void applySettings(EmitterSettingsPayload payload) {
        this.particle = ParticleOption.fromName(payload.particleId());
        this.frequency = MathHelper.clamp(payload.frequency(), 5, 100); // minimum 5 ticks between emissions
        this.velX = payload.velX();
        this.velY = payload.velY();
        this.velZ = payload.velZ();
        this.spreadX = payload.spreadX();
        this.spreadY = payload.spreadY();
        this.spreadZ = payload.spreadZ();
        this.count = MathHelper.clamp(payload.count(), 1, 64);

        String id = payload.customTextureId();
        this.customTextureId = id == null ? "" : id.trim();

        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("Particle", particle.name());
        nbt.putInt("Frequency", frequency);
        nbt.putFloat("VelX", velX);
        nbt.putFloat("VelY", velY);
        nbt.putFloat("VelZ", velZ);
        nbt.putFloat("SpreadX", spreadX);
        nbt.putFloat("SpreadY", spreadY);
        nbt.putFloat("SpreadZ", spreadZ);
        nbt.putInt("Count", count);
        nbt.putString("CustomTextureId", customTextureId);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.particle = ParticleOption.fromName(nbt.getString("Particle"));
        this.frequency = nbt.getInt("Frequency");
        this.velX = nbt.getFloat("VelX");
        this.velY = nbt.getFloat("VelY");
        this.velZ = nbt.getFloat("VelZ");
        this.spreadX = nbt.getFloat("SpreadX");
        this.spreadY = nbt.getFloat("SpreadY");
        this.spreadZ = nbt.getFloat("SpreadZ");
        this.count = nbt.getInt("Count");
        this.customTextureId = nbt.getString("CustomTextureId");
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.donuts_particles.particle_emitter");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ParticleEmitterScreenHandler(syncId, inv, this.pos);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    public ParticleOption getParticle() { return particle; }
    public int getFrequency() { return frequency; }
    public float getVelX() { return velX; }
    public float getVelY() { return velY; }
    public float getVelZ() { return velZ; }
    public float getSpreadX() { return spreadX; }
    public float getSpreadY() { return spreadY; }
    public float getSpreadZ() { return spreadZ; }
    public int getCount() { return count; }
    public String getCustomTextureId() { return customTextureId; }
}