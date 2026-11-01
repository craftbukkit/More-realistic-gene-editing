package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.util.StackData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.Arrays;

/** Durable in-slot reservations. Commit consumes the exact snapshot only once. */
public abstract class TimedMachineBlockEntity extends BlockEntity implements ImplementedInventory, SidedInventory, NamedScreenHandlerFactory {
    private final DefaultedList<ItemStack> inventory;
    private final DefaultedList<ItemStack> reserved;
    private final int outputSlot;
    private ItemStack pendingOutput = ItemStack.EMPTY;
    private int remainingTicks;
    private java.util.UUID jobOwner;
    private int state; // 0 idle, 1 processing/output blocked, 2 done, 3 invalidated
    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        public int get(int index) { return state; }
        public void set(int index, int value) { }
        public int size() { return 1; }
    };
    protected TimedMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, int outputSlot) {
        super(type,pos,state); this.outputSlot=outputSlot;
        this.inventory=DefaultedList.ofSize(size,ItemStack.EMPTY);
        this.reserved=DefaultedList.ofSize(size,ItemStack.EMPTY);
    }
    protected abstract boolean acceptsInput(int slot, ItemStack stack);
    protected final boolean startJob(PlayerEntity player, ItemStack output, int ticks, int... inputSlots) {
        if (world == null || world.isClient || !canPlayerUse(player) || state==1 || ticks<=0 || output.isEmpty() || !canMerge(getStack(outputSlot),output)) return false;
        if (inputSlots.length==0 || Arrays.stream(inputSlots).distinct().count()!=inputSlots.length) return false;
        for(int slot:inputSlots) if(slot<0 || slot>=outputSlot || !acceptsInput(slot,getStack(slot)) || getStack(slot).isEmpty()) return false;
        reserved.replaceAll(stack -> ItemStack.EMPTY);
        for(int slot:inputSlots) reserved.set(slot,getStack(slot).copyWithCount(1));
        jobOwner=player.getUuid(); pendingOutput=output.copy(); remainingTicks=ticks; state=1; markDirty(); return true;
    }
    protected final void tickJob() {
        if(world==null || world.isClient || isRemoved() || world.getBlockEntity(pos)!=this) return;
        if(state==2 && getStack(outputSlot).isEmpty()) { state=0;markDirty(); }
        if(state!=1) return;
        for(int i=0;i<reserved.size();i++) {
            ItemStack snapshot=reserved.get(i), actual=getStack(i);
            if(!snapshot.isEmpty() && (!ItemStack.areItemsAndComponentsEqual(snapshot,actual) || actual.getCount()<snapshot.getCount())) {
                clearJob(); state=3; markDirty(); return;
            }
        }
        if(remainingTicks>0) { remainingTicks--; markDirty(); }
        if(remainingTicks!=0 || !canMerge(getStack(outputSlot),pendingOutput)) return;
        // No callback can outlive the entity; all mutations occur in one server tick.
        for(int i=0;i<reserved.size();i++) if(!reserved.get(i).isEmpty()) inventory.get(i).decrement(reserved.get(i).getCount());
        ItemStack existing=inventory.get(outputSlot);
        if(existing.isEmpty()) inventory.set(outputSlot,pendingOutput.copy()); else existing.increment(pendingOutput.getCount());
        ItemStack completed=pendingOutput.copy(); java.util.UUID owner=jobOwner;
        clearJob(); state=2; markDirty(); onJobCommitted(owner,completed);
    }
    protected void onJobCommitted(java.util.UUID owner,ItemStack output) { }
    private void clearJob() { reserved.replaceAll(stack -> ItemStack.EMPTY); pendingOutput=ItemStack.EMPTY; remainingTicks=0; jobOwner=null; }
    public final boolean isInputLocked(int slot) { return state==1 && slot>=0 && slot<reserved.size() && !reserved.get(slot).isEmpty(); }
    public final boolean isOutputSlot(int slot) { return slot>=outputSlot; }
    public final int getRemainingTicks() { return remainingTicks; }
    public final boolean hasPendingJob() { return state==1; }
    public static boolean canMerge(ItemStack current, ItemStack added) {
        return !added.isEmpty() && added.getCount()<=added.getMaxCount() && (current.isEmpty() || (ItemStack.areItemsAndComponentsEqual(current,added) && (long)current.getCount()+added.getCount()<=current.getMaxCount()));
    }
    protected static ItemStack derived(Item item, ItemStack source, String operation) {
        ItemStack result=new ItemStack(item); NbtCompound data=StackData.read(source);
        data.putString("Operation",operation); data.putBoolean("Simulation",true); StackData.write(result,data); return result;
    }
    @Override public DefaultedList<ItemStack> getItems() { return inventory; }
    @Override public boolean isValid(int slot,ItemStack stack) { return slot>=0 && slot<outputSlot && !isInputLocked(slot) && acceptsInput(slot,stack); }
    @Override public int[] getAvailableSlots(Direction side) { return java.util.stream.IntStream.range(0,size()).toArray(); }
    @Override public boolean canInsert(int slot,ItemStack stack,Direction side) { return isValid(slot,stack); }
    @Override public boolean canExtract(int slot,ItemStack stack,Direction side) { return isOutputSlot(slot); }
    @Override public ItemStack removeStack(int slot,int count) { return isInputLocked(slot)?ItemStack.EMPTY:ImplementedInventory.super.removeStack(slot,count); }
    @Override public ItemStack removeStack(int slot) { return isInputLocked(slot)?ItemStack.EMPTY:ImplementedInventory.super.removeStack(slot); }
    @Override public void setStack(int slot,ItemStack stack) { if(!isInputLocked(slot)) ImplementedInventory.super.setStack(slot,stack); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return world!=null && player.getWorld()==world && world.getBlockEntity(pos)==this && player.squaredDistanceTo(pos.toCenterPos())<=64; }
    @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt,lookup); Inventories.writeNbt(nbt,inventory,lookup);
        NbtCompound job=new NbtCompound(); Inventories.writeNbt(job,reserved,lookup);
        if(jobOwner!=null)job.putUuid("Owner",jobOwner);
        job.putInt("State",state); job.putInt("Remaining",remainingTicks);
        if(!pendingOutput.isEmpty()) job.put("Output",pendingOutput.toNbt(lookup));
        nbt.put("MachineJob",job);
    }
    @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt,lookup); inventory.replaceAll(s->ItemStack.EMPTY); Inventories.readNbt(nbt,inventory,lookup);
        clearJob(); NbtCompound job=nbt.getCompound("MachineJob");
        jobOwner=job.containsUuid("Owner")?job.getUuid("Owner"):null;
        state=job.getInt("State"); remainingTicks=Math.max(0,Math.min(72000,job.getInt("Remaining")));
        Inventories.readNbt(job,reserved,lookup);
        pendingOutput=job.contains("Output")?ItemStack.fromNbt(lookup,job.getCompound("Output")).orElse(ItemStack.EMPTY):ItemStack.EMPTY;
        boolean validReservation=jobOwner!=null && reserved.stream().anyMatch(stack->!stack.isEmpty());
        for(int i=0;i<reserved.size();i++)if(!reserved.get(i).isEmpty() && (i>=outputSlot || !acceptsInput(i,reserved.get(i))))validReservation=false;
        if(state<0 || state>3 || (state==1 && (pendingOutput.isEmpty() || !validReservation))) { clearJob();state=0; }
    }
}
