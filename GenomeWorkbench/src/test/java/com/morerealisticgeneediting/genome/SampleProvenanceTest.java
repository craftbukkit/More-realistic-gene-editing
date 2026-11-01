package com.morerealisticgeneediting.genome;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SampleProvenanceTest {
    private static NbtCompound provenance(Genome genome) {
        NbtCompound data = new NbtCompound();
        data.putUuid("GenomeId", genome.getUUID());
        data.putUuid("Owner", genome.getOwner());
        data.putLong("Revision", genome.getRevision());
        return data;
    }

    @Test void oldAliquotCannotVerifyAnEditedRevisionOfTheSameSample() {
        Genome before = Genome.createFromUnpackedSequence(UUID.randomUUID(), "ACGTACGT");
        NbtCompound aliquot = provenance(before);
        Genome after = before.performInsertion(2, "TT");
        assertTrue(SampleProvenance.matchesSnapshot(aliquot, before));
        assertFalse(SampleProvenance.matchesSnapshot(aliquot, after));
        aliquot.putLong("Revision", after.getRevision());
        assertTrue(SampleProvenance.matchesSnapshot(aliquot, after));
    }

    @Test void zeroRevisionDoesNotAcceptMissingOrIncorrectlyTypedFields() {
        Genome genome = Genome.createFromUnpackedSequence(UUID.randomUUID(), "ACGT");
        NbtCompound missingRevision = provenance(genome); missingRevision.remove("Revision");
        NbtCompound integerRevision = provenance(genome); integerRevision.putInt("Revision", 0);
        NbtCompound missingOwner = provenance(genome); missingOwner.remove("Owner");
        NbtCompound stringOwner = provenance(genome); stringOwner.putString("Owner", genome.getOwner().toString());
        NbtCompound missingId = provenance(genome); missingId.remove("GenomeId");
        for (NbtCompound invalid : new NbtCompound[]{missingRevision, integerRevision, missingOwner, stringOwner, missingId})
            assertFalse(SampleProvenance.matchesSnapshot(invalid, genome));
    }

    @Test void derivedLibraryRetainsExactIdentityAndOwner() {
        Genome genome = Genome.createFromUnpackedSequence(UUID.randomUUID(), "ACGT").performInsertion(1, "A");
        NbtCompound library = provenance(genome);
        library.putString("SampleType", "sequencing_library");
        library.putString("ProcessingStep", "library_preparation");
        assertTrue(SampleProvenance.matchesSnapshot(library, genome));
        NbtCompound wrongOwner = library.copy(); wrongOwner.putUuid("Owner", UUID.randomUUID());
        NbtCompound wrongSample = library.copy(); wrongSample.putUuid("GenomeId", UUID.randomUUID());
        assertFalse(SampleProvenance.matchesSnapshot(wrongOwner, genome));
        assertFalse(SampleProvenance.matchesSnapshot(wrongSample, genome));
    }
}
