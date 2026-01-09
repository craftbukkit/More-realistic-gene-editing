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
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Bioreactor/Incubator Block Entity - Cell culture and fermentation.
 * 
 * Based on real equipment:
 * - T1: DIY Styrofoam Incubator (25-42°C, basic)
 * - T2: Yiheng THZ-300C Shaking Incubator (4-60°C, 30-300 rpm)
 * - T3: Sartorius Ambr 250 Modular (precise control, pH/DO, parallel cultures)
 * 
 * Culture Parameters:
 * - Temperature: Critical for growth rate
 * - Shaking/Stirring: Aeration and mixing
 * - CO2: For mammalian cells (5%)
 * - Humidity: Prevent evaporation
 * - pH: Optimal range 6.8-7.4 for most cells
 * - Dissolved Oxygen (DO): Aerobic vs anaerobic
 * 
 * Growth Simulation:
 * - Lag phase → Log phase → Stationary phase → Death phase
 * - Doubling time varies by organism and conditions
 */
public class BioreactorBlockEntity extends LabEquipmentBlockEntity {

    // Slots
    public static final int CULTURE_SLOT = 0;       // Cell culture / inoculum
    public static final int MEDIUM_SLOT = 1;        // Growth medium
    public static final int SUPPLEMENT_SLOT = 2;    // FBS, antibiotics, etc.
    public static final int VESSEL_SLOT = 3;        // Flask or plate
    public static final int OUTPUT_SLOT = 4;        // Grown culture

    // Growth phases
    public enum GrowthPhase {
        IDLE,
        LAG,        // Adaptation phase
        LOG,        // Exponential growth
        STATIONARY, // Growth = death
        DECLINE,    // Death phase
        COMPLETE
    }

    // Culture parameters
    private float temperature = 37.0f;      // °C
    private float targetTemperature = 37.0f;
    private int shakingSpeed = 0;           // rpm
    private int targetShakingSpeed = 0;
    private float co2Percentage = 0;        // %
    private float humidity = 0;             // %
    private float ph = 7.0f;
    private float dissolvedOxygen = 100;    // % saturation

    // Culture state
    private GrowthPhase phase = GrowthPhase.IDLE;
    private float cellDensity = 0;          // cells/mL (log scale stored)
    private float maxCellDensity = 1e9f;    // Saturation density
    private int cultureAge = 0;             // Ticks since start
    private String cultureType = "unknown";
    private float doublingTime = 1200;      // Ticks (1 minute = optimal)
    private float viability = 1.0f;         // 0-1

    // Culture history for visualization
    private float[] densityHistory = new float[100];
    private int historyIndex = 0;

    private EquipmentSpecs.IncubatorSpec spec;

