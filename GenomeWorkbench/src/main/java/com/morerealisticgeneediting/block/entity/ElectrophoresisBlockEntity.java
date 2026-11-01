package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.*;
import com.morerealisticgeneediting.item.*;
import com.morerealisticgeneediting.screen.equipment.ElectrophoresisScreenHandler;
import com.morerealisticgeneediting.util.StackData;
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

/** Virtual gel: explicit fragment metadata and fictional migration, never inferred sequence identity. */
public class ElectrophoresisBlockEntity extends LabEquipmentBlockEntity {
    public static final int GEL_SLOT=0,BUFFER_SLOT=1,LADDER_SLOT=2,STAIN_SLOT=3,LOADING_DYE_SLOT=4,
            SAMPLE_START_SLOT=5,MAX_SAMPLE_SLOTS=8,LANES=9,MAX_BANDS=20,SLOTS=13;
    public enum ElectrophoresisPhase { IDLE, PREPARING, LOADING, RUNNING, STAINING, VISUALIZING, COMPLETE }
    private ElectrophoresisPhase phase=ElectrophoresisPhase.IDLE;
    private int voltage,targetVoltage=48,runTimeRemaining,totalRunTime=400;
    private float gelPercentage=1;
    private boolean gelReady;
    private final float[][] bandPositions=new float[LANES][MAX_BANDS];
    private final int[] bandCounts=new int[LANES];
    private final List<BandPattern> bandPatterns=new ArrayList<>();
    private final DefaultedList<ItemStack> reserved=DefaultedList.ofSize(SLOTS,ItemStack.EMPTY);
    private final NbtCompound[] sampleSnapshots=new NbtCompound[MAX_SAMPLE_SLOTS];
    private EquipmentSpecs.ElectrophoresisSpec spec;
    private final PropertyDelegate measurements=new PropertyDelegate(){
        public int size(){return ElectrophoresisScreenHandler.PROPERTY_COUNT;}
        public void set(int index,int value){} // Authoritative server observations.
        public int get(int index){
            if(index>=7&&index<7+LANES)return bandCounts[index-7];
            int offset=index-7-LANES;
            if(offset>=0&&offset<LANES*MAX_BANDS)return Math.round(bandPositions[offset/MAX_BANDS][offset%MAX_BANDS]*100);
            return switch(index){case 0->progress;case 1->maxProgress;case 2->voltage;case 3->runTimeRemaining;
                case 4->Math.round(gelPercentage*10);case 5->tier.getLevel();case 6->isProcessing?1:0;
                case ElectrophoresisScreenHandler.PHASE_INDEX->phase.ordinal();default->0;};
        }
    };
    public ElectrophoresisBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state,SLOTS);updateSpec();}
    private void updateSpec(){spec=EquipmentSpecs.getElectrophoresisSpec(tier);}
    @Override public void setTier(EquipmentTier tier){super.setTier(tier);updateSpec();if(!isProcessing)targetVoltage=Math.min(targetVoltage,spec.maxVoltage());}
    @Override protected boolean isInputSlot(int slot){return slot>=0&&slot<SLOTS;}
    @Override protected boolean isOutputSlot(int slot){return false;}
    @Override public boolean isValid(int slot,ItemStack stack){
        if(isProcessing||stack.isEmpty()||slot<0||slot>=SLOTS)return false;
        return switch(slot){
            case GEL_SLOT->stack.isOf(ModItems.AGAROSE_GEL);
            case BUFFER_SLOT->stack.isOf(LabEquipmentItems.TAE_BUFFER)||stack.isOf(LabEquipmentItems.TBE_BUFFER);
            case LADDER_SLOT->getLadderSizes(stack).length>0;
            case STAIN_SLOT->stack.isOf(LabEquipmentItems.SYBR_SAFE)||stack.isOf(LabEquipmentItems.ETHIDIUM_BROMIDE);
            case LOADING_DYE_SLOT->stack.isOf(LabEquipmentItems.LOADING_DYE);
            default->isSample(stack)&&fragments(stack).length>0;
        };
    }
    private boolean isSample(ItemStack stack){return stack.isOf(ModItems.DNA_SAMPLE)||stack.isOf(ModItems.GENOME_SAMPLE)||stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT);}
    private int[] fragments(ItemStack sample){NbtCompound data=StackData.read(sample);return MeasurementModels.fragmentSizes(data.getIntArray("FragmentSizes"),data.getInt("FragmentLength"));}
    @Override protected boolean canProcess(){
        if(phase!=ElectrophoresisPhase.IDLE||isProcessing||reserved.stream().anyMatch(stack->!stack.isEmpty()))return false;
        for(int slot=0;slot<SAMPLE_START_SLOT;slot++)if(!isValid(slot,getStack(slot)))return false;
        boolean any=false;
        for(int slot=SAMPLE_START_SLOT;slot<SLOTS;slot++)if(!getStack(slot).isEmpty()){
            if(!isValid(slot,getStack(slot)))return false;any=true;
        }
        return any;
    }
    public void startElectrophoresis(){
        if(world==null||world.isClient||!canProcess())return;
        clearResults();
        addBands(0,getLadderSizes(getStack(LADDER_SLOT)));
        for(int sample=0;sample<MAX_SAMPLE_SLOTS;sample++){
            ItemStack stack=getStack(SAMPLE_START_SLOT+sample);
            if(!stack.isEmpty()){sampleSnapshots[sample]=StackData.read(stack);addBands(sample+1,fragments(stack));}
        }
        for(int slot=0;slot<SLOTS;slot++)if(!getStack(slot).isEmpty())reserved.set(slot,getStack(slot).split(1));
        phase=ElectrophoresisPhase.PREPARING;progress=0;maxProgress=40;voltage=0;runTimeRemaining=0;
        setActive(true);markDirty();
    }
    private void addBands(int lane,int[] sizes){
        bandCounts[lane]=Math.min(MAX_BANDS,sizes.length);
        for(int band=0;band<bandCounts[lane];band++)bandPatterns.add(new BandPattern(lane,band,sizes[band],1));
    }
    @Override public void tick(){
        if(world==null||world.isClient||isRemoved())return;
        if(!isProcessing){if(reserved.stream().anyMatch(stack->!stack.isEmpty())||(phase!=ElectrophoresisPhase.IDLE&&phase!=ElectrophoresisPhase.COMPLETE))reset();return;}
        progress++;
        if(phase==ElectrophoresisPhase.RUNNING){
            runTimeRemaining=Math.max(0,totalRunTime-progress);
            for(BandPattern band:bandPatterns)bandPositions[band.lane][band.bandIndex]=MeasurementModels.bandPosition(
                    band.size,voltage/(float)Math.max(1,spec.maxVoltage()),Math.min(1,(float)progress/totalRunTime),gelPercentage);
        }
        if(progress>=maxProgress)switch(phase){
            case PREPARING->{gelReady=true;phase=ElectrophoresisPhase.LOADING;progress=0;maxProgress=20;}
            case LOADING->{phase=ElectrophoresisPhase.RUNNING;voltage=targetVoltage;progress=0;maxProgress=totalRunTime;runTimeRemaining=totalRunTime;}
            case RUNNING->{voltage=0;phase=ElectrophoresisPhase.STAINING;progress=0;maxProgress=20;}
            case STAINING->{phase=ElectrophoresisPhase.VISUALIZING;progress=0;maxProgress=20;}
            case VISUALIZING->completeProcess();
            default->reset();
        }
        markDirty();
    }
    @Override protected void completeProcess(){
        reserved.replaceAll(stack->ItemStack.EMPTY);phase=ElectrophoresisPhase.COMPLETE;progress=maxProgress;
        voltage=0;runTimeRemaining=0;setActive(false);markDirty();
    }
    public void reset(){
        if(world==null||world.isClient)return;
        refundReserved();clearResults();phase=ElectrophoresisPhase.IDLE;voltage=0;runTimeRemaining=0;progress=0;maxProgress=40;setActive(false);markDirty();
    }
    private void clearResults(){
        gelReady=false;bandPatterns.clear();Arrays.fill(bandCounts,0);Arrays.fill(sampleSnapshots,null);
        for(float[] positions:bandPositions)Arrays.fill(positions,0);
    }
    private void refundReserved(){
        for(int slot=0;slot<SLOTS;slot++){
            ItemStack refund=reserved.get(slot);if(refund.isEmpty())continue;ItemStack current=getStack(slot);
            if(current.isEmpty())inventory.set(slot,refund);
            else if(ItemStack.areItemsAndComponentsEqual(current,refund)&&current.getCount()+refund.getCount()<=current.getMaxCount())current.increment(refund.getCount());
            else ItemScatterer.spawn(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,refund);
            reserved.set(slot,ItemStack.EMPTY);
        }
    }
    @Override public void dropContents(World world,BlockPos pos){ItemScatterer.spawn(world,pos,reserved);reserved.replaceAll(stack->ItemStack.EMPTY);super.dropContents(world,pos);}
    private int[] getLadderSizes(ItemStack ladder){
        if(ladder.isOf(LabEquipmentItems.DNA_LADDER_100BP))return new int[]{100,200,300,400,500,600,700,800,900,1000};
        if(ladder.isOf(LabEquipmentItems.DNA_LADDER_1KB))return new int[]{250,500,750,1000,1500,2000,2500,3000,4000,5000,6000,8000,10000};
        if(ladder.isOf(LabEquipmentItems.DNA_LADDER_1KB_PLUS))return new int[]{100,200,300,400,500,650,850,1000,1650,2000,3000,4000,5000,6000,8000,10000,12000};
        if(ladder.isOf(ModItems.DNA_LADDER))return new int[]{500,1000,2000,3000,5000};
        return new int[0];
    }
    public ElectrophoresisPhase getPhase(){return phase;}public int getVoltage(){return voltage;}
    public int getTargetVoltage(){return targetVoltage;}public int getRunTimeRemaining(){return runTimeRemaining;}
    public float getGelPercentage(){return gelPercentage;}public boolean isGelReady(){return gelReady;}
    public float[][] getBandPositions(){return Arrays.stream(bandPositions).map(float[]::clone).toArray(float[][]::new);}
    public int[] getBandCounts(){return bandCounts.clone();}public List<BandPattern> getBandPatterns(){return List.copyOf(bandPatterns);}
    public void setTargetVoltage(int value){if(!isProcessing){targetVoltage=Math.max(0,Math.min(spec.maxVoltage(),value));markDirty();}}
    public void setGelPercentage(float value){if(!isProcessing&&Float.isFinite(value)){gelPercentage=Math.max(.5f,Math.min(3,value));markDirty();}}
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){
        super.writeNbt(nbt,lookup);nbt.putString("Phase",phase.name());nbt.putInt("TargetVoltage",targetVoltage);nbt.putInt("Voltage",voltage);
        nbt.putInt("RunTimeRemaining",runTimeRemaining);nbt.putInt("TotalRunTime",totalRunTime);nbt.putFloat("GelPercentage",gelPercentage);nbt.putBoolean("GelReady",gelReady);
        NbtCompound held=new NbtCompound();Inventories.writeNbt(held,reserved,lookup);nbt.put("Reserved",held);
        NbtList patterns=new NbtList();
        for(BandPattern band:bandPatterns){NbtCompound b=new NbtCompound();b.putInt("Lane",band.lane);b.putInt("Index",band.bandIndex);b.putInt("Size",band.size);
            b.putFloat("Position",bandPositions[band.lane][band.bandIndex]);patterns.add(b);}
        nbt.put("MeasuredBands",patterns);NbtList samples=new NbtList();
        for(int lane=0;lane<sampleSnapshots.length;lane++)if(sampleSnapshots[lane]!=null){NbtCompound s=new NbtCompound();s.putInt("Lane",lane);s.put("Sample",sampleSnapshots[lane].copy());samples.add(s);}
        nbt.put("SampleSnapshots",samples);
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){
        super.readNbt(nbt,lookup);updateSpec();clearResults();
        try{phase=ElectrophoresisPhase.valueOf(nbt.getString("Phase"));}catch(IllegalArgumentException e){phase=ElectrophoresisPhase.IDLE;}
        targetVoltage=Math.max(0,Math.min(spec.maxVoltage(),nbt.getInt("TargetVoltage")));voltage=Math.max(0,Math.min(spec.maxVoltage(),nbt.getInt("Voltage")));
        totalRunTime=Math.max(1,Math.min(2000,nbt.getInt("TotalRunTime")));runTimeRemaining=Math.max(0,Math.min(totalRunTime,nbt.getInt("RunTimeRemaining")));
        float pct=nbt.getFloat("GelPercentage");gelPercentage=Float.isFinite(pct)?Math.max(.5f,Math.min(3,pct)):1;gelReady=nbt.getBoolean("GelReady");
        reserved.replaceAll(stack->ItemStack.EMPTY);Inventories.readNbt(nbt.getCompound("Reserved"),reserved,lookup);
        NbtList patterns=nbt.getList("MeasuredBands",NbtElement.COMPOUND_TYPE);
        for(int i=0;i<Math.min(LANES*MAX_BANDS,patterns.size());i++){
            NbtCompound b=patterns.getCompound(i);int lane=b.getInt("Lane"),band=b.getInt("Index"),size=b.getInt("Size");float position=b.getFloat("Position");
            if(lane<0||lane>=LANES||band<0||band>=MAX_BANDS||size<1||size>1_000_000||!Float.isFinite(position))continue;
            bandPatterns.add(new BandPattern(lane,band,size,1));bandCounts[lane]=Math.max(bandCounts[lane],band+1);bandPositions[lane][band]=Math.max(0,Math.min(100,position));
        }
        NbtList samples=nbt.getList("SampleSnapshots",NbtElement.COMPOUND_TYPE);
        for(int i=0;i<Math.min(MAX_SAMPLE_SLOTS,samples.size());i++){NbtCompound s=samples.getCompound(i);int lane=s.getInt("Lane");if(lane>=0&&lane<MAX_SAMPLE_SLOTS)sampleSnapshots[lane]=s.getCompound("Sample").copy();}
        boolean held=reserved.stream().anyMatch(stack->!stack.isEmpty());
        isProcessing=held&&phase!=ElectrophoresisPhase.IDLE&&phase!=ElectrophoresisPhase.COMPLETE;
        if(!held&&phase!=ElectrophoresisPhase.COMPLETE){phase=ElectrophoresisPhase.IDLE;clearResults();}
        progress=Math.max(0,Math.min(maxProgress,progress));
    }
    @Override public Text getDisplayName(){return Text.translatable(switch(tier){case BASIC->"block.morerealisticgeneediting.portable_electrophoresis";case ADVANCED->"block.morerealisticgeneediting.vertical_electrophoresis";case ELITE->"block.morerealisticgeneediting.auto_electrophoresis";});}
    @Override public ScreenHandler createMenu(int id,PlayerInventory inventory,PlayerEntity player){return new ElectrophoresisScreenHandler(id,inventory,this,measurements);}
    public record BandPattern(int lane,int bandIndex,int size,float intensity){}
}
