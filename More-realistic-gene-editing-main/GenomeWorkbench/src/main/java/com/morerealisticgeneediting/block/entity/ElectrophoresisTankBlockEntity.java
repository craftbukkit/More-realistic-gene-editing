package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.ElectrophoresisTankScreenHandler;
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

public class ElectrophoresisTankBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {

    private enum State {
        IDLE, RUNNING, DONE, ERROR
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private State currentState = State.IDLE;
    private static final RateLimiters startElectrophoresisLimiter = new RateLimiters(3000L);

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

    public ElectrophoresisTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROPHORESIS_TANK, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putString("electrophoresis.state", this.currentState.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        try {
            this.currentState = State.valueOf(nbt.getString("electrophoresis.state"));
        } catch (IllegalArgumentException e) {
            this.currentState = State.IDLE;
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, ElectrophoresisTankBlockEntity entity) {
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
            case RUNNING:
            default:
                break;
        }
        markDirty(world, pos, state);
    }

    public void startElectrophoresis(PlayerEntity player) {
        if (!startElectrophoresisLimiter.tryAcquire(player.getUuid())) return;
        if (this.currentState != State.IDLE || !canStartElectrophoresis()) return;

        setState(State.RUNNING);
        NbtCompound geneNbt = getStack(0).getNbt();
        removeStack(0, 1);

        AsyncJobs.submit(
            (ServerWorld) world,
            () -> {
                try {
                    Thread.sleep(4000);
                    ItemStack result = new ItemStack(ModItems.PURIFIED_PLASMID);
                    if (geneNbt != null) {
                        result.setNbt(geneNbt.copy());
                    }
                    return result;
                } catch (InterruptedException e) {
                    return ItemStack.EMPTY;
                }
            },
            (outputStack) -> {
                if (!outputStack.isEmpty()) {
                    setStack(1, outputStack);
                    setState(State.DONE);
                } else {
                    setState(State.ERROR);
                }
            }
        );
    }

    private boolean canStartElectrophoresis() {
        ItemStack input = getStack(0);
        return input.getItem() == ModItems.RECOMBINANT_PLASMID && input.hasNbt() && getStack(1).isEmpty();
    }

    private void setState(State newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            markDirty();
        }
    }

    @Override
    public DefaultedList<ItemStack> getItems() { return inventory; }

    @Override
    public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.electrophoresis_tank"); }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ElectrophoresisTankScreenHandler(syncId, inv, this, this.propertyDelegate);
    }
}
