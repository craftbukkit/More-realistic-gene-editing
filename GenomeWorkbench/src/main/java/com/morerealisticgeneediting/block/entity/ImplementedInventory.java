package com.morerealisticgeneediting.block.entity;

import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface ImplementedInventory extends Inventory {
    DefaultedList<ItemStack> getItems();

    static ImplementedInventory of(DefaultedList<ItemStack> items) {
        return () -> items;
    }

    @Override
    default int size() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(getItems(), slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    default ItemStack removeStack(int slot) {
        ItemStack removed=Inventories.removeStack(getItems(), slot); if(!removed.isEmpty())markDirty(); return removed;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > Math.min(getMaxCountPerStack(),stack.getMaxCount())) {
            stack.setCount(Math.min(getMaxCountPerStack(),stack.getMaxCount()));
        }
        markDirty();
    }

    @Override
    default void clear() {
        getItems().clear();markDirty();
    }

    @Override
    default void markDirty() {
        // Override in your block entity if you need to do something when the inventory is marked dirty
    }

    @Override
    default boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) {
        if(this instanceof net.minecraft.block.entity.BlockEntity be) {
            return be.getWorld()!=null && player.getWorld()==be.getWorld() && be.getWorld().getBlockEntity(be.getPos())==be && player.squaredDistanceTo(be.getPos().toCenterPos())<=64;
        }
        return false;
    }
}
