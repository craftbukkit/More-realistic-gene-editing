package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.EquipmentTier;
import com.morerealisticgeneediting.genome.pcr.PcrSimulator;
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

/**
 * PCR Machine (Thermal Cycler) Block Entity
 * 
 * Function: Amplifies DNA through polymerase chain reaction.
 * 
 * Process:
 * - Input: DNA template, primers, PCR tube with reagents
 * - Output: Amplified DNA fragments
 * 
 * Thermal Cycling Steps:
 * 1. Initial Denaturation: 94-98°C, 2-5 min
 * 2. Denaturation: 94-98°C, 20-30 sec
 * 3. Annealing: 50-65°C, 20-40 sec
 * 4. Extension: 72°C, 1 min/kb
 * 5. Final Extension: 72°C, 5-10 min
 * 
 * Tier differences:
 * - Basic (OpenPCR): Manual programming, slower ramp rate
 * - Advanced (T100): Fast ramp, gradient capability
 * - Elite (qPCR): Real-time quantification, high throughput
 */
public class ThermalCyclerBlockEntity extends LabEquipmentBlockEntity {
    
    // Slots
    public static final int TEMPLATE_SLOT = 0;      // DNA template
    public static final int PRIMER_F_SLOT = 1;      // Forward primer
    public static final int PRIMER_R_SLOT = 2;      // Reverse primer
    public static final int TUBE_SLOT = 3;          // PCR tube with reagents
    public static final int OUTPUT_SLOT = 4;        // Amplified product
    
    // PCR Parameters
    private int currentCycle = 0;
    private int totalCycles = 30;
    private int currentStep = 0;  // 0=idle, 1=denature, 2=anneal, 3=extend
    private float currentTemperature = 25.0f;
    private float targetTemperature = 25.0f;
    private float annealingTemp = 55.0f;
    
    // Temperature ramp rate (°C per tick)
    private float rampRate;
    
    // Step names for display
    private static final String[] STEP_NAMES = {
        "Idle", "Denaturation", "Annealing", "Extension", "Final Extension", "Complete"
    };
    
