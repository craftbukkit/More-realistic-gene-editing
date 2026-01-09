package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.ethics.EthicsCase;
import com.morerealisticgeneediting.ethics.EthicsManager;
import com.morerealisticgeneediting.inventory.ImplementedInventory;
import com.morerealisticgeneediting.screen.EthicsCaseScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EthicsCaseBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(0, ItemStack.EMPTY);
    @Nullable
    private EthicsCase ethicsCase;
    @Nullable
    private UUID assignedPlayerId;

    public EthicsCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ETHICS_CASE_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("ethicsCase")) {
            this.ethicsCase = EthicsManager.getCase(nbt.getString("ethicsCase"));
        }
        if (nbt.containsUuid("assignedPlayer")) {
            this.assignedPlayerId = nbt.getUuid("assignedPlayer");
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.ethicsCase != null) {
            nbt.putString("ethicsCase", this.ethicsCase.getId());
        }
        if (this.assignedPlayerId != null) {
            nbt.putUuid("assignedPlayer", this.assignedPlayerId);
        }
    }

    @Nullable
    public EthicsCase getEthicsCase() {
        return this.ethicsCase;
    }

    public void setEthicsCase(EthicsCase ethicsCase, PlayerEntity player) {
        this.ethicsCase = ethicsCase;
        this.assignedPlayerId = player.getUuid();
        markDirty();
    }

    public boolean makeChoice(PlayerEntity player, int choiceIndex) {
        if (this.ethicsCase == null || this.world == null || this.world.isClient()) {
            return false;
        }

        if (!player.getUuid().equals(this.assignedPlayerId)) {
            return false;
        }

        this.ethicsCase.handleChoice((ServerPlayerEntity) player, choiceIndex);

        world.removeBlock(pos, false);

        return true;
    }


    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.morerealisticgeneediting.ethics_case");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new EthicsCaseScreenHandler(syncId, inv, this, this);
    }
}
