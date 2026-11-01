package com.morerealisticgeneediting.screen.equipment;

import com.morerealisticgeneediting.block.entity.SpectrophotometerBlockEntity;
import com.morerealisticgeneediting.screen.slot.MachineSlot;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;

public class SpectrophotometerScreenHandler extends ScreenHandler {
    private static final int SLOTS=3,INPUTS=3;
    private final Inventory inventory;
    private final PropertyDelegate properties;
    public SpectrophotometerScreenHandler(int id,PlayerInventory playerInventory){this(id,playerInventory,new SimpleInventory(SLOTS),new ArrayPropertyDelegate(5));}
    public SpectrophotometerScreenHandler(int id,PlayerInventory playerInventory,Inventory inventory,PropertyDelegate properties){
        super(ModEquipmentScreenHandlers.SPECTROPHOTOMETER_SCREEN_HANDLER,id);checkSize(inventory,SLOTS);checkDataCount(properties,5);
        this.inventory=inventory;this.properties=properties;inventory.onOpen(playerInventory.player);
        addSlot(new MachineSlot(inventory,0,44,35,false));
        addSlot(new MachineSlot(inventory,1,80,35,false));
        addSlot(new MachineSlot(inventory,2,116,35,false));
        addPlayerSlots(playerInventory);addProperties(properties);
    }
    @Override public boolean onButtonClick(PlayerEntity player,int id){
        if(!canUse(player)||!(inventory instanceof SpectrophotometerBlockEntity machine)||player.getWorld().isClient)return false;
        switch(id){case 0->machine.performBlank();case 1->machine.measure();default->{return false;}}
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
    public float getAbsorbance(){return properties.get(0)<0?Float.NaN:properties.get(0)/1000f;}
    public float getConcentration(){return properties.get(1)<0?Float.NaN:properties.get(1)/100f;}
    public int getWavelength(){return properties.get(2);}public int getTier(){return properties.get(3);}
    public boolean hasResults(){return properties.get(4)!=0;}public boolean isMeasuring(){return false;}
}
