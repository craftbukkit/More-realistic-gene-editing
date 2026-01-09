package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.PcrMachineScreenHandler;
import com.morerealisticgeneediting.security.RateLimiters;
import com.morerealisticgeneediting.util.AsyncJobs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PcrMachineBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {

    private enum State {
        IDLE,
        PROCESSING,
        DONE,
        ERROR
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private State currentState = State.IDLE;
    private static final RateLimiters startPcrLimiter = new RateLimiters(2000L); // 2-second cooldown

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return currentState.ordinal();
        }

        @Override
        public void set(int index, int value) { /* Server-authoritative */ }

        @Override
        public int size() {
            return 1;
        }
    };

    public PcrMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PCR_MACHINE, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putString("pcr.state", this.currentState.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
        try {
            this.currentState = State.valueOf(nbt.getString("pcr.state"));
        } catch (IllegalArgumentException e) {
            this.currentState = State.IDLE;
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, PcrMachineBlockEntity entity) {
        if (world.isClient()) return;

        switch (entity.currentState) {
            case DONE:
                if (entity.getStack(1).isEmpty()) {
                    entity.setState(State.IDLE);
                }
                break;
            case ERROR:
                entity.setState(State.IDLE);
                break;
            case IDLE:
            case PROCESSING:
            default:
                break;
        }
        markDirty(world, pos, state);
    }

    public void startPcr(PlayerEntity player) {
        if (!startPcrLimiter.tryAcquire(player.getUuid())) return;
        if (this.currentState != State.IDLE || !canStartPcr()) return;

        setState(State.PROCESSING);
        ItemStack inputStack = getStack(0);
        NbtCompound readSetNbt = inputStack.getNbt();
        inputStack.decrement(1);

        AsyncJobs.submit(
            (ServerWorld) world,
            () -> {
                try {
                    Thread.sleep(3000);
                    ItemStack result = new ItemStack(ModItems.AMPLIFIED_GENE_FRAGMENT);
                    if (readSetNbt != null) {
                        result.setNbt(readSetNbt.copy());
                    }
                    return result;
                } catch (InterruptedException e) {
                    return ItemStack.EMPTY;
                }
            },
            (amplifiedStack) -> {
                if (!amplifiedStack.isEmpty()) {
                    setStack(1, amplifiedStack);
                    setState(State.DONE);
                } else {
                    setState(State.ERROR);
                }
            }
        );
    }

    private boolean canStartPcr() {
        ItemStack input = getStack(0);
        return input.getItem() == ModItems.READ_SET_ITEM &&
               input.hasNbt() &&
               !input.isEmpty() &&
               getStack(1).isEmpty();
    }

    private void setState(State newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            markDirty();
        }
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.morerealisticgeneediting.pcr_machine");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new PcrMachineScreenHandler(syncId, inv, this, this.propertyDelegate);
    }
}
