package com.morerealisticgeneediting.network;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.minecraft.util.Identifier;

public class C2SPackets {
    public static final Identifier REQUEST_GENOME_SLICE = new Identifier(MoreRealisticGeneEditing.MOD_ID, "request_genome_slice");
    public static final Identifier PERFORM_GENE_KNOCKOUT = new Identifier(MoreRealisticGeneEditing.MOD_ID, "perform_gene_knockout");
    public static final Identifier REQUEST_MOTIF_SEARCH = new Identifier(MoreRealisticGeneEditing.MOD_ID, "request_motif_search");
    public static final Identifier PERFORM_GENE_INSERTION = new Identifier(MoreRealisticGeneEditing.MOD_ID, "perform_gene_insertion");
    public static final Identifier OPEN_GENE_INSERTION_SCREEN = new Identifier(MoreRealisticGeneEditing.MOD_ID, "open_gene_insertion_screen");
}
