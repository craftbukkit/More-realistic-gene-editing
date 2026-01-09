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
 * Automated Liquid Handling Workstation
 * 
 * Based on real equipment:
 * - T2: Eppendorf epMotion 5075
 *   - 8-channel pipetting
 *   - 1-1000 μL range
 *   - Deck positions for plates/tubes
 *   - Pre-programmed methods
 * 
 * - T3: Waters Andrew+ Robot
 *   - Collaborative robot arm
 *   - Any labware compatibility
 *   - AI-assisted protocols
 *   - Multi-instrument integration
 * 
 * Capabilities:
 * - Serial dilution
 * - PCR setup
 * - Library preparation
 * - Cherry picking
 * - Plate replication
 * - Normalization
 */
public class LiquidHandlerBlockEntity extends LabEquipmentBlockEntity {

    // Deck positions (slots)
    public static final int DECK_POSITION_1 = 0;   // Source plate/tubes
    public static final int DECK_POSITION_2 = 1;   // Destination plate
    public static final int DECK_POSITION_3 = 2;   // Reagent reservoir
    public static final int DECK_POSITION_4 = 3;   // Tip rack 1
    public static final int DECK_POSITION_5 = 4;   // Tip rack 2
    public static final int DECK_POSITION_6 = 5;   // Waste
    public static final int DECK_POSITION_7 = 6;   // Additional position (T3)
    public static final int DECK_POSITION_8 = 7;   // Additional position (T3)

    // Protocol types
    public enum Protocol {
        SERIAL_DILUTION("Serial Dilution", 600),
        PCR_SETUP("PCR Setup", 1200),
        LIBRARY_PREP("Library Preparation", 2400),
        CHERRY_PICK("Cherry Picking", 400),
        PLATE_REPLICATE("Plate Replication", 800),
        NORMALIZATION("DNA Normalization", 1000),
        CUSTOM("Custom Protocol", 500)
    }

    // State
    private Protocol currentProtocol = null;
    private boolean isRunning = false;
    private int protocolProgress = 0;
    private int protocolMaxProgress = 0;
    private int currentStep = 0;
    private int totalSteps = 0;
    
    // Protocol parameters
    private int sourceWells = 96;
    private int destWells = 96;
    private float transferVolume = 10.0f;   // μL
    private int dilutionFactor = 2;
    private int dilutionSteps = 6;
    private float targetConcentration = 0;  // For normalization

    // Statistics
    private int tipsUsed = 0;
    private int transfersCompleted = 0;
    private float totalVolumeTransferred = 0;  // μL
    private List<String> protocolLog = new ArrayList<>();

    // Arm position (for visualization)
    private int armPositionX = 0;  // 0-100
    private int armPositionY = 0;
    private int armPositionZ = 0;  // Height

