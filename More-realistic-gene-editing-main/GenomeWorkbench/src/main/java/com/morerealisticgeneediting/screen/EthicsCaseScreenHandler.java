package com.morerealisticgeneediting.screen;

import com.morerealisticgeneediting.block.entity.EthicsCaseBlockEntity;
import com.morerealisticgeneediting.ethics.EthicsCase;
import com.morerealisticgeneediting.util.NetGuards;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

public class EthicsCaseScreenHandler extends ScreenHandler {
    @Nullable
    public final EthicsCase ethicsCase;
    @Nullable
    private final EthicsCaseBlockEntity blockEntity;
    private final Inventory inventory;

    // Client-side constructor
    public EthicsCaseScreenHandler(int syncId, PlayerInventory playerInventory) {
        // The client doesn't need a real inventory, so a dummy one is sufficient.
        this(syncId, playerInventory, new SimpleInventory(0), null);
    }

    // Server-side constructor
    public EthicsCaseScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, @Nullable EthicsCaseBlockEntity blockEntity) {
        super(ModScreenHandlers.ETHICS_CASE_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        this.blockEntity = blockEntity;
        this.ethicsCase = (blockEntity != null) ? blockEntity.getEthicsCase() : null;
        inventory.onOpen(playerInventory.player);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (blockEntity == null) {
            return false;
        }
        // id represents the choice index.
        // We add a proximity guard to prevent remote exploitation.
        if (NetGuards.isPlayerClose(player, blockEntity.getWorld(), blockEntity.getPos())) {
            // Delegate the choice handling to the block entity for secure processing.
            return blockEntity.makeChoice(player, id);
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // This is a crucial security check to ensure the player is allowed to interact with the block.
        return this.inventory.canPlayerUse(player);
    }
}
