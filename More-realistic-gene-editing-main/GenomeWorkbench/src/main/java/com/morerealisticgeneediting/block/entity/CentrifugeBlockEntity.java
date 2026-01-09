package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.EquipmentTier;
import com.morerealisticgeneediting.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Centrifuge Block Entity
 * 
 * Function: Separates biological samples by density using centrifugal force.
 * 
 * Process:
 * - Input: Biological sample (e.g., blood, cell culture)
 * - Output: Separated components (DNA, proteins, cell debris)
 * 
 * Tier differences:
 * - Basic (Palm): Low speed, small capacity, room temp only
 * - Advanced (Refrigerated): Higher speed, temperature control
 * - Elite (Ultracentrifuge): Ultra-high speed for subcellular fractionation
 */
public class CentrifugeBlockEntity extends LabEquipmentBlockEntity {
    
    // Slots: 0 = input, 1-3 = outputs (pellet, supernatant, interface)
    public static final int INPUT_SLOT = 0;
    public static final int PELLET_SLOT = 1;      // Heavy fraction
    public static final int SUPERNATANT_SLOT = 2;  // Light fraction
    public static final int INTERFACE_SLOT = 3;    // Middle fraction (if applicable)
    
    private int rpm = 0;
    private int targetRpm = 0;
    private float temperature = 20.0f;  // Celsius
    private int runTime = 0;            // Ticks remaining
    
    public CentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4);
        this.maxProgress = 200;  // 10 seconds at 20 tps
    }
    
    @Override
    protected boolean isInputSlot(int slot) {
        return slot == INPUT_SLOT;
    }
    
    @Override
    protected boolean isOutputSlot(int slot) {
        return slot >= PELLET_SLOT && slot <= INTERFACE_SLOT;
    }
    
    @Override
    protected boolean canProcess() {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) return false;
        
        // Check if input is a valid sample
        if (!isValidInput(input)) return false;
        
        // Check if outputs have space
        return hasSpaceForOutputs();
    }
    
    @Override
    protected void completeProcess() {
        ItemStack input = getStack(INPUT_SLOT);
        if (input.isEmpty()) return;
        
        Random random = new Random();
        
        // Process based on input type and tier
        if (isGenomeSample(input) || isDnaSample(input)) {
            // DNA extraction centrifugation
            // Pellet: cell debris
            // Supernatant: DNA in solution
            
            if (getStack(PELLET_SLOT).isEmpty()) {
                setStack(PELLET_SLOT, new ItemStack(Items.GRAY_DYE, 1)); // Placeholder for debris
            }
            
            if (getStack(SUPERNATANT_SLOT).isEmpty()) {
                // Higher tier = better yield
                float successChance = 0.7f + tier.getSuccessRateBonus();
                if (random.nextFloat() < successChance) {
                    setStack(SUPERNATANT_SLOT, new ItemStack(ModItems.DNA_SAMPLE, 1));
                }
            }
        }
        
        // Consume input
        input.decrement(1);
        
        // Reset RPM
        rpm = 0;
    }
    
    private boolean isValidInput(ItemStack stack) {
        // Check for valid biological samples
        return isGenomeSample(stack) || 
               isDnaSample(stack) ||
               stack.isOf(Items.ROTTEN_FLESH) ||  // Placeholder for biological material
               stack.isOf(Items.BONE);
    }
    
    private boolean isGenomeSample(ItemStack stack) {
        return stack.isOf(ModItems.GENOME_SAMPLE);
    }
    
    private boolean isDnaSample(ItemStack stack) {
        return stack.isOf(ModItems.DNA_SAMPLE);
    }
    
    private boolean hasSpaceForOutputs() {
        return getStack(PELLET_SLOT).isEmpty() || 
               getStack(SUPERNATANT_SLOT).isEmpty() ||
               getStack(INTERFACE_SLOT).isEmpty();
    }
    
    // ========== Centrifuge-specific methods ==========
    
    /**
     * Get max RPM based on tier
     */
    public int getMaxRpm() {
        return switch (tier) {
            case BASIC -> 6000;
            case ADVANCED -> 20000;
            case ELITE -> 100000;
        };
    }
    
    /**
     * Get current RCF (Relative Centrifugal Force)
     * RCF = 1.118 × 10^-5 × r × RPM²
     * Assuming r = 10cm = 0.1m for benchtop
     */
    public double getCurrentRcf() {
        double r = tier == EquipmentTier.ELITE ? 0.15 : 0.10;  // meters
        return 1.118e-5 * r * rpm * rpm;
    }
    
    public void setTargetRpm(int rpm) {
        this.targetRpm = Math.min(rpm, getMaxRpm());
    }
    
    public int getRpm() {
        return rpm;
    }
    
    public float getTemperature() {
        return temperature;
    }
    
    public void setTemperature(float temp) {
        // Only advanced+ tiers can control temperature
        if (tier != EquipmentTier.BASIC) {
            this.temperature = Math.max(-20, Math.min(40, temp));
        }
    }
    
    // ========== NBT ==========
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("Rpm", rpm);
        nbt.putInt("TargetRpm", targetRpm);
        nbt.putFloat("Temperature", temperature);
        nbt.putInt("RunTime", runTime);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        rpm = nbt.getInt("Rpm");
        targetRpm = nbt.getInt("TargetRpm");
        temperature = nbt.getFloat("Temperature");
        runTime = nbt.getInt("RunTime");
    }
    
    // ========== Screen ==========
    
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.morerealisticgeneediting.centrifuge." + tier.getId());
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        // TODO: Create CentrifugeScreenHandler
        return null;
    }
}
