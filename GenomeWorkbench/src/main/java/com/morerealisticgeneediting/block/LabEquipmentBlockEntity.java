package com.morerealisticgeneediting.block;

import com.morerealisticgeneediting.equipment.EquipmentTier;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for laboratory equipment block entities.
 *
 * Provides:
 * - Inventory management with sided access
 * - Progress tracking for processes
 * - Energy/resource consumption simulation
 * - NBT serialization
 * - Network synchronization
 */
public abstract class LabEquipmentBlockEntity extends BlockEntity
        implements NamedScreenHandlerFactory, SidedInventory {

    protected DefaultedList<ItemStack> inventory;
    protected int progress = 0;
    protected int maxProgress = 200;
    protected EquipmentTier tier = EquipmentTier.BASIC;
    protected boolean isProcessing = false;

    // Property delegate for GUI sync
    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> tier.getLevel();
                case 3 -> isProcessing ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 3 -> isProcessing = value != 0;
            }
        }

        @Override
        public int size() {
            return 4;
        }
    };

    public LabEquipmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        this.inventory = DefaultedList.ofSize(inventorySize, ItemStack.EMPTY);
    }

    // ========== Inventory Implementation ==========

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if(!canExtractForPlayer(slot))return ItemStack.EMPTY;
        ItemStack result=Inventories.splitStack(inventory, slot, amount);markDirty();return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if(!canExtractForPlayer(slot))return ItemStack.EMPTY;
        ItemStack result=Inventories.removeStack(inventory, slot);markDirty();return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > Math.min(getMaxCountPerStack(),stack.getMaxCount())) {
            stack.setCount(Math.min(getMaxCountPerStack(),stack.getMaxCount()));
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && player.getWorld()==world &&
               world.getBlockEntity(pos) == this &&
               player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        inventory.clear();markDirty();
    }

    // ========== Sided Inventory ==========

    @Override
    public int[] getAvailableSlots(Direction side) {
        int[] slots = new int[inventory.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isInputSlot(slot) && !isProcessing && isValid(slot,stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return isOutputSlot(slot);
    }

    /**
     * Check if slot is for input items
     */
    protected abstract boolean isInputSlot(int slot);

    /**
     * Check if slot is for output items
     */
    protected abstract boolean isOutputSlot(int slot);

    public boolean canExtractForPlayer(int slot) { return isOutputSlot(slot) || !isProcessing; }
    public boolean isOutputForPlayer(int slot) { return isOutputSlot(slot); }
    @Override public boolean isValid(int slot,ItemStack stack) { return slot>=0 && slot<size() && isInputSlot(slot) && !isProcessing; }

    // ========== NBT Serialization ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putInt("Progress", progress);
        nbt.putInt("MaxProgress", maxProgress);
        nbt.putInt("Tier", tier.getLevel());
        nbt.putBoolean("IsProcessing", isProcessing);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        inventory.replaceAll(stack->ItemStack.EMPTY);
        Inventories.readNbt(nbt, inventory, registryLookup);
        progress = nbt.getInt("Progress");
        maxProgress = Math.max(1,Math.min(72000,nbt.getInt("MaxProgress")));
        int tierLevel = nbt.getInt("Tier");
        tier = switch (tierLevel) {
            case 2 -> EquipmentTier.ADVANCED;
            case 3 -> EquipmentTier.ELITE;
            default -> EquipmentTier.BASIC;
        };
        isProcessing = nbt.getBoolean("IsProcessing");
    }

    // ========== Network Sync ==========

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    // ========== Processing Logic ==========

    /**
     * Tick method for processing logic
     */
    public static <T extends LabEquipmentBlockEntity> void tick(World world,BlockPos pos,BlockState state,T entity) { entity.tick(); }

    /**
     * Check if processing can continue
     */
    protected abstract boolean canProcess();

    /**
     * Called when processing completes
     */
    protected abstract void completeProcess();

    /**
     * Get adjusted max progress based on tier
     */
    protected int getAdjustedMaxProgress() {
        return (int) (maxProgress * tier.getSpeedMultiplier());
    }

    // ========== Utility Methods ==========

    /**
     * Drop all contents when block is broken
     */
    public void dropContents(World world, BlockPos pos) {
        ItemScatterer.spawn(world, pos, inventory);
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    public EquipmentTier getTier() {
        return tier;
    }

    public void setTier(EquipmentTier tier) {
        this.tier = tier;
        // Subclasses update their own run configuration; do not compound duration on repeated assignment.
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    /**
     * Get processing progress as percentage (0-1)
     */
    public float getProgressPercent() {
        return maxProgress > 0 ? (float) progress / maxProgress : 0;
    }

    /**
     * Set active state (for block state updates)
     */
    public void setActive(boolean active) {
        this.isProcessing = active;
        if (world != null && !world.isClient) {
            BlockState state = getCachedState();
            if (state.getBlock() instanceof LabEquipmentBlock) {
                LabEquipmentBlock.setActive(world, pos, state, active);
            }
        }
        markDirty();
    }

    /**
     * Instance tick method - override this in subclasses for custom logic
     */
    public void tick() {
        if (world == null || world.isClient || isRemoved() || world.getBlockEntity(pos)!=this) return;

        if (canProcess()) {
            isProcessing = true;
            progress++;

            if (progress >= maxProgress) {
                completeProcess();
                progress = 0;
            }

            markDirty();
        } else {
            if (progress > 0) {
                progress = Math.max(0, progress - 2);
            }
            if (isProcessing) {
                isProcessing = false;
                markDirty();
            }
        }
    }

    /**
     * Check if can process with slot validation
     */
    protected boolean canProcess(int slot) {
        return canProcess();
    }
}
