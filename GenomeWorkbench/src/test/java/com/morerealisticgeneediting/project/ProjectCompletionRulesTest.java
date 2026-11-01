package com.morerealisticgeneediting.project;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectCompletionRulesTest {
    private static ResearchProject project(String mode) {
        return new ResearchProject("exercise","Exercise","A virtual sample task",List.of(),List.of(),List.of("intro"),mode,List.of());
    }
    private static PlayerProjectState active() { return new PlayerProjectState(Set.of("exercise"),Set.of("intro")); }
    @Test void activeFlagAloneCannotCompleteAnExperiment() {
        assertFalse(ProjectCompletionRules.mayComplete(project("sample_edit_verify"),active(),List.of(),true));
    }
    @Test void prerequisiteMustBeCompletedBeforeStarting() {
        assertFalse(ProjectCompletionRules.mayStart(project("sample_edit_verify"),PlayerProjectState.EMPTY));
        assertTrue(ProjectCompletionRules.mayStart(project("sample_edit_verify"),new PlayerProjectState(Set.of(),Set.of("intro"))));
    }
    @Test void cannotReplayCompletedTask() {
        assertFalse(ProjectCompletionRules.mayStart(project("sample_edit_verify"),new PlayerProjectState(Set.of(),Set.of("intro","exercise"))));
    }
    @Test void sameSampleOrderedEvidenceCanCompleteTeachingTask() {
        var e=ExperimentEvidence.sampled(UUID.randomUUID(),0).edited(1).verified(1);
        assertTrue(ProjectCompletionRules.mayComplete(project("sample_edit_verify"),active(),List.of(e),false));
    }
    @Test void evidenceFromDifferentSamplesCannotBeCombined() {
        var edited=ExperimentEvidence.sampled(UUID.randomUUID(),0).edited(1);
        var sampled=ExperimentEvidence.sampled(UUID.randomUUID(),0).verified(1);
        assertFalse(ProjectCompletionRules.mayComplete(project("sample_edit_verify"),active(),List.of(edited,sampled),true));
    }
    @Test void staleOrPrematureVerificationCannotCompleteTask() {
        var e=ExperimentEvidence.sampled(UUID.randomUUID(),0);
        assertFalse(e.verified(1).complete());
        assertFalse(e.edited(2).verified(1).complete());
        assertFalse(e.edited(1).verified(1).edited(2).complete());
    }
    @Test void clinicalReferenceCannotBeCompletedWithGenericEvidence() {
        var e=ExperimentEvidence.sampled(UUID.randomUUID(),0).edited(1).verified(1);
        assertFalse(ProjectCompletionRules.mayComplete(project("reference_only"),active(),List.of(e),true));
        assertFalse(ProjectCompletionRules.mayStart(project("reference_only"),new PlayerProjectState(Set.of(),Set.of("intro"))));
    }
    @Test void introductionRequiresReadAcknowledgement() {
        assertFalse(ProjectCompletionRules.mayComplete(project("introduction"),active(),List.of(),false));
        assertTrue(ProjectCompletionRules.mayComplete(project("introduction"),active(),List.of(),true));
    }
    @Test void evidenceAndStateRejectContradictionsAndMutableAliases() {
        UUID id=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new ExperimentEvidence(id,2,1,1));
        assertThrows(IllegalArgumentException.class,()->new ExperimentEvidence(id,0,2,1));
        assertThrows(IllegalArgumentException.class,()->new PlayerProjectState(Set.of("x"),Set.of("x")));
        Set<String> mutable=new HashSet<>();var state=new PlayerProjectState(mutable,Set.of());mutable.add("x");assertFalse(state.isActive("x"));
    }
}
