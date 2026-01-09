package com.morerealisticgeneediting.screens;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;

import java.util.UUID;

public class GenomeTerminalScreenHandler extends ScreenHandler {

    public final UUID genomeId;

    // Client-side constructor
    public GenomeTerminalScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readUuid());
    }

    // Server-side constructor
    public GenomeTerminalScreenHandler(int syncId, PlayerInventory playerInventory, UUID genomeId) {
        super(ModScreenHandlers.GENOME_TERMINAL_SCREEN_HANDLER, syncId);
        this.genomeId = genomeId;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
