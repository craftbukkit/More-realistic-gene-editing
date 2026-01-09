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
 * Screen Handler for Incubator/Bioreactor equipment
 * 
 * Slot Layout:
 * - Slots 0-5: Culture vessels (flasks, plates)
 * - Slot 6: Growth medium
 * - Slot 7: Output (grown cultures)
 */
public class IncubatorEquipmentScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    // Property indices
    public static final int TEMPERATURE_INDEX = 0;
    public static final int TARGET_TEMP_INDEX = 1;
    public static final int CO2_LEVEL_INDEX = 2;
    public static final int HUMIDITY_INDEX = 3;
    public static final int SHAKE_SPEED_INDEX = 4;
    public static final int PROGRESS_INDEX = 5;
    public static final int MAX_PROGRESS_INDEX = 6;
    public static final int TIER_INDEX = 7;
    
    public IncubatorEquipmentScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(8), new ArrayPropertyDelegate(8));
    }
    
    public IncubatorEquipmentScreenHandler(int syncId, PlayerInventory playerInventory,
                                           Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.INCUBATOR_EQUIPMENT_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 8);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // Culture vessel slots (2x3 grid)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new Slot(inventory, index, 26 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return isValidCultureVessel(stack);
                    }
                });
            }
        }
        
        // Growth medium slot
        this.addSlot(new Slot(inventory, 6, 26, 53) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return isValidMedium(stack);
            }
        });
        
        // Output slot
        this.addSlot(new OutputSlot(inventory, 7, 134, 35));
        
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addProperties(delegate);
    }
    
    private boolean isValidCultureVessel(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("flask") || 
               itemId.contains("plate") || 
               itemId.contains("dish") ||
               itemId.contains("culture");
    }
    
    private boolean isValidMedium(ItemStack stack) {
        String itemId = stack.getItem().toString();
        return itemId.contains("medium") || 
               itemId.contains("dmem") || 
               itemId.contains("rpmi");
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 8) {
                if (!this.insertItem(originalStack, 8, 44, true)) return ItemStack.EMPTY;
            } else {
                if (isValidCultureVessel(originalStack)) {
                    if (!this.insertItem(originalStack, 0, 6, false)) return ItemStack.EMPTY;
                } else if (isValidMedium(originalStack)) {
                    if (!this.insertItem(originalStack, 6, 7, false)) return ItemStack.EMPTY;
                } else return ItemStack.EMPTY;
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
    public float getTemperature() { return propertyDelegate.get(TEMPERATURE_INDEX) / 10.0f; }
    public float getTargetTemperature() { return propertyDelegate.get(TARGET_TEMP_INDEX) / 10.0f; }
    public float getCO2Level() { return propertyDelegate.get(CO2_LEVEL_INDEX) / 10.0f; }
    public float getHumidity() { return propertyDelegate.get(HUMIDITY_INDEX) / 10.0f; }
    public int getShakeSpeed() { return propertyDelegate.get(SHAKE_SPEED_INDEX); }
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
