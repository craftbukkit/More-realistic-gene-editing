package com.morerealisticgeneediting.data;

import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

/**
 * Defines a plan for a gene editing experiment.
 */
public class EditPlan {

    private final UUID planId;
    private final UUID targetSampleId;
    private final UUID constructId;
    private final long targetLocus;

    public EditPlan(UUID targetSampleId, UUID constructId, long targetLocus) {
        this.planId = UUID.randomUUID();
        this.targetSampleId = targetSampleId;
        this.constructId = constructId;
        this.targetLocus = targetLocus;
    }

    private EditPlan(UUID planId, UUID targetSampleId, UUID constructId, long targetLocus) {
        this.planId = planId;
        this.targetSampleId = targetSampleId;
        this.constructId = constructId;
        this.targetLocus = targetLocus;
    }

    // Getters...

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("planId", planId);
        nbt.putUuid("targetSampleId", targetSampleId);
        nbt.putUuid("constructId", constructId);
        nbt.putLong("targetLocus", targetLocus);
        return nbt;
    }

    public static EditPlan fromNbt(NbtCompound nbt) {
        return new EditPlan(
            nbt.getUuid("planId"),
            nbt.getUuid("targetSampleId"),
            nbt.getUuid("constructId"),
            nbt.getLong("targetLocus")
        );
    }
}
