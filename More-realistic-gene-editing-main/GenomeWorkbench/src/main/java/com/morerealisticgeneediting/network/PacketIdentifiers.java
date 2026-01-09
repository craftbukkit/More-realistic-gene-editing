package com.morerealisticgeneediting.network;

import com.morerealisticgeneediting.MoreRealisticGeneEditing;
import net.minecraft.util.Identifier;

public class PacketIdentifiers {
    // C2S
    public static final Identifier REQUEST_GENOME_SLICE = new Identifier(MoreRealisticGeneEditing.MOD_ID, "request_genome_slice");
    public static final Identifier PERFORM_GENE_KNOCKOUT = new Identifier(MoreRealisticGeneEditing.MOD_ID, "perform_gene_knockout");
    public static final Identifier REQUEST_MOTIF_SEARCH = new Identifier(MoreRealisticGeneEditing.MOD_ID, "request_motif_search");

    // S2C
    public static final Identifier SEND_GENOME_SLICE = new Identifier(MoreRealisticGeneEditing.MOD_ID, "send_genome_slice");
    public static final Identifier SEND_MOTIF_SEARCH_RESULTS = new Identifier(MoreRealisticGeneEditing.MOD_ID, "send_motif_search_results");
}
