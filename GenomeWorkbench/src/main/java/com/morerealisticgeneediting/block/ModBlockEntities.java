package com.morerealisticgeneediting.block;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.block.entity.SequencerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    // 1. Define the Sequencer Block Entity Type
    public static BlockEntityType<SequencerBlockEntity> SEQUENCER_BLOCK_ENTITY;

    // 2. A method to be called in the Mod Initializer
    public static void registerBlockEntities() {
        SEQUENCER_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MoreRealisticGeneEditing.MOD_ID, "sequencer"),
                FabricBlockEntityTypeBuilder.create(SequencerBlockEntity::new, ModBlocks.SEQUENCER_BLOCK).build(null));
    }
}
