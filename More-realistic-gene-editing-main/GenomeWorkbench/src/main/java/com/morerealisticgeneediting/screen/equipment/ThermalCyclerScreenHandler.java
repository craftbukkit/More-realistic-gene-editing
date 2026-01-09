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
 * Screen Handler for Thermal Cycler / PCR equipment (all tiers)
 * 
 * Slot Layout:
 * - Slot 0: DNA Template
 * - Slot 1: Forward Primer
 * - Slot 2: Reverse Primer
 * - Slot 3: PCR Tube/Reagents
 * - Slot 4: Output (Amplified Product)
 */
public class ThermalCyclerScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    // Property indices
    public static final int CURRENT_CYCLE_INDEX = 0;
    public static final int TOTAL_CYCLES_INDEX = 1;
    public static final int CURRENT_STEP_INDEX = 2;
    public static final int CURRENT_TEMP_INDEX = 3;
    public static final int TARGET_TEMP_INDEX = 4;
    public static final int ANNEALING_TEMP_INDEX = 5;
    public static final int TIER_INDEX = 6;
    public static final int IS_RUNNING_INDEX = 7;
    
    // Slot positions - arranged to represent PCR setup
    private static final int TEMPLATE_SLOT_X = 44;
    private static final int TEMPLATE_SLOT_Y = 17;
    private static final int PRIMER_F_SLOT_X = 26;
    private static final int PRIMER_F_SLOT_Y = 35;
    private static final int PRIMER_R_SLOT_X = 62;
    private static final int PRIMER_R_SLOT_Y = 35;
    private static final int TUBE_SLOT_X = 44;
    private static final int TUBE_SLOT_Y = 53;
    private static final int OUTPUT_SLOT_X = 116;
    private static final int OUTPUT_SLOT_Y = 35;
    
    public ThermalCyclerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(5), new ArrayPropertyDelegate(8));
    }
    
    public ThermalCyclerScreenHandler(int syncId, PlayerInventory playerInventory,
                                       Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.THERMAL_CYCLER_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 5);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // DNA Template slot
        this.addSlot(new Slot(inventory, 0, TEMPLATE_SLOT_X, TEMPLATE_SLOT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidTemplate(stack);
            }
        });
        
        // Forward Primer slot
        this.addSlot(new Slot(inventory, 1, PRIMER_F_SLOT_X, PRIMER_F_SLOT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidPrimer(stack);
            }
        });
        
        // Reverse Primer slot
        this.addSlot(new Slot(inventory, 2, PRIMER_R_SLOT_X, PRIMER_R_SLOT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidPrimer(stack);
            }
        });
        
        // PCR Tube slot
        this.addSlot(new Slot(inventory, 3, TUBE_SLOT_X, TUBE_SLOT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidTube(stack);
            }
        });
        
        // Output slot
        this.addSlot(new OutputSlot(inventory, 4, OUTPUT_SLOT_X, OUTPUT_SLOT_Y));
        
        // Player inventory
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        
        addProperties(delegate);
    }
    
    private boolean isValidTemplate(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("dna") || 
               itemId.contains("sample") ||
               itemId.contains("genome") ||
               itemId.contains("fragment");
    }
    
    private boolean isValidPrimer(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("primer");
    }
    
    private boolean isValidTube(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("pcr") || 
               itemId.contains("tube") ||
               itemId.contains("strip") ||
               itemId.contains("plate");
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            
            if (invSlot < 5) {
                if (!this.insertItem(originalStack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try to insert into appropriate slot based on item type
                if (isValidTemplate(originalStack)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidPrimer(originalStack)) {
                    if (!this.insertItem(originalStack, 1, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidTube(originalStack)) {
                    if (!this.insertItem(originalStack, 3, 4, false)) {
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
    
    public int getCurrentCycle() {
        return propertyDelegate.get(CURRENT_CYCLE_INDEX);
    }
    
    public int getTotalCycles() {
        return propertyDelegate.get(TOTAL_CYCLES_INDEX);
    }
    
    public float getCycleProgress() {
        int total = getTotalCycles();
        return total > 0 ? (float) getCurrentCycle() / total : 0;
    }
    
    public int getCurrentStep() {
        return propertyDelegate.get(CURRENT_STEP_INDEX);
    }
    
    public String getCurrentStepName() {
        return switch (getCurrentStep()) {
            case 0 -> "Idle";
            case 1 -> "Denaturation";
            case 2 -> "Annealing";
            case 3 -> "Extension";
            case 4 -> "Final Extension";
            default -> "Unknown";
        };
    }
    
    public float getCurrentTemperature() {
        return propertyDelegate.get(CURRENT_TEMP_INDEX) / 10.0f;
    }
    
    public float getTargetTemperature() {
        return propertyDelegate.get(TARGET_TEMP_INDEX) / 10.0f;
    }
    
    public float getAnnealingTemperature() {
        return propertyDelegate.get(ANNEALING_TEMP_INDEX) / 10.0f;
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
