package com.morerealisticgeneediting.project;
import java.util.UUID;
/** Server-recorded provenance of one virtual sample, not clinical efficacy evidence. */
public record ExperimentEvidence(UUID sampleId, long sampledRevision, long editedRevision, long verifiedRevision) {
    public ExperimentEvidence {
        if (sampleId == null || sampledRevision < 0 || editedRevision < -1 || verifiedRevision < -1)
            throw new IllegalArgumentException("Invalid evidence");
        if (editedRevision != -1 && editedRevision <= sampledRevision)
            throw new IllegalArgumentException("Edit must follow sampling");
        if (verifiedRevision != -1 && verifiedRevision != editedRevision)
            throw new IllegalArgumentException("Verification must match edited revision");
    }
    public static ExperimentEvidence sampled(UUID id, long revision) { return new ExperimentEvidence(id,revision,-1,-1); }
    public ExperimentEvidence edited(long revision) {
        if (revision <= sampledRevision || revision <= editedRevision) return this;
        return new ExperimentEvidence(sampleId,sampledRevision,revision,-1);
    }
    public ExperimentEvidence verified(long revision) {
        return revision == editedRevision && editedRevision > sampledRevision
            ? new ExperimentEvidence(sampleId,sampledRevision,editedRevision,revision) : this;
    }
    public boolean complete() { return editedRevision > sampledRevision && verifiedRevision == editedRevision; }
}
