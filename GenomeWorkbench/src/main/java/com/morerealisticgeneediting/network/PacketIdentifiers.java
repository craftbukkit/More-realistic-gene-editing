package com.morerealisticgeneediting.network;
import net.minecraft.util.Identifier;
/** Stable namespace names. Typed payload IDs are defined on their records. */
public final class PacketIdentifiers {
    public static final Identifier SEND_GENOME_SLICE=Identifier.of("morerealisticgeneediting","send_genome_slice");
    public static final Identifier SEND_MOTIF_SEARCH_RESULTS=Identifier.of("morerealisticgeneediting","send_motif_search_results");
}
