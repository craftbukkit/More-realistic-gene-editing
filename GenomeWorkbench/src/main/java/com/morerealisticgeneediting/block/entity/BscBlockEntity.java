package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.EquipmentSpecs;
import com.morerealisticgeneediting.equipment.EquipmentTier;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Biosafety Cabinet (BSC) Block Entity - Sterile work environment.
 * 
 * Based on real equipment:
 * - T1: Laminar Flow Hood (Simple) - Product protection only
 * - T2: Class II BSC (Esco Airstream) - Personnel AND product protection
 * - T3: Isolator Workstation (Esco Isoclean) - Complete containment
 */
public class BscBlockEntity extends LabEquipmentBlockEntity {

    // Work surface slots
    public static final int WORK_SLOT_1 = 0;
    public static final int WORK_SLOT_2 = 1;
    public static final int WORK_SLOT_3 = 2;
    public static final int WORK_SLOT_4 = 3;
    public static final int WORK_SLOT_5 = 4;
    public static final int WORK_SLOT_6 = 5;

    // Cabinet state
    private boolean airflowOn = false;
    private boolean uvLampOn = false;
    private boolean sashOpen = true;
    private float sashHeight = 100;
    
    // Air flow parameters
    private float airflowVelocity = 0;
    private float targetAirflowVelocity = 0.45f;  // m/s
    private int hepaFilterLife = 10000;
    private static final int MAX_HEPA_LIFE = 10000;
    
    // UV decontamination
    private boolean uvCycleActive = false;
    private int uvCycleProgress = 0;
    private static final int UV_CYCLE_DURATION = 600;  // 30 seconds game time
    
    // Sterility metrics
    private float sterilityLevel = 100;
    private float contaminationRisk = 0;
    private int particleCount = 0;
    
    // Alarm states
    private boolean airflowAlarm = false;
    private boolean filterAlarm = false;
    private boolean sashAlarm = false;

    private EquipmentSpecs.BiosafetySpec spec;

