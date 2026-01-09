package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.IncubatorScreenHandler;
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

public class IncubatorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {

    private enum State {
        IDLE, INCUBATING, DONE, ERROR
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);
    private State currentState = State.IDLE;
    private static final RateLimiters startIncubationLimiter = new RateLimiters(5000L);

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

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATOR, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putString("incubator.state", this.currentState.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
        try {
            this.currentState = State.valueOf(nbt.getString("incubator.state"));
        } catch (IllegalArgumentException e) {
            this.currentState = State.IDLE;
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, IncubatorBlockEntity entity) {
        if (world.isClient()) return;
        switch (entity.currentState) {
            case DONE:
                if (entity.getStack(3).isEmpty()) {
                    entity.setState(State.IDLE);
                }
                break;
            case ERROR:
                entity.setState(State.IDLE);
                break;
            case IDLE:
            case INCUBATING:
            default:
                break;
        }
        markDirty(world, pos, state);
    }

    public void startIncubation(PlayerEntity player) {
        if (!startIncubationLimiter.tryAcquire(player.getUuid())) return;
        if (this.currentState != State.IDLE || !canStartIncubation()) return;

        setState(State.INCUBATING);
        NbtCompound geneNbt = getStack(0).getNbt();

        AsyncJobs.submit(
            (ServerWorld) world,
            () -> {
                try {
                    Thread.sleep(10000);
                    ItemStack result = new ItemStack(ModItems.RECOMBINANT_PLASMID, 1);
                    if (geneNbt != null) {
                        result.setNbt(geneNbt.copy()); // Carry over the gene data
                    }
                    return result;
                } catch (InterruptedException e) {
                    return ItemStack.EMPTY;
                }
            },
            (outputStack) -> {
                if (!outputStack.isEmpty()) {
                    setStack(3, outputStack);
                    // Consume items only on success
                    removeStack(0, 1);
                    removeStack(1, 1);
                    removeStack(2, 1);
                    setState(State.DONE);
                } else {
                    setState(State.ERROR);
                }
            }
        );
    }

    private boolean canStartIncubation() {
        boolean hasFragment = getStack(0).getItem() == ModItems.AMPLIFIED_GENE_FRAGMENT && getStack(0).hasNbt();
        boolean hasVector = getStack(1).getItem() == ModItems.PLASMID_VECTOR;
        boolean hasLigase = getStack(2).getItem() == ModItems.DNA_LIGASE;
        return hasFragment && hasVector && hasLigase && getStack(3).isEmpty();
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
    public Text getDisplayName() { return Text.translatable("block.morerealisticgeneediting.incubator"); }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new IncubatorScreenHandler(syncId, inv, this, this.propertyDelegate);
    }
}
