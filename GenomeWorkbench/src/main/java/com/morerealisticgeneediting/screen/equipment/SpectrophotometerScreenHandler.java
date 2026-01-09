package com.morerealisticgeneediting.screen.equipment;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Screen Handler for Spectrophotometer equipment
 */
public class SpectrophotometerScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    public SpectrophotometerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2), new ArrayPropertyDelegate(5));
    }
    
    public SpectrophotometerScreenHandler(int syncId, PlayerInventory playerInventory,
                                          Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.SPECTROPHOTOMETER_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 2);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // Sample slot
        this.addSlot(new Slot(inventory, 0, 80, 35));
        
        // Output/Result slot
        this.addSlot(new OutputSlot(inventory, 1, 116, 35));
        
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addProperties(delegate);
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 2) {
                if (!this.insertItem(originalStack, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(originalStack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (originalStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }
    
    @Override
    public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }
    
    private void addPlayerInventory(PlayerInventory inv) {
        for (int i = 0; i < 3; ++i)
            for (int l = 0; l < 9; ++l)
                this.addSlot(new Slot(inv, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
    }
    
    private void addPlayerHotbar(PlayerInventory inv) {
        for (int i = 0; i < 9; ++i)
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
    
    // Property getters
    public float getAbsorbance() { return propertyDelegate.get(0) / 1000.0f; }
    public float getConcentration() { return propertyDelegate.get(1) / 100.0f; }
    public int getWavelength() { return propertyDelegate.get(2); }
    public int getTier() { return propertyDelegate.get(3); }
    public boolean isMeasuring() { return propertyDelegate.get(4) != 0; }
    
    private static class OutputSlot extends Slot {
        public OutputSlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack stack) { return false; }
    }
}
