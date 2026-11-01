package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.*;
import com.morerealisticgeneediting.item.*;
import com.morerealisticgeneediting.screen.equipment.ElectroporatorScreenHandler;
import com.morerealisticgeneediting.util.StackData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** In-game delivery simulation. Delivery and survival never establish genomic editing. */
public class ElectroporatorBlockEntity extends LabEquipmentBlockEntity {
    public static final int CELLS_SLOT=0,DNA_SLOT=1,BUFFER_SLOT=2,CUVETTE_SLOT=3,OUTPUT_SLOT=4;
    private final EquipmentSpecs.ElectroporatorSpec spec=EquipmentSpecs.GENE_PULSER_XCELL;
    private int voltage=1000,capacitance=25,resistance=200,cuvetteGap=2,pulseCount=1,pulseDuration=5;
    private boolean squareWave,isPulsing,hasResults;
    private int pulseProgress,pulseTicks;
    private float calculatedFieldStrength,calculatedTimeConstant;
    private float transformationEfficiency=Float.NaN,cellViability=Float.NaN,deliveryEfficiency=Float.NaN;
    private DefaultedList<ItemStack> reserved=DefaultedList.ofSize(4,ItemStack.EMPTY);
    private final PropertyDelegate properties=new PropertyDelegate(){
        public int get(int i){return switch(i){case 0->voltage;case 1->pulseDuration;case 2->pulseCount;case 3->pulseProgress;case 4->pulseTicks;case 5->tier.getLevel();default->0;};}
        public void set(int i,int value){}
        public int size(){return 6;}
    };
    public ElectroporatorBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state,5);calculateParameters();}
    private void calculateParameters(){calculatedFieldStrength=voltage/(cuvetteGap*.1f);calculatedTimeConstant=resistance*capacitance/1000f;}
    @Override protected boolean isInputSlot(int slot){return slot>=0&&slot<4;}
    @Override protected boolean isOutputSlot(int slot){return slot==OUTPUT_SLOT;}
    @Override public boolean isValid(int slot,ItemStack stack){return switch(slot){
        case CELLS_SLOT->stack.isOf(ModItems.CELL_CULTURE_FLASK)&&"virtual_cell_culture".equals(StackData.read(stack).getString("SampleType"));
        case DNA_SLOT->stack.isOf(ModItems.DNA_SAMPLE)||stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT)||stack.isOf(ModItems.RECOMBINANT_PLASMID);
        case BUFFER_SLOT->stack.isOf(LabEquipmentItems.ELECTROPORATION_BUFFER);
        case CUVETTE_SLOT->stack.isOf(ModItems.ELECTROPORATION_CUVETTE);
        default->false;
    };}
    @Override protected boolean canProcess(){
        if(isPulsing||reserved.stream().anyMatch(stack->!stack.isEmpty())||!getStack(OUTPUT_SLOT).isEmpty())return false;
        for(int i=0;i<4;i++)if(!isValid(i,getStack(i)))return false;
        return !StackData.read(getStack(CELLS_SLOT)).getString("CellType").isBlank();
    }
    public void pulse(){
        if(world==null||world.isClient||!canProcess())return;
        for(int i=0;i<4;i++)reserved.set(i,getStack(i).split(1));
        pulseProgress=0;pulseTicks=20*pulseCount;hasResults=false;isPulsing=true;
        // Dimensionless, explicitly fictional tuning. No cell-type-specific real-world protocol.
        MeasurementModels.DeliveryOutcome result=MeasurementModels.delivery(Math.min(1,calculatedFieldStrength/(spec.maxVoltage()/.1f))
                * Math.min(1,(squareWave?pulseDuration:calculatedTimeConstant)/100f),pulseCount);
        deliveryEfficiency=result.deliveryFraction();cellViability=result.survivingFraction();
        transformationEfficiency=Float.NaN;setActive(true);markDirty();
    }
    @Override public void tick(){
        if(world==null||world.isClient)return;
        if(!isPulsing){if(reserved.stream().anyMatch(stack->!stack.isEmpty()))cancel();else if(isProcessing())setActive(false);return;}
        if(pulseProgress<pulseTicks){pulseProgress++;markDirty();}
        if(pulseProgress>=pulseTicks)completePulse();
    }
    private void completePulse(){
        if(reserved.get(CELLS_SLOT).isEmpty()){isPulsing=false;setActive(false);return;}
        ItemStack output=reserved.get(CELLS_SLOT).copyWithCount(1);
        NbtCompound data=StackData.read(output);
        data.putString("Type","delivery_simulation");data.putString("DeliveryModel","unitless-game-v1");
        data.putFloat("DeliveryFraction",deliveryEfficiency);data.putFloat("CellViability",cellViability);
        data.putString("EditingStatus","not_assessed");data.remove("TransformationEfficiency");
        data.put("DeliveredConstruct",StackData.read(reserved.get(DNA_SLOT)));
        StackData.write(output,data);
        ItemStack existing=getStack(OUTPUT_SLOT);
        if(!canMerge(existing,output))return;
        if(existing.isEmpty())setStack(OUTPUT_SLOT,output);else existing.increment(output.getCount());
        reserved=DefaultedList.ofSize(4,ItemStack.EMPTY);isPulsing=false;hasResults=true;setActive(false);markDirty();
    }
    private static boolean canMerge(ItemStack existing,ItemStack output){return existing.isEmpty()||(ItemStack.areItemsAndComponentsEqual(existing,output)&&existing.getCount()+output.getCount()<=existing.getMaxCount());}
    @Override public void dropContents(World world,BlockPos pos){
        for(ItemStack item:reserved)if(!item.isEmpty())ItemScatterer.spawn(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,item);
        reserved=DefaultedList.ofSize(4,ItemStack.EMPTY);isPulsing=false;super.dropContents(world,pos);
    }
    public void cancel() {
        if(world==null||world.isClient)return;
        for(int i=0;i<4;i++) {
            ItemStack held=reserved.get(i);if(held.isEmpty())continue;
            ItemStack current=getStack(i);
            if(current.isEmpty())setStack(i,held.copy());
            else if(canMerge(current,held))current.increment(held.getCount());
            else ItemScatterer.spawn(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,held.copy());
        }
        reserved=DefaultedList.ofSize(4,ItemStack.EMPTY);isPulsing=false;hasResults=false;
        transformationEfficiency=deliveryEfficiency=cellViability=Float.NaN;setActive(false);markDirty();
    }
    @Override protected void completeProcess(){}
    public void setVoltage(int value){if(isPulsing)return;voltage=Math.clamp(value,spec.minVoltage(),spec.maxVoltage());calculateParameters();markDirty();}
    public void setCapacitance(int value){if(isPulsing)return;capacitance=Math.clamp(value,1,spec.maxCapacitance());calculateParameters();markDirty();}
    public void setResistance(int value){if(isPulsing)return;resistance=Math.clamp(value,50,1000);calculateParameters();markDirty();}
    public void setCuvetteGap(int value){if(isPulsing)return;for(int permitted:spec.cuvetteSizes())if(value==permitted){cuvetteGap=value;calculateParameters();markDirty();return;}}
    public void setSquareWave(boolean value){if(!isPulsing&&spec.hasSquareWave()){squareWave=value;markDirty();}}
    public void setPulseDuration(int value){if(!isPulsing){pulseDuration=Math.clamp(value,1,100);markDirty();}}
    public void setPulseCount(int value){if(!isPulsing){pulseCount=Math.clamp(value,1,10);markDirty();}}
    public int getVoltage(){return voltage;}public int getCapacitance(){return capacitance;}public int getResistance(){return resistance;}
    public int getCuvetteGap(){return cuvetteGap;}public boolean isSquareWave(){return squareWave;}public int getPulseCount(){return pulseCount;}
    public int getPulseDuration(){return pulseDuration;}public boolean isPulsing(){return isPulsing;}public int getPulseProgress(){return pulseProgress;}
    public float getFieldStrength(){return calculatedFieldStrength;}public float getTimeConstant(){return calculatedTimeConstant;}
    /** CFU per DNA mass requires colony counts and DNA mass; the pulse alone supplies neither. */
    public float getTransformationEfficiency(){return transformationEfficiency;}
    public float getDeliveryEfficiency(){return deliveryEfficiency;}public float getCellViability(){return cellViability;}public boolean hasResults(){return hasResults;}
    /** Retained compatibility; cell identity cannot select a validated experiment protocol. */
    @Deprecated public static int[] getSuggestedParams(String cellType){return new int[0];}
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);nbt.putInt("Voltage",voltage);nbt.putInt("Capacitance",capacitance);nbt.putInt("Resistance",resistance);nbt.putInt("CuvetteGap",cuvetteGap);
        nbt.putBoolean("SquareWave",squareWave);nbt.putInt("PulseCount",pulseCount);nbt.putInt("PulseDurationMs",pulseDuration);
        nbt.putBoolean("IsPulsing",isPulsing);nbt.putInt("PulseProgress",pulseProgress);nbt.putInt("PulseTicks",pulseTicks);
        nbt.putBoolean("HasResults",hasResults);nbt.putFloat("DeliveryFraction",deliveryEfficiency);nbt.putFloat("CellViability",cellViability);
        NbtCompound held=new NbtCompound();Inventories.writeNbt(held,reserved,registries);nbt.put("Reserved",held);
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);voltage=Math.clamp(nbt.getInt("Voltage"),spec.minVoltage(),spec.maxVoltage());capacitance=Math.clamp(nbt.getInt("Capacitance"),1,spec.maxCapacitance());
        resistance=Math.clamp(nbt.getInt("Resistance"),50,1000);int gap=nbt.getInt("CuvetteGap");cuvetteGap=gap==1||gap==2||gap==4?gap:2;
        squareWave=nbt.getBoolean("SquareWave");pulseCount=Math.clamp(nbt.getInt("PulseCount"),1,10);pulseDuration=Math.clamp(nbt.getInt("PulseDurationMs"),1,100);
        pulseTicks=Math.clamp(nbt.getInt("PulseTicks"),20,200);pulseProgress=Math.clamp(nbt.getInt("PulseProgress"),0,pulseTicks);
        reserved=DefaultedList.ofSize(4,ItemStack.EMPTY);Inventories.readNbt(nbt.getCompound("Reserved"),reserved,registries);
        isPulsing=nbt.getBoolean("IsPulsing")&&reserved.stream().noneMatch(ItemStack::isEmpty);hasResults=nbt.getBoolean("HasResults");
        deliveryEfficiency=nbt.getFloat("DeliveryFraction");cellViability=nbt.getFloat("CellViability");transformationEfficiency=Float.NaN;calculateParameters();
    }
    @Override public Text getDisplayName(){return Text.translatable("block.morerealisticgeneediting.electroporator");}
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity player){return new ElectroporatorScreenHandler(syncId,inventory,this,properties);}
}
