package com.morerealisticgeneediting.block;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.block.custom.ElectrophoresisTankBlock;
import com.morerealisticgeneediting.block.custom.IncubatorBlock;
import com.morerealisticgeneediting.block.custom.PcrMachineBlock;
import com.morerealisticgeneediting.block.custom.SequencerBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlocks {

    public static final Block CELL_PROCESSOR = registerBlock("cell_processor",
            new CellProcessorBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f).requiresTool()), ItemGroup.MISC);

    public static final Block SEQUENCER = registerBlock("sequencer",
            new SequencerBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f).requiresTool()), ItemGroup.MISC);

    public static final Block PCR_MACHINE = registerBlock("pcr_machine",
            new PcrMachineBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f).requiresTool()), ItemGroup.MISC);

    public static final Block INCUBATOR = registerBlock("incubator",
            new IncubatorBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f).requiresTool()), ItemGroup.MISC);

    public static final Block ELECTROPHORESIS_TANK = registerBlock("electrophoresis_tank",
            new ElectrophoresisTankBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f).requiresTool()), ItemGroup.MISC);

    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, new Identifier(MoreRealisticGeneEditing.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        return Registry.register(Registry.ITEM, new Identifier(MoreRealisticGeneEditing.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings().group(group)));
    }

    public static void registerModBlocks() {
        MoreRealisticGeneEditing.LOGGER.info("Registering ModBlocks for " + MoreRealisticGeneEditing.MOD_ID);
    }
}
