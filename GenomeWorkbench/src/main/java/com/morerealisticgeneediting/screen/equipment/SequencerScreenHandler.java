package com.morerealisticgeneediting.screen.equipment;

import com.morerealisticgeneediting.block.entity.BenchtopSequencerBlockEntity;
import com.morerealisticgeneediting.screen.slot.MachineSlot;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;

public class SequencerScreenHandler extends ScreenHandler {
    private static final int SLOTS=7,INPUTS=5;
    private final Inventory inventory;
    private final PropertyDelegate properties;
    public SequencerScreenHandler(int id,PlayerInventory playerInventory){this(id,playerInventory,new SimpleInventory(SLOTS),new ArrayPropertyDelegate(6));}
    public SequencerScreenHandler(int id,PlayerInventory playerInventory,Inventory inventory,PropertyDelegate properties){
        super(ModEquipmentScreenHandlers.SEQUENCER_SCREEN_HANDLER,id);checkSize(inventory,SLOTS);checkDataCount(properties,6);
        this.inventory=inventory;this.properties=properties;inventory.onOpen(playerInventory.player);
        addSlot(new MachineSlot(inventory,0,26,17,false));
        addSlot(new MachineSlot(inventory,1,26,53,false));
        addSlot(new MachineSlot(inventory,2,62,17,false));
        addSlot(new MachineSlot(inventory,3,62,53,false));
        addSlot(new MachineSlot(inventory,4,80,35,false));
        addSlot(new MachineSlot(inventory,5,134,17,true));
        addSlot(new MachineSlot(inventory,6,134,53,true));
        addPlayerSlots(playerInventory);addProperties(properties);
    }
    @Override public boolean onButtonClick(PlayerEntity player,int id){
        if(!canUse(player)||!(inventory instanceof BenchtopSequencerBlockEntity machine)||player.getWorld().isClient)return false;
        switch(id){case 0->machine.startSequencing(true,player);case 1->machine.startSequencing(false,player);case 2->machine.resetFlowCell(true);case 3->machine.resetFlowCell(false);default->{return false;}}
        return true;
    }
    @Override public boolean canUse(PlayerEntity player){return inventory.canPlayerUse(player);}
    @Override public void onClosed(PlayerEntity player){super.onClosed(player);inventory.onClose(player);}
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(index<0||index>=slots.size()||!canUse(player))return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasStack()||!slot.canTakeItems(player))return ItemStack.EMPTY;
        ItemStack source=slot.getStack(),before=source.copy();
        if(index<SLOTS){if(!insertItem(source,SLOTS,slots.size(),true))return ItemStack.EMPTY;}
        else if(!insertItem(source,0,INPUTS,false))return ItemStack.EMPTY;
        if(source.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();
        slot.onTakeItem(player,source);return before;
    }
    private void addPlayerSlots(PlayerInventory inventory){
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)
            addSlot(new Slot(inventory,9+row*9+column,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(new Slot(inventory,column,8+column*18,142));
    }
    public static final int PROGRESS_INDEX=0,MAX_PROGRESS_INDEX=1,READS_GENERATED_INDEX=2,QUALITY_SCORE_INDEX=3,TIER_INDEX=4,IS_RUNNING_INDEX=5;
    public int getProgress(){return properties.get(0);}public int getMaxProgress(){return properties.get(1);}public int getReadsGenerated(){return properties.get(2);}
    public int getQualityScore(){return properties.get(3);}public int getTier(){return properties.get(4);}public boolean isRunning(){return properties.get(5)!=0;}
    public float getProgressPercent(){return getMaxProgress()>0?(float)getProgress()/getMaxProgress():0;}
}
