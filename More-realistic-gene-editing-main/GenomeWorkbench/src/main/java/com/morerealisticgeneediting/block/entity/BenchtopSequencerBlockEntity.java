package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.EquipmentTier;
import com.morerealisticgeneediting.item.LabEquipmentItems;
import com.morerealisticgeneediting.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Benchtop Sequencer Block Entity - High-throughput short-read sequencing.
 * 
 * Based on: MGI Tech DNBSEQ-G99
 * - Flow cells: 2 (can run simultaneously)
 * - Read length: 50-150bp
 * - Output: Up to 720 Gb per run
 * - Run time: 12-48 hours (accelerated in game)
 * - DNB (DNA Nanoball) technology
 * 
 * Process:
 * 1. Load prepared library (DNA fragments with adapters)
 * 2. Insert flow cell
 * 3. Start sequencing run
 * 4. Collect sequence data
 * 
 * Output:
 * - FASTQ-like data stored in NBT
 * - Quality scores (Phred)
 * - Coverage statistics
 */
public class BenchtopSequencerBlockEntity extends LabEquipmentBlockEntity {

    // Slots
    public static final int LIBRARY_SLOT_A = 0;     // DNA library for flow cell A
    public static final int LIBRARY_SLOT_B = 1;     // DNA library for flow cell B
    public static final int FLOWCELL_SLOT_A = 2;    // Flow cell A
    public static final int FLOWCELL_SLOT_B = 3;    // Flow cell B
    public static final int REAGENT_SLOT = 4;       // Sequencing reagents
    public static final int OUTPUT_SLOT_A = 5;      // Results A
    public static final int OUTPUT_SLOT_B = 6;      // Results B

    // Sequencing state
    public enum SequencingPhase {
        IDLE,
        LOADING,            // Loading reagents
        CLUSTER_GENERATION, // DNB generation
        SEQUENCING,         // Active sequencing
        ANALYZING,          // Base calling
        COMPLETE
    }

    // Per-flow-cell state
    private SequencingPhase phaseA = SequencingPhase.IDLE;
    private SequencingPhase phaseB = SequencingPhase.IDLE;
    private int progressA = 0;
    private int progressB = 0;
    private int maxProgressA = 0;
    private int maxProgressB = 0;

    // Sequencing parameters
    private int readLength = 100;           // bp per read
    private boolean pairedEnd = true;       // PE or SE
    private int targetCoverage = 30;        // X coverage
    
    // Run statistics (per flow cell)
    private long totalReadsA = 0;
    private long totalReadsB = 0;
    private long totalBasesA = 0;
    private long totalBasesB = 0;
    private float meanQualityA = 0;
    private float meanQualityB = 0;
    private float q30PercentA = 0;          // % bases >= Q30
    private float q30PercentB = 0;
    private int currentCycleA = 0;
    private int currentCycleB = 0;

    // Real-time quality tracking
    private float[] qualityPerCycleA;
    private float[] qualityPerCycleB;

