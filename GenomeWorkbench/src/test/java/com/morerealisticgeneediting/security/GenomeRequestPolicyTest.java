package com.morerealisticgeneediting.security;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class GenomeRequestPolicyTest {
    @Test void boundsCannotOverflowOrExceedPayloadBudget(){
        assertFalse(GenomeRequestPolicy.validRange(-1,10,100,4096));
        assertFalse(GenomeRequestPolicy.validRange(Long.MAX_VALUE-2,10,Long.MAX_VALUE,4096));
        assertFalse(GenomeRequestPolicy.validRange(0,4097,5000,4096));
        assertFalse(GenomeRequestPolicy.validRange(0,0,100,4096));
        assertTrue(GenomeRequestPolicy.validRange(90,10,100,4096));
    }
    @Test void emptyAmbiguousAndOversizedMotifsAreRejected(){
        assertFalse(GenomeRequestPolicy.validDna("",64));assertFalse(GenomeRequestPolicy.validDna("ACGN",64));
        assertFalse(GenomeRequestPolicy.validDna("A".repeat(65),64));assertTrue(GenomeRequestPolicy.validDna("acgt",64));
    }
    @Test void lateOrForeignRequestsCannotMatchActiveSession(){
        UUID sample=UUID.randomUUID();
        assertTrue(GenomeRequestPolicy.matches(sample,sample,3,3,4,4));
        assertFalse(GenomeRequestPolicy.matches(UUID.randomUUID(),sample,3,3,4,4));
        assertFalse(GenomeRequestPolicy.matches(sample,sample,2,3,4,4));
        assertFalse(GenomeRequestPolicy.matches(sample,sample,3,3,3,4));
        assertFalse(GenomeRequestPolicy.matches(sample,sample,3,3,-1,-1));
    }
}
