package com.morerealisticgeneediting.block;
import com.mojang.serialization.MapCodec;
import com.morerealisticgeneediting.block.MachineBlock;
import com.morerealisticgeneediting.block.entity.ModBlockEntities;
public class EthicsCaseBlock extends MachineBlock {
 public static final MapCodec<EthicsCaseBlock> CODEC=createCodec(EthicsCaseBlock::new);
 public EthicsCaseBlock(Settings settings){super(settings,()->ModBlockEntities.ETHICS_CASE_BLOCK_ENTITY);}
 @Override protected MapCodec<EthicsCaseBlock> getCodec(){return CODEC;}
}
