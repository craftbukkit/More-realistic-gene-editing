package com.morerealisticgeneediting.block.custom;
import com.mojang.serialization.MapCodec;
import com.morerealisticgeneediting.block.MachineBlock;
import com.morerealisticgeneediting.block.entity.ModBlockEntities;
public class ElectrophoresisTankBlock extends MachineBlock {
 public static final MapCodec<ElectrophoresisTankBlock> CODEC=createCodec(ElectrophoresisTankBlock::new);
 public ElectrophoresisTankBlock(Settings settings){super(settings,()->ModBlockEntities.ELECTROPHORESIS_TANK);}
 @Override protected MapCodec<ElectrophoresisTankBlock> getCodec(){return CODEC;}
}
