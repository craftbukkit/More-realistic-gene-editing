package com.morerealisticgeneediting.screen;

import com.morerealisticgeneediting.block.entity.ElectrophoresisTankBlockEntity;
import com.morerealisticgeneediting.screen.slot.ModResultSlot;
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

public class ElectrophoresisTankScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    @Nullable
    private final ElectrophoresisTankBlockEntity blockEntity;

    // Client-side constructor
    public ElectrophoresisTankScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2), new ArrayPropertyDelegate(1));
    }

    // Server-side constructor
    public ElectrophoresisTankScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate delegate) {
        super(ModScreenHandlers.ELECTROPHORESIS_TANK_SCREEN_HANDLER, syncId);
        checkSize(inventory, 2);
        checkDataCount(delegate, 1);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        inventory.onOpen(playerInventory.player);

        this.blockEntity = (inventory instanceof ElectrophoresisTankBlockEntity) ? (ElectrophoresisTankBlockEntity) inventory : null;

        this.addSlot(new Slot(inventory, 0, 80, 17));
        this.addSlot(new ModResultSlot(inventory, 1, 80, 53));

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

        if (id == 0) { // Assuming 0 is the 'start' button
            blockEntity.startElectrophoresis(player);
            return true;
        }
        return super.onButtonClick(player, id);
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

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 86 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 144));
        }
    }
}
