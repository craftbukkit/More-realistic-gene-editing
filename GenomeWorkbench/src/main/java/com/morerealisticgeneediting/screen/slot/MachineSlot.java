package com.morerealisticgeneediting.screen.slot;
import com.morerealisticgeneediting.block.entity.TimedMachineBlockEntity;
import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
/** Both click and quick-move respect server reservations and output-only slots. */
public class MachineSlot extends Slot {
    private final boolean output;
    public MachineSlot(Inventory inventory,int index,int x,int y,boolean output) { super(inventory,index,x,y);this.output=output; }
    @Override public boolean canInsert(ItemStack stack) { return !output && inventory.isValid(getIndex(),stack); }
    @Override public boolean canTakeItems(PlayerEntity player) {
        if(inventory instanceof TimedMachineBlockEntity timed && timed.isInputLocked(getIndex())) return false;
        if(inventory instanceof LabEquipmentBlockEntity lab && !lab.canExtractForPlayer(getIndex())) return false;
        return inventory.canPlayerUse(player);
    }
}
