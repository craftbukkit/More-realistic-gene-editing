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
 * Screen Handler for Biosafety Cabinet / Laminar Flow Hood
 * 
 * Provides a sterile workspace for handling samples.
 * 
 * Slot Layout:
 * - Slots 0-8: 3x3 work area (sterile workspace)
 * - Slot 9: UV lamp (for sterilization)
 * - Slot 10: Waste container
 */
public class BiosafetyScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    
    // Property indices
    public static final int UV_ON_INDEX = 0;
    public static final int AIRFLOW_SPEED_INDEX = 1;
    public static final int HEPA_STATUS_INDEX = 2;
    public static final int STERILIZATION_PROGRESS_INDEX = 3;
    public static final int TIER_INDEX = 4;
    
    public BiosafetyScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(11), new ArrayPropertyDelegate(5));
    }
    
    public BiosafetyScreenHandler(int syncId, PlayerInventory playerInventory,
                                  Inventory inventory, PropertyDelegate delegate) {
        super(ModEquipmentScreenHandlers.BIOSAFETY_SCREEN_HANDLER, syncId);
        
        checkSize(inventory, 11);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        
        inventory.onOpen(playerInventory.player);
        
        // 3x3 sterile work area
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new Slot(inventory, index, 62 + col * 18, 17 + row * 18));
            }
        }
        
        // UV lamp slot
        this.addSlot(new Slot(inventory, 9, 26, 35) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem().toString().contains("uv") ||
                       stack.getItem().toString().contains("lamp");
            }
        });
        
        // Waste container slot
        this.addSlot(new Slot(inventory, 10, 134, 35));
        
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
            if (invSlot < 11) {
                if (!this.insertItem(originalStack, 11, 47, true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(originalStack, 0, 9, false)) return ItemStack.EMPTY;
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
    public boolean isUVOn() { return propertyDelegate.get(UV_ON_INDEX) != 0; }
    public int getAirflowSpeed() { return propertyDelegate.get(AIRFLOW_SPEED_INDEX); }
    public int getHepaStatus() { return propertyDelegate.get(HEPA_STATUS_INDEX); }
    public int getSterilizationProgress() { return propertyDelegate.get(STERILIZATION_PROGRESS_INDEX); }
    public int getTier() { return propertyDelegate.get(TIER_INDEX); }
    
    /**
     * Check if the workspace is currently sterile
     */
    public boolean isSterile() {
        return getSterilizationProgress() >= 100;
    }
}
