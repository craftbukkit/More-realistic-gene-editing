package com.morerealisticgeneediting.block.entity;

import com.morerealisticgeneediting.block.LabEquipmentBlockEntity;
import com.morerealisticgeneediting.equipment.EquipmentSpecs;
import com.morerealisticgeneediting.equipment.EquipmentTier;
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
 * Spectrophotometer Block Entity - Measures DNA/RNA/Protein concentration.
 * 
 * Based on real equipment:
 * - T1: 721 Visible Spectrophotometer (340-1000nm, cuvette)
 * - T2: Thermo NanoDrop One (190-850nm, micro-volume)
 * - T3: Unchained Labs Lunatic (230-750nm, 96-well, high throughput)
 * 
 * Measurements:
 * - A260: DNA/RNA absorbance
 * - A280: Protein absorbance
 * - A260/A280 ratio: Purity indicator (pure DNA ~1.8, pure RNA ~2.0)
 * - A260/A230 ratio: Contamination indicator (ideal >2.0)
 * 
 * Concentration calculation:
 * - dsDNA: c (ng/μL) = A260 × 50
 * - ssDNA: c (ng/μL) = A260 × 33
 * - RNA: c (ng/μL) = A260 × 40
 */
public class SpectrophotometerBlockEntity extends LabEquipmentBlockEntity {

    // Slots
    public static final int SAMPLE_SLOT = 0;
    public static final int BLANK_SLOT = 1;  // Blank/reference
    public static final int CUVETTE_SLOT = 2;  // For T1 only

    // Measurement state
    private boolean blanked = false;
    private float blankA260 = 0;
    private float blankA280 = 0;
    private float blankA230 = 0;

    // Results
    private float absorbance260 = 0;
    private float absorbance280 = 0;
    private float absorbance230 = 0;
    private float concentration = 0;      // ng/μL
    private float purityRatio260_280 = 0;
    private float purityRatio260_230 = 0;
    private String sampleType = "unknown";
    private boolean hasResults = false;

    // Full spectrum data (for higher tiers)
    private float[] spectrumData;  // Absorbance at each wavelength
    private int spectrumMinWl = 230;
    private int spectrumMaxWl = 850;

    private EquipmentSpecs.SpectrophotometerSpec spec;

