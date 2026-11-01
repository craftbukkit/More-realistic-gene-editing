package com.morerealisticgeneediting.project;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectPersistenceTest {
    @Test void worldSaveRoundTripPreservesEvidenceAndSeparatesPlayers() {
        var first=UUID.randomUUID();var other=UUID.randomUUID();var sample=UUID.randomUUID();
        var store=new ProjectStatePersistence();
        store.setPlayerState(first,new PlayerProjectState(Set.of("simulated_sample_edit"),Set.of("welcome_to_the_lab")));
        store.acknowledgeIntroduction(first);
        store.putEvidence(first,ExperimentEvidence.sampled(sample,0).edited(3).verified(3));
        var restored=ProjectStatePersistence.fromNbt(store.writeNbt(new NbtCompound(),null),null);
        assertEquals(store.getPlayerState(first),restored.getPlayerState(first));
        assertTrue(restored.hasReadIntroduction(first));
        assertTrue(restored.getEvidence(first,sample).orElseThrow().complete());
        assertTrue(restored.getEvidence(other).isEmpty());
        assertEquals(PlayerProjectState.EMPTY,restored.getPlayerState(other));
    }
    @Test void olderSavesCannotPreserveUnverifiedCompletionFlags() {
        var player=UUID.randomUUID();var row=new NbtCompound();row.putUuid("UUID",player);
        NbtList completed=new NbtList();completed.add(NbtString.of("blood_disease_beam101"));row.put("CompletedProjects",completed);
        NbtList players=new NbtList();players.add(row);var old=new NbtCompound();old.put("PlayerProjectStates",players);
        assertTrue(ProjectStatePersistence.fromNbt(old,null).getPlayerState(player).completedProjectIds().isEmpty());
    }
    @Test void evidenceHistoryIsBoundedAndRetainsLatestRecords() {
        var player=UUID.randomUUID();var store=new ProjectStatePersistence();UUID first=UUID.randomUUID();
        store.putEvidence(player,ExperimentEvidence.sampled(first,0));
        UUID last=null;
        for(int i=0;i<40;i++){last=UUID.randomUUID();store.putEvidence(player,ExperimentEvidence.sampled(last,0));}
        assertEquals(32,store.getEvidence(player).size());assertTrue(store.getEvidence(player,first).isEmpty());assertTrue(store.getEvidence(player,last).isPresent());
    }
}
