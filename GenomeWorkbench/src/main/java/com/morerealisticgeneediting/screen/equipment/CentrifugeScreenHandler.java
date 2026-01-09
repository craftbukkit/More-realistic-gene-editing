package com.morerealisticgeneediting.screen.equipment;

import com.morerealisticgeneediting.block.entity.CentrifugeBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

/**
 * Screen Handler for Centrifuge equipment (all tiers)
 * 
 * Slot Layout:
 * - Slot 0: Input sample
 * - Slot 1: Output (pellet - heavy fraction)
 * - Slot 2: Output (supernatant - light fraction)  
 * - Slot 3: Output (interface - middle fraction)
 */
public class CentrifugeScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    // Property indices
    public static final int PROGRESS_INDEX = 0;
    public static final int MAX_PROGRESS_INDEX = 1;
    public static final int RPM_INDEX = 2;
    public static final int TARGET_RPM_INDEX = 3;
    public static final int TEMPERATURE_INDEX = 4;
    public static final int TIER_INDEX = 5;
    
    // Slot positions
    private static final int INPUT_SLOT_X = 56;
    private static final int INPUT_SLOT_Y = 35;
    private static final int OUTPUT_SLOT_X = 116;
    private static final int OUTPUT_SLOT_Y = 17;
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(4), new ArrayPropertyDelegate(6));
    }
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory playerInventory, 
                                   Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.CENTRIFUGE_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 4);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // Input slot
        this.addSlot(new Slot(inventory, 0, INPUT_SLOT_X, INPUT_SLOT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidCentrifugeInput(stack);
            }
        });
        
        // Output slots (pellet, supernatant, interface)
        this.addSlot(new OutputSlot(inventory, 1, OUTPUT_SLOT_X, OUTPUT_SLOT_Y));
        this.addSlot(new OutputSlot(inventory, 2, OUTPUT_SLOT_X, OUTPUT_SLOT_Y + 18));
        this.addSlot(new OutputSlot(inventory, 3, OUTPUT_SLOT_X, OUTPUT_SLOT_Y + 36));
        
        // Player inventory
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        
        addProperties(delegate);
    }
    
    private boolean isValidCentrifugeInput(ItemStack stack) {
        // Accept biological samples
        String itemId = stack.getItem().toString();
        return itemId.contains("sample") || 
               itemId.contains("blood") ||
               itemId.contains("tissue") ||
               itemId.contains("culture");
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            
            if (invSlot < 4) {
                // From centrifuge to player inventory
                if (!this.insertItem(originalStack, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to centrifuge input
                if (isValidCentrifugeInput(originalStack)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (invSlot < 31) {
                    // Move between inventory and hotbar
                    if (!this.insertItem(originalStack, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.insertItem(originalStack, 4, 31, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            
            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        
        return newStack;
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
    
    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }
    
    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
    
    // ========== Property Getters ==========
    
    public int getProgress() {
        return propertyDelegate.get(PROGRESS_INDEX);
    }
    
    public int getMaxProgress() {
        return propertyDelegate.get(MAX_PROGRESS_INDEX);
    }
    
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }
    
    public int getRpm() {
        return propertyDelegate.get(RPM_INDEX);
    }
    
    public int getTargetRpm() {
        return propertyDelegate.get(TARGET_RPM_INDEX);
    }
    
    public int getTemperature() {
        return propertyDelegate.get(TEMPERATURE_INDEX);
    }
    
    public int getTier() {
        return propertyDelegate.get(TIER_INDEX);
    }
    
    public boolean isProcessing() {
        return getProgress() > 0;
    }
    
    /**
     * Output-only slot
     */
    private static class OutputSlot extends Slot {
        public OutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
        
        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
