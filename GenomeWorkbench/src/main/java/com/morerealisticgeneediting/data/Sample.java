package com.morerealisticgeneediting.data;

import com.morerealisticgeneediting.util.SafeNBT;
import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

/**
 * Represents a biological sample, which has a genome.
 */
public class Sample {

    private static final int MAX_SOURCE_ENTITY_NAME_LENGTH = 128;

    private final UUID sampleId;
    private final UUID genomeRefId; // The base reference genome
    private final UUID genomePatchId; // The patch containing modifications
    private final String sourceEntityName;

    public Sample(UUID genomeRefId, UUID genomePatchId, String sourceEntityName) {
        this.sampleId = UUID.randomUUID();
        this.genomeRefId = genomeRefId;
        this.genomePatchId = genomePatchId;
        this.sourceEntityName = sourceEntityName;
    }

    private Sample(UUID sampleId, UUID genomeRefId, UUID genomePatchId, String sourceEntityName) {
        this.sampleId = sampleId;
        this.genomeRefId = genomeRefId;
        this.genomePatchId = genomePatchId;
        this.sourceEntityName = sourceEntityName;
    }

    // Getters...

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("sampleId", sampleId);
        nbt.putUuid("genomeRefId", genomeRefId);
        nbt.putUuid("genomePatchId", genomePatchId);
        SafeNBT.putString(nbt, "sourceEntityName", sourceEntityName, MAX_SOURCE_ENTITY_NAME_LENGTH);
        return nbt;
    }

    public static Sample fromNbt(NbtCompound nbt) {
        String name = nbt.getString("sourceEntityName");
        if (name.length() > MAX_SOURCE_ENTITY_NAME_LENGTH) {
            throw new IllegalArgumentException("sourceEntityName exceeds max length in NBT");
        }
        return new Sample(
            nbt.getUuid("sampleId"),
            nbt.getUuid("genomeRefId"),
            nbt.getUuid("genomePatchId"),
            name
        );
    }
}
