package com.morerealisticgeneediting.block.entity;
import com.morerealisticgeneediting.util.StackData;
import com.morerealisticgeneediting.screen.LabEquipmentScreenHandler;

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
 * - Output: Illustrative pellet and supernatant; centrifugation alone does not purify DNA.
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
    private int targetRpm = 3000;
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
        if (input.isEmpty() || targetRpm<=0) return false;
        
        // Check if input is a valid sample
        if (!isValidInput(input)) return false;
        
        // Check if outputs have space
        return hasSpaceForOutputs();
    }
    
    @Override
    protected void completeProcess() {
        if(!canProcess())return;
        ItemStack input=getStack(INPUT_SLOT);
        ItemStack pellet=input.copyWithCount(1);
        NbtCompound pelletData=StackData.read(pellet);
        pelletData.putString("Fraction","pellet");pelletData.putBoolean("Simulation",true);
        pelletData.putBoolean("DnaPurificationPerformed",false);StackData.write(pellet,pelletData);
        ItemStack supernatant=new ItemStack(Items.GLASS_BOTTLE);
        NbtCompound liquidData=new NbtCompound();liquidData.putString("Fraction","supernatant");
        liquidData.putBoolean("Simulation",true);liquidData.putBoolean("DnaPurificationPerformed",false);
        StackData.write(supernatant,liquidData);
        setStack(PELLET_SLOT,pellet);setStack(SUPERNATANT_SLOT,supernatant);input.decrement(1);
        rpm=0;
    }
    private boolean isValidInput(ItemStack stack) {
        return stack.isOf(ModItems.GENOME_SAMPLE) && StackData.has(stack);
    }
    private boolean hasSpaceForOutputs() {
        return getStack(PELLET_SLOT).isEmpty() && getStack(SUPERNATANT_SLOT).isEmpty();
    }
    @Override public boolean isValid(int slot,ItemStack stack) {
        return super.isValid(slot,stack) && isValidInput(stack);
    }
    @Override public void tick() {
        if(world==null || world.isClient || isRemoved() || world.getBlockEntity(pos)!=this)return;
        if(canProcess())rpm=Math.min(targetRpm,rpm+100);else rpm=Math.max(0,rpm-200);
        super.tick();
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
     * The coefficient uses radius in centimetres; radius is illustrative, not a vendor specification.
     */
    public double getCurrentRcf() {
        double r = tier == EquipmentTier.ELITE ? 15.0 : 10.0;  // centimetres
        return 1.118e-5 * r * rpm * rpm;
    }
    
    public void setTargetRpm(int rpm) {
        this.targetRpm = Math.max(0, Math.min(rpm, getMaxRpm()));
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
        return new LabEquipmentScreenHandler(syncId, playerInventory, this);
    }
}
