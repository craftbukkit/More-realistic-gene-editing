package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.EquipmentSpecs;
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
 * Electrophoresis System Block Entity - Simulates gel electrophoresis.
 * 
 * Based on real equipment:
 * - T1: blueGel Portable (8 wells, 48V, built-in transilluminator)
 * - T2: DYCZ-24DN Vertical (24 wells, up to 300V, separate power)
 * - T3: Agilent 4200 TapeStation (96 samples, fully automated)
 * 
 * Process:
 * 1. Prepare agarose gel with buffer (TAE/TBE)
 * 2. Load DNA samples with loading dye
 * 3. Load DNA ladder in reference lane
 * 4. Apply voltage and run
 * 5. Visualize with stain (EtBr/SYBR Safe)
 * 
 * DNA migration: smaller fragments move faster
 * Distance ∝ log(1/size)
 */
public class ElectrophoresisBlockEntity extends LabEquipmentBlockEntity {

    // Slots
    public static final int GEL_SLOT = 0;           // Agarose gel
    public static final int BUFFER_SLOT = 1;        // TAE or TBE
    public static final int LADDER_SLOT = 2;        // DNA ladder
    public static final int STAIN_SLOT = 3;         // EtBr or SYBR Safe
    public static final int LOADING_DYE_SLOT = 4;   // Loading dye
    public static final int SAMPLE_START_SLOT = 5;  // Samples start here
    public static final int MAX_SAMPLE_SLOTS = 8;   // Up to 8 samples (T1)

    // Running state
    public enum ElectrophoresisPhase {
        IDLE,
        PREPARING,      // Gel solidifying
        LOADING,        // Samples being loaded
        RUNNING,        // Voltage applied
        STAINING,       // Stain application
        VISUALIZING,    // UV/Blue light visualization
        COMPLETE
    }

    private ElectrophoresisPhase phase = ElectrophoresisPhase.IDLE;
    private int voltage = 0;
    private int targetVoltage = 100;
    private int runTimeRemaining = 0;  // Ticks
    private int totalRunTime = 0;
    private float gelPercentage = 1.0f;  // 0.5% - 3% agarose

    // Gel state
    private boolean gelReady = false;
    private List<BandPattern> bandPatterns = new ArrayList<>();

    // Results - positions of bands for each lane (0-100 scale)
    private float[][] bandPositions;  // [lane][band]
    private int[] bandCounts;         // Number of bands per lane

    private EquipmentSpecs.ElectrophoresisSpec spec;

