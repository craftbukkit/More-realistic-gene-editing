package com.morerealisticgeneediting.data;

import com.morerealisticgeneediting.util.SafeNBT;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

/**
 * Represents a reference to a known, base genome sequence.
 * This object is lightweight and only contains metadata, not the sequence itself.
 */
public class GenomeRef {

    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_SOURCE_LENGTH = 256;

    private final UUID genomeId;
    private final String name;
    private final String sourceOrganism;
    private final long length;

    public GenomeRef(String name, String sourceOrganism, long length) {
        this.genomeId = UUID.randomUUID();
        this.name = name;
        this.sourceOrganism = sourceOrganism;
        this.length = length;
    }

    private GenomeRef(UUID genomeId, String name, String sourceOrganism, long length) {
        this.genomeId = genomeId;
        this.name = name;
        this.sourceOrganism = sourceOrganism;
        this.length = length;
    }

    public UUID getGenomeId() {
        return genomeId;
    }

    public String getName() {
        return name;
    }

    public String getSourceOrganism() {
        return sourceOrganism;
    }

    public long getLength() {
        return length;
    }

    /**
     * Writes the reference to an NBT compound safely.
     */
    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("genomeId", genomeId);
        nbt.putLong("length", length);
        SafeNBT.putString(nbt, "name", name, MAX_NAME_LENGTH);
        SafeNBT.putString(nbt, "sourceOrganism", sourceOrganism, MAX_SOURCE_LENGTH);
        return nbt;
    }

    /**
     * Creates a GenomeRef from an NBT compound.
     */
    public static GenomeRef fromNbt(NbtCompound nbt) {
        UUID genomeId = nbt.getUuid("genomeId");
        long length = nbt.getLong("length");
        String name = nbt.getString("name");
        String sourceOrganism = nbt.getString("sourceOrganism");

        if (name.length() > MAX_NAME_LENGTH || sourceOrganism.length() > MAX_SOURCE_LENGTH) {
             throw new IllegalArgumentException("String length exceeds limit in GenomeRef NBT");
        }

        return new GenomeRef(genomeId, name, sourceOrganism, length);
    }
}
