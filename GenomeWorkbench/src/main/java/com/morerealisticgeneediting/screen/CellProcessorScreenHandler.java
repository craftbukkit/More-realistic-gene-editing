package com.morerealisticgeneediting.screen;

import com.morerealisticgeneediting.block.entity.CellProcessorBlockEntity;
import com.morerealisticgeneediting.util.NetGuards;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import com.morerealisticgeneediting.screen.slot.MachineSlot;
import org.jetbrains.annotations.Nullable;

public class CellProcessorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    @Nullable
    private final CellProcessorBlockEntity blockEntity;

    // Client-side constructor
    public CellProcessorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(4), new ArrayPropertyDelegate(1));
    }

    // Server-side constructor
    public CellProcessorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.CELL_PROCESSOR_SCREEN_HANDLER, syncId);
        checkDataCount(propertyDelegate, 1);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        inventory.onOpen(playerInventory.player);

        this.blockEntity = (inventory instanceof CellProcessorBlockEntity) ? (CellProcessorBlockEntity) inventory : null;

        this.addSlot(new MachineSlot(inventory, 0, 8, 53, false));   // Input Slot
        this.addSlot(new MachineSlot(inventory, 1, 34, 53, false));  // Reagent Slot
        this.addSlot(new MachineSlot(inventory, 2, 80, 53, true));  // Output Slot
        this.addSlot(new MachineSlot(inventory, 3, 106, 53, true)); // Waste/Byproduct Slot

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addProperties(propertyDelegate);
    }

    public int getState() {
        return propertyDelegate.get(0);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (blockEntity == null || !canUse(player)) return false;
        if (!NetGuards.isPlayerClose(player, blockEntity.getWorld(), blockEntity.getPos())) return false;

        if (id == 0) { // Assuming 0 is the 'start' button
            blockEntity.startProcessing(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        if(invSlot<0 || invSlot>=this.slots.size() || !canUse(player)) return ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if(!slot.canTakeItems(player)) return ItemStack.EMPTY;
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
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
}