    public SpectrophotometerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3);
        updateSpec();
    }

    private void updateSpec() {
        this.spec = switch (tier) {
            case BASIC -> EquipmentSpecs.VISIBLE_721;
            case ADVANCED -> EquipmentSpecs.NANODROP_ONE;
            case ELITE -> EquipmentSpecs.LUNATIC;
        };
        
        if (spec != null) {
            spectrumMinWl = spec.minWavelength();
            spectrumMaxWl = spec.maxWavelength();
            int range = spectrumMaxWl - spectrumMinWl;
            spectrumData = new float[range];
        }
    }

    @Override
    public void setTier(EquipmentTier tier) {
        super.setTier(tier);
        updateSpec();
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == SAMPLE_SLOT || slot == BLANK_SLOT || slot == CUVETTE_SLOT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;  // Non-destructive measurement
    }

    @Override
    protected boolean canProcess() {
        // Need blanked reference and sample
        if (!blanked) return false;
        
        ItemStack sample = getStack(SAMPLE_SLOT);
        if (sample.isEmpty()) return false;
        
        // T1 requires cuvette
        if (tier == EquipmentTier.BASIC && getStack(CUVETTE_SLOT).isEmpty()) {
            return false;
        }
        
        return isValidSample(sample);
    }

    /**
     * Perform blank measurement.
     */
    public void performBlank() {
        ItemStack blank = getStack(BLANK_SLOT);
        
        // Generate baseline absorbance values
        Random random = new Random();
        blankA260 = 0.01f + random.nextFloat() * 0.02f;  // Small baseline
        blankA280 = 0.01f + random.nextFloat() * 0.02f;
        blankA230 = 0.02f + random.nextFloat() * 0.03f;
        
        blanked = true;
        hasResults = false;
        markDirty();
    }

    /**
     * Perform measurement on sample.
     */
    public void measure() {
        if (!canProcess()) return;
        
        ItemStack sample = getStack(SAMPLE_SLOT);
        Random random = new Random();
        
        // Determine sample type and generate realistic values
        sampleType = determineSampleType(sample);
        
        // Generate raw absorbance based on sample
        float rawA260, rawA280, rawA230;
        
        switch (sampleType) {
            case "dsDNA" -> {
                // Pure DNA: A260/A280 ~1.8, A260/A230 >2.0
                rawA260 = 0.5f + random.nextFloat() * 2.0f;  // Variable concentration
                rawA280 = rawA260 / (1.7f + random.nextFloat() * 0.2f);
                rawA230 = rawA260 / (1.8f + random.nextFloat() * 0.5f);
            }
            case "RNA" -> {
                // Pure RNA: A260/A280 ~2.0, A260/A230 >2.0
                rawA260 = 0.3f + random.nextFloat() * 1.5f;
                rawA280 = rawA260 / (1.9f + random.nextFloat() * 0.2f);
                rawA230 = rawA260 / (2.0f + random.nextFloat() * 0.4f);
            }
            case "protein" -> {
                // Protein: Higher A280
                rawA280 = 0.5f + random.nextFloat() * 1.5f;
                rawA260 = rawA280 * (0.5f + random.nextFloat() * 0.3f);
                rawA230 = rawA280 * (0.8f + random.nextFloat() * 0.4f);
            }
            default -> {
                // Unknown/mixed sample
                rawA260 = 0.2f + random.nextFloat() * 1.0f;
                rawA280 = 0.2f + random.nextFloat() * 1.0f;
                rawA230 = 0.3f + random.nextFloat() * 1.2f;
            }
        }
        
        // Subtract blank
        absorbance260 = Math.max(0, rawA260 - blankA260);
        absorbance280 = Math.max(0, rawA280 - blankA280);
        absorbance230 = Math.max(0, rawA230 - blankA230);
        
        // Calculate ratios
        purityRatio260_280 = absorbance280 > 0.001f ? absorbance260 / absorbance280 : 0;
        purityRatio260_230 = absorbance230 > 0.001f ? absorbance260 / absorbance230 : 0;
        
        // Calculate concentration
        concentration = calculateConcentration(absorbance260, sampleType);
        
        // Generate full spectrum for higher tiers
        if (tier != EquipmentTier.BASIC && spectrumData != null) {
            generateSpectrum(rawA260, rawA280, rawA230, random);
        }
        
        // Add precision noise based on tier
        float noise = spec != null ? spec.precision() : 0.1f;
        absorbance260 += (random.nextFloat() - 0.5f) * noise;
        absorbance280 += (random.nextFloat() - 0.5f) * noise;
        
        hasResults = true;
        markDirty();
    }

    private String determineSampleType(ItemStack sample) {
        if (sample.isOf(ModItems.DNA_SAMPLE) || sample.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT)) {
            return "dsDNA";
        }
        // Check NBT for specific type
        NbtCompound nbt = sample.getNbt();
        if (nbt != null && nbt.contains("SampleType")) {
            return nbt.getString("SampleType");
        }
        return "unknown";
    }

    private float calculateConcentration(float a260, String type) {
        // Beer-Lambert law coefficients
        return switch (type) {
            case "dsDNA" -> a260 * 50.0f;     // ng/μL
            case "ssDNA" -> a260 * 33.0f;
            case "RNA" -> a260 * 40.0f;
            case "protein" -> absorbance280 * 1000.0f;  // Rough estimate
            default -> a260 * 50.0f;
        };
    }

    private void generateSpectrum(float a260, float a280, float a230, Random random) {
        // Generate realistic absorption spectrum
        for (int i = 0; i < spectrumData.length; i++) {
            int wl = spectrumMinWl + i;
            float absorbance;
            
            // Nucleic acids peak at ~260nm
            if (wl >= 250 && wl <= 270) {
                float distFrom260 = Math.abs(wl - 260);
                absorbance = a260 * (float) Math.exp(-distFrom260 * distFrom260 / 200.0);
            }
            // Proteins peak at ~280nm
            else if (wl >= 270 && wl <= 290) {
                float distFrom280 = Math.abs(wl - 280);
                absorbance = a280 * (float) Math.exp(-distFrom280 * distFrom280 / 150.0);
            }
            // Contamination at ~230nm
            else if (wl >= 220 && wl <= 240) {
                float distFrom230 = Math.abs(wl - 230);
                absorbance = a230 * (float) Math.exp(-distFrom230 * distFrom230 / 100.0);
            }
            else {
                // Background
                absorbance = 0.02f + random.nextFloat() * 0.02f;
            }
            
            spectrumData[i] = Math.max(0, absorbance);
        }
    }

    /**
     * Get quality assessment based on ratios.
     */
    public String getQualityAssessment() {
        if (!hasResults) return "No measurement";
        
        StringBuilder sb = new StringBuilder();
        
        // A260/A280 assessment
        if (sampleType.equals("dsDNA")) {
            if (purityRatio260_280 >= 1.7 && purityRatio260_280 <= 1.9) {
                sb.append("DNA purity: Good");
            } else if (purityRatio260_280 < 1.7) {
                sb.append("DNA purity: Protein contamination likely");
            } else {
                sb.append("DNA purity: RNA contamination possible");
            }
        } else if (sampleType.equals("RNA")) {
            if (purityRatio260_280 >= 1.9 && purityRatio260_280 <= 2.1) {
                sb.append("RNA purity: Good");
            } else {
                sb.append("RNA purity: Check for contamination");
            }
        }
        
        // A260/A230 assessment
        sb.append("\n");
        if (purityRatio260_230 >= 2.0) {
            sb.append("Organic contamination: Low");
        } else if (purityRatio260_230 >= 1.5) {
            sb.append("Organic contamination: Moderate");
        } else {
            sb.append("Organic contamination: High (phenol/guanidine)");
        }
        
        return sb.toString();
    }

    private boolean isValidSample(ItemStack stack) {
        return stack.isOf(ModItems.DNA_SAMPLE) ||
               stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT) ||
               stack.isOf(ModItems.GENOME_SAMPLE);
    }

    public void clearResults() {
        hasResults = false;
        absorbance260 = 0;
        absorbance280 = 0;
        absorbance230 = 0;
        concentration = 0;
        purityRatio260_280 = 0;
        purityRatio260_230 = 0;
        markDirty();
    }

    // ========== Getters ==========

    public boolean isBlanked() { return blanked; }
    public boolean hasResults() { return hasResults; }
    public float getAbsorbance260() { return absorbance260; }
    public float getAbsorbance280() { return absorbance280; }
    public float getAbsorbance230() { return absorbance230; }
    public float getConcentration() { return concentration; }
    public float getPurityRatio260_280() { return purityRatio260_280; }
    public float getPurityRatio260_230() { return purityRatio260_230; }
    public String getSampleType() { return sampleType; }
    public float[] getSpectrumData() { return spectrumData; }
    public int getSpectrumMinWl() { return spectrumMinWl; }
    public int getSpectrumMaxWl() { return spectrumMaxWl; }

    // ========== NBT ==========

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("Blanked", blanked);
        nbt.putFloat("BlankA260", blankA260);
        nbt.putFloat("BlankA280", blankA280);
        nbt.putFloat("BlankA230", blankA230);
        nbt.putBoolean("HasResults", hasResults);
        nbt.putFloat("A260", absorbance260);
        nbt.putFloat("A280", absorbance280);
        nbt.putFloat("A230", absorbance230);
        nbt.putFloat("Concentration", concentration);
        nbt.putFloat("Ratio260_280", purityRatio260_280);
        nbt.putFloat("Ratio260_230", purityRatio260_230);
        nbt.putString("SampleType", sampleType);

        if (spectrumData != null) {
            int[] intSpectrum = new int[spectrumData.length];
            for (int i = 0; i < spectrumData.length; i++) {
                intSpectrum[i] = Float.floatToIntBits(spectrumData[i]);
            }
            nbt.putIntArray("Spectrum", intSpectrum);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        blanked = nbt.getBoolean("Blanked");
        blankA260 = nbt.getFloat("BlankA260");
        blankA280 = nbt.getFloat("BlankA280");
        blankA230 = nbt.getFloat("BlankA230");
        hasResults = nbt.getBoolean("HasResults");
        absorbance260 = nbt.getFloat("A260");
        absorbance280 = nbt.getFloat("A280");
        absorbance230 = nbt.getFloat("A230");
        concentration = nbt.getFloat("Concentration");
        purityRatio260_280 = nbt.getFloat("Ratio260_280");
        purityRatio260_230 = nbt.getFloat("Ratio260_230");
        sampleType = nbt.getString("SampleType");

        if (nbt.contains("Spectrum")) {
            int[] intSpectrum = nbt.getIntArray("Spectrum");
            spectrumData = new float[intSpectrum.length];
            for (int i = 0; i < intSpectrum.length; i++) {
                spectrumData[i] = Float.intBitsToFloat(intSpectrum[i]);
            }
        }

        updateSpec();
    }

    @Override
    protected boolean canProcess(int ignored) { return canProcess(); }
    @Override
    protected void completeProcess() { /* Non-destructive */ }

    // ========== Screen ==========

    @Override
    public Text getDisplayName() {
        String key = switch (tier) {
            case BASIC -> "block.morerealisticgeneediting.visible_spectrophotometer";
            case ADVANCED -> "block.morerealisticgeneediting.nanodrop";
            case ELITE -> "block.morerealisticgeneediting.ht_spectrophotometer";
        };
        return Text.translatable(key);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return null;  // TODO: SpectrophotometerScreenHandler
    }
}
