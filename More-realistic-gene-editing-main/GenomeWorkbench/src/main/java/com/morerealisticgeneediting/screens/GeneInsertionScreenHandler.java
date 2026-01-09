package com.morerealisticgeneediting.screens;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

import java.util.UUID;

public class GeneInsertionScreenHandler extends ScreenHandler {
    private final String genomeId;
    private final long knockoutPosition;

    public GeneInsertionScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(MoreRealisticGeneEditing.GENE_INSERTION_SCREEN_HANDLER, syncId, playerInventory, buf.readUuid().toString(), buf.readLong());
    }

    public GeneInsertionScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, String genomeId, long knockoutPosition) {
        super(type, syncId);
        this.genomeId = genomeId;
        this.knockoutPosition = knockoutPosition;
    }

    public String getGenomeId() {
        return genomeId;
    }

    public long getKnockoutPosition() {
        return knockoutPosition;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