    public BioreactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5);
        updateSpec();
    }

    private void updateSpec() {
        this.spec = switch (tier) {
            case BASIC -> EquipmentSpecs.DIY_INCUBATOR;
            case ADVANCED -> EquipmentSpecs.SHAKING_INCUBATOR;
            case ELITE -> EquipmentSpecs.AMBR_250;
        };
    }

    @Override
    public void setTier(EquipmentTier tier) {
        super.setTier(tier);
        updateSpec();
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= CULTURE_SLOT && slot <= VESSEL_SLOT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected boolean canProcess() {
        if (phase != GrowthPhase.IDLE && phase != GrowthPhase.COMPLETE) return true;

        ItemStack culture = getStack(CULTURE_SLOT);
        ItemStack medium = getStack(MEDIUM_SLOT);
        ItemStack vessel = getStack(VESSEL_SLOT);

        return !culture.isEmpty() && !medium.isEmpty() && !vessel.isEmpty() &&
               isValidCulture(culture) && isValidMedium(medium) && isValidVessel(vessel);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;

        // Update temperature
        updateTemperature();

        // Update shaking
        updateShaking();

        if (phase == GrowthPhase.IDLE && canProcess()) {
            startCulture();
        }

        if (phase != GrowthPhase.IDLE && phase != GrowthPhase.COMPLETE) {
            tickCulture();
        }
    }

    private void updateTemperature() {
        float rampRate = tier == EquipmentTier.ELITE ? 0.5f : 0.1f;
        
        if (temperature < targetTemperature) {
            temperature = Math.min(targetTemperature, temperature + rampRate);
        } else if (temperature > targetTemperature) {
            temperature = Math.max(targetTemperature, temperature - rampRate);
        }
    }

    private void updateShaking() {
        if (!spec.hasShaking()) {
            shakingSpeed = 0;
            return;
        }

        int rampRate = tier == EquipmentTier.ELITE ? 20 : 5;
        
        if (shakingSpeed < targetShakingSpeed) {
            shakingSpeed = Math.min(targetShakingSpeed, shakingSpeed + rampRate);
        } else if (shakingSpeed > targetShakingSpeed) {
            shakingSpeed = Math.max(targetShakingSpeed, shakingSpeed - rampRate);
        }
    }

    private void startCulture() {
        ItemStack culture = getStack(CULTURE_SLOT);
        
        // Get culture info
        NbtCompound nbt = culture.getNbt();
        if (nbt != null) {
            if (nbt.contains("CellType")) {
                cultureType = nbt.getString("CellType");
            }
            if (nbt.contains("CellDensity")) {
                cellDensity = nbt.getFloat("CellDensity");
            } else {
                cellDensity = 1e5f;  // Default starting density
            }
        } else {
            cultureType = guessCultureType(culture);
            cellDensity = 1e5f;
        }

        // Set optimal conditions based on culture type
        setOptimalConditions();

        phase = GrowthPhase.LAG;
        cultureAge = 0;
        viability = 1.0f;
        historyIndex = 0;
        
        for (int i = 0; i < densityHistory.length; i++) {
            densityHistory[i] = 0;
        }

        setActive(true);
        markDirty();
    }

    private void setOptimalConditions() {
        switch (cultureType) {
            case "ecoli", "bacteria" -> {
                targetTemperature = 37.0f;
                targetShakingSpeed = 200;
                doublingTime = 600;  // 30 seconds (accelerated)
                maxCellDensity = 1e10f;
            }
            case "yeast" -> {
                targetTemperature = 30.0f;
                targetShakingSpeed = 180;
                doublingTime = 1200;
                maxCellDensity = 1e8f;
            }
            case "mammalian", "hela", "cho" -> {
                targetTemperature = 37.0f;
                co2Percentage = 5.0f;
                humidity = 95.0f;
                targetShakingSpeed = 0;  // Adherent cells don't shake
                doublingTime = 4800;  // 4 minutes (accelerated from 24 hours)
                maxCellDensity = 1e7f;
            }
            case "plant" -> {
                targetTemperature = 25.0f;
                targetShakingSpeed = 100;
                doublingTime = 9600;
                maxCellDensity = 1e6f;
            }
            default -> {
                targetTemperature = 37.0f;
                doublingTime = 1200;
                maxCellDensity = 1e8f;
            }
        }
    }

    private String guessCultureType(ItemStack culture) {
        // Try to determine culture type from item
        if (culture.isOf(ModItems.GENOME_SAMPLE)) {
            return "bacteria";  // Default assumption
        }
        return "unknown";
    }

    private void tickCulture() {
        cultureAge++;
        Random random = new Random();

        // Calculate growth rate modifier based on conditions
        float growthModifier = calculateGrowthModifier();

        // Phase transitions
        switch (phase) {
            case LAG -> {
                // Lag phase: adaptation, minimal growth
                if (cultureAge > doublingTime * 0.5) {
                    phase = GrowthPhase.LOG;
                }
            }
            case LOG -> {
                // Exponential growth
                if (growthModifier > 0) {
                    float growthRate = (float) Math.log(2) / doublingTime * growthModifier;
                    cellDensity *= (1 + growthRate);
                }

                // Check for stationary phase
                if (cellDensity >= maxCellDensity * 0.9f) {
                    phase = GrowthPhase.STATIONARY;
                }
            }
            case STATIONARY -> {
                // Slow growth, equilibrium
                cellDensity = Math.min(maxCellDensity, cellDensity * 1.001f);
                viability -= 0.0001f;

                // Decline after long stationary
                if (cultureAge > doublingTime * 10) {
                    phase = GrowthPhase.DECLINE;
                }
            }
            case DECLINE -> {
                // Cell death
                cellDensity *= 0.99f;
                viability -= 0.001f;

                if (viability <= 0.1f || cellDensity < 1e3f) {
                    phase = GrowthPhase.COMPLETE;
                    completeCulture();
                }
            }
        }

        // Update history
        if (cultureAge % 20 == 0) {  // Every second
            densityHistory[historyIndex] = (float) Math.log10(Math.max(1, cellDensity));
            historyIndex = (historyIndex + 1) % densityHistory.length;
        }

        // Random contamination check (reduced with better equipment)
        float contaminationRisk = 0.0001f / (tier.ordinal() + 1);
        if (random.nextFloat() < contaminationRisk) {
            handleContamination();
        }

        markDirty();
    }

    private float calculateGrowthModifier() {
        float modifier = 1.0f;

        // Temperature effect (bell curve around optimal)
        float optimalTemp = targetTemperature;
        float tempDiff = Math.abs(temperature - optimalTemp);
        modifier *= Math.exp(-tempDiff * tempDiff / 50);

        // Shaking effect (for bacteria/yeast)
        if (cultureType.equals("bacteria") || cultureType.equals("yeast")) {
            if (shakingSpeed > 0) {
                modifier *= 0.8f + 0.4f * Math.min(1, shakingSpeed / 200.0f);
            } else {
                modifier *= 0.5f;  // Poor aeration without shaking
            }
        }

        // CO2 for mammalian cells
        if (cultureType.equals("mammalian") || cultureType.equals("hela") || cultureType.equals("cho")) {
            if (spec.hasCO2Control()) {
                if (co2Percentage >= 4 && co2Percentage <= 6) {
                    modifier *= 1.0f;
                } else {
                    modifier *= 0.5f;
                }
            } else {
                modifier *= 0.3f;  // Need CO2 incubator
            }
        }

        // pH effect
        float optimalPh = 7.2f;
        float phDiff = Math.abs(ph - optimalPh);
        if (phDiff > 0.5f) {
            modifier *= Math.max(0.1f, 1.0f - phDiff);
        }

        return Math.max(0, modifier);
    }

    private void handleContamination() {
        // Contamination reduces viability and may ruin culture
        viability *= 0.5f;
        phase = GrowthPhase.DECLINE;
    }

    private void completeCulture() {
        // Create output based on final culture state
        if (cellDensity > 1e6f && viability > 0.5f) {
            ItemStack output = new ItemStack(ModItems.GENOME_SAMPLE);
            
            NbtCompound nbt = output.getOrCreateNbt();
            nbt.putString("Type", "cell_culture");
            nbt.putString("CellType", cultureType);
            nbt.putFloat("CellDensity", cellDensity);
            nbt.putFloat("Viability", viability);
            nbt.putInt("CultureAge", cultureAge);
            nbt.putFloat("FinalOD600", calculateOD600());

            ItemStack existing = getStack(OUTPUT_SLOT);
            if (existing.isEmpty()) {
                setStack(OUTPUT_SLOT, output);
            }
        }

        // Consume inputs
        getStack(CULTURE_SLOT).decrement(1);
        getStack(MEDIUM_SLOT).decrement(1);

        setActive(false);
    }

    private float calculateOD600() {
        // OD600 approximation: log relationship to cell density
        // OD600 of 1 ≈ 8×10^8 cells/mL for E. coli
        return (float) (cellDensity / 8e8);
    }

    public void harvestCulture() {
        if (phase == GrowthPhase.LOG || phase == GrowthPhase.STATIONARY) {
            phase = GrowthPhase.COMPLETE;
            completeCulture();
        }
    }

    public void resetCulture() {
        phase = GrowthPhase.IDLE;
        cellDensity = 0;
        cultureAge = 0;
        viability = 1.0f;
        setActive(false);
        markDirty();
    }

    // ========== Validation ==========

    private boolean isValidCulture(ItemStack stack) {
        return stack.isOf(ModItems.GENOME_SAMPLE);  // Cell cultures stored as genome samples
    }

    private boolean isValidMedium(ItemStack stack) {
        return stack.isOf(LabEquipmentItems.DMEM_MEDIUM) ||
               stack.isOf(LabEquipmentItems.RPMI_MEDIUM);
    }

    private boolean isValidVessel(ItemStack stack) {
        return stack.isOf(LabEquipmentItems.T25_FLASK) ||
               stack.isOf(LabEquipmentItems.T75_FLASK) ||
               stack.isOf(LabEquipmentItems.T175_FLASK) ||
               stack.isOf(LabEquipmentItems.WELL_PLATE_6) ||
               stack.isOf(LabEquipmentItems.WELL_PLATE_24) ||
               stack.isOf(LabEquipmentItems.WELL_PLATE_96);
    }

    // ========== Getters & Setters ==========

    public GrowthPhase getPhase() { return phase; }
    public float getTemperature() { return temperature; }
    public float getTargetTemperature() { return targetTemperature; }
    public int getShakingSpeed() { return shakingSpeed; }
    public float getCellDensity() { return cellDensity; }
    public float getViability() { return viability; }
    public int getCultureAge() { return cultureAge; }
    public String getCultureType() { return cultureType; }
    public float getCo2Percentage() { return co2Percentage; }
    public float getHumidity() { return humidity; }
    public float getPh() { return ph; }
    public float[] getDensityHistory() { return densityHistory; }
    public int getHistoryIndex() { return historyIndex; }
    public float getOD600() { return calculateOD600(); }

    public void setTargetTemperature(float temp) {
        this.targetTemperature = Math.max(spec.minTemp(), Math.min(spec.maxTemp(), temp));
        markDirty();
    }

    public void setTargetShakingSpeed(int rpm) {
        if (spec.hasShaking()) {
            this.targetShakingSpeed = Math.max(0, Math.min(spec.maxShakingRpm(), rpm));
            markDirty();
        }
    }

    public void setCo2Percentage(float co2) {
        if (spec.hasCO2Control()) {
            this.co2Percentage = Math.max(0, Math.min(20, co2));
            markDirty();
        }
    }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("Phase", phase.name());
        nbt.putFloat("Temperature", temperature);
        nbt.putFloat("TargetTemperature", targetTemperature);
        nbt.putInt("ShakingSpeed", shakingSpeed);
        nbt.putInt("TargetShakingSpeed", targetShakingSpeed);
        nbt.putFloat("CO2", co2Percentage);
        nbt.putFloat("Humidity", humidity);
        nbt.putFloat("PH", ph);
        nbt.putFloat("DO", dissolvedOxygen);
        nbt.putFloat("CellDensity", cellDensity);
        nbt.putFloat("MaxCellDensity", maxCellDensity);
        nbt.putInt("CultureAge", cultureAge);
        nbt.putString("CultureType", cultureType);
        nbt.putFloat("DoublingTime", doublingTime);
        nbt.putFloat("Viability", viability);
        nbt.putInt("HistoryIndex", historyIndex);

        // Store density history
        int[] historyInt = new int[densityHistory.length];
        for (int i = 0; i < densityHistory.length; i++) {
            historyInt[i] = Float.floatToIntBits(densityHistory[i]);
        }
        nbt.putIntArray("DensityHistory", historyInt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        try {
            phase = GrowthPhase.valueOf(nbt.getString("Phase"));
        } catch (Exception e) {
            phase = GrowthPhase.IDLE;
        }
        temperature = nbt.getFloat("Temperature");
        targetTemperature = nbt.getFloat("TargetTemperature");
        shakingSpeed = nbt.getInt("ShakingSpeed");
        targetShakingSpeed = nbt.getInt("TargetShakingSpeed");
        co2Percentage = nbt.getFloat("CO2");
        humidity = nbt.getFloat("Humidity");
        ph = nbt.getFloat("PH");
        dissolvedOxygen = nbt.getFloat("DO");
        cellDensity = nbt.getFloat("CellDensity");
        maxCellDensity = nbt.getFloat("MaxCellDensity");
        cultureAge = nbt.getInt("CultureAge");
        cultureType = nbt.getString("CultureType");
        doublingTime = nbt.getFloat("DoublingTime");
        viability = nbt.getFloat("Viability");
        historyIndex = nbt.getInt("HistoryIndex");

        int[] historyInt = nbt.getIntArray("DensityHistory");
        for (int i = 0; i < Math.min(historyInt.length, densityHistory.length); i++) {
            densityHistory[i] = Float.intBitsToFloat(historyInt[i]);
        }

        updateSpec();
    }

    @Override
    protected void completeProcess() { /* Handled in completeCulture */ }

    // ========== Screen ==========

    @Override
    public Text getDisplayName() {
        String key = switch (tier) {
            case BASIC -> "block.morerealisticgeneediting.diy_incubator";
            case ADVANCED -> "block.morerealisticgeneediting.shaking_incubator";
            case ELITE -> "block.morerealisticgeneediting.modular_bioreactor";
        };
        return Text.translatable(key);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return null;  // TODO: BioreactorScreenHandler
    }
}
