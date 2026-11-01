package com.morerealisticgeneediting.block.custom;
import com.mojang.serialization.MapCodec;
import com.morerealisticgeneediting.block.MachineBlock;
import com.morerealisticgeneediting.block.entity.ModBlockEntities;
public class PcrMachineBlock extends MachineBlock {
 public static final MapCodec<PcrMachineBlock> CODEC=createCodec(PcrMachineBlock::new);
 public PcrMachineBlock(Settings settings){super(settings,()->ModBlockEntities.PCR_MACHINE);}
 @Override protected MapCodec<PcrMachineBlock> getCodec(){return CODEC;}
}
