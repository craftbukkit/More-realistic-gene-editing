package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.data.ReadSet;
import com.morerealisticgeneediting.data.Sample;
import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.SequencerScreenHandler;
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

import java.util.concurrent.ThreadLocalRandom;

public class SequencerBlockEntity extends BlockEntity implements ImplementedInventory, NamedScreenHandlerFactory {

    private enum State {
        IDLE,       // Waiting for input and a start signal
        PROCESSING, // Heavy computation is in progress
        DONE,       // Processing is complete, output is available
        ERROR       // An error occurred during processing
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private State currentState = State.IDLE;
    private static final RateLimiters startSequencingLimiter = new RateLimiters(2000L); // 2-second cooldown

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return currentState.ordinal();
        }

        @Override
        public void set(int index, int value) {
            // This is a server-authoritative value. The client should not be able to change it.
        }

        @Override
        public int size() {
            return 1; // We only need to sync one integer: the state's ordinal.
        }
    };

    public SequencerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEQUENCER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putString("sequencer.state", this.currentState.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        try {
            this.currentState = State.valueOf(nbt.getString("sequencer.state"));
        } catch (IllegalArgumentException e) {
            this.currentState = State.IDLE; // Safely default to IDLE if the saved state is invalid
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, SequencerBlockEntity entity) {
        if (world.isClient()) return;

        switch (entity.currentState) {
            case DONE:
                // If the output is taken, reset to IDLE
                if (entity.getStack(1).isEmpty()) {
                    entity.setState(State.IDLE);
                }
                break;
            case ERROR:
                // After a few seconds in the ERROR state, reset to IDLE
                // (This could be a timer)
                entity.setState(State.IDLE);
                break;
            case IDLE:
            case PROCESSING:
            default:
                // Do nothing
                break;
        }
        markDirty(world, pos, state);
    }

    public void startSequencing(PlayerEntity player) {
        if (!startSequencingLimiter.tryAcquire(player.getUuid())) return;
        if (this.currentState != State.IDLE || !canStartSequencing()) return;

        setState(State.PROCESSING);
        ItemStack inputStack = getStack(0);
        NbtCompound sampleNbt = inputStack.getNbt().getCompound("sample_data");
        inputStack.decrement(1);

        AsyncJobs.submit(
            (ServerWorld) world,
            () -> {
                try {
                    Thread.sleep(5000);
                    Sample sample = Sample.fromNbt(sampleNbt);
                    long readCount = ThreadLocalRandom.current().nextLong(1_000_000, 5_000_000);
                    int avgLength = ThreadLocalRandom.current().nextInt(150, 300);
                    return new ReadSet(sample.getSampleId(), readCount, avgLength);
                } catch (InterruptedException | IllegalArgumentException e) {
                    return null;
                }
            },
            (readSet) -> {
                if (readSet != null) {
                    ItemStack outputStack = new ItemStack(ModItems.READ_SET_ITEM);
                    outputStack.setNbt(readSet.writeNbt());
                    setStack(1, outputStack);
                    setState(State.DONE);
                } else {
                    setState(State.ERROR);
                }
            }
        );
    }

    private boolean canStartSequencing() {
        ItemStack input = getStack(0);
        return !input.isEmpty() &&
               input.getItem() == ModItems.SAMPLE_ITEM &&
               input.hasNbt() &&
               input.getNbt().contains("sample_data") &&
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
        return Text.translatable("container.sequencer");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new SequencerScreenHandler(syncId, inv, this, this.propertyDelegate);
    }
}
