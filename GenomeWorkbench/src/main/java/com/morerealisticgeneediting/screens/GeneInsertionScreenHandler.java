package com.morerealisticgeneediting.screens;
import net.minecraft.entity.player.PlayerInventory;
public class GeneInsertionScreenHandler extends GenomeSessionScreenHandler {
    private final long pamPosition;
    public GeneInsertionScreenHandler(int syncId,PlayerInventory inventory,GeneInsertionOpenData data) {
        super(com.morerealisticgeneediting.screen.ModScreenHandlers.GENE_INSERTION_SCREEN_HANDLER,syncId,data.genomeId(),data.pos(),data.totalLength(),data.revision(),data.portable());
        this.pamPosition=data.pamPosition();
    }
    public String getGenomeId() { return genomeId.toString(); }
    public long getKnockoutPosition() { return pamPosition; }
}
