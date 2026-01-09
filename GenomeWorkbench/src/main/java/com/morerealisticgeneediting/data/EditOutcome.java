package com.morerealisticgeneediting.data;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

/**
 * Represents the outcome of a gene editing experiment.
 * 
 * This class models the molecular result of CRISPR or other gene editing techniques,
 * including insertions, deletions, replacements, and their associated metadata.
 */
public class EditOutcome {

    /**
     * Types of editing outcomes
     */
    public enum Type {
        NO_CHANGE,      // No edit occurred
        DELETION,       // Bases were removed
        INSERTION,      // Bases were added
        REPLACEMENT,    // Bases were replaced (HDR with template)
        POINT_MUTATION, // Single base change
        COMPLEX_INDEL   // Complex rearrangement
    }

    private final UUID outcomeId;
    private final Type type;
    private final long position;
    private final int size;
    private final String sequence;
    private final String description;
    
    // Legacy fields for compatibility
    private UUID planId;
    private GenomePatch mainPatch;
    private GenomePatch bystanderPatch;
    private double offTargetRisk;
    private double chimerism;

    /**
     * Constructor for CRISPR engine results
     */
    public EditOutcome(Type type, long position, int size, String sequence, String description) {
        this.outcomeId = UUID.randomUUID();
        this.type = type;
        this.position = position;
        this.size = size;
        this.sequence = sequence;
        this.description = description;
        this.offTargetRisk = 0.0;
        this.chimerism = 0.0;
    }

    /**
     * Legacy constructor for existing code compatibility
     */
    public EditOutcome(UUID planId, GenomePatch mainPatch, GenomePatch bystanderPatch, double offTargetRisk, double chimerism) {
        this.outcomeId = UUID.randomUUID();
        this.planId = planId;
        this.mainPatch = mainPatch;
        this.bystanderPatch = bystanderPatch;
        this.offTargetRisk = offTargetRisk;
        this.chimerism = chimerism;
        
        // Derive type from patch
        this.type = deriveTypeFromPatch(mainPatch);
        this.position = mainPatch != null ? mainPatch.getFirstModificationPosition() : 0;
        this.size = mainPatch != null ? mainPatch.getTotalModificationSize() : 0;
        this.sequence = "";
        this.description = "Legacy edit outcome";
    }

    private EditOutcome(UUID outcomeId, Type type, long position, int size, String sequence,
                        String description, UUID planId, GenomePatch mainPatch, 
                        GenomePatch bystanderPatch, double offTargetRisk, double chimerism) {
        this.outcomeId = outcomeId;
        this.type = type;
        this.position = position;
        this.size = size;
        this.sequence = sequence;
        this.description = description;
        this.planId = planId;
        this.mainPatch = mainPatch;
        this.bystanderPatch = bystanderPatch;
        this.offTargetRisk = offTargetRisk;
        this.chimerism = chimerism;
    }

    private Type deriveTypeFromPatch(GenomePatch patch) {
        if (patch == null) return Type.NO_CHANGE;
        if (!patch.getDeletions().isEmpty() && !patch.getInsertions().isEmpty()) {
            return Type.COMPLEX_INDEL;
        } else if (!patch.getDeletions().isEmpty()) {
            return Type.DELETION;
        } else if (!patch.getInsertions().isEmpty()) {
            return Type.INSERTION;
        } else if (!patch.getPointMutations().isEmpty()) {
            return Type.POINT_MUTATION;
        }
        return Type.NO_CHANGE;
    }

    // Getters
    public UUID getOutcomeId() { return outcomeId; }
    public Type getType() { return type; }
    public long getPosition() { return position; }
    public int size() { return size; }
    public String getSequence() { return sequence; }
    public String getDescription() { return description; }
    public UUID getPlanId() { return planId; }
    public GenomePatch getMainPatch() { return mainPatch; }
    public GenomePatch getBystanderPatch() { return bystanderPatch; }
    public double getOffTargetRisk() { return offTargetRisk; }
    public double getChimerism() { return chimerism; }

    /**
     * Check if this outcome causes a frameshift (deletion/insertion not divisible by 3)
     */
    public boolean causesFrameshift() {
        if (type == Type.DELETION || type == Type.INSERTION) {
            return size % 3 != 0;
        }
        return false;
    }

    /**
     * Get a human-readable summary of the outcome
     */
    public String getSummary() {
        return switch (type) {
            case NO_CHANGE -> "No modification";
            case DELETION -> String.format("%dbp deletion at position %d", size, position);
            case INSERTION -> String.format("%dbp insertion at position %d", size, position);
            case REPLACEMENT -> String.format("%dbp replacement at position %d", size, position);
            case POINT_MUTATION -> String.format("Point mutation at position %d", position);
            case COMPLEX_INDEL -> String.format("Complex indel at position %d", position);
        };
    }

    /**
     * Convert to GenomePatch for applying to genome
     */
    public GenomePatch toPatch(UUID genomeId) {
        GenomePatch patch = new GenomePatch(genomeId);
        
        switch (type) {
            case DELETION -> patch.addDeletion(position, size);
            case INSERTION -> patch.addInsertion(position, sequence);
            case REPLACEMENT -> {
                patch.addDeletion(position, size);
                patch.addInsertion(position, sequence);
            }
            case POINT_MUTATION -> {
                if (!sequence.isEmpty()) {
                    byte base = switch (sequence.charAt(0)) {
                        case 'A', 'a' -> 0;
                        case 'C', 'c' -> 1;
                        case 'G', 'g' -> 2;
                        case 'T', 't' -> 3;
                        default -> 0;
                    };
                    patch.addPointMutation(position, base);
                }
            }
            default -> {}
        }
        
        return patch;
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("outcomeId", outcomeId);
        nbt.putString("type", type.name());
        nbt.putLong("position", position);
        nbt.putInt("size", size);
        nbt.putString("sequence", sequence != null ? sequence : "");
        nbt.putString("description", description != null ? description : "");
        
        if (planId != null) {
            nbt.putUuid("planId", planId);
        }
        if (mainPatch != null) {
            nbt.put("mainPatch", mainPatch.writeNbt());
        }
        if (bystanderPatch != null) {
            nbt.put("bystanderPatch", bystanderPatch.writeNbt());
        }
        nbt.putDouble("offTargetRisk", offTargetRisk);
        nbt.putDouble("chimerism", chimerism);
        return nbt;
    }

    public static EditOutcome fromNbt(NbtCompound nbt) {
        UUID outcomeId = nbt.getUuid("outcomeId");
        Type type = Type.valueOf(nbt.getString("type"));
        long position = nbt.getLong("position");
        int size = nbt.getInt("size");
        String sequence = nbt.getString("sequence");
        String description = nbt.getString("description");
        
        UUID planId = nbt.contains("planId") ? nbt.getUuid("planId") : null;
        GenomePatch mainPatch = nbt.contains("mainPatch") ? 
            GenomePatch.fromNbt(nbt.getCompound("mainPatch")) : null;
        GenomePatch bystanderPatch = nbt.contains("bystanderPatch") ? 
            GenomePatch.fromNbt(nbt.getCompound("bystanderPatch")) : null;
        double offTargetRisk = nbt.getDouble("offTargetRisk");
        double chimerism = nbt.getDouble("chimerism");
        
        return new EditOutcome(outcomeId, type, position, size, sequence, description,
                              planId, mainPatch, bystanderPatch, offTargetRisk, chimerism);
    }

    @Override
    public String toString() {
        return "EditOutcome{" +
               "type=" + type +
               ", position=" + position +
               ", size=" + size +
               ", description='" + description + '\'' +
               '}';
    }
}
