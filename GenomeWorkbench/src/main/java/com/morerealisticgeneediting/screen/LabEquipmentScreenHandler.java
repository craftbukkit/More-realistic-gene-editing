package com.morerealisticgeneediting.screen;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.block.entity.*;
import com.morerealisticgeneediting.screen.slot.MachineSlot;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;

/** One synchronized equipment menu with padded, inaccessible unused slots. */
public class LabEquipmentScreenHandler extends GenericContainerScreenHandler {
    private final LabEquipmentBlockEntity machine;
    private final PropertyDelegate properties;
    public LabEquipmentScreenHandler(int syncId,PlayerInventory player) {
        this(syncId,player,new SimpleInventory(27),null,new ArrayPropertyDelegate(6));
    }
    public LabEquipmentScreenHandler(int syncId,PlayerInventory player,LabEquipmentBlockEntity machine) {
        this(syncId,player,new Padded(machine),machine,new PropertyDelegate(){
            public int get(int i){return switch(i){case 0->kind(machine);case 1->machine.getProgress();case 2->machine.getMaxProgress();case 3->machine.isProcessing()?1:0;case 4->machine instanceof LiquidHandlerBlockEntity l && l.getCurrentProtocol()!=null?l.getCurrentProtocol().ordinal():0;default->0;};}
            public void set(int i,int value){} public int size(){return 6;}
        });
    }
    private LabEquipmentScreenHandler(int id,PlayerInventory player,Inventory inventory,LabEquipmentBlockEntity machine,PropertyDelegate properties) {
        super(ModScreenHandlers.LAB_EQUIPMENT,id,player,inventory,3);
        this.machine=machine;this.properties=properties;addProperties(properties);
        for(int i=0;i<27;i++) {
            final int index=i;var old=slots.get(i);
            var replacement=new MachineSlot(inventory,i,old.x,old.y,machine!=null && (i>=machine.size() || machine.isOutputForPlayer(i))){
                @Override public boolean canTakeItems(PlayerEntity user){return (machine==null || (index<machine.size() && machine.canExtractForPlayer(index))) && inventory.canPlayerUse(user);}
            };
            replacement.id=old.id;slots.set(i,replacement);
        }
    }
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(index<0 || index>=slots.size() || !canUse(player) || !slots.get(index).canTakeItems(player))return ItemStack.EMPTY;
        return super.quickMove(player,index);
    }
    @Override public boolean onButtonClick(PlayerEntity player,int id){
        if(machine==null || !canUse(player) || player.getWorld().isClient)return false;
        if(machine instanceof LiquidHandlerBlockEntity liquid){
            if(id==0)liquid.startProtocol();else if(id==1)liquid.stopProtocol();else if(id==2){
                var protocols=LiquidHandlerBlockEntity.Protocol.values();var current=liquid.getCurrentProtocol();
                liquid.setProtocol(protocols[(current==null?0:current.ordinal()+1)%protocols.length]);
            }else return false;
        }else if(machine instanceof BioreactorBlockEntity culture && id==0) culture.harvestCulture();
        else if(machine instanceof ElectrophoresisBlockEntity gel && id==0 && !gel.isProcessing())gel.reset();
        else if(machine instanceof BscBlockEntity cabinet){if(id==0)cabinet.toggleAirflow();else if(id==1)cabinet.startUvCycle();else return false;}
        else return false;
        return true;
    }
    public int getMachineKind(){return properties.get(0);} public int getProgress(){return properties.get(1);}
    public int getMaxProgress(){return properties.get(2);} public int getStatus(){return properties.get(3);} public int getDetail(){return properties.get(4);}
    private static int kind(LabEquipmentBlockEntity m){return m instanceof CentrifugeBlockEntity?1:m instanceof ThermalCyclerBlockEntity || m instanceof ThermalCyclerBlockEntityV2?2:m instanceof LiquidHandlerBlockEntity?3:m instanceof BioreactorBlockEntity?4:m instanceof ElectrophoresisBlockEntity?5:m instanceof BscBlockEntity?6:0;}
    private record Padded(LabEquipmentBlockEntity machine) implements Inventory {
        public int size(){return 27;} public boolean isEmpty(){return machine.isEmpty();}
        public ItemStack getStack(int i){return i>=0 && i<machine.size()?machine.getStack(i):ItemStack.EMPTY;}
        public ItemStack removeStack(int i,int count){return i>=0 && i<machine.size()?machine.removeStack(i,count):ItemStack.EMPTY;}
        public ItemStack removeStack(int i){return i>=0 && i<machine.size()?machine.removeStack(i):ItemStack.EMPTY;}
        public void setStack(int i,ItemStack stack){if(i>=0 && i<machine.size())machine.setStack(i,stack);}
        public void markDirty(){machine.markDirty();} public boolean canPlayerUse(PlayerEntity player){return machine.canPlayerUse(player);}
        public void clear(){machine.clear();}
        public boolean isValid(int i,ItemStack stack){return i>=0 && i<machine.size() && machine.isValid(i,stack);}
    }
}
