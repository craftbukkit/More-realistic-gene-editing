package com.morerealisticgeneediting.data;

import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

/**
 * Represents a set of sequencing reads.
 * This is a summary and does not contain the actual read data.
 */
public class ReadSet {

    private final UUID readSetId;
    private final UUID sourceSampleId;
    private final long readCount;
    private final int averageReadLength;

    public ReadSet(UUID sourceSampleId, long readCount, int averageReadLength) {
        this.readSetId = UUID.randomUUID();
        this.sourceSampleId = sourceSampleId;
        this.readCount = readCount;
        this.averageReadLength = averageReadLength;
    }

    private ReadSet(UUID readSetId, UUID sourceSampleId, long readCount, int averageReadLength) {
        this.readSetId = readSetId;
        this.sourceSampleId = sourceSampleId;
        this.readCount = readCount;
        this.averageReadLength = averageReadLength;
    }

    // Getters...

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("readSetId", readSetId);
        nbt.putUuid("sourceSampleId", sourceSampleId);
        nbt.putLong("readCount", readCount);
        nbt.putInt("averageReadLength", averageReadLength);
        return nbt;
    }

    public static ReadSet fromNbt(NbtCompound nbt) {
        return new ReadSet(
            nbt.getUuid("readSetId"),
            nbt.getUuid("sourceSampleId"),
            nbt.getLong("readCount"),
            nbt.getInt("averageReadLength")
        );
    }
}
