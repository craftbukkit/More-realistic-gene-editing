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
 * Electroporator Block Entity - Delivers electrical pulses for DNA/RNA delivery.
 * 
 * Based on: Bio-Rad Gene Pulser Xcell
 * - Voltage: 10-3000V
 * - Capacitance: 0.5-200 μF
 * - Pulse types: Exponential decay, Square wave
 * - Cuvettes: 1mm, 2mm, 4mm gap
 * 
 * Process:
 * 1. Mix cells with DNA/RNA in electroporation buffer
 * 2. Transfer to cuvette
 * 3. Apply electrical pulse
 * 4. Transfer to recovery medium
 * 
 * Parameters affect transformation efficiency:
 * - Voltage: Higher = more permeabilization, but more cell death
 * - Pulse duration: Controlled by capacitance and resistance
 * - Field strength (V/cm) = Voltage / gap distance
 * 
 * Typical settings:
 * - Bacteria: 1800V, 25μF, 200Ω (1mm cuvette) → ~2.5ms pulse
 * - Mammalian: 250V, 500μF (4mm cuvette) → exponential decay
 */
public class ElectroporatorBlockEntity extends LabEquipmentBlockEntity {

    // Slots
    public static final int CELLS_SLOT = 0;          // Competent cells
    public static final int DNA_SLOT = 1;            // DNA/plasmid to transform
    public static final int BUFFER_SLOT = 2;         // Electroporation buffer
    public static final int CUVETTE_SLOT = 3;        // Electroporation cuvette
    public static final int OUTPUT_SLOT = 4;         // Transformed cells

    // Pulse parameters
    private int voltage = 1800;              // Volts
    private int capacitance = 25;            // μF
    private int resistance = 200;            // Ω (for exponential decay)
    private int cuvetteGap = 1;              // mm
    private boolean squareWave = false;      // true = square wave, false = exponential
    private int pulseCount = 1;              // Number of pulses
    private int pulseDuration = 5;           // ms (for square wave)

    // State
    private boolean isPulsing = false;
    private int pulseProgress = 0;
    private float calculatedFieldStrength = 0;  // V/cm
    private float calculatedTimeConstant = 0;   // ms
    private float lastPulseVoltage = 0;
    private float lastPulseDuration = 0;

    // Results
    private float transformationEfficiency = 0;  // CFU/μg DNA
    private float cellViability = 0;             // Percentage surviving
    private boolean hasResults = false;

    private EquipmentSpecs.ElectroporatorSpec spec;

