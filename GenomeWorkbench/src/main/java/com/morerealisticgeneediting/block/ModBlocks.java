package com.morerealisticgeneediting.block;
import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import com.morerealisticgeneediting.block.custom.*;
import com.morerealisticgeneediting.item.ModItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
public final class ModBlocks {
 private static final List<Block> BLOCKS=new ArrayList<>();
 public static final Block CELL_PROCESSOR=register("cell_processor",new CellProcessorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f)));
 public static final Block SEQUENCER=register("sequencer",new SequencerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f)));
 public static final Block PCR_MACHINE=register("pcr_machine",new PcrMachineBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f)));
 public static final Block INCUBATOR=register("incubator",new IncubatorBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f)));
 public static final Block ELECTROPHORESIS_TANK=register("electrophoresis_tank",new ElectrophoresisTankBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f)));
 public static final Block ETHICS_CASE_BLOCK=register("ethics_case_block",new EthicsCaseBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(4.0f)));
 public static final Block SEQUENCER_BLOCK=SEQUENCER;
 private static Block register(String name,Block block){
  Identifier id=Identifier.of(MoreRealisticGeneEditing.MOD_ID,name);
  Registry.register(Registries.BLOCK,id,block);
  Registry.register(Registries.ITEM,id,new BlockItem(block,new Item.Settings()));
  BLOCKS.add(block);return block;
 }
 public static void registerModBlocks(){ItemGroupEvents.modifyEntriesEvent(ModItems.MRGE_GROUP_KEY).register(e->BLOCKS.forEach(e::add));}
}
