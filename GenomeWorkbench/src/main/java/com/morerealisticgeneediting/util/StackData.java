package com.morerealisticgeneediting.util;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
/** Mutable copies must be written back; component values are immutable snapshots. */
public final class StackData {
    private StackData() { }
    public static NbtCompound read(ItemStack stack) { return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt(); }
    public static boolean has(ItemStack stack) { return !read(stack).isEmpty(); }
    public static void write(ItemStack stack, NbtCompound data) { stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data)); }
}
