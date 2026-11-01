package com.morerealisticgeneediting.screen.equipment;

import com.morerealisticgeneediting.block.entity.ElectrophoresisBlockEntity;
import com.morerealisticgeneediting.screen.slot.MachineSlot;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;

/** Thirteen physical input slots; the completed gel is a synchronized observation, not an output item. */
public class ElectrophoresisScreenHandler extends ScreenHandler {
    public static final int SLOTS=13,LANES=9,MAX_BANDS=20;
    public static final int PROGRESS_INDEX=0,MAX_PROGRESS_INDEX=1,VOLTAGE_INDEX=2,RUN_TIME_INDEX=3,
            GEL_PERCENT_INDEX=4,TIER_INDEX=5,IS_RUNNING_INDEX=6,PHASE_INDEX=7+LANES+LANES*MAX_BANDS,PROPERTY_COUNT=PHASE_INDEX+1;
    private final Inventory inventory;private final PropertyDelegate properties;
    public ElectrophoresisScreenHandler(int id,PlayerInventory player){this(id,player,new SimpleInventory(SLOTS),new ArrayPropertyDelegate(PROPERTY_COUNT));}
    public ElectrophoresisScreenHandler(int id,PlayerInventory player,Inventory inventory,PropertyDelegate properties){
        super(ModEquipmentScreenHandlers.ELECTROPHORESIS_SCREEN_HANDLER,id);checkSize(inventory,SLOTS);checkDataCount(properties,PROPERTY_COUNT);
        this.inventory=inventory;this.properties=properties;inventory.onOpen(player.player);
        for(int slot=0;slot<5;slot++)addSlot(new MachineSlot(inventory,slot,44+slot*18,17,false));
        for(int sample=0;sample<8;sample++)addSlot(new MachineSlot(inventory,5+sample,17+sample*18,53,false));
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(player,9+row*9+column,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(new Slot(player,column,8+column*18,142));
        addProperties(properties);
    }
    @Override public boolean onButtonClick(PlayerEntity player,int id){
        if(!canUse(player)||!(inventory instanceof ElectrophoresisBlockEntity machine)||player.getWorld().isClient)return false;
        switch(id){case 0->machine.startElectrophoresis();case 1->machine.reset();default->{return false;}}return true;
    }
    @Override public boolean canUse(PlayerEntity player){return inventory.canPlayerUse(player);}
    @Override public void onClosed(PlayerEntity player){super.onClosed(player);inventory.onClose(player);}
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(index<0||index>=slots.size()||!canUse(player))return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasStack()||!slot.canTakeItems(player))return ItemStack.EMPTY;
        ItemStack source=slot.getStack(),before=source.copy();
        if(index<SLOTS){if(!insertItem(source,SLOTS,slots.size(),true))return ItemStack.EMPTY;}
        else if(!insertItem(source,0,SLOTS,false))return ItemStack.EMPTY;
        if(source.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();slot.onTakeItem(player,source);return before;
    }
    public int getProgress(){return properties.get(0);}public int getMaxProgress(){return properties.get(1);}
    public float getProgressPercent(){return getMaxProgress()>0?(float)getProgress()/getMaxProgress():0;}
    public int getVoltage(){return properties.get(2);}public int getRunTime(){return properties.get(3);}
    public float getGelPercent(){return properties.get(4)/10f;}public int getTier(){return properties.get(5);}
    public boolean isRunning(){return properties.get(6)!=0;}public int getPhase(){return properties.get(PHASE_INDEX);}
    public int getBandCount(int lane){return lane<0||lane>=LANES?0:Math.max(0,Math.min(MAX_BANDS,properties.get(7+lane)));}
    public float getBandPosition(int lane,int band){return lane<0||lane>=LANES||band<0||band>=getBandCount(lane)?Float.NaN:properties.get(7+LANES+lane*MAX_BANDS+band)/100f;}
}