    public ElectroporatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5);
        this.spec = EquipmentSpecs.GENE_PULSER_XCELL;
        calculateParameters();
    }

    private void calculateParameters() {
        // Field strength E = V / d (V/cm)
        calculatedFieldStrength = voltage / (cuvetteGap * 0.1f);  // Convert mm to cm
        
        // Time constant τ = R × C (for exponential decay)
        calculatedTimeConstant = resistance * capacitance / 1000.0f;  // Convert to ms
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot >= CELLS_SLOT && slot <= CUVETTE_SLOT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected boolean canProcess() {
        ItemStack cells = getStack(CELLS_SLOT);
        ItemStack dna = getStack(DNA_SLOT);
        ItemStack buffer = getStack(BUFFER_SLOT);
        ItemStack cuvette = getStack(CUVETTE_SLOT);
        ItemStack output = getStack(OUTPUT_SLOT);

        if (cells.isEmpty() || dna.isEmpty() || buffer.isEmpty() || cuvette.isEmpty()) {
            return false;
        }

        // Validate inputs
        if (!isValidCells(cells)) return false;
        if (!isValidDna(dna)) return false;
        if (!isValidBuffer(buffer)) return false;
        if (!isValidCuvette(cuvette)) return false;

        // Check output space
        return output.isEmpty() || output.getCount() < output.getMaxCount();
    }

    /**
     * Execute electroporation pulse.
     */
    public void pulse() {
        if (!canProcess() || isPulsing) return;

        isPulsing = true;
        pulseProgress = 0;
        hasResults = false;
        setActive(true);
        
        // Store actual pulse parameters
        lastPulseVoltage = voltage;
        if (squareWave) {
            lastPulseDuration = pulseDuration;
        } else {
            lastPulseDuration = calculatedTimeConstant;
        }
        
        markDirty();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;

        if (isPulsing) {
            pulseProgress++;
            
            // Pulse takes about 1 second in game time (20 ticks)
            int pulseTicks = squareWave ? pulseDuration : (int)(calculatedTimeConstant * 2);
            pulseTicks = Math.max(20, Math.min(100, pulseTicks));
            
            if (pulseProgress >= pulseTicks) {
                completePulse();
            }
            markDirty();
        }
    }

    private void completePulse() {
        isPulsing = false;
        pulseProgress = 0;
        setActive(false);

        // Calculate transformation results
        Random random = new Random();
        
        // Base efficiency depends on field strength
        float optimalFieldStrength = getOptimalFieldStrength();
        float fieldRatio = calculatedFieldStrength / optimalFieldStrength;
        
        // Too low = poor transformation, too high = cell death
        float efficiencyFactor;
        if (fieldRatio < 0.5f) {
            efficiencyFactor = fieldRatio * 0.5f;
        } else if (fieldRatio > 2.0f) {
            efficiencyFactor = Math.max(0, 1.0f - (fieldRatio - 2.0f) * 0.5f);
        } else if (fieldRatio >= 0.8f && fieldRatio <= 1.2f) {
            efficiencyFactor = 1.0f;
        } else {
            efficiencyFactor = 0.7f + 0.3f * (1.0f - Math.abs(1.0f - fieldRatio));
        }

        // Cell viability inversely related to field strength
        float baseViability = 0.8f - (calculatedFieldStrength / 30000.0f);
        cellViability = Math.max(0.1f, Math.min(1.0f, baseViability + (random.nextFloat() - 0.5f) * 0.2f));

        // Transformation efficiency (CFU/μg)
        float baseEfficiency = 1e6f;  // Baseline for competent cells
        transformationEfficiency = baseEfficiency * efficiencyFactor * cellViability * (0.8f + random.nextFloat() * 0.4f);

        hasResults = true;

        // Create output if successful
        if (transformationEfficiency > 1e4f && cellViability > 0.2f) {
            createTransformedCells();
        }

        // Consume reagents
        consumeReagents();

        markDirty();
    }

    private float getOptimalFieldStrength() {
        // Optimal field strength depends on cell type
        ItemStack cells = getStack(CELLS_SLOT);
        NbtCompound nbt = cells.getNbt();
        
        String cellType = nbt != null && nbt.contains("CellType") ? 
                         nbt.getString("CellType") : "bacteria";
        
        return switch (cellType) {
            case "bacteria" -> 18000;   // E. coli: ~18 kV/cm
            case "yeast" -> 7500;       // Yeast: ~7.5 kV/cm
            case "mammalian" -> 625;    // Mammalian: ~0.625 kV/cm
            case "plant" -> 1250;       // Plant protoplasts: ~1.25 kV/cm
            default -> 12500;
        };
    }

    private void createTransformedCells() {
        ItemStack output = new ItemStack(ModItems.GENOME_SAMPLE);  // Transformed cells
        
        NbtCompound nbt = output.getOrCreateNbt();
        nbt.putString("Type", "transformed_cells");
        nbt.putFloat("TransformationEfficiency", transformationEfficiency);
        nbt.putFloat("CellViability", cellViability);
        nbt.putFloat("PulseVoltage", lastPulseVoltage);
        nbt.putFloat("PulseDuration", lastPulseDuration);
        
        // Copy DNA info
        ItemStack dna = getStack(DNA_SLOT);
        if (dna.getNbt() != null) {
            nbt.put("TransformedWith", dna.getNbt().copy());
        }

        ItemStack existing = getStack(OUTPUT_SLOT);
        if (existing.isEmpty()) {
            setStack(OUTPUT_SLOT, output);
        } else {
            existing.increment(1);
        }
    }

    private void consumeReagents() {
        getStack(CELLS_SLOT).decrement(1);
        getStack(DNA_SLOT).decrement(1);
        getStack(BUFFER_SLOT).decrement(1);
        // Cuvette can be reused a few times
        ItemStack cuvette = getStack(CUVETTE_SLOT);
        if (cuvette.getDamage() >= cuvette.getMaxDamage() - 1) {
            cuvette.decrement(1);
        } else {
            cuvette.setDamage(cuvette.getDamage() + 1);
        }
    }

    // ========== Validation ==========

    private boolean isValidCells(ItemStack stack) {
        // Competent cells or cell culture
        return stack.isOf(ModItems.GENOME_SAMPLE);  // Placeholder
    }

    private boolean isValidDna(ItemStack stack) {
        return stack.isOf(ModItems.DNA_SAMPLE) ||
               stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT) ||
               stack.isOf(ModItems.RECOMBINANT_PLASMID);
    }

    private boolean isValidBuffer(ItemStack stack) {
        return stack.isOf(LabEquipmentItems.ELECTROPORATION_BUFFER);
    }

    private boolean isValidCuvette(ItemStack stack) {
        // Check for electroporation cuvette
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("CuvetteGap")) {
            cuvetteGap = nbt.getInt("CuvetteGap");
            calculateParameters();
        }
        return true;  // Placeholder - accept any item for now
    }

    // ========== Parameter Setters ==========

    public void setVoltage(int v) {
        this.voltage = Math.max(spec.minVoltage(), Math.min(spec.maxVoltage(), v));
        calculateParameters();
        markDirty();
    }

    public void setCapacitance(int c) {
        this.capacitance = Math.max(1, Math.min(spec.maxCapacitance(), c));
        calculateParameters();
        markDirty();
    }

    public void setResistance(int r) {
        this.resistance = Math.max(50, Math.min(1000, r));
        calculateParameters();
        markDirty();
    }

    public void setCuvetteGap(int gap) {
        boolean valid = false;
        for (int validGap : spec.cuvetteSizes()) {
            if (gap == validGap) {
                valid = true;
                break;
            }
        }
        if (valid) {
            this.cuvetteGap = gap;
            calculateParameters();
            markDirty();
        }
    }

    public void setSquareWave(boolean square) {
        if (spec.hasSquareWave()) {
            this.squareWave = square;
            markDirty();
        }
    }

    public void setPulseDuration(int ms) {
        this.pulseDuration = Math.max(1, Math.min(100, ms));
        markDirty();
    }

    public void setPulseCount(int count) {
        this.pulseCount = Math.max(1, Math.min(10, count));
        markDirty();
    }

    // ========== Getters ==========

    public int getVoltage() { return voltage; }
    public int getCapacitance() { return capacitance; }
    public int getResistance() { return resistance; }
    public int getCuvetteGap() { return cuvetteGap; }
    public boolean isSquareWave() { return squareWave; }
    public int getPulseCount() { return pulseCount; }
    public int getPulseDuration() { return pulseDuration; }
    public boolean isPulsing() { return isPulsing; }
    public int getPulseProgress() { return pulseProgress; }
    public float getFieldStrength() { return calculatedFieldStrength; }
    public float getTimeConstant() { return calculatedTimeConstant; }
    public float getTransformationEfficiency() { return transformationEfficiency; }
    public float getCellViability() { return cellViability; }
    public boolean hasResults() { return hasResults; }

    // ========== Suggested Protocol ==========

    /**
     * Get suggested parameters for a cell type.
     */
    public static int[] getSuggestedParams(String cellType) {
        // Returns: voltage, capacitance, resistance, cuvetteGap
        return switch (cellType) {
            case "ecoli" -> new int[]{1800, 25, 200, 1};
            case "yeast" -> new int[]{1500, 25, 200, 2};
            case "mammalian" -> new int[]{250, 500, 0, 4};
            case "plant" -> new int[]{500, 125, 0, 4};
            default -> new int[]{1000, 25, 200, 2};
        };
    }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("Voltage", voltage);
        nbt.putInt("Capacitance", capacitance);
        nbt.putInt("Resistance", resistance);
        nbt.putInt("CuvetteGap", cuvetteGap);
        nbt.putBoolean("SquareWave", squareWave);
        nbt.putInt("PulseCount", pulseCount);
        nbt.putInt("PulseDurationMs", pulseDuration);
        nbt.putBoolean("IsPulsing", isPulsing);
        nbt.putInt("PulseProgress", pulseProgress);
        nbt.putFloat("TransformationEfficiency", transformationEfficiency);
        nbt.putFloat("CellViability", cellViability);
        nbt.putBoolean("HasResults", hasResults);
        nbt.putFloat("LastPulseVoltage", lastPulseVoltage);
        nbt.putFloat("LastPulseDuration", lastPulseDuration);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        voltage = nbt.getInt("Voltage");
        capacitance = nbt.getInt("Capacitance");
        resistance = nbt.getInt("Resistance");
        cuvetteGap = nbt.getInt("CuvetteGap");
        squareWave = nbt.getBoolean("SquareWave");
        pulseCount = nbt.getInt("PulseCount");
        pulseDuration = nbt.getInt("PulseDurationMs");
        isPulsing = nbt.getBoolean("IsPulsing");
        pulseProgress = nbt.getInt("PulseProgress");
        transformationEfficiency = nbt.getFloat("TransformationEfficiency");
        cellViability = nbt.getFloat("CellViability");
        hasResults = nbt.getBoolean("HasResults");
        lastPulseVoltage = nbt.getFloat("LastPulseVoltage");
        lastPulseDuration = nbt.getFloat("LastPulseDuration");
        
        calculateParameters();
    }

    @Override
    protected void completeProcess() { /* Handled in completePulse */ }

    // ========== Screen ==========

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.morerealisticgeneediting.electroporator");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return null;  // TODO: ElectroporatorScreenHandler
    }
}
