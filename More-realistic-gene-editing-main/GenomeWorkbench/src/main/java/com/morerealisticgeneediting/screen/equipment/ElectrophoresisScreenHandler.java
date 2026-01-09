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
 * Screen Handler for Electrophoresis equipment (all tiers)
 * 
 * Slot Layout:
 * - Slot 0: DNA Sample
 * - Slot 1: DNA Ladder
 * - Slot 2: Loading Dye
 * - Slot 3: Agarose/Gel
 * - Slot 4: Buffer
 * - Slot 5-10: Gel Lanes (6 lanes for samples)
 * - Slot 11: Output (Analyzed result)
 */
public class ElectrophoresisScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    // Property indices
    public static final int PROGRESS_INDEX = 0;
    public static final int MAX_PROGRESS_INDEX = 1;
    public static final int VOLTAGE_INDEX = 2;
    public static final int RUN_TIME_INDEX = 3;
    public static final int GEL_PERCENT_INDEX = 4;
    public static final int TIER_INDEX = 5;
    public static final int IS_RUNNING_INDEX = 6;
    
    public ElectrophoresisScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(12), new ArrayPropertyDelegate(7));
    }
    
    public ElectrophoresisScreenHandler(int syncId, PlayerInventory playerInventory,
                                        Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.ELECTROPHORESIS_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 12);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // DNA Sample slot
        this.addSlot(new Slot(inventory, 0, 17, 17) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidDnaSample(stack);
            }
        });
        
        // DNA Ladder slot
        this.addSlot(new Slot(inventory, 1, 17, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidLadder(stack);
            }
        });
        
        // Loading Dye slot
        this.addSlot(new Slot(inventory, 2, 17, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidDye(stack);
            }
        });
        
        // Agarose/Gel slot
        this.addSlot(new Slot(inventory, 3, 35, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidGel(stack);
            }
        });
        
        // Buffer slot
        this.addSlot(new Slot(inventory, 4, 53, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidBuffer(stack);
            }
        });
        
        // Gel lanes (6 lanes) - arranged horizontally
        for (int i = 0; i < 6; i++) {
            final int laneIndex = 5 + i;
            this.addSlot(new Slot(inventory, laneIndex, 80 + (i * 14), 35) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return isValidDnaSample(stack) || isValidLadder(stack);
                }
                
                @Override
                public int getMaxItemCount() {
                    return 1; // One sample per lane
                }
            });
        }
        
        // Output slot
        this.addSlot(new OutputSlot(inventory, 11, 152, 35));
        
        // Player inventory
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        
        addProperties(delegate);
    }
    
    private boolean isValidDnaSample(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("dna") || 
               itemId.contains("sample") ||
               itemId.contains("fragment") ||
               itemId.contains("pcr");
    }
    
    private boolean isValidLadder(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("ladder");
    }
    
    private boolean isValidDye(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("dye") || itemId.contains("loading");
    }
    
    private boolean isValidGel(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("agarose") || itemId.contains("gel");
    }
    
    private boolean isValidBuffer(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("buffer") || 
               itemId.contains("tae") || 
               itemId.contains("tbe");
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            
            if (invSlot < 12) {
                if (!this.insertItem(originalStack, 12, 48, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try to insert based on item type
                if (isValidDnaSample(originalStack)) {
                    if (!this.insertItem(originalStack, 0, 1, false) &&
                        !this.insertItem(originalStack, 5, 11, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidLadder(originalStack)) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidDye(originalStack)) {
                    if (!this.insertItem(originalStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidGel(originalStack)) {
                    if (!this.insertItem(originalStack, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidBuffer(originalStack)) {
                    if (!this.insertItem(originalStack, 4, 5, false)) {
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
    
    public int getVoltage() {
        return propertyDelegate.get(VOLTAGE_INDEX);
    }
    
    public int getRunTime() {
        return propertyDelegate.get(RUN_TIME_INDEX);
    }
    
    public float getGelPercent() {
        return propertyDelegate.get(GEL_PERCENT_INDEX) / 10.0f;
    }
    
    public int getTier() {
        return propertyDelegate.get(TIER_INDEX);
    }
    
    public boolean isRunning() {
        return propertyDelegate.get(IS_RUNNING_INDEX) != 0;
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