    public ElectrophoresisBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SAMPLE_START_SLOT + MAX_SAMPLE_SLOTS + 1);
        updateSpec();
        bandPositions = new float[MAX_SAMPLE_SLOTS + 1][10];  // +1 for ladder
        bandCounts = new int[MAX_SAMPLE_SLOTS + 1];
    }

    private void updateSpec() {
        this.spec = EquipmentSpecs.getElectrophoresisSpec(tier);
    }

    @Override
    public void setTier(EquipmentTier tier) {
        super.setTier(tier);
        updateSpec();
        // Increase sample capacity for higher tiers
        if (tier == EquipmentTier.ADVANCED) {
            // 24 wells
        } else if (tier == EquipmentTier.ELITE) {
            // 96 wells (TapeStation)
        }
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot <= SAMPLE_START_SLOT + MAX_SAMPLE_SLOTS;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;  // No output - results are visual
    }

    @Override
    protected boolean canProcess() {
        if (phase != ElectrophoresisPhase.IDLE) return true;

        // Check required materials
        ItemStack gel = getStack(GEL_SLOT);
        ItemStack buffer = getStack(BUFFER_SLOT);
        ItemStack ladder = getStack(LADDER_SLOT);

        if (gel.isEmpty() || buffer.isEmpty() || ladder.isEmpty()) return false;

        // Check for at least one sample
        for (int i = 0; i < MAX_SAMPLE_SLOTS; i++) {
            if (!getStack(SAMPLE_START_SLOT + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;

        switch (phase) {
            case IDLE -> {
                if (canProcess()) {
                    startElectrophoresis();
                }
            }
            case PREPARING -> tickPreparing();
            case LOADING -> tickLoading();
            case RUNNING -> tickRunning();
            case STAINING -> tickStaining();
            case VISUALIZING -> {
                // Results visible, waiting for player
            }
            case COMPLETE -> {
                // Stay in complete until reset
            }
        }
    }

    private void startElectrophoresis() {
        phase = ElectrophoresisPhase.PREPARING;
        progress = 0;
        maxProgress = 100;  // 5 seconds to prepare gel
        gelReady = false;
        bandPatterns.clear();
        setActive(true);
        markDirty();
    }

    private void tickPreparing() {
        progress++;
        if (progress >= maxProgress) {
            gelReady = true;
            phase = ElectrophoresisPhase.LOADING;
            progress = 0;
            maxProgress = 40;  // 2 seconds to load
        }
        markDirty();
    }

    private void tickLoading() {
        progress++;
        if (progress >= maxProgress) {
            // Analyze samples and create band patterns
            analyzeSamples();
            
            phase = ElectrophoresisPhase.RUNNING;
            progress = 0;
            voltage = targetVoltage;
            totalRunTime = calculateRunTime();
            runTimeRemaining = totalRunTime;
            maxProgress = totalRunTime;
        }
        markDirty();
    }

    private void tickRunning() {
        if (runTimeRemaining > 0) {
            runTimeRemaining--;
            progress = totalRunTime - runTimeRemaining;

            // Simulate band migration
            updateBandPositions();
        }

        if (runTimeRemaining <= 0) {
            voltage = 0;
            
            // Check if stain is available
            if (!getStack(STAIN_SLOT).isEmpty()) {
                phase = ElectrophoresisPhase.STAINING;
                progress = 0;
                maxProgress = 60;  // 3 seconds staining
            } else {
                phase = ElectrophoresisPhase.VISUALIZING;
            }
        }
        markDirty();
    }

    private void tickStaining() {
        progress++;
        if (progress >= maxProgress) {
            getStack(STAIN_SLOT).decrement(1);
            phase = ElectrophoresisPhase.VISUALIZING;
        }
        markDirty();
    }

    private void analyzeSamples() {
        Random random = new Random();
        
        // Analyze ladder first (lane 0)
        ItemStack ladder = getStack(LADDER_SLOT);
        if (isValidLadder(ladder)) {
            int[] ladderSizes = getLadderSizes(ladder);
            bandCounts[0] = ladderSizes.length;
            for (int i = 0; i < ladderSizes.length; i++) {
                // Initial position at well (0)
                bandPositions[0][i] = 0;
                // Store size in pattern for later calculation
                bandPatterns.add(new BandPattern(0, i, ladderSizes[i], 1.0f));
            }
        }

        // Analyze samples
        for (int lane = 0; lane < MAX_SAMPLE_SLOTS; lane++) {
            ItemStack sample = getStack(SAMPLE_START_SLOT + lane);
            if (!sample.isEmpty() && isValidSample(sample)) {
                // Get sample info from NBT or estimate
                int[] fragmentSizes = estimateFragmentSizes(sample, random);
                bandCounts[lane + 1] = fragmentSizes.length;
                
                for (int i = 0; i < fragmentSizes.length; i++) {
                    bandPositions[lane + 1][i] = 0;
                    float intensity = 0.5f + random.nextFloat() * 0.5f;
                    bandPatterns.add(new BandPattern(lane + 1, i, fragmentSizes[i], intensity));
                }
            }
        }
    }

    private void updateBandPositions() {
        // DNA migration follows: distance ∝ log(1/size) * voltage * time
        float migrationFactor = voltage / 100.0f * (1.0f / gelPercentage);
        
        for (BandPattern band : bandPatterns) {
            // Smaller fragments move faster
            float speed = (float) (Math.log10(10000.0 / band.size) * migrationFactor * 0.1);
            float newPos = bandPositions[band.lane][band.bandIndex] + speed;
            
            // Cap at 100 (end of gel)
            bandPositions[band.lane][band.bandIndex] = Math.min(100, newPos);
        }
    }

    private int calculateRunTime() {
        int baseTime = spec != null ? spec.runTimeMinutes() * 20 : 600;  // Convert to ticks
        // Higher voltage = faster
        float voltageFactor = 100.0f / Math.max(50, targetVoltage);
        return (int) (baseTime * voltageFactor);
    }

    private int[] getLadderSizes(ItemStack ladder) {
        if (ladder.isOf(LabEquipmentItems.DNA_LADDER_100BP)) {
            return new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        } else if (ladder.isOf(LabEquipmentItems.DNA_LADDER_1KB)) {
            return new int[]{250, 500, 750, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 8000, 10000};
        } else if (ladder.isOf(LabEquipmentItems.DNA_LADDER_1KB_PLUS)) {
            return new int[]{100, 200, 300, 400, 500, 650, 850, 1000, 1650, 2000, 3000, 4000, 5000, 6000, 8000, 10000, 12000};
        }
        return new int[]{500, 1000, 2000, 3000, 5000};  // Default
    }

    private int[] estimateFragmentSizes(ItemStack sample, Random random) {
        // Check NBT for actual sizes
        NbtCompound nbt = sample.getNbt();
        if (nbt != null && nbt.contains("FragmentSizes")) {
            return nbt.getIntArray("FragmentSizes");
        }

        // Estimate based on item type
        if (sample.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT)) {
            // PCR product - typically single band
            int size = 500 + random.nextInt(2000);
            return new int[]{size};
        } else if (sample.isOf(ModItems.DNA_SAMPLE)) {
            // Genomic DNA - may be degraded or intact
            int numBands = 1 + random.nextInt(3);
            int[] sizes = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                sizes[i] = 1000 + random.nextInt(10000);
            }
            return sizes;
        }

        return new int[]{1000};
    }

    private boolean isValidLadder(ItemStack stack) {
        return stack.isOf(LabEquipmentItems.DNA_LADDER_100BP) ||
               stack.isOf(LabEquipmentItems.DNA_LADDER_1KB) ||
               stack.isOf(LabEquipmentItems.DNA_LADDER_1KB_PLUS);
    }

    private boolean isValidSample(ItemStack stack) {
        return stack.isOf(ModItems.DNA_SAMPLE) ||
               stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT) ||
               stack.isOf(ModItems.GENOME_SAMPLE);
    }

    @Override
    protected void completeProcess() {
        // Consume reagents
        getStack(GEL_SLOT).decrement(1);
        getStack(BUFFER_SLOT).decrement(1);
        getStack(LADDER_SLOT).decrement(1);
        if (!getStack(LOADING_DYE_SLOT).isEmpty()) {
            getStack(LOADING_DYE_SLOT).decrement(1);
        }

        // Samples consumed during loading
        for (int i = 0; i < MAX_SAMPLE_SLOTS; i++) {
            ItemStack sample = getStack(SAMPLE_START_SLOT + i);
            if (!sample.isEmpty()) {
                sample.decrement(1);
            }
        }

        phase = ElectrophoresisPhase.COMPLETE;
        setActive(false);
    }

    public void reset() {
        phase = ElectrophoresisPhase.IDLE;
        voltage = 0;
        runTimeRemaining = 0;
        progress = 0;
        gelReady = false;
        bandPatterns.clear();
        for (int i = 0; i < bandCounts.length; i++) {
            bandCounts[i] = 0;
        }
        setActive(false);
        markDirty();
    }

    // ========== Getters ==========

    public ElectrophoresisPhase getPhase() { return phase; }
    public int getVoltage() { return voltage; }
    public int getTargetVoltage() { return targetVoltage; }
    public int getRunTimeRemaining() { return runTimeRemaining; }
    public float getGelPercentage() { return gelPercentage; }
    public boolean isGelReady() { return gelReady; }
    public float[][] getBandPositions() { return bandPositions; }
    public int[] getBandCounts() { return bandCounts; }
    public List<BandPattern> getBandPatterns() { return bandPatterns; }

    public void setTargetVoltage(int v) {
        int maxV = spec != null ? spec.maxVoltage() : 100;
        this.targetVoltage = Math.max(10, Math.min(maxV, v));
    }

    public void setGelPercentage(float pct) {
        this.gelPercentage = Math.max(0.5f, Math.min(3.0f, pct));
    }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("Phase", phase.name());
        nbt.putInt("Voltage", voltage);
        nbt.putInt("TargetVoltage", targetVoltage);
        nbt.putInt("RunTimeRemaining", runTimeRemaining);
        nbt.putInt("TotalRunTime", totalRunTime);
        nbt.putFloat("GelPercentage", gelPercentage);
        nbt.putBoolean("GelReady", gelReady);
        nbt.putIntArray("BandCounts", bandCounts);

        // Save band positions
        NbtList posList = new NbtList();
        for (float[] lanePositions : bandPositions) {
            NbtCompound laneNbt = new NbtCompound();
            for (int i = 0; i < lanePositions.length; i++) {
                laneNbt.putFloat("b" + i, lanePositions[i]);
            }
            posList.add(laneNbt);
        }
        nbt.put("BandPositions", posList);

        // Save band patterns
        NbtList patternList = new NbtList();
        for (BandPattern pattern : bandPatterns) {
            NbtCompound p = new NbtCompound();
            p.putInt("Lane", pattern.lane);
            p.putInt("Index", pattern.bandIndex);
            p.putInt("Size", pattern.size);
            p.putFloat("Intensity", pattern.intensity);
            patternList.add(p);
        }
        nbt.put("BandPatterns", patternList);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        try {
            phase = ElectrophoresisPhase.valueOf(nbt.getString("Phase"));
        } catch (Exception e) {
            phase = ElectrophoresisPhase.IDLE;
        }
        voltage = nbt.getInt("Voltage");
        targetVoltage = nbt.getInt("TargetVoltage");
        runTimeRemaining = nbt.getInt("RunTimeRemaining");
        totalRunTime = nbt.getInt("TotalRunTime");
        gelPercentage = nbt.getFloat("GelPercentage");
        gelReady = nbt.getBoolean("GelReady");

        int[] counts = nbt.getIntArray("BandCounts");
        if (counts.length > 0) {
            System.arraycopy(counts, 0, bandCounts, 0, Math.min(counts.length, bandCounts.length));
        }

        // Load band positions
        NbtList posList = nbt.getList("BandPositions", 10);
        for (int lane = 0; lane < Math.min(posList.size(), bandPositions.length); lane++) {
            NbtCompound laneNbt = posList.getCompound(lane);
            for (int i = 0; i < bandPositions[lane].length; i++) {
                bandPositions[lane][i] = laneNbt.getFloat("b" + i);
            }
        }

        // Load band patterns
        bandPatterns.clear();
        NbtList patternList = nbt.getList("BandPatterns", 10);
        for (int i = 0; i < patternList.size(); i++) {
            NbtCompound p = patternList.getCompound(i);
            bandPatterns.add(new BandPattern(
                p.getInt("Lane"),
                p.getInt("Index"),
                p.getInt("Size"),
                p.getFloat("Intensity")
            ));
        }

        updateSpec();
    }

    // ========== Screen ==========

    @Override
    public Text getDisplayName() {
        String key = switch (tier) {
            case BASIC -> "block.morerealisticgeneediting.portable_electrophoresis";
            case ADVANCED -> "block.morerealisticgeneediting.vertical_electrophoresis";
            case ELITE -> "block.morerealisticgeneediting.auto_electrophoresis";
        };
        return Text.translatable(key);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        // TODO: Create ElectrophoresisScreenHandler
        return null;
    }

    // ========== Inner Classes ==========

    public record BandPattern(int lane, int bandIndex, int size, float intensity) {}
}
