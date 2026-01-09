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
 * Screen Handler for Sequencer equipment
 * 
 * Slot Layout:
 * - Slot 0: DNA Sample to sequence
 * - Slot 1: Sequencing reagent/chip
 * - Slot 2: Output (Sequence data item)
 */
public class SequencerScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    // Property indices
    public static final int PROGRESS_INDEX = 0;
    public static final int MAX_PROGRESS_INDEX = 1;
    public static final int READS_GENERATED_INDEX = 2;
    public static final int QUALITY_SCORE_INDEX = 3;
    public static final int TIER_INDEX = 4;
    public static final int IS_RUNNING_INDEX = 5;
    
    public SequencerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3), new ArrayPropertyDelegate(6));
    }
    
    public SequencerScreenHandler(int syncId, PlayerInventory playerInventory,
                                  Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.SEQUENCER_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 3);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // DNA Sample slot
        this.addSlot(new Slot(inventory, 0, 44, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidSample(stack);
            }
        });
        
        // Sequencing chip/reagent slot
        this.addSlot(new Slot(inventory, 1, 62, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidChip(stack);
            }
        });
        
        // Output slot
        this.addSlot(new OutputSlot(inventory, 2, 116, 35));
        
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addProperties(delegate);
    }
    
    private boolean isValidSample(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("dna") || itemId.contains("sample") || itemId.contains("fragment");
    }
    
    private boolean isValidChip(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("chip") || itemId.contains("sequencing") || itemId.contains("flowcell");
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            
            if (invSlot < 3) {
                if (!this.insertItem(originalStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (isValidSample(originalStack)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidChip(originalStack)) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
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
    
    // Property getters
    public int getProgress() { return propertyDelegate.get(PROGRESS_INDEX); }
    public int getMaxProgress() { return propertyDelegate.get(MAX_PROGRESS_INDEX); }
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max > 0 ? (float) getProgress() / max : 0;
    }
    public int getReadsGenerated() { return propertyDelegate.get(READS_GENERATED_INDEX); }
    public int getQualityScore() { return propertyDelegate.get(QUALITY_SCORE_INDEX); }
    public int getTier() { return propertyDelegate.get(TIER_INDEX); }
    public boolean isRunning() { return propertyDelegate.get(IS_RUNNING_INDEX) != 0; }
    
    private static class OutputSlot extends Slot {
        public OutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
        @Override
        public boolean canInsert(ItemStack stack) { return false; }
    }
}
