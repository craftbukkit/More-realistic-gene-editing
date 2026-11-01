package com.morerealisticgeneediting.block;
import com.mojang.serialization.MapCodec;
import com.morerealisticgeneediting.block.MachineBlock;
import com.morerealisticgeneediting.block.entity.ModBlockEntities;
public class CellProcessorBlock extends MachineBlock {
 public static final MapCodec<CellProcessorBlock> CODEC=createCodec(CellProcessorBlock::new);
 public CellProcessorBlock(Settings settings){super(settings,()->ModBlockEntities.CELL_PROCESSOR_BLOCK_ENTITY);}
 @Override protected MapCodec<CellProcessorBlock> getCodec(){return CODEC;}
}
