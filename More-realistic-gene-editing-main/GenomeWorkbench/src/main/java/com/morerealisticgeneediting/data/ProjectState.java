package com.morerealisticgeneediting.data;

import com.morerealisticgeneediting.util.SafeNBT;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the state of a research project.
 * Refactored for security, state integrity, and NBT safety.
 */
public class ProjectState {

    public enum Step {
        STARTED,
        SAMPLE_COLLECTED,
        CONSTRUCT_DESIGNED,
        EDIT_PERFORMED,
        ANALYSIS_COMPLETE,
        PEER_REVIEWED,
        COMPLETED
    }

    private static final int MAX_EVIDENCE_CHAIN_SIZE = 64;

    private final UUID projectStateId;
    private final UUID projectId;
    private final UUID playerOwner;
    private Step currentStep;
    private final List<UUID> evidenceChain;
    private double score;
    private boolean completed;
    private boolean rewardClaimed; // For idempotency

    public ProjectState(UUID projectId, UUID playerOwner) {
        this.projectStateId = UUID.randomUUID();
        this.projectId = projectId;
        this.playerOwner = playerOwner;
        this.currentStep = Step.STARTED;
        this.evidenceChain = new ArrayList<>();
        this.completed = false;
        this.rewardClaimed = false;
    }

    // Private constructor for NBT deserialization
    private ProjectState(UUID projectStateId, UUID projectId, UUID playerOwner, Step currentStep, List<UUID> evidenceChain, double score, boolean completed, boolean rewardClaimed) {
        this.projectStateId = projectStateId;
        this.projectId = projectId;
        this.playerOwner = playerOwner;
        this.currentStep = currentStep;
        this.evidenceChain = evidenceChain;
        this.score = score;
        this.completed = completed;
        this.rewardClaimed = rewardClaimed;
    }

    // --- Getters ---
    public UUID getProjectStateId() { return projectStateId; }
    public UUID getProjectId() { return projectId; }
    public UUID getPlayerOwner() { return playerOwner; }
    public Step getCurrentStep() { return currentStep; }
    public List<UUID> getEvidenceChain() { return List.copyOf(evidenceChain); } // Return immutable copy
    public double getScore() { return score; }
    public boolean isCompleted() { return completed; }
    public boolean isRewardClaimed() { return rewardClaimed; }

    // --- Server-Side State Transitions ---

    /**
     * Advances the project to a new step. Logic should be server-side.
     */
    public void advanceTo(Step newStep) {
        // In a real implementation, we would check for valid state transitions
        this.currentStep = newStep;
    }

    /**
     * Adds a piece of evidence, checking for size limits. Server-side only.
     */
    public boolean addEvidence(UUID reportId) {
        if (evidenceChain.size() < MAX_EVIDENCE_CHAIN_SIZE) {
            return this.evidenceChain.add(reportId);
        }
        return false;
    }

    /**
     * Completes the project. Server-side only.
     */
    public void completeProject(double finalScore) {
        this.score = finalScore;
        this.completed = true;
        this.currentStep = Step.COMPLETED;
    }

    /**
     * Marks the reward as claimed. Server-side only.
     */
    public void claimReward() {
        this.rewardClaimed = true;
    }

    // --- NBT Serialization ---

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("projectStateId", projectStateId);
        nbt.putUuid("projectId", projectId);
        nbt.putUuid("playerOwner", playerOwner);
        nbt.putString("currentStep", currentStep.name());
        nbt.putDouble("score", score);
        nbt.putBoolean("completed", completed);
        nbt.putBoolean("rewardClaimed", rewardClaimed);

        NbtList evidenceNbt = new NbtList();
        for (UUID uuid : evidenceChain) {
            evidenceNbt.add(NbtString.of(uuid.toString()));
        }
        SafeNBT.putList(nbt, "evidenceChain", evidenceNbt, MAX_EVIDENCE_CHAIN_SIZE);

        return nbt;
    }

    public static ProjectState fromNbt(NbtCompound nbt) {
        UUID projectStateId = nbt.getUuid("projectStateId");
        UUID projectId = nbt.getUuid("projectId");
        UUID playerOwner = nbt.getUuid("playerOwner");
        Step step = Step.valueOf(nbt.getString("currentStep"));
        double score = nbt.getDouble("score");
        boolean completed = nbt.getBoolean("completed");
        boolean rewardClaimed = nbt.getBoolean("rewardClaimed");

        NbtList evidenceNbt = nbt.getList("evidenceChain", NbtElement.STRING_TYPE);
        if (evidenceNbt.size() > MAX_EVIDENCE_CHAIN_SIZE) {
            throw new IllegalArgumentException("Evidence chain exceeds max size in NBT");
        }
        List<UUID> evidenceChain = new ArrayList<>();
        for (NbtElement element : evidenceNbt) {
            evidenceChain.add(UUID.fromString(element.asString()));
        }

        return new ProjectState(projectStateId, projectId, playerOwner, step, evidenceChain, score, completed, rewardClaimed);
    }
}
