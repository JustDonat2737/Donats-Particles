package donut.mods;

import donut.mods.network.EmitterSettingsPayload;
import donut.mods.network.TextureDataPayload;
import donut.mods.network.TextureListPayload;
import donut.mods.network.TextureRequestPayload;
import donut.mods.particle.ParticleEmitterBlockEntity;
import donut.mods.registry.ModBlockEntities;
import donut.mods.registry.ModBlocks;
import donut.mods.registry.ModScreenHandlers;
import donut.mods.texture.ParticleTextureDirectory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import donut.mods.network.TextureUploadPayload;
import java.util.Arrays;

public class Donuts_Particles implements ModInitializer {
    public static final String MOD_ID = "donuts_particles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Wenn du dassssssss liest bist DU ein noob! ezezez");

        ModBlocks.register();
        ModBlockEntities.register();
        ModScreenHandlers.register();

        // Server-side directory of admin-provided particle textures. Players only
        // ever pick from this list - they can never point the block at an arbitrary
        // path/URL anymore.
        ParticleTextureDirectory.init();

        registerNetworking();
        registerCommands();
    }

    private void registerNetworking() {
        PayloadTypeRegistry.playC2S().register(EmitterSettingsPayload.ID, EmitterSettingsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TextureRequestPayload.ID, TextureRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TextureListPayload.ID, TextureListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TextureDataPayload.ID, TextureDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TextureUploadPayload.ID, TextureUploadPayload.CODEC);

        // Send every player the current texture list as soon as they join.

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.getPlayer(), new TextureListPayload(ParticleTextureDirectory.getAvailableIds())));

        // PNG signature check - the first 8 bytes of every valid PNG file are always
        // this exact sequence. This isn't a full image validator, but it reliably
        // rejects non-image junk without needing a heavyweight decode on the server.
        final byte[] pngSignature = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

        ServerPlayNetworking.registerGlobalReceiver(TextureUploadPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();

                    // Gate uploads to operators - writing files to server disk based on
                    // client input is the one part of this system that's genuinely risky
                    // if left open to everyone. Non-ops can still pick any texture an
                    // admin already uploaded.
                    if (!player.hasPermissionLevel(2)) {
                        player.sendMessage(Text.literal("Only operators may upload particle textures."), true);
                        return;
                    }

                    byte[] data = payload.data();
                    if (data.length < 8 || !Arrays.equals(Arrays.copyOf(data, 8), pngSignature)) {
                        player.sendMessage(Text.literal("Upload rejected: file is not a valid PNG."), true);
                        return;
                    }
                    if (data.length > TextureUploadPayload.MAX_BYTES) {
                        player.sendMessage(Text.literal("Upload rejected: file exceeds 512 KB."), true);
                        return;
                    }

                    String id = ParticleTextureDirectory.sanitizeIdPublic(payload.desiredId());
                    if (id.isEmpty()) {
                        id = "upload_" + System.currentTimeMillis();
                    }

                    try {
                        ParticleTextureDirectory.writeTextureFile(id, data);
                        ParticleTextureDirectory.rescan();

                        // Broadcast the updated list to EVERY connected player, not just
                        // the uploader - this is what makes the new texture immediately
                        // selectable for everyone on the server.
                        TextureListPayload listPayload = new TextureListPayload(ParticleTextureDirectory.getAvailableIds());
                        context.server().getPlayerManager().getPlayerList()
                                .forEach(p -> ServerPlayNetworking.send(p, listPayload));

                        player.sendMessage(Text.literal("Uploaded particle texture: " + id), true);
                    } catch (IOException e) {
                        LOGGER.error("Failed to save uploaded particle texture", e);
                        player.sendMessage(Text.literal("Upload failed: " + e.getMessage()), true);
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(EmitterSettingsPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    World world = player.getWorld();

                    double distSq = player.squaredDistanceTo(
                            payload.pos().getX() + 0.5,
                            payload.pos().getY() + 0.5,
                            payload.pos().getZ() + 0.5);
                    if (distSq > 64.0) {
                        return;
                    }

                    String requestedTextureId = payload.customTextureId();
                    // Must exist in the server's own directory. Under normal play this
                    // always passes (players only pick from a list we sent them) - this
                    // guards against a modified client, or a stale selection after an
                    // admin removed a file.
                    if (!requestedTextureId.isBlank() && !ParticleTextureDirectory.exists(requestedTextureId)) {
                        player.sendMessage(Text.literal("That particle texture is no longer available."), true);
                        return;
                    }

                    BlockEntity be = world.getBlockEntity(payload.pos());
                    if (be instanceof ParticleEmitterBlockEntity emitter) {
                        emitter.applySettings(payload);
                    }
                }));

        // A client asked for the raw bytes of one texture id. Reading is disk IO, so
        // it's done off the network thread; the response is sent once ready.
        ServerPlayNetworking.registerGlobalReceiver(TextureRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String id = payload.textureId();

            CompletableFuture.runAsync(() -> {
                try {
                    byte[] data = ParticleTextureDirectory.readTextureBytes(id);
                    ServerPlayNetworking.send(player, new TextureDataPayload(id, true, data));
                } catch (IOException e) {
                    ServerPlayNetworking.send(player, new TextureDataPayload(id, false, new byte[0]));
                }
            });
        });
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("particleemitter")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload")
                                .executes(ctx -> {
                                    ParticleTextureDirectory.rescan();
                                    TextureListPayload payload = new TextureListPayload(ParticleTextureDirectory.getAvailableIds());
                                    ctx.getSource().getServer().getPlayerManager().getPlayerList()
                                            .forEach(p -> ServerPlayNetworking.send(p, payload));
                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                            "Reloaded particle textures: " + ParticleTextureDirectory.getAvailableIds().size() + " found."), true);
                                    return 1;
                                }))));
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}