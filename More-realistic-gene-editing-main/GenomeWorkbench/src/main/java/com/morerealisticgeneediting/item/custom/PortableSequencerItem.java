package com.morerealisticgeneediting.item.custom;

import com.morerealisticgeneediting.item.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Portable Nanopore Sequencer - Based on Oxford Nanopore MinION Mk1C
 * 
 * Real device specifications:
 * - Real-time sequencing
 * - Up to 2Mb+ read lengths (ultra-long reads)
 * - ~50 Gb throughput per flow cell
 * - Direct RNA/DNA sequencing
 * - USB-C powered
 * - Portable (handheld)
 * 
 * Game mechanics:
 * - Use on a DNA sample to sequence it
 * - Returns sequence data with quality scores
 * - Flow cell has limited lifespan (uses)
 * - Higher quality = more accurate gene identification
 */
public class PortableSequencerItem extends Item {

    // Flow cell properties
    private static final int MAX_FLOW_CELL_USES = 50;
    private static final float BASE_ACCURACY = 0.90f;  // 90% base accuracy
    private static final int MAX_READ_LENGTH = 50000;   // Simplified for gameplay

    public PortableSequencerItem(Settings settings) {
        super(settings.maxDamage(MAX_FLOW_CELL_USES));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack sequencer = player.getStackInHand(hand);
        
        // Check if player has a valid sample in off-hand
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack sample = player.getStackInHand(otherHand);
        
        if (sample.isEmpty() || !isValidSample(sample)) {
            if (!world.isClient) {
                player.sendMessage(Text.translatable("message.morerealisticgeneediting.no_sample_for_sequencing")
                    .formatted(Formatting.RED), true);
            }
            return TypedActionResult.fail(sequencer);
        }

        if (!world.isClient) {
            // Perform sequencing
            SequencingResult result = performSequencing(sample, sequencer);
            
            // Create result item
            ItemStack resultItem = createSequencingResult(result);
            
            // Give to player or drop
            if (!player.getInventory().insertStack(resultItem)) {
                player.dropItem(resultItem, false);
            }
            
            // Consume sample
            sample.decrement(1);
            
            // Damage flow cell
            sequencer.damage(1, player, p -> p.sendToolBreakStatus(hand));
            
            // Send result message
            player.sendMessage(Text.translatable("message.morerealisticgeneediting.sequencing_complete",
                result.readCount, String.format("%.1f", result.meanQuality))
                .formatted(Formatting.GREEN), true);
        }

        return TypedActionResult.success(sequencer, world.isClient);
    }

    private SequencingResult performSequencing(ItemStack sample, ItemStack sequencer) {
        Random random = new Random();
        NbtCompound sampleNbt = sample.getNbt();
        
        // Determine sequence characteristics
        int sampleLength = 1000;  // Default
        String sampleType = "unknown";
        
        if (sampleNbt != null) {
            if (sampleNbt.contains("SequenceLength")) {
                sampleLength = sampleNbt.getInt("SequenceLength");
            }
            if (sampleNbt.contains("Type")) {
                sampleType = sampleNbt.getString("Type");
            }
        }

        // Simulate nanopore sequencing
        SequencingResult result = new SequencingResult();
        result.technology = "nanopore";
        result.sampleType = sampleType;
        
        // Generate reads (nanopore produces long reads)
        int numReads = 10 + random.nextInt(90);  // 10-100 reads
        result.readCount = numReads;
        
        // Calculate coverage
        int totalBases = 0;
        result.readLengths = new int[numReads];
        result.qualityScores = new float[numReads];
        
        for (int i = 0; i < numReads; i++) {
            // Nanopore read length distribution (log-normal)
            double logLength = 7 + random.nextGaussian() * 1.5;  // Mean ~1000bp, high variance
            int readLength = (int) Math.min(MAX_READ_LENGTH, Math.exp(logLength));
            result.readLengths[i] = readLength;
            totalBases += readLength;
            
            // Quality score (Phred-like, nanopore typically Q10-Q20)
            result.qualityScores[i] = 10 + random.nextFloat() * 15;
        }
        
        // Calculate statistics
        result.totalBases = totalBases;
        result.coverage = (float) totalBases / sampleLength;
        result.meanReadLength = totalBases / numReads;
        
        // Calculate mean quality
        float totalQ = 0;
        for (float q : result.qualityScores) totalQ += q;
        result.meanQuality = totalQ / numReads;
        
        // N50 calculation (simplified)
        java.util.Arrays.sort(result.readLengths);
        int halfBases = totalBases / 2;
        int cumulative = 0;
        for (int i = result.readLengths.length - 1; i >= 0; i--) {
            cumulative += result.readLengths[i];
            if (cumulative >= halfBases) {
                result.n50 = result.readLengths[i];
                break;
            }
        }
        
        // Generate consensus sequence (simplified)
        if (result.coverage >= 10) {
            result.consensusAccuracy = BASE_ACCURACY + (1 - BASE_ACCURACY) * (1 - 1.0f / result.coverage);
        } else {
            result.consensusAccuracy = BASE_ACCURACY * (result.coverage / 10);
        }
        
        // Identify features (genes, variants) based on coverage and quality
        result.featuresIdentified = identifyFeatures(result, random);
        
        return result;
    }

    private List<String> identifyFeatures(SequencingResult result, Random random) {
        List<String> features = new ArrayList<>();
        
        if (result.coverage >= 5) {
            features.add("Full sequence assembled");
        }
        if (result.coverage >= 20 && result.consensusAccuracy > 0.95) {
            features.add("High-confidence variant calling");
        }
        if (result.meanReadLength > 5000) {
            features.add("Structural variants detectable");
        }
        if (result.n50 > 10000) {
            features.add("Complete gene sequences");
        }
        
        // Random gene discovery based on quality
        if (random.nextFloat() < result.consensusAccuracy * 0.5f) {
            String[] genes = {"GFP", "lacZ", "ampR", "kanR", "ori", "Cas9", "sgRNA_scaffold"};
            features.add("Detected: " + genes[random.nextInt(genes.length)]);
        }
        
        return features;
    }

    private ItemStack createSequencingResult(SequencingResult result) {
        ItemStack output = new ItemStack(ModItems.DNA_SAMPLE);  // Or a dedicated sequencing result item
        
        NbtCompound nbt = output.getOrCreateNbt();
        nbt.putString("Type", "sequencing_result");
        nbt.putString("Technology", result.technology);
        nbt.putInt("ReadCount", result.readCount);
        nbt.putInt("TotalBases", result.totalBases);
        nbt.putFloat("Coverage", result.coverage);
        nbt.putInt("MeanReadLength", result.meanReadLength);
        nbt.putFloat("MeanQuality", result.meanQuality);
        nbt.putInt("N50", result.n50);
        nbt.putFloat("ConsensusAccuracy", result.consensusAccuracy);
        
        // Store features as string list
        StringBuilder featuresStr = new StringBuilder();
        for (String feature : result.featuresIdentified) {
            if (featuresStr.length() > 0) featuresStr.append(";");
            featuresStr.append(feature);
        }
        nbt.putString("Features", featuresStr.toString());
        
        return output;
    }

    private boolean isValidSample(ItemStack stack) {
        return stack.isOf(ModItems.DNA_SAMPLE) ||
               stack.isOf(ModItems.GENOME_SAMPLE) ||
               stack.isOf(ModItems.AMPLIFIED_GENE_FRAGMENT);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        int usesRemaining = stack.getMaxDamage() - stack.getDamage();
        
        tooltip.add(Text.translatable("tooltip.morerealisticgeneediting.minion.line1")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.morerealisticgeneediting.minion.line2")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.morerealisticgeneediting.flow_cell_uses", usesRemaining, MAX_FLOW_CELL_USES)
            .formatted(usesRemaining > 10 ? Formatting.GREEN : Formatting.RED));
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.morerealisticgeneediting.minion.usage")
            .formatted(Formatting.YELLOW));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0F - (float)stack.getDamage() * 13.0F / (float)stack.getMaxDamage());
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = (float)(stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
        if (ratio > 0.5f) return 0x00FF00;  // Green
        if (ratio > 0.25f) return 0xFFFF00; // Yellow
        return 0xFF0000;  // Red
    }

    // ========== Inner Classes ==========

    private static class SequencingResult {
        String technology;
        String sampleType;
        int readCount;
        int totalBases;
        float coverage;
        int meanReadLength;
        float meanQuality;
        int n50;
        float consensusAccuracy;
        int[] readLengths;
        float[] qualityScores;
        List<String> featuresIdentified = new ArrayList<>();
    }
}