    public ThermalCyclerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5);
        this.maxProgress = 600;  // Per cycle
        updateRampRate();
    }
    
    private void updateRampRate() {
        // Ramp rate in °C per tick
        rampRate = switch (tier) {
            case BASIC -> 0.5f;    // ~10°C/sec (slow)
            case ADVANCED -> 2.0f;  // ~40°C/sec (standard)
            case ELITE -> 4.0f;     // ~80°C/sec (fast)
        };
    }
    
    @Override
    public void setTier(EquipmentTier tier) {
        super.setTier(tier);
        updateRampRate();
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
        // Check all required inputs
        ItemStack template = getStack(TEMPLATE_SLOT);
        ItemStack primerF = getStack(PRIMER_F_SLOT);
        ItemStack primerR = getStack(PRIMER_R_SLOT);
        ItemStack tube = getStack(TUBE_SLOT);
        ItemStack output = getStack(OUTPUT_SLOT);
        
        if (template.isEmpty() || primerF.isEmpty() || primerR.isEmpty() || tube.isEmpty()) {
            return false;
        }
        
        // Check valid inputs
        if (!isValidTemplate(template)) return false;
        if (!isValidPrimer(primerF) || !isValidPrimer(primerR)) return false;
        if (!isValidTube(tube)) return false;
        
        // Check output space
        return output.isEmpty() || 
               (output.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT) && output.getCount() < output.getMaxCount());
    }
    
    @Override
    protected void completeProcess() {
        currentCycle++;
        
        if (currentCycle >= totalCycles) {
            // PCR complete - produce output
            ItemStack output = getStack(OUTPUT_SLOT);
            
            if (output.isEmpty()) {
                // Create amplified product
                ItemStack product = new ItemStack(ModItems.AMPLIFIED_GENE_FRAGMENT, 1);
                
                // Store PCR result data in NBT
                NbtCompound nbt = product.getOrCreateNbt();
                nbt.putInt("Cycles", totalCycles);
                nbt.putFloat("AnnealingTemp", annealingTemp);
                nbt.putString("Tier", tier.getId());
                
                // Copy template information if available
                ItemStack template = getStack(TEMPLATE_SLOT);
                if (template.hasNbt()) {
                    nbt.put("SourceData", template.getNbt().copy());
                }
                
                setStack(OUTPUT_SLOT, product);
            } else {
                output.increment(1);
            }
            
            // Consume reagents
            getStack(TEMPLATE_SLOT).decrement(1);
            getStack(PRIMER_F_SLOT).decrement(1);
            getStack(PRIMER_R_SLOT).decrement(1);
            getStack(TUBE_SLOT).decrement(1);
            
            // Reset
            currentCycle = 0;
            currentStep = 0;
            currentTemperature = 25.0f;
        }
    }
    
    /**
     * Custom tick for thermal cycling simulation
     */
    public void tickThermalCycling() {
        if (!canProcess()) {
            currentStep = 0;
            return;
        }
        
        // Update temperature towards target
        if (Math.abs(currentTemperature - targetTemperature) > 0.5f) {
            if (currentTemperature < targetTemperature) {
                currentTemperature = Math.min(currentTemperature + rampRate, targetTemperature);
            } else {
                currentTemperature = Math.max(currentTemperature - rampRate, targetTemperature);
            }
            return;  // Wait for temperature to stabilize
        }
        
        // Progress through PCR steps
        progress++;
        
        int stepDuration = getStepDuration(currentStep);
        
        if (progress >= stepDuration) {
            progress = 0;
            advanceStep();
        }
    }
    
    private void advanceStep() {
        currentStep++;
        
        if (currentStep > 3) {
            // Completed one cycle
            currentStep = 1;  // Back to denaturation
            completeProcess();
        }
        
        // Set target temperature for next step
        targetTemperature = switch (currentStep) {
            case 1 -> 95.0f;  // Denaturation
            case 2 -> annealingTemp;  // Annealing
            case 3 -> 72.0f;  // Extension
            default -> 25.0f;
        };
    }
    
    private int getStepDuration(int step) {
        // Duration in ticks (20 ticks = 1 second)
        int baseDuration = switch (step) {
            case 1 -> 30 * 20;   // Denaturation: 30 sec
            case 2 -> 30 * 20;   // Annealing: 30 sec
            case 3 -> 60 * 20;   // Extension: 60 sec
            default -> 0;
        };
        
        // Apply tier speed multiplier
        return (int) (baseDuration * tier.getSpeedMultiplier());
    }
    
    // ========== Validation ==========
    
    private boolean isValidTemplate(ItemStack stack) {
        return stack.isOf(ModItems.DNA_SAMPLE) || 
               stack.isOf(ModItems.GENOME_SAMPLE) ||
               stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT);
    }
    
    private boolean isValidPrimer(ItemStack stack) {
        return stack.isOf(ModItems.PRIMER);
    }
    
    private boolean isValidTube(ItemStack stack) {
        return stack.isOf(ModItems.PCR_TUBE);
    }
    
    // ========== Getters/Setters ==========
    
    public int getCurrentCycle() {
        return currentCycle;
    }
    
    public int getTotalCycles() {
        return totalCycles;
    }
    
    public void setTotalCycles(int cycles) {
        this.totalCycles = Math.max(1, Math.min(50, cycles));
    }
    
    public int getCurrentStep() {
        return currentStep;
    }
    
    public String getCurrentStepName() {
        return currentStep < STEP_NAMES.length ? STEP_NAMES[currentStep] : "Unknown";
    }
    
    public float getCurrentTemperature() {
        return currentTemperature;
    }
    
    public float getTargetTemperature() {
        return targetTemperature;
    }
    
    public float getAnnealingTemp() {
        return annealingTemp;
    }
    
    public void setAnnealingTemp(float temp) {
        this.annealingTemp = Math.max(45, Math.min(72, temp));
    }
    
    /**
     * Get cycle progress as percentage
     */
    public float getCycleProgress() {
        return (float) currentCycle / totalCycles;
    }
    
    // ========== NBT ==========
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("CurrentCycle", currentCycle);
        nbt.putInt("TotalCycles", totalCycles);
        nbt.putInt("CurrentStep", currentStep);
        nbt.putFloat("CurrentTemp", currentTemperature);
        nbt.putFloat("TargetTemp", targetTemperature);
        nbt.putFloat("AnnealingTemp", annealingTemp);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        currentCycle = nbt.getInt("CurrentCycle");
        totalCycles = nbt.getInt("TotalCycles");
        currentStep = nbt.getInt("CurrentStep");
        currentTemperature = nbt.getFloat("CurrentTemp");
        targetTemperature = nbt.getFloat("TargetTemp");
        annealingTemp = nbt.getFloat("AnnealingTemp");
    }
    
    // ========== Screen ==========
    
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.morerealisticgeneediting.thermal_cycler." + tier.getId());
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        // TODO: Create ThermalCyclerScreenHandler
        return null;
    }
}
