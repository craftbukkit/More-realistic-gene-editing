package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.item.GenomeSampleItem;
import com.morerealisticgeneediting.genome.GenomeRepository;
import com.morerealisticgeneediting.genome.sequencing.SequencingSimulator;
import com.morerealisticgeneediting.screen.SequencerScreenHandler;
import com.morerealisticgeneediting.util.StackData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class SequencerBlockEntity extends TimedMachineBlockEntity {
    public SequencerBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.SEQUENCER_BLOCK_ENTITY,pos,state,2,1); }
    @Override protected boolean acceptsInput(int slot,ItemStack stack) { return slot==0 && stack.isOf(ModItems.GENOME_SAMPLE) && GenomeSampleItem.getGenomeId(stack)!=null; }
    public void startSequencing(PlayerEntity player) {
        if(world==null || world.isClient || !canPlayerUse(player) || hasPendingJob()) return;
        var id=GenomeSampleItem.getGenomeId(getStack(0)); if(id==null)return;
        GenomeRepository.get(world.getServer()).findOwned(id,player.getUuid()).ifPresent(genome -> {
            if(!GenomeSampleItem.matchesSnapshot(getStack(0),genome))return;
            long length=genome.getTotalLength(); if(length<1 || length>10000)return;
            var result=new SequencingSimulator(id.getMostSignificantBits()^genome.getRevision()).runSequencing(genome,SequencingSimulator.Technology.PACBIO_HIFI,10,0,length);
            ItemStack output=derived(ModItems.READ_SET_ITEM,getStack(0),"simulated_sequencing");
            NbtCompound data=StackData.read(output);
            data.putLong("GenomeRevision",genome.getRevision()); data.putInt("ReadCount",result.stats().totalReads());
            data.putLong("TotalBases",result.stats().totalBases()); data.putLong("RegionStart",0);data.putLong("RegionLength",length);
            data.putDouble("MeanQuality",result.stats().meanQuality()); data.putDouble("Coverage",result.stats().coverageDepth());
            var verification=com.morerealisticgeneediting.genome.SequenceVerification.verify(genome,result.reads(),10,3);
            data.putBoolean("ConcordancePassed",verification.passed());data.putInt("ConcordanceCoveredBases",verification.coveredBases());
            data.putInt("ConcordanceMismatches",verification.mismatches());data.putString("VerificationScope","current_virtual_sample_sequence_only");
            data.putBoolean("VariantsAssessed",false);StackData.write(output,data);
            startJob(player,output,100,0);
        });
    }
    @Override protected void onJobCommitted(java.util.UUID owner,ItemStack output) {
        if(owner==null || world==null || world.getServer()==null)return;
        var id=GenomeSampleItem.getGenomeId(output); NbtCompound data=StackData.read(output);
        if(id!=null && data.getBoolean("ConcordancePassed") && data.getInt("ConcordanceCoveredBases")==data.getLong("RegionLength"))
            GenomeRepository.get(world.getServer()).findOwned(id,owner).filter(g->g.getRevision()==data.getLong("GenomeRevision")).ifPresent(g->
                com.morerealisticgeneediting.project.ServerProjectManager.recordVerified(world.getServer(),owner,id,g.getRevision()));
    }
    public static void tick(World world,BlockPos pos,BlockState state,SequencerBlockEntity entity) { entity.tickJob(); }
    @Override public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.sequencer"); }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inv,PlayerEntity player) { return new SequencerScreenHandler(syncId,inv,this,propertyDelegate); }
}
