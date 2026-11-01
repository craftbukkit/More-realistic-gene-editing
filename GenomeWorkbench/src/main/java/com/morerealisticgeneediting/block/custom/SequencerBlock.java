package com.morerealisticgeneediting.block.custom;
import com.mojang.serialization.MapCodec;
import com.morerealisticgeneediting.block.MachineBlock;
import com.morerealisticgeneediting.block.entity.ModBlockEntities;
public class SequencerBlock extends MachineBlock {
 public static final MapCodec<SequencerBlock> CODEC=createCodec(SequencerBlock::new);
 public SequencerBlock(Settings settings){super(settings,()->ModBlockEntities.SEQUENCER_BLOCK_ENTITY);}
 @Override protected MapCodec<SequencerBlock> getCodec(){return CODEC;}
}