    public LiquidHandlerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8);
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot <= DECK_POSITION_5;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == DECK_POSITION_6 || slot == DECK_POSITION_7 || slot == DECK_POSITION_8;
    }

    @Override
    protected boolean canProcess() {
        if (currentProtocol == null) return false;
        
        // Check for tip rack
        ItemStack tips1 = getStack(DECK_POSITION_4);
        ItemStack tips2 = getStack(DECK_POSITION_5);
        if (tips1.isEmpty() && tips2.isEmpty()) return false;
        
        // Check for source and destination
        return !getStack(DECK_POSITION_1).isEmpty() && !getStack(DECK_POSITION_2).isEmpty();
    }

    public void setProtocol(Protocol protocol) {
        this.currentProtocol = protocol;
        this.protocolMaxProgress = protocol.duration;
        
        // Calculate steps based on protocol
        this.totalSteps = calculateSteps(protocol);
        this.currentStep = 0;
        
        markDirty();
    }

    private int calculateSteps(Protocol protocol) {
        return switch (protocol) {
            case SERIAL_DILUTION -> dilutionSteps * destWells;
            case PCR_SETUP -> sourceWells * 3;  // Template + primers + master mix
            case LIBRARY_PREP -> sourceWells * 8;  // Multiple steps
            case CHERRY_PICK -> Math.min(sourceWells, 24);  // Selective transfer
            case PLATE_REPLICATE -> sourceWells;
            case NORMALIZATION -> sourceWells * 2;  // Measure + transfer
            case CUSTOM -> 50;
        };
    }

    public void startProtocol() {
        if (!canProcess() || isRunning) return;
        
        isRunning = true;
        protocolProgress = 0;
        currentStep = 0;
        tipsUsed = 0;
        transfersCompleted = 0;
        totalVolumeTransferred = 0;
        protocolLog.clear();
        
        logProtocol("Starting protocol: " + currentProtocol.name);
        setActive(true);
        markDirty();
    }

    public void stopProtocol() {
        isRunning = false;
        logProtocol("Protocol stopped by user at step " + currentStep);
        setActive(false);
        markDirty();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;
        
        if (!isRunning) return;

        protocolProgress++;
        
        // Update arm position for animation
        updateArmPosition();

        // Execute protocol steps
        int stepsPerTick = tier == EquipmentTier.ELITE ? 2 : 1;
        for (int i = 0; i < stepsPerTick && currentStep < totalSteps; i++) {
            executeStep();
        }

        // Check completion
        if (protocolProgress >= protocolMaxProgress || currentStep >= totalSteps) {
            completeProtocol();
        }

        markDirty();
    }

    private void executeStep() {
        Random random = new Random();
        
        switch (currentProtocol) {
            case SERIAL_DILUTION -> executeSerialDilution(currentStep);
            case PCR_SETUP -> executePcrSetup(currentStep);
            case LIBRARY_PREP -> executeLibraryPrep(currentStep);
            case CHERRY_PICK -> executeCherryPick(currentStep);
            case PLATE_REPLICATE -> executePlateReplicate(currentStep);
            case NORMALIZATION -> executeNormalization(currentStep);
            case CUSTOM -> executeCustom(currentStep);
        }

        // Consume tips periodically
        if (currentStep % 8 == 0) {  // Change tips every 8 transfers (8-channel)
            consumeTip();
        }

        currentStep++;
        transfersCompleted++;
        totalVolumeTransferred += transferVolume;
    }

    private void executeSerialDilution(int step) {
        int row = step / dilutionSteps;
        int dilution = step % dilutionSteps;
        
        if (dilution == 0) {
            logProtocol(String.format("Row %d: Initial transfer %.1f μL", row + 1, transferVolume));
        } else {
            float dilutedVol = transferVolume * (float) Math.pow(dilutionFactor, dilution);
            logProtocol(String.format("Row %d, Dilution %d: 1:%d", row + 1, dilution + 1, 
                (int) Math.pow(dilutionFactor, dilution + 1)));
        }
    }

    private void executePcrSetup(int step) {
        int well = step / 3;
        int component = step % 3;
        
        String componentName = switch (component) {
            case 0 -> "Master Mix";
            case 1 -> "Template";
            default -> "Primers";
        };
        
        if (step % 24 == 0) {
            logProtocol(String.format("Setting up wells %d-%d: %s", well + 1, Math.min(well + 8, sourceWells), componentName));
        }
    }

    private void executeLibraryPrep(int step) {
        int well = step / 8;
        int substep = step % 8;
        
        String[] substepNames = {
            "End repair", "A-tailing", "Adapter ligation", "Clean-up 1",
            "Size selection", "PCR enrichment", "Clean-up 2", "QC aliquot"
        };
        
        if (substep == 0) {
            logProtocol(String.format("Sample %d: Starting library prep", well + 1));
        }
    }

    private void executeCherryPick(int step) {
        Random random = new Random();
        int sourceWell = random.nextInt(96) + 1;
        int destWell = step + 1;
        
        logProtocol(String.format("Transfer: Source A%d -> Dest %c%d", 
            sourceWell, (char)('A' + destWell / 12), destWell % 12 + 1));
    }

    private void executePlateReplicate(int step) {
        if (step % 8 == 0) {
            logProtocol(String.format("Replicating column %d", step / 8 + 1));
        }
    }

    private void executeNormalization(int step) {
        int well = step / 2;
        boolean isMeasure = step % 2 == 0;
        
        if (isMeasure) {
            // Simulated concentration measurement
            Random random = new Random();
            float conc = 10 + random.nextFloat() * 90;  // ng/μL
            logProtocol(String.format("Well %d: Measured %.1f ng/μL", well + 1, conc));
        } else {
            logProtocol(String.format("Well %d: Normalized to %.1f ng/μL", well + 1, targetConcentration));
        }
    }

    private void executeCustom(int step) {
        if (step % 10 == 0) {
            logProtocol(String.format("Custom step %d/%d", step + 1, totalSteps));
        }
    }

    private void updateArmPosition() {
        // Simulate robot arm movement
        int targetX = (currentStep * 100 / Math.max(1, totalSteps)) % 100;
        int targetY = (currentStep * 7) % 100;
        
        armPositionX += Math.signum(targetX - armPositionX) * 5;
        armPositionY += Math.signum(targetY - armPositionY) * 5;
        
        // Z movement for pipetting
        armPositionZ = (protocolProgress % 40 < 20) ? 0 : 50;
    }

    private void consumeTip() {
        ItemStack tips = getStack(DECK_POSITION_4);
        if (tips.isEmpty()) {
            tips = getStack(DECK_POSITION_5);
        }
        
        if (!tips.isEmpty()) {
            if (tips.getDamage() >= tips.getMaxDamage() - 1) {
                tips.decrement(1);
            } else {
                tips.setDamage(tips.getDamage() + 1);
            }
            tipsUsed++;
        }
    }

    private void completeProtocol() {
        isRunning = false;
        setActive(false);
        
        logProtocol("Protocol complete!");
        logProtocol(String.format("Statistics: %d transfers, %d tips used, %.1f μL total", 
            transfersCompleted, tipsUsed, totalVolumeTransferred));

        // Mark destination plate as processed
        ItemStack destPlate = getStack(DECK_POSITION_2);
        if (!destPlate.isEmpty()) {
            NbtCompound nbt = destPlate.getOrCreateNbt();
            nbt.putString("ProcessedBy", "LiquidHandler");
            nbt.putString("Protocol", currentProtocol.name);
            nbt.putInt("TransfersCompleted", transfersCompleted);
            nbt.putFloat("TotalVolume", totalVolumeTransferred);
        }
    }

    private void logProtocol(String message) {
        protocolLog.add(String.format("[%d] %s", protocolProgress, message));
        // Keep log size manageable
        while (protocolLog.size() > 100) {
            protocolLog.remove(0);
        }
    }

    // ========== Getters & Setters ==========

    public Protocol getCurrentProtocol() { return currentProtocol; }
    public boolean isRunning() { return isRunning; }
    public int getProtocolProgress() { return protocolProgress; }
    public int getProtocolMaxProgress() { return protocolMaxProgress; }
    public int getCurrentStep() { return currentStep; }
    public int getTotalSteps() { return totalSteps; }
    public int getTipsUsed() { return tipsUsed; }
    public int getTransfersCompleted() { return transfersCompleted; }
    public float getTotalVolumeTransferred() { return totalVolumeTransferred; }
    public List<String> getProtocolLog() { return protocolLog; }
    public int getArmPositionX() { return armPositionX; }
    public int getArmPositionY() { return armPositionY; }
    public int getArmPositionZ() { return armPositionZ; }

    public void setTransferVolume(float volume) {
        this.transferVolume = Math.max(0.5f, Math.min(1000, volume));
        markDirty();
    }

    public void setDilutionFactor(int factor) {
        this.dilutionFactor = Math.max(2, Math.min(10, factor));
        markDirty();
    }

    public void setDilutionSteps(int steps) {
        this.dilutionSteps = Math.max(1, Math.min(12, steps));
        markDirty();
    }

    public void setTargetConcentration(float conc) {
        this.targetConcentration = Math.max(1, Math.min(1000, conc));
        markDirty();
    }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        
        if (currentProtocol != null) {
            nbt.putString("Protocol", currentProtocol.name());
        }
        nbt.putBoolean("IsRunning", isRunning);
        nbt.putInt("ProtocolProgress", protocolProgress);
        nbt.putInt("ProtocolMaxProgress", protocolMaxProgress);
        nbt.putInt("CurrentStep", currentStep);
        nbt.putInt("TotalSteps", totalSteps);
        nbt.putInt("TipsUsed", tipsUsed);
        nbt.putInt("TransfersCompleted", transfersCompleted);
        nbt.putFloat("TotalVolumeTransferred", totalVolumeTransferred);
        nbt.putFloat("TransferVolume", transferVolume);
        nbt.putInt("DilutionFactor", dilutionFactor);
        nbt.putInt("DilutionSteps", dilutionSteps);
        nbt.putFloat("TargetConcentration", targetConcentration);
        nbt.putInt("ArmX", armPositionX);
        nbt.putInt("ArmY", armPositionY);
        nbt.putInt("ArmZ", armPositionZ);
        
        // Save log
        NbtList logList = new NbtList();
        for (String entry : protocolLog) {
            NbtCompound entryNbt = new NbtCompound();
            entryNbt.putString("Entry", entry);
            logList.add(entryNbt);
        }
        nbt.put("Log", logList);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        
        try {
            if (nbt.contains("Protocol")) {
                currentProtocol = Protocol.valueOf(nbt.getString("Protocol"));
            }
        } catch (Exception e) {
            currentProtocol = null;
        }
        
        isRunning = nbt.getBoolean("IsRunning");
        protocolProgress = nbt.getInt("ProtocolProgress");
        protocolMaxProgress = nbt.getInt("ProtocolMaxProgress");
        currentStep = nbt.getInt("CurrentStep");
        totalSteps = nbt.getInt("TotalSteps");
        tipsUsed = nbt.getInt("TipsUsed");
        transfersCompleted = nbt.getInt("TransfersCompleted");
        totalVolumeTransferred = nbt.getFloat("TotalVolumeTransferred");
        transferVolume = nbt.getFloat("TransferVolume");
        dilutionFactor = nbt.getInt("DilutionFactor");
        dilutionSteps = nbt.getInt("DilutionSteps");
        targetConcentration = nbt.getFloat("TargetConcentration");
        armPositionX = nbt.getInt("ArmX");
        armPositionY = nbt.getInt("ArmY");
        armPositionZ = nbt.getInt("ArmZ");
        
        // Load log
        protocolLog.clear();
        NbtList logList = nbt.getList("Log", 10);
        for (int i = 0; i < logList.size(); i++) {
            protocolLog.add(logList.getCompound(i).getString("Entry"));
        }
    }

    @Override
    protected void completeProcess() { /* Handled in completeProtocol */ }

    @Override
    public Text getDisplayName() {
        return Text.translatable(tier == EquipmentTier.ELITE ? 
            "block.morerealisticgeneediting.robot_pipetting_workstation" :
            "block.morerealisticgeneediting.auto_pipetting_workstation");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return null;  // TODO: LiquidHandlerScreenHandler
    }

    // Protocol enum with duration
    public enum ProtocolDef {
        ;
        public final String name;
        public final int duration;

        ProtocolDef(String name, int duration) {
            this.name = name;
            this.duration = duration;
        }
    }
}
