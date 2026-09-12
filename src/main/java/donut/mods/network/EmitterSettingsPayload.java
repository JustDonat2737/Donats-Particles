package donut.mods.network;

import donut.mods.Donuts_Particles;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record EmitterSettingsPayload(
        BlockPos pos,
        String particleId,
        int frequency,
        float velX, float velY, float velZ,
        float spreadX, float spreadY, float spreadZ,
        int count,
        String customTextureId
) implements CustomPayload {

    public static final int MAX_ID_LENGTH = 64;

    public static final CustomPayload.Id<EmitterSettingsPayload> ID =
            new CustomPayload.Id<>(Donuts_Particles.id("emitter_settings"));

    public static final PacketCodec<RegistryByteBuf, EmitterSettingsPayload> CODEC = PacketCodec.of(
            (payload, buf) -> payload.write(buf),
            EmitterSettingsPayload::read
    );

    private void write(RegistryByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(particleId);
        buf.writeVarInt(frequency);
        buf.writeFloat(velX);
        buf.writeFloat(velY);
        buf.writeFloat(velZ);
        buf.writeFloat(spreadX);
        buf.writeFloat(spreadY);
        buf.writeFloat(spreadZ);
        buf.writeVarInt(count);
        buf.writeString(customTextureId == null ? "" : customTextureId, MAX_ID_LENGTH);
    }

    private static EmitterSettingsPayload read(RegistryByteBuf buf) {
        return new EmitterSettingsPayload(
                buf.readBlockPos(),
                buf.readString(),
                buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readVarInt(),
                buf.readString(MAX_ID_LENGTH)
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}