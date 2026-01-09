package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.EquipmentSpecs;
import com.morerealisticgeneediting.equipment.EquipmentTier;
import com.morerealisticgeneediting.item.LabEquipmentItems;
import com.morerealisticgeneediting.item.ModItems;
import com.morerealisticgeneediting.screen.equipment.ThermalCyclerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Thermal Cycler Block Entity - Implements realistic PCR cycling.
 * 
 * Based on real equipment:
 * - T1: OpenPCR (16 wells, 1°C/s, no gradient)
 * - T2: Bio-Rad T100 (96 wells, 3°C/s, gradient capable)
 * - T3: Roche LightCycler 480 II (384 wells, 4.8°C/s, qPCR)
 * 
 * PCR Process Simulation:
 * 1. Initial Denaturation: 95°C, 2-5 min
 * 2. Cycling (25-40 cycles):
 *    - Denaturation: 95°C, 15-30 sec
 *    - Annealing: 50-65°C, 15-60 sec
 *    - Extension: 72°C, 1 min per kb
 * 3. Final Extension: 72°C, 5-10 min
 * 4. Hold: 4°C
 */
public class ThermalCyclerBlockEntityV2 extends LabEquipmentBlockEntity {

    // Slots
    public static final int TEMPLATE_SLOT = 0;      // DNA template
    public static final int PRIMER_F_SLOT = 1;      // Forward primer
    public static final int PRIMER_R_SLOT = 2;      // Reverse primer
    public static final int POLYMERASE_SLOT = 3;    // Taq/HF polymerase
    public static final int DNTPS_SLOT = 4;         // dNTPs
    public static final int OUTPUT_SLOT = 5;        // PCR product
    public static final int BUFFER_SLOT = 6;        // PCR buffer
    public static final int TUBE_SLOT = 7;          // PCR tube/plate

    // PCR State
    public enum PcrPhase {
        IDLE,
        INITIAL_DENATURATION,
        DENATURATION,
        ANNEALING,
        EXTENSION,
        FINAL_EXTENSION,
        HOLD,
        COMPLETE
    }

    private PcrPhase currentPhase = PcrPhase.IDLE;
    private float currentTemp = 25.0f;       // Current block temperature
    private float targetTemp = 25.0f;        // Target temperature
    private int currentCycle = 0;            // Current cycle number
    private int totalCycles = 30;            // Total cycles to run
    private int phaseProgress = 0;           // Progress within current phase
    private int phaseMaxProgress = 0;        // Max progress for current phase

    // PCR Parameters (configurable by player)
    private float annealingTemp = 55.0f;     // Annealing temperature
    private int denatureTime = 30;           // Seconds at 95°C
    private int annealTime = 30;             // Seconds at annealing temp
    private int extensionTime = 60;          // Seconds at 72°C (per kb)
    private boolean useGradient = false;     // Use gradient (T2+ only)
    private float gradientRange = 0.0f;      // Gradient range in °C

    // qPCR specific (T3 only)
    private boolean realTimeMode = false;
    private float[] fluorescenceData;        // Fluorescence readings per cycle
    private float ctValue = 0.0f;            // Calculated Ct value

    // Equipment spec
    private EquipmentSpecs.ThermalCyclerSpec spec;

