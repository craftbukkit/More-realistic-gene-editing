package com.morerealisticgeneediting.screens;
import net.minecraft.entity.player.PlayerInventory;
public class GenomeTerminalScreenHandler extends GenomeSessionScreenHandler {
    public GenomeTerminalScreenHandler(int syncId,PlayerInventory inventory,TerminalOpenData data) {
        super(com.morerealisticgeneediting.screen.ModScreenHandlers.GENOME_TERMINAL_SCREEN_HANDLER,syncId,data.genomeId(),data.pos(),data.totalLength(),data.revision(),data.portable());
    }
}
