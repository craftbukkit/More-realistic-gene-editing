package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    public static BlockEntityType<CellProcessorBlockEntity> CELL_PROCESSOR_BLOCK_ENTITY;
    public static BlockEntityType<SequencerBlockEntity> SEQUENCER_BLOCK_ENTITY;
    public static BlockEntityType<PcrMachineBlockEntity> PCR_MACHINE;
    public static BlockEntityType<IncubatorBlockEntity> INCUBATOR;
    public static BlockEntityType<ElectrophoresisTankBlockEntity> ELECTROPHORESIS_TANK;
    public static BlockEntityType<EthicsCaseBlockEntity> ETHICS_CASE_BLOCK_ENTITY;


    public static void registerBlockEntities() {
        CELL_PROCESSOR_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MoreRealisticGeneEditing.MOD_ID, "cell_processor"),
                FabricBlockEntityTypeBuilder.create(CellProcessorBlockEntity::new, ModBlocks.CELL_PROCESSOR).build(null));

        SEQUENCER_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MoreRealisticGeneEditing.MOD_ID, "sequencer"),
                FabricBlockEntityTypeBuilder.create(SequencerBlockEntity::new, ModBlocks.SEQUENCER).build(null));

        PCR_MACHINE = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MoreRealisticGeneEditing.MOD_ID, "pcr_machine"),
                FabricBlockEntityTypeBuilder.create(PcrMachineBlockEntity::new, ModBlocks.PCR_MACHINE).build(null));

        INCUBATOR = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MoreRealisticGeneEditing.MOD_ID, "incubator"),
                FabricBlockEntityTypeBuilder.create(IncubatorBlockEntity::new, ModBlocks.INCUBATOR).build(null));

        ELECTROPHORESIS_TANK = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MoreRealisticGeneEditing.MOD_ID, "electrophoresis_tank"),
                FabricBlockEntityTypeBuilder.create(ElectrophoresisTankBlockEntity::new, ModBlocks.ELECTROPHORESIS_TANK).build(null));

        ETHICS_CASE_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MoreRealisticGeneEditing.MOD_ID, "ethics_case_block"),
                FabricBlockEntityTypeBuilder.create(EthicsCaseBlockEntity::new, ModBlocks.ETHICS_CASE_BLOCK).build(null));
    }
}