    public ThermalCyclerBlockEntityV2(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8);
        this.maxProgress = 0;  // We use phase-based progress
        updateSpec();
    }

    private void updateSpec() {
        this.spec = EquipmentSpecs.getThermalCyclerSpec(tier);
        if (tier == EquipmentTier.ELITE) {
            fluorescenceData = new float[50];  // Up to 50 cycles
        }
    }

    @Override
    public void setTier(EquipmentTier tier) {
        super.setTier(tier);
        updateSpec();
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= TEMPLATE_SLOT && slot <= TUBE_SLOT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected boolean canProcess() {
        if (currentPhase != PcrPhase.IDLE) return true;  // Already running

        // Check all required reagents
        ItemStack template = getStack(TEMPLATE_SLOT);
        ItemStack primerF = getStack(PRIMER_F_SLOT);
        ItemStack primerR = getStack(PRIMER_R_SLOT);
        ItemStack polymerase = getStack(POLYMERASE_SLOT);
        ItemStack dntps = getStack(DNTPS_SLOT);
        ItemStack tube = getStack(TUBE_SLOT);

        if (template.isEmpty() || primerF.isEmpty() || primerR.isEmpty()) return false;
        if (polymerase.isEmpty() || dntps.isEmpty() || tube.isEmpty()) return false;

        // Validate items
        if (!isValidTemplate(template)) return false;
        if (!isValidPolymerase(polymerase)) return false;
        if (!isValidTube(tube)) return false;

        // Check output space
        return getStack(OUTPUT_SLOT).isEmpty() || 
               (getStack(OUTPUT_SLOT).getCount() < getStack(OUTPUT_SLOT).getMaxCount());
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;

        if (currentPhase == PcrPhase.IDLE && canProcess()) {
            startPcr();
        }

        if (currentPhase != PcrPhase.IDLE && currentPhase != PcrPhase.COMPLETE) {
            tickPcr();
        }
    }

    private void startPcr() {
        currentCycle = 0;
        currentPhase = PcrPhase.INITIAL_DENATURATION;
        targetTemp = 95.0f;
        phaseProgress = 0;
        phaseMaxProgress = 5 * 20;  // 5 seconds (100 ticks)

        if (realTimeMode && fluorescenceData != null) {
            for (int i = 0; i < fluorescenceData.length; i++) {
                fluorescenceData[i] = 0;
            }
        }

        markDirty();
        setActive(true);
    }

    private void tickPcr() {
        // Temperature ramping
        rampTemperature();

        // Only progress when at target temp
        if (Math.abs(currentTemp - targetTemp) < 0.5f) {
            phaseProgress++;
        }

        // Check phase completion
        if (phaseProgress >= phaseMaxProgress) {
            advancePhase();
        }

        markDirty();
    }

    private void rampTemperature() {
        float rampRate = spec != null ? spec.rampRate() / 20.0f : 0.1f;  // Per tick

        if (currentTemp < targetTemp) {
            currentTemp = Math.min(targetTemp, currentTemp + rampRate);
        } else if (currentTemp > targetTemp) {
            currentTemp = Math.max(targetTemp, currentTemp - rampRate);
        }
    }

    private void advancePhase() {
        switch (currentPhase) {
            case INITIAL_DENATURATION -> {
                currentPhase = PcrPhase.DENATURATION;
                targetTemp = 95.0f;
                phaseMaxProgress = denatureTime * 20 / 10;  // Scaled for gameplay
            }
            case DENATURATION -> {
                currentPhase = PcrPhase.ANNEALING;
                targetTemp = annealingTemp;
                phaseMaxProgress = annealTime * 20 / 10;
            }
            case ANNEALING -> {
                currentPhase = PcrPhase.EXTENSION;
                targetTemp = 72.0f;
                phaseMaxProgress = extensionTime * 20 / 10;

                // qPCR fluorescence measurement
                if (realTimeMode && currentCycle < fluorescenceData.length) {
                    fluorescenceData[currentCycle] = calculateFluorescence();
                }
            }
            case EXTENSION -> {
                currentCycle++;
                if (currentCycle >= totalCycles) {
                    currentPhase = PcrPhase.FINAL_EXTENSION;
                    targetTemp = 72.0f;
                    phaseMaxProgress = 10 * 20 / 10;  // 10 seconds scaled
                } else {
                    currentPhase = PcrPhase.DENATURATION;
                    targetTemp = 95.0f;
                    phaseMaxProgress = denatureTime * 20 / 10;
                }
            }
            case FINAL_EXTENSION -> {
                currentPhase = PcrPhase.HOLD;
                targetTemp = 4.0f;
                phaseMaxProgress = 20;  // Brief hold before complete
            }
            case HOLD -> {
                completeProcess();
                currentPhase = PcrPhase.COMPLETE;
            }
            default -> {}
        }
        phaseProgress = 0;
    }

    private float calculateFluorescence() {
        // Simulate exponential amplification
        // F = F0 * (1 + efficiency)^cycle
        float baseline = 100.0f;
        float efficiency = 0.9f + (tier.ordinal() * 0.03f);  // 90-96% based on tier
        return (float) (baseline * Math.pow(1 + efficiency, currentCycle));
    }

    @Override
    protected void completeProcess() {
        Random random = new Random();
        
        // Calculate success based on parameters
        float successChance = calculateSuccessChance();
        
        if (random.nextFloat() < successChance) {
            // Create PCR product
            ItemStack product = new ItemStack(ModItems.AMPLIFIED_GENE_FRAGMENT);
            
            // Add metadata
            NbtCompound nbt = product.getOrCreateNbt();
            nbt.putInt("Cycles", totalCycles);
            nbt.putFloat("AnnealingTemp", annealingTemp);
            nbt.putString("Polymerase", getPolymeraseType());
            
            if (realTimeMode && ctValue > 0) {
                nbt.putFloat("CtValue", ctValue);
            }
            
            // Calculate approximate yield
            double yield = Math.pow(2, totalCycles) / 1e9;  // Normalized
            nbt.putDouble("RelativeYield", Math.min(1.0, yield));
            
            // Add to output
            ItemStack output = getStack(OUTPUT_SLOT);
            if (output.isEmpty()) {
                setStack(OUTPUT_SLOT, product);
            } else {
                output.increment(1);
            }
        }

        // Consume reagents
        consumeReagents();

        // Reset state
        currentPhase = PcrPhase.IDLE;
        currentCycle = 0;
        currentTemp = 25.0f;
        setActive(false);
    }

    private float calculateSuccessChance() {
        float base = spec != null ? 0.7f + (1.0f - spec.precision()) : 0.7f;
        
        // Annealing temp affects success
        float optimalAnnealing = 55.0f;  // Simplified optimal
        float annealPenalty = Math.abs(annealingTemp - optimalAnnealing) * 0.01f;
        base -= annealPenalty;

        // High fidelity polymerase bonus
        ItemStack polymerase = getStack(POLYMERASE_SLOT);
        if (isHighFidelityPolymerase(polymerase)) {
            base += 0.1f;
        }

        // Tier bonus
        base += tier.getSuccessRateBonus();

        return Math.max(0.1f, Math.min(0.99f, base));
    }

    private void consumeReagents() {
        getStack(TEMPLATE_SLOT).decrement(1);
        getStack(PRIMER_F_SLOT).decrement(1);
        getStack(PRIMER_R_SLOT).decrement(1);
        getStack(DNTPS_SLOT).decrement(1);
        getStack(TUBE_SLOT).decrement(1);
        
        // Polymerase and buffer last multiple reactions
        ItemStack poly = getStack(POLYMERASE_SLOT);
        if (poly.getDamage() >= poly.getMaxDamage() - 1) {
            poly.decrement(1);
        } else {
            poly.setDamage(poly.getDamage() + 1);
        }
    }

    private String getPolymeraseType() {
        ItemStack poly = getStack(POLYMERASE_SLOT);
        if (poly.isOf(LabEquipmentItems.HF_POLYMERASE)) {
            return "high_fidelity";
        }
        return "taq";
    }

    // ========== Validation Methods ==========

    private boolean isValidTemplate(ItemStack stack) {
        return stack.isOf(ModItems.DNA_SAMPLE) || 
               stack.isOf(ModItems.GENOME_SAMPLE) ||
               stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT);
    }

    private boolean isValidPolymerase(ItemStack stack) {
        return stack.isOf(LabEquipmentItems.TAQ_POLYMERASE) ||
               stack.isOf(LabEquipmentItems.HF_POLYMERASE);
    }

    private boolean isHighFidelityPolymerase(ItemStack stack) {
        return stack.isOf(LabEquipmentItems.HF_POLYMERASE);
    }

    private boolean isValidTube(ItemStack stack) {
        return stack.isOf(LabEquipmentItems.PCR_TUBE_STRIP) ||
               stack.isOf(LabEquipmentItems.PCR_PLATE_96) ||
               stack.isOf(LabEquipmentItems.PCR_PLATE_384);
    }

    // ========== Getters and Setters ==========

    public PcrPhase getCurrentPhase() { return currentPhase; }
    public float getCurrentTemp() { return currentTemp; }
    public float getTargetTemp() { return targetTemp; }
    public int getCurrentCycle() { return currentCycle; }
    public int getTotalCycles() { return totalCycles; }
    public float getAnnealingTemp() { return annealingTemp; }
    public boolean isRealTimeMode() { return realTimeMode; }
    public float[] getFluorescenceData() { return fluorescenceData; }
    public int getPhaseProgress() { return phaseProgress; }
    public int getPhaseMaxProgress() { return phaseMaxProgress; }

    public void setTotalCycles(int cycles) {
        this.totalCycles = Math.max(1, Math.min(50, cycles));
    }

    public void setAnnealingTemp(float temp) {
        int minTemp = spec != null ? spec.minTemp() : 37;
        int maxTemp = spec != null ? spec.maxTemp() : 95;
        this.annealingTemp = Math.max(minTemp, Math.min(maxTemp, temp));
    }

    public void setRealTimeMode(boolean enabled) {
        // Only T3 supports real-time
        if (tier == EquipmentTier.ELITE) {
            this.realTimeMode = enabled;
        }
    }

    public void setGradient(boolean enabled, float range) {
        if (spec != null && spec.hasGradient()) {
            this.useGradient = enabled;
            this.gradientRange = Math.min(25.0f, Math.max(0, range));
        }
    }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("Phase", currentPhase.name());
        nbt.putFloat("CurrentTemp", currentTemp);
        nbt.putFloat("TargetTemp", targetTemp);
        nbt.putInt("CurrentCycle", currentCycle);
        nbt.putInt("TotalCycles", totalCycles);
        nbt.putFloat("AnnealingTemp", annealingTemp);
        nbt.putInt("DenatureTime", denatureTime);
        nbt.putInt("AnnealTime", annealTime);
        nbt.putInt("ExtensionTime", extensionTime);
        nbt.putBoolean("UseGradient", useGradient);
        nbt.putFloat("GradientRange", gradientRange);
        nbt.putBoolean("RealTimeMode", realTimeMode);
        nbt.putInt("PhaseProgress", phaseProgress);
        nbt.putInt("PhaseMaxProgress", phaseMaxProgress);

        if (fluorescenceData != null) {
            int[] intData = new int[fluorescenceData.length];
            for (int i = 0; i < fluorescenceData.length; i++) {
                intData[i] = Float.floatToIntBits(fluorescenceData[i]);
            }
            nbt.putIntArray("FluorescenceData", intData);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        try {
            currentPhase = PcrPhase.valueOf(nbt.getString("Phase"));
        } catch (Exception e) {
            currentPhase = PcrPhase.IDLE;
        }
        currentTemp = nbt.getFloat("CurrentTemp");
        targetTemp = nbt.getFloat("TargetTemp");
        currentCycle = nbt.getInt("CurrentCycle");
        totalCycles = nbt.getInt("TotalCycles");
        annealingTemp = nbt.getFloat("AnnealingTemp");
        denatureTime = nbt.getInt("DenatureTime");
        annealTime = nbt.getInt("AnnealTime");
        extensionTime = nbt.getInt("ExtensionTime");
        useGradient = nbt.getBoolean("UseGradient");
        gradientRange = nbt.getFloat("GradientRange");
        realTimeMode = nbt.getBoolean("RealTimeMode");
        phaseProgress = nbt.getInt("PhaseProgress");
        phaseMaxProgress = nbt.getInt("PhaseMaxProgress");

        if (nbt.contains("FluorescenceData")) {
            int[] intData = nbt.getIntArray("FluorescenceData");
            fluorescenceData = new float[intData.length];
            for (int i = 0; i < intData.length; i++) {
                fluorescenceData[i] = Float.intBitsToFloat(intData[i]);
            }
        }

        updateSpec();
    }

    // ========== Screen ==========

    @Override
    public Text getDisplayName() {
        String key = switch (tier) {
            case BASIC -> "block.morerealisticgeneediting.open_pcr";
            case ADVANCED -> "block.morerealisticgeneediting.thermal_cycler";
            case ELITE -> "block.morerealisticgeneediting.qpcr_system";
        };
        return Text.translatable(key);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ThermalCyclerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }
}
