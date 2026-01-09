package com.morerealisticgeneediting.screen;

import com.morerealisticgeneediting.block.entity.IncubatorBlockEntity;
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
import org.jetbrains.annotations.Nullable;

public class IncubatorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    @Nullable
    private final IncubatorBlockEntity blockEntity;

    // Client-side constructor
    public IncubatorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(4), new ArrayPropertyDelegate(1));
    }

    // Server-side constructor
    public IncubatorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate delegate) {
        super(ModScreenHandlers.INCUBATOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, 4);
        checkDataCount(delegate, 1);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        inventory.onOpen(playerInventory.player);

        this.blockEntity = (inventory instanceof IncubatorBlockEntity) ? (IncubatorBlockEntity) inventory : null;

        this.addSlot(new Slot(inventory, 0, 44, 35)); // Amplified Gene Fragment
        this.addSlot(new Slot(inventory, 1, 62, 35)); // Plasmid Vector
        this.addSlot(new Slot(inventory, 2, 80, 35)); // DNA Ligase
        this.addSlot(new Slot(inventory, 3, 134, 35)); // Output

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addProperties(delegate);
    }

    public int getState() {
        return propertyDelegate.get(0);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (blockEntity == null) return false;
        if (!NetGuards.isPlayerClose(player, blockEntity.getWorld(), blockEntity.getPos())) return false;

        if (id == 0) { // Assuming 0 is the 'start' button ID
            blockEntity.startIncubation(player);
            return true;
        }
        return super.onButtonClick(player, id);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
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
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
