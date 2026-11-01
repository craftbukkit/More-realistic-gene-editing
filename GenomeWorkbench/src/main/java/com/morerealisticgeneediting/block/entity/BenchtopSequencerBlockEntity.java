package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.*;
import com.morerealisticgeneediting.genome.*;
import com.morerealisticgeneediting.genome.sequencing.SequencingSimulator;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.equipment.SequencerScreenHandler;
import com.morerealisticgeneediting.util.StackData;
import com.morerealisticgeneediting.project.ServerProjectManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

/** Bounded two-channel sequencing of immutable virtual-sample snapshots. */
public class BenchtopSequencerBlockEntity extends LabEquipmentBlockEntity {
    public static final int LIBRARY_SLOT_A=0,LIBRARY_SLOT_B=1,FLOWCELL_SLOT_A=2,FLOWCELL_SLOT_B=3,REAGENT_SLOT=4,OUTPUT_SLOT_A=5,OUTPUT_SLOT_B=6;
    public enum SequencingPhase{IDLE,LOADING,CLUSTER_GENERATION,SEQUENCING,ANALYZING,COMPLETE}
    private static final int MAX_REGION_BASES=4096;
    private int readLength=100,targetCoverage=30;
    private boolean pairedEnd=true;
    private final Run[] runs={new Run(),new Run()};
    private static final class Run{
        SequencingPhase phase=SequencingPhase.IDLE;
        int progress,maxProgress,cycle;
        long reads,bases;
        float meanQuality=Float.NaN,q30Percent=Float.NaN;
        float[] qualities=new float[0];
        UUID actor,sample;long revision;boolean concordant;
        // Three held consumables and the immutable result that will be delivered once.
        DefaultedList<ItemStack> held=DefaultedList.ofSize(4,ItemStack.EMPTY);
        boolean busy(){return phase!=SequencingPhase.IDLE&&phase!=SequencingPhase.COMPLETE;}
    }
    private final PropertyDelegate properties=new PropertyDelegate(){
        public int get(int i){Run r=runs[0].busy()?runs[0]:runs[1];return switch(i){
            case 0->r.progress;case 1->r.maxProgress;case 2->(int)Math.min(32767,r.reads);
            case 3->Float.isFinite(r.meanQuality)?Math.round(r.meanQuality):-1;case 4->tier.getLevel();case 5->busy()?1:0;default->0;};}
        public void set(int i,int value){}
        public int size(){return 6;}
    };
    public BenchtopSequencerBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state,7);tier=EquipmentTier.ADVANCED;}
    private boolean busy(){return runs[0].busy()||runs[1].busy();}
    @Override protected boolean isInputSlot(int slot){return slot>=0&&slot<=4;}
    @Override protected boolean isOutputSlot(int slot){return slot==5||slot==6;}
    @Override public boolean isValid(int slot,ItemStack stack){return switch(slot){
        case 0,1->stack.isOf(ModItems.SEQUENCING_LIBRARY)||stack.isOf(ModItems.GENOME_SAMPLE)||stack.isOf(ModItems.DNA_SAMPLE)||stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT);
        case 2,3->stack.isOf(ModItems.SEQUENCING_FLOW_CELL);
        case 4->stack.isOf(ModItems.SEQUENCING_REAGENT);
        default->false;
    };}
    @Override protected boolean canProcess(){return canStart(0)||canStart(1);}
    private boolean canStart(int channel){return !runs[channel].busy()&&isValid(channel,getStack(channel))&&isValid(channel+2,getStack(channel+2))&&isValid(4,getStack(4))&&getStack(channel+5).isEmpty();}
    public void startSequencing(boolean flowCellA,PlayerEntity player){
        int channel=flowCellA?0:1;
        if(world==null||world.isClient||player==null||!canPlayerUse(player)||!canStart(channel))return;
        NbtCompound source=StackData.read(getStack(channel));
        if(!source.containsUuid("GenomeId"))return;
        Genome genome=GenomeRepository.get(world.getServer()).findOwned(source.getUuid("GenomeId"),player.getUuid()).orElse(null);
        if(genome==null||genome.getTotalLength()<1)return;
        int regionLength=(int)Math.min(MAX_REGION_BASES,genome.getTotalLength());
        long seed=world.random.nextLong();
        SequencingSimulator.SequencingResult result=new SequencingSimulator(seed).runSequencing(genome,
            SequencingSimulator.Technology.ILLUMINA_PE150,0L,regionLength,targetCoverage,readLength,pairedEnd);
        Run run=new Run();
        run.actor=player.getUuid();run.sample=genome.getUUID();run.revision=genome.getRevision();
        if(genome.getTotalLength()<=MAX_REGION_BASES){
            SequenceVerification.Result verification=SequenceVerification.verify(genome,result.reads(),10,3);
            run.concordant=verification.passed()&&verification.coveredBases()==genome.getTotalLength();
        }
        run.reads=result.stats().totalReads();run.bases=result.stats().totalBases();
        run.meanQuality=(float)result.stats().meanQuality();run.q30Percent=(float)result.stats().q30Percentage();
        run.qualities=MeasurementModels.cycleQuality(result.reads().stream().map(SequencingSimulator.SequencingRead::qualityScores).toList(),readLength,pairedEnd);
        ItemStack output=new ItemStack(ModItems.READ_SET_ITEM);
        NbtCompound data=new NbtCompound();
        data.putString("Type","sequencing_data");data.putString("SimulationModel","virtual-short-read-v1");
        data.putUuid("GenomeId",genome.getUUID());data.putUuid("Owner",genome.getOwner());data.putLong("ReferenceRevision",genome.getRevision());
        data.putLong("RegionStart",0);data.putInt("RegionLength",regionLength);data.putInt("ReadLength",readLength);data.putBoolean("PairedEnd",pairedEnd);
        data.putLong("TotalReads",run.reads);data.putLong("TotalBases",run.bases);data.putFloat("MeanQuality",run.meanQuality);data.putFloat("Q30Percent",run.q30Percent);
        data.putDouble("Coverage",result.stats().coverageDepth());data.putString("VariantStatus","not_assessed");
        data.putString("VirtualConcordance",run.concordant?"passed_against_stored_sample":"not_passed");
        data.putString("ReadStorage","preview_only");data.putLong("SimulationSeed",seed);
        NbtList preview=new NbtList();
        for(int i=0;i<Math.min(8,result.reads().size());i++)preview.add(NbtString.of(result.reads().get(i).toFastq()));
        data.put("FastqPreview",preview);StackData.write(output,data);
        run.held.set(0,getStack(channel).split(1));run.held.set(1,getStack(channel+2).split(1));run.held.set(2,getStack(REAGENT_SLOT).split(1));run.held.set(3,output);
        run.phase=SequencingPhase.LOADING;run.maxProgress=40;runs[channel]=run;setActive(true);markDirty();
    }
    @Override public void tick(){
        if(world==null||world.isClient)return;
        for(int channel=0;channel<2;channel++){
            Run run=runs[channel];if(!run.busy())continue;
            if(run.held.get(3).isEmpty()){refund(channel);continue;}
            if(run.phase==SequencingPhase.ANALYZING&&run.progress>=run.maxProgress){complete(channel);continue;}
            run.progress++;
            if(run.phase==SequencingPhase.SEQUENCING)run.cycle=Math.min(run.qualities.length,run.progress);
            if(run.progress>=run.maxProgress){
                switch(run.phase){
                    case LOADING->{run.phase=SequencingPhase.CLUSTER_GENERATION;run.maxProgress=40;run.progress=0;}
                    case CLUSTER_GENERATION->{run.phase=SequencingPhase.SEQUENCING;run.maxProgress=Math.max(1,run.qualities.length);run.progress=0;}
                    case SEQUENCING->{run.phase=SequencingPhase.ANALYZING;run.maxProgress=40;run.progress=0;}
                    case ANALYZING->complete(channel);
                    default->{}
                }
            }
            markDirty();
        }
        if(isProcessing()!=busy())setActive(busy());
    }
    private void complete(int channel){
        Run run=runs[channel];ItemStack output=run.held.get(3);ItemStack current=getStack(channel+5);
        if(output.isEmpty())return;
        if(!current.isEmpty()&&(!ItemStack.areItemsAndComponentsEqual(current,output)||current.getCount()+output.getCount()>current.getMaxCount()))return;
        if(current.isEmpty())setStack(channel+5,output.copy());else current.increment(output.getCount());
        run.held=DefaultedList.ofSize(4,ItemStack.EMPTY);run.phase=SequencingPhase.COMPLETE;
        if(run.concordant&&run.actor!=null&&run.sample!=null){
            var currentSample=GenomeRepository.get(world.getServer()).findOwned(run.sample,run.actor);
            if(currentSample.isPresent()&&currentSample.get().getRevision()==run.revision)
                ServerProjectManager.recordVerified(world.getServer(),run.actor,run.sample,run.revision);
        }
        markDirty();
    }
    public void resetFlowCell(boolean isA){if(world==null||world.isClient)return;refund(isA?0:1);}
    private void refund(int channel){
        Run run=runs[channel];int[] slots={channel,channel+2,REAGENT_SLOT};
        for(int i=0;i<3;i++){
            ItemStack held=run.held.get(i);if(held.isEmpty())continue;
            ItemStack current=getStack(slots[i]);
            if(current.isEmpty())setStack(slots[i],held.copy());
            else if(ItemStack.areItemsAndComponentsEqual(current,held)&&current.getCount()+held.getCount()<=current.getMaxCount())current.increment(held.getCount());
            else ItemScatterer.spawn(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,held.copy());
        }
        runs[channel]=new Run();setActive(busy());markDirty();
    }
    @Override public void dropContents(World world,BlockPos pos){
        for(Run run:runs)for(int i=0;i<3;i++)if(!run.held.get(i).isEmpty())ItemScatterer.spawn(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,run.held.get(i));
        runs[0]=new Run();runs[1]=new Run();super.dropContents(world,pos);
    }
    public SequencingPhase getPhaseA(){return runs[0].phase;}public SequencingPhase getPhaseB(){return runs[1].phase;}
    public int getProgressA(){return runs[0].progress;}public int getProgressB(){return runs[1].progress;}
    public int getMaxProgressA(){return runs[0].maxProgress;}public int getMaxProgressB(){return runs[1].maxProgress;}
    public int getReadLength(){return readLength;}public boolean isPairedEnd(){return pairedEnd;}
    public long getTotalReadsA(){return runs[0].reads;}public long getTotalReadsB(){return runs[1].reads;}
    public long getTotalBasesA(){return runs[0].bases;}public long getTotalBasesB(){return runs[1].bases;}
    public int getCurrentCycleA(){return runs[0].cycle;}public int getCurrentCycleB(){return runs[1].cycle;}
    public float getMeanQualityA(){return runs[0].meanQuality;}public float getMeanQualityB(){return runs[1].meanQuality;}
    public float getQ30PercentA(){return runs[0].q30Percent;}public float getQ30PercentB(){return runs[1].q30Percent;}
    public float[] getQualityPerCycleA(){return runs[0].qualities.clone();}public float[] getQualityPerCycleB(){return runs[1].qualities.clone();}
    public void setReadLength(int length){if(!busy()){readLength=Math.clamp(length,50,150);markDirty();}}
    public void setPairedEnd(boolean value){if(!busy()){pairedEnd=value;markDirty();}}
    @Override protected void completeProcess(){}
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);nbt.putInt("ReadLength",readLength);nbt.putBoolean("PairedEnd",pairedEnd);nbt.putInt("TargetCoverage",targetCoverage);
        for(int channel=0;channel<2;channel++){
            Run run=runs[channel];NbtCompound value=new NbtCompound();Inventories.writeNbt(value,run.held,registries);
            if(run.actor!=null)value.putUuid("Actor",run.actor);if(run.sample!=null)value.putUuid("Sample",run.sample);
            value.putLong("Revision",run.revision);value.putBoolean("Concordant",run.concordant);
            value.putString("Phase",run.phase.name());value.putInt("Progress",run.progress);value.putInt("MaxProgress",run.maxProgress);value.putInt("Cycle",run.cycle);
            value.putLong("Reads",run.reads);value.putLong("Bases",run.bases);value.putFloat("MeanQuality",run.meanQuality);value.putFloat("Q30",run.q30Percent);
            int[] bits=new int[run.qualities.length];for(int i=0;i<bits.length;i++)bits[i]=Float.floatToIntBits(run.qualities[i]);value.putIntArray("QualityCycles",bits);
            nbt.put("Run"+channel,value);
        }
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);readLength=Math.clamp(nbt.getInt("ReadLength"),50,150);pairedEnd=nbt.getBoolean("PairedEnd");targetCoverage=Math.clamp(nbt.getInt("TargetCoverage"),1,50);
        for(int channel=0;channel<2;channel++){
            NbtCompound value=nbt.getCompound("Run"+channel);Run run=new Run();Inventories.readNbt(value,run.held,registries);
            run.actor=value.containsUuid("Actor")?value.getUuid("Actor"):null;run.sample=value.containsUuid("Sample")?value.getUuid("Sample"):null;
            run.revision=Math.max(0,value.getLong("Revision"));run.concordant=value.getBoolean("Concordant");
            try{run.phase=SequencingPhase.valueOf(value.getString("Phase"));}catch(IllegalArgumentException ignored){run.phase=SequencingPhase.IDLE;}
            run.maxProgress=Math.clamp(value.getInt("MaxProgress"),0,300);run.progress=Math.clamp(value.getInt("Progress"),0,run.maxProgress);
            run.cycle=Math.clamp(value.getInt("Cycle"),0,300);run.reads=Math.max(0,value.getLong("Reads"));run.bases=Math.max(0,value.getLong("Bases"));
            run.meanQuality=value.getFloat("MeanQuality");run.q30Percent=value.getFloat("Q30");
            int[] bits=value.getIntArray("QualityCycles");run.qualities=new float[Math.min(300,bits.length)];for(int i=0;i<run.qualities.length;i++)run.qualities[i]=Float.intBitsToFloat(bits[i]);
            if(run.busy()&&run.held.stream().allMatch(ItemStack::isEmpty))run.phase=SequencingPhase.IDLE;
            runs[channel]=run;
        }
    }
    @Override public Text getDisplayName(){return Text.translatable("block.morerealisticgeneediting.benchtop_sequencer");}
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity player){return new SequencerScreenHandler(syncId,inventory,this,properties);}
}
