package com.morerealisticgeneediting.block.custom;
import com.mojang.serialization.MapCodec;
import com.morerealisticgeneediting.block.MachineBlock;
import com.morerealisticgeneediting.block.entity.ModBlockEntities;
public class IncubatorBlock extends MachineBlock {
 public static final MapCodec<IncubatorBlock> CODEC=createCodec(IncubatorBlock::new);
 public IncubatorBlock(Settings settings){super(settings,()->ModBlockEntities.INCUBATOR);}
 @Override protected MapCodec<IncubatorBlock> getCodec(){return CODEC;}
}