    public BenchtopSequencerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 7);
        this.tier = EquipmentTier.ADVANCED;
        qualityPerCycleA = new float[150];
        qualityPerCycleB = new float[150];
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= LIBRARY_SLOT_A && slot <= REAGENT_SLOT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT_A || slot == OUTPUT_SLOT_B;
    }

    @Override
    protected boolean canProcess() {
        return canStartFlowCellA() || canStartFlowCellB();
    }

    private boolean canStartFlowCellA() {
        if (phaseA != SequencingPhase.IDLE) return false;
        return !getStack(LIBRARY_SLOT_A).isEmpty() &&
               !getStack(FLOWCELL_SLOT_A).isEmpty() &&
               !getStack(REAGENT_SLOT).isEmpty() &&
               getStack(OUTPUT_SLOT_A).isEmpty();
    }

    private boolean canStartFlowCellB() {
        if (phaseB != SequencingPhase.IDLE) return false;
        return !getStack(LIBRARY_SLOT_B).isEmpty() &&
               !getStack(FLOWCELL_SLOT_B).isEmpty() &&
               !getStack(REAGENT_SLOT).isEmpty() &&
               getStack(OUTPUT_SLOT_B).isEmpty();
    }

    public void startSequencing(boolean flowCellA) {
        if (flowCellA && canStartFlowCellA()) {
            phaseA = SequencingPhase.LOADING;
            progressA = 0;
            maxProgressA = 100;  // Loading phase
            currentCycleA = 0;
            totalReadsA = 0;
            totalBasesA = 0;
            setActive(true);
        } else if (!flowCellA && canStartFlowCellB()) {
            phaseB = SequencingPhase.LOADING;
            progressB = 0;
            maxProgressB = 100;
            currentCycleB = 0;
            totalReadsB = 0;
            totalBasesB = 0;
            setActive(true);
        }
        markDirty();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;

        boolean wasActive = isActive();
        
        // Tick flow cell A
        if (phaseA != SequencingPhase.IDLE && phaseA != SequencingPhase.COMPLETE) {
            tickFlowCell(true);
        }

        // Tick flow cell B
        if (phaseB != SequencingPhase.IDLE && phaseB != SequencingPhase.COMPLETE) {
            tickFlowCell(false);
        }

        // Update active state
        boolean shouldBeActive = (phaseA != SequencingPhase.IDLE && phaseA != SequencingPhase.COMPLETE) ||
                                (phaseB != SequencingPhase.IDLE && phaseB != SequencingPhase.COMPLETE);
        
        if (wasActive != shouldBeActive) {
            setActive(shouldBeActive);
        }
    }

    private void tickFlowCell(boolean isA) {
        int progress = isA ? progressA : progressB;
        int maxProgress = isA ? maxProgressA : maxProgressB;
        SequencingPhase phase = isA ? phaseA : phaseB;

        progress++;

        if (progress >= maxProgress) {
            advancePhase(isA);
            progress = 0;
        } else if (phase == SequencingPhase.SEQUENCING) {
            // Update cycle progress
            int cycle = isA ? currentCycleA : currentCycleB;
            int cyclesPerTick = 1;
            cycle += cyclesPerTick;
            
            if (isA) {
                currentCycleA = Math.min(readLength * (pairedEnd ? 2 : 1), cycle);
            } else {
                currentCycleB = Math.min(readLength * (pairedEnd ? 2 : 1), cycle);
            }

            // Simulate data generation
            simulateSequencing(isA);
        }

        if (isA) {
            progressA = progress;
        } else {
            progressB = progress;
        }

        markDirty();
    }

    private void advancePhase(boolean isA) {
        SequencingPhase phase = isA ? phaseA : phaseB;
        SequencingPhase nextPhase;
        int nextMaxProgress;

        switch (phase) {
            case LOADING -> {
                nextPhase = SequencingPhase.CLUSTER_GENERATION;
                nextMaxProgress = 200;  // DNB generation
            }
            case CLUSTER_GENERATION -> {
                nextPhase = SequencingPhase.SEQUENCING;
                // Sequencing time based on read length and PE/SE
                int totalCycles = readLength * (pairedEnd ? 2 : 1);
                nextMaxProgress = totalCycles * 2;  // Accelerated
            }
            case SEQUENCING -> {
                nextPhase = SequencingPhase.ANALYZING;
                nextMaxProgress = 100;  // Base calling
            }
            case ANALYZING -> {
                nextPhase = SequencingPhase.COMPLETE;
                nextMaxProgress = 0;
                completeSequencing(isA);
            }
            default -> {
                nextPhase = SequencingPhase.IDLE;
                nextMaxProgress = 0;
            }
        }

        if (isA) {
            phaseA = nextPhase;
            maxProgressA = nextMaxProgress;
        } else {
            phaseB = nextPhase;
            maxProgressB = nextMaxProgress;
        }
    }

    private void simulateSequencing(boolean isA) {
        Random random = new Random();
        
        // Generate reads based on progress
        long readsPerTick = 10000 + random.nextInt(5000);
        long basesPerTick = readsPerTick * readLength;
        
        if (isA) {
            totalReadsA += readsPerTick;
            totalBasesA += basesPerTick;
            
            // Quality simulation (typical Q30 > 85% for DNBseq)
            int cycle = currentCycleA;
            if (cycle > 0 && cycle <= qualityPerCycleA.length) {
                // Quality drops slightly at end of reads
                float cyclePosition = (float) cycle / (readLength * (pairedEnd ? 2 : 1));
                qualityPerCycleA[cycle - 1] = 30 + random.nextFloat() * 8 - cyclePosition * 5;
            }
        } else {
            totalReadsB += readsPerTick;
            totalBasesB += basesPerTick;
            
            int cycle = currentCycleB;
            if (cycle > 0 && cycle <= qualityPerCycleB.length) {
                float cyclePosition = (float) cycle / (readLength * (pairedEnd ? 2 : 1));
                qualityPerCycleB[cycle - 1] = 30 + random.nextFloat() * 8 - cyclePosition * 5;
            }
        }
    }

    private void completeSequencing(boolean isA) {
        Random random = new Random();
        
        // Calculate final statistics
        float meanQ = 0;
        float q30Count = 0;
        float[] qualityData = isA ? qualityPerCycleA : qualityPerCycleB;
        int cycles = readLength * (pairedEnd ? 2 : 1);
        
        for (int i = 0; i < cycles; i++) {
            meanQ += qualityData[i];
            if (qualityData[i] >= 30) q30Count++;
        }
        meanQ /= cycles;
        float q30Pct = (q30Count / cycles) * 100;

        if (isA) {
            meanQualityA = meanQ;
            q30PercentA = q30Pct;
        } else {
            meanQualityB = meanQ;
            q30PercentB = q30Pct;
        }

        // Create output item
        ItemStack output = createSequencingOutput(isA);
        
        int outputSlot = isA ? OUTPUT_SLOT_A : OUTPUT_SLOT_B;
        if (getStack(outputSlot).isEmpty()) {
            setStack(outputSlot, output);
        }

        // Consume reagents
        int librarySlot = isA ? LIBRARY_SLOT_A : LIBRARY_SLOT_B;
        int flowCellSlot = isA ? FLOWCELL_SLOT_A : FLOWCELL_SLOT_B;
        
        getStack(librarySlot).decrement(1);
        getStack(flowCellSlot).decrement(1);
        
        // Reagents are shared
        ItemStack reagent = getStack(REAGENT_SLOT);
        if (reagent.getDamage() >= reagent.getMaxDamage() - 1) {
            reagent.decrement(1);
        } else {
            reagent.setDamage(reagent.getDamage() + 1);
        }
    }

    private ItemStack createSequencingOutput(boolean isA) {
        ItemStack output = new ItemStack(ModItems.DNA_SAMPLE);
        NbtCompound nbt = output.getOrCreateNbt();
        
        nbt.putString("Type", "sequencing_data");
        nbt.putString("Platform", "DNBSEQ-G99");
        nbt.putString("Technology", "DNBseq");
        nbt.putBoolean("PairedEnd", pairedEnd);
        nbt.putInt("ReadLength", readLength);
        
        if (isA) {
            nbt.putLong("TotalReads", totalReadsA);
            nbt.putLong("TotalBases", totalBasesA);
            nbt.putFloat("MeanQuality", meanQualityA);
            nbt.putFloat("Q30Percent", q30PercentA);
        } else {
            nbt.putLong("TotalReads", totalReadsB);
            nbt.putLong("TotalBases", totalBasesB);
            nbt.putFloat("MeanQuality", meanQualityB);
            nbt.putFloat("Q30Percent", q30PercentB);
        }
        
        // Estimate coverage (assuming typical genome size)
        long totalBases = isA ? totalBasesA : totalBasesB;
        float estimatedCoverage = totalBases / 3e9f;  // Human genome reference
        nbt.putFloat("EstimatedCoverage", estimatedCoverage);
        
        return output;
    }

    public void resetFlowCell(boolean isA) {
        if (isA) {
            phaseA = SequencingPhase.IDLE;
            progressA = 0;
            currentCycleA = 0;
            totalReadsA = 0;
            totalBasesA = 0;
        } else {
            phaseB = SequencingPhase.IDLE;
            progressB = 0;
            currentCycleB = 0;
            totalReadsB = 0;
            totalBasesB = 0;
        }
        markDirty();
    }

    // ========== Getters ==========

    public SequencingPhase getPhaseA() { return phaseA; }
    public SequencingPhase getPhaseB() { return phaseB; }
    public int getProgressA() { return progressA; }
    public int getProgressB() { return progressB; }
    public int getMaxProgressA() { return maxProgressA; }
    public int getMaxProgressB() { return maxProgressB; }
    public int getReadLength() { return readLength; }
    public boolean isPairedEnd() { return pairedEnd; }
    public long getTotalReadsA() { return totalReadsA; }
    public long getTotalReadsB() { return totalReadsB; }
    public long getTotalBasesA() { return totalBasesA; }
    public long getTotalBasesB() { return totalBasesB; }
    public int getCurrentCycleA() { return currentCycleA; }
    public int getCurrentCycleB() { return currentCycleB; }
    public float getMeanQualityA() { return meanQualityA; }
    public float getMeanQualityB() { return meanQualityB; }
    public float getQ30PercentA() { return q30PercentA; }
    public float getQ30PercentB() { return q30PercentB; }
    public float[] getQualityPerCycleA() { return qualityPerCycleA; }
    public float[] getQualityPerCycleB() { return qualityPerCycleB; }

    public void setReadLength(int length) {
        this.readLength = Math.max(50, Math.min(150, length));
        markDirty();
    }

    public void setPairedEnd(boolean pe) {
        this.pairedEnd = pe;
        markDirty();
    }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        
        nbt.putString("PhaseA", phaseA.name());
        nbt.putString("PhaseB", phaseB.name());
        nbt.putInt("ProgressA", progressA);
        nbt.putInt("ProgressB", progressB);
        nbt.putInt("MaxProgressA", maxProgressA);
        nbt.putInt("MaxProgressB", maxProgressB);
        nbt.putInt("ReadLength", readLength);
        nbt.putBoolean("PairedEnd", pairedEnd);
        nbt.putInt("TargetCoverage", targetCoverage);
        
        nbt.putLong("TotalReadsA", totalReadsA);
        nbt.putLong("TotalReadsB", totalReadsB);
        nbt.putLong("TotalBasesA", totalBasesA);
        nbt.putLong("TotalBasesB", totalBasesB);
        nbt.putFloat("MeanQualityA", meanQualityA);
        nbt.putFloat("MeanQualityB", meanQualityB);
        nbt.putFloat("Q30PercentA", q30PercentA);
        nbt.putFloat("Q30PercentB", q30PercentB);
        nbt.putInt("CurrentCycleA", currentCycleA);
        nbt.putInt("CurrentCycleB", currentCycleB);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        
        try {
            phaseA = SequencingPhase.valueOf(nbt.getString("PhaseA"));
            phaseB = SequencingPhase.valueOf(nbt.getString("PhaseB"));
        } catch (Exception e) {
            phaseA = SequencingPhase.IDLE;
            phaseB = SequencingPhase.IDLE;
        }
        
        progressA = nbt.getInt("ProgressA");
        progressB = nbt.getInt("ProgressB");
        maxProgressA = nbt.getInt("MaxProgressA");
        maxProgressB = nbt.getInt("MaxProgressB");
        readLength = nbt.getInt("ReadLength");
        pairedEnd = nbt.getBoolean("PairedEnd");
        targetCoverage = nbt.getInt("TargetCoverage");
        
        totalReadsA = nbt.getLong("TotalReadsA");
        totalReadsB = nbt.getLong("TotalReadsB");
        totalBasesA = nbt.getLong("TotalBasesA");
        totalBasesB = nbt.getLong("TotalBasesB");
        meanQualityA = nbt.getFloat("MeanQualityA");
        meanQualityB = nbt.getFloat("MeanQualityB");
        q30PercentA = nbt.getFloat("Q30PercentA");
        q30PercentB = nbt.getFloat("Q30PercentB");
        currentCycleA = nbt.getInt("CurrentCycleA");
        currentCycleB = nbt.getInt("CurrentCycleB");
    }

    @Override
    protected void completeProcess() { /* Handled per flow cell */ }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.morerealisticgeneediting.benchtop_sequencer");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return null;  // TODO: BenchtopSequencerScreenHandler
    }
}
