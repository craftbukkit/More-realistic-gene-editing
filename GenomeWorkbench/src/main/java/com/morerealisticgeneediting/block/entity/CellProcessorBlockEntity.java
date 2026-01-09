package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.recipe.CellProcessorRecipe;
import com.morerealisticgeneediting.screen.CellProcessorScreenHandler;
import com.morerealisticgeneediting.security.RateLimiters;
import com.morerealisticgeneediting.util.AsyncJobs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
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

import java.util.Optional;

public class CellProcessorBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {

    private enum State {
        IDLE, PROCESSING, DONE, ERROR
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);
    private State currentState = State.IDLE;
    private static final RateLimiters startProcessingLimiter = new RateLimiters(2000L);

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

    public CellProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CELL_PROCESSOR_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putString("processor.state", this.currentState.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        try {
            this.currentState = State.valueOf(nbt.getString("processor.state"));
        } catch (IllegalArgumentException e) {
            this.currentState = State.IDLE;
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, CellProcessorBlockEntity entity) {
        if (world.isClient()) return;

        switch (entity.currentState) {
            case DONE:
                if (entity.getStack(2).isEmpty()) {
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

    public void startProcessing(PlayerEntity player) {
        if (!startProcessingLimiter.tryAcquire(player.getUuid())) return;

        findRecipe().ifPresent(recipe -> {
            if (this.currentState != State.IDLE) return;

            setState(State.PROCESSING);

            ItemStack input = getStack(0);
            ItemStack reagent = getStack(1);
            ItemStack recipeOutput = recipe.getOutput().copy();

            AsyncJobs.submit(
                (ServerWorld) world,
                () -> {
                    try {
                        Thread.sleep(2500);
                        return recipeOutput;
                    } catch (InterruptedException e) {
                        return ItemStack.EMPTY;
                    }
                },
                (outputStack) -> {
                    if (!outputStack.isEmpty()) {
                        setStack(2, outputStack);
                        input.decrement(1);
                        reagent.decrement(1);
                        setState(State.DONE);
                    } else {
                        setState(State.ERROR);
                    }
                }
            );
        });
    }

    private Optional<CellProcessorRecipe> findRecipe() {
        if (world == null) return Optional.empty();
        SimpleInventory tempInventory = new SimpleInventory(this.size());
        for (int i = 0; i < this.size(); i++) {
            tempInventory.setStack(i, this.getStack(i).copy());
        }

        return world.getRecipeManager()
            .getFirstMatch(CellProcessorRecipe.Type.INSTANCE, tempInventory, world)
            .filter(recipe -> canAcceptOutput(recipe.getOutput()));
    }

    private boolean canAcceptOutput(ItemStack output) {
        ItemStack outputSlot = getStack(2);
        return outputSlot.isEmpty() || (ItemStack.areItemsEqual(outputSlot, output) && outputSlot.getCount() + output.getCount() <= outputSlot.getMaxCount());
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
        return Text.translatable("block.morerealisticgeneediting.cell_processor");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new CellProcessorScreenHandler(syncId, inv, this, this.propertyDelegate);
    }
}
