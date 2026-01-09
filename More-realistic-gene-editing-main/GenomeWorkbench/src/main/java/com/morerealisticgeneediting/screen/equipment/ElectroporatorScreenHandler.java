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
 * Screen Handler for Electroporator equipment
 * 
 * Slot Layout:
 * - Slot 0: Cells to transform
 * - Slot 1: DNA/Plasmid to introduce
 * - Slot 2: Electroporation cuvette
 * - Slot 3: Electroporation buffer
 * - Slot 4: Output (transformed cells)
 */
public class ElectroporatorScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    // Property indices
    public static final int VOLTAGE_INDEX = 0;
    public static final int PULSE_LENGTH_INDEX = 1;
    public static final int PULSE_COUNT_INDEX = 2;
    public static final int PROGRESS_INDEX = 3;
    public static final int MAX_PROGRESS_INDEX = 4;
    public static final int TIER_INDEX = 5;
    
    public ElectroporatorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), new ArrayPropertyDelegate(6));
    }
    
    public ElectroporatorScreenHandler(int syncId, PlayerInventory playerInventory,
                                       Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.ELECTROPORATOR_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // Cells slot
        this.addSlot(new Slot(inventory, 0, 35, 17) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem().toString().contains("cell") ||
                       stack.getItem().toString().contains("culture");
            }
        });
        
        // DNA/Plasmid slot
        this.addSlot(new Slot(inventory, 1, 53, 17) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem().toString().contains("dna") ||
                       stack.getItem().toString().contains("plasmid") ||
                       stack.getItem().toString().contains("rnp");
            }
        });
        
        // Cuvette slot
        this.addSlot(new Slot(inventory, 2, 44, 35) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem().toString().contains("cuvette");
            }
        });
        
        // Buffer slot
        this.addSlot(new Slot(inventory, 3, 44, 53) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem().toString().contains("buffer") ||
                       stack.getItem().toString().contains("electroporation");
            }
        });
        
        // Output slot
        this.addSlot(new OutputSlot(inventory, 4, 116, 35));
        
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
            if (invSlot < 5) {
                if (!this.insertItem(originalStack, 5, 41, true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(originalStack, 0, 4, false)) return ItemStack.EMPTY;
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
    public int getVoltage() { return propertyDelegate.get(VOLTAGE_INDEX); }
    public int getPulseLength() { return propertyDelegate.get(PULSE_LENGTH_INDEX); }
    public int getPulseCount() { return propertyDelegate.get(PULSE_COUNT_INDEX); }
    public int getProgress() { return propertyDelegate.get(PROGRESS_INDEX); }
    public int getMaxProgress() { return propertyDelegate.get(MAX_PROGRESS_INDEX); }
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }
    public int getTier() { return propertyDelegate.get(TIER_INDEX); }
    
    private static class OutputSlot extends Slot {
        public OutputSlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack stack) { return false; }
    }
}