    public BscBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6);
        updateSpec();
    }

    private void updateSpec() {
        this.spec = switch (tier) {
            case BASIC -> EquipmentSpecs.LAMINAR_FLOW_HOOD;
            case ADVANCED -> EquipmentSpecs.BIOSAFETY_CABINET_II;
            case ELITE -> EquipmentSpecs.ISOLATOR;
        };
    }

    @Override
    public void setTier(EquipmentTier tier) {
        super.setTier(tier);
        updateSpec();
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return true;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean canProcess() {
        return airflowOn;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;

        if (airflowOn) {
            updateAirflow();
            consumeFilterLife();
            updateSterility();
        } else {
            degradeSterility();
        }

        if (uvCycleActive) {
            tickUvCycle();
        }

        checkAlarms();
        markDirty();
    }

    private void updateAirflow() {
        float rampRate = 0.01f;
        
        if (airflowVelocity < targetAirflowVelocity) {
            airflowVelocity = Math.min(targetAirflowVelocity, airflowVelocity + rampRate);
        } else if (airflowVelocity > targetAirflowVelocity) {
            airflowVelocity = Math.max(0, airflowVelocity - rampRate * 2);
        }

        Random random = new Random();
        float filterEfficiency = (float) hepaFilterLife / MAX_HEPA_LIFE;
        
        if (tier == EquipmentTier.ELITE) {
            particleCount = random.nextInt(10);
        } else if (tier == EquipmentTier.ADVANCED) {
            particleCount = (int) (50 * (1 - filterEfficiency) + random.nextInt(50));
        } else {
            particleCount = (int) (100 * (1 - filterEfficiency) + random.nextInt(100));
        }
    }

    private void consumeFilterLife() {
        if (hepaFilterLife > 0) {
            hepaFilterLife--;
        }
    }

    private void updateSterility() {
        float targetSterility = 100 * spec.protectionFactor();
        
        if (tier != EquipmentTier.ELITE) {
            if (sashHeight > 80) {
                targetSterility *= 0.7f;
            } else if (sashHeight < 20) {
                targetSterility *= 0.9f;
            }
        }

        float filterFactor = 0.5f + 0.5f * ((float) hepaFilterLife / MAX_HEPA_LIFE);
        targetSterility *= filterFactor;

        if (sterilityLevel < targetSterility) {
            sterilityLevel += 0.1f;
        } else if (sterilityLevel > targetSterility) {
            sterilityLevel -= 0.05f;
        }

        sterilityLevel = Math.max(0, Math.min(100, sterilityLevel));
        contaminationRisk = (100 - sterilityLevel) / 100.0f * 0.01f;
    }

    private void degradeSterility() {
        sterilityLevel = Math.max(0, sterilityLevel - 0.5f);
        contaminationRisk = (100 - sterilityLevel) / 100.0f * 0.1f;
    }

    private void tickUvCycle() {
        uvCycleProgress++;
        
        if (uvCycleProgress % 20 == 0) {
            sterilityLevel = Math.min(100, sterilityLevel + 5);
        }

        if (uvCycleProgress >= UV_CYCLE_DURATION) {
            uvCycleActive = false;
            uvLampOn = false;
            uvCycleProgress = 0;
            sterilityLevel = 100;
        }
    }

    private void checkAlarms() {
        airflowAlarm = airflowOn && airflowVelocity < targetAirflowVelocity * 0.8f;
        filterAlarm = hepaFilterLife < MAX_HEPA_LIFE * 0.1f;
        sashAlarm = tier == EquipmentTier.ADVANCED && sashOpen && sashHeight > 80;
    }

    /**
     * Get sterility bonus for operations performed in cabinet.
     */
    public float getSterilityBonus() {
        if (!airflowOn) return 1.0f;
        float bonus = 1.0f + (sterilityLevel / 100.0f) * 0.2f;
        if (tier == EquipmentTier.ELITE) bonus += 0.1f;
        return bonus;
    }

    /**
     * Check if contamination occurred.
     */
    public boolean checkContamination() {
        if (!airflowOn) return new Random().nextFloat() < 0.1f;
        return new Random().nextFloat() < contaminationRisk;
    }

    // ========== Controls ==========

    public void toggleAirflow() {
        airflowOn = !airflowOn;
        if (!airflowOn) airflowVelocity = 0;
        setActive(airflowOn);
        markDirty();
    }

    public void startUvCycle() {
        if (!spec.hasUV() || uvCycleActive) return;
        if (tier != EquipmentTier.ELITE) {
            sashOpen = false;
            sashHeight = 0;
        }
        uvCycleActive = true;
        uvLampOn = true;
        uvCycleProgress = 0;
        markDirty();
    }

    public void setSashHeight(float height) {
        if (tier == EquipmentTier.ELITE) return;
        this.sashHeight = Math.max(0, Math.min(100, height));
        this.sashOpen = sashHeight > 5;
        markDirty();
    }

    public void replaceFilter() {
        hepaFilterLife = MAX_HEPA_LIFE;
        filterAlarm = false;
        markDirty();
    }

    // ========== Getters ==========

    public boolean isAirflowOn() { return airflowOn; }
    public boolean isUvLampOn() { return uvLampOn; }
    public boolean isSashOpen() { return sashOpen; }
    public float getSashHeight() { return sashHeight; }
    public float getAirflowVelocity() { return airflowVelocity; }
    public int getHepaFilterLife() { return hepaFilterLife; }
    public int getMaxHepaLife() { return MAX_HEPA_LIFE; }
    public float getSterilityLevel() { return sterilityLevel; }
    public float getContaminationRisk() { return contaminationRisk; }
    public int getParticleCount() { return particleCount; }
    public boolean isAirflowAlarm() { return airflowAlarm; }
    public boolean isFilterAlarm() { return filterAlarm; }
    public boolean isSashAlarm() { return sashAlarm; }
    public boolean isUvCycleActive() { return uvCycleActive; }
    public int getUvCycleProgress() { return uvCycleProgress; }
    public int getUvCycleDuration() { return UV_CYCLE_DURATION; }
    public int getSafetyClass() { return spec != null ? spec.safetyClass() : 0; }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("AirflowOn", airflowOn);
        nbt.putBoolean("UvLampOn", uvLampOn);
        nbt.putBoolean("SashOpen", sashOpen);
        nbt.putFloat("SashHeight", sashHeight);
        nbt.putFloat("AirflowVelocity", airflowVelocity);
        nbt.putInt("HepaFilterLife", hepaFilterLife);
        nbt.putFloat("SterilityLevel", sterilityLevel);
        nbt.putFloat("ContaminationRisk", contaminationRisk);
        nbt.putInt("ParticleCount", particleCount);
        nbt.putBoolean("UvCycleActive", uvCycleActive);
        nbt.putInt("UvCycleProgress", uvCycleProgress);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        airflowOn = nbt.getBoolean("AirflowOn");
        uvLampOn = nbt.getBoolean("UvLampOn");
        sashOpen = nbt.getBoolean("SashOpen");
        sashHeight = nbt.getFloat("SashHeight");
        airflowVelocity = nbt.getFloat("AirflowVelocity");
        hepaFilterLife = nbt.getInt("HepaFilterLife");
        sterilityLevel = nbt.getFloat("SterilityLevel");
        contaminationRisk = nbt.getFloat("ContaminationRisk");
        particleCount = nbt.getInt("ParticleCount");
        uvCycleActive = nbt.getBoolean("UvCycleActive");
        uvCycleProgress = nbt.getInt("UvCycleProgress");
        updateSpec();
    }

    @Override
    protected void completeProcess() { /* Continuous operation */ }

    @Override
    public Text getDisplayName() {
        String key = switch (tier) {
            case BASIC -> "block.morerealisticgeneediting.laminar_flow_hood";
            case ADVANCED -> "block.morerealisticgeneediting.biosafety_cabinet_ii";
            case ELITE -> "block.morerealisticgeneediting.isolator_workstation";
        };
        return Text.translatable(key);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return null;
    }
}
