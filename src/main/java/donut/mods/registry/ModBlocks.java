package donut.mods.registry;

import donut.mods.Donuts_Particles;
import donut.mods.particle.ParticleEmitterBlock;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlocks {

    public static final Block PARTICLE_EMITTER = new ParticleEmitterBlock(
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.GRAY)
                    .strength(1.5f)
                    .nonOpaque()
                    .requiresTool()
    );

    public static void register() {
        Registry.register(Registries.BLOCK, Donuts_Particles.id("particle_emitter"), PARTICLE_EMITTER);
        Registry.register(Registries.ITEM, Donuts_Particles.id("particle_emitter"),
                new BlockItem(PARTICLE_EMITTER, new Item.Settings()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE)
                .register(entries -> entries.add(PARTICLE_EMITTER));
    }
}