package com.morerealisticgeneediting.genome;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/** Physical aliquots and derived libraries retain the exact sample revision they came from. */
public final class SampleProvenance {
    private SampleProvenance() { }

    public static boolean matchesSnapshot(NbtCompound data, Genome genome) {
        return data != null && genome != null
            && data.containsUuid("GenomeId")
            && data.containsUuid("Owner")
            && data.contains("Revision", NbtElement.LONG_TYPE)
            && data.getUuid("GenomeId").equals(genome.getUUID())
            && data.getUuid("Owner").equals(genome.getOwner())
            && data.getLong("Revision") == genome.getRevision();
    }
}
