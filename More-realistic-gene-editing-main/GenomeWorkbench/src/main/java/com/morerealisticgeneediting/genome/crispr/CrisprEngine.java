package com.morerealisticgeneediting.genome.crispr;

import com.morerealisticgeneediting.data.EditOutcome;
import com.morerealisticgeneediting.genome.Genome;

import java.util.*;

/**
 * CrisprEngine - Simulates CRISPR-Cas9 gene editing with realistic outcomes.
 * 
 * This engine models the key aspects of CRISPR-Cas9 editing:
 * - PAM site recognition (NGG for SpCas9)
 * - Guide RNA targeting
 * - Double-strand break (DSB) induction
 * - DNA repair pathways (NHEJ vs HDR)
 * - Indel formation with realistic distributions
 * - Off-target effects based on sequence similarity
 * 
 * Educational note: Real CRISPR editing has ~70-90% editing efficiency
 * with most outcomes being small indels from NHEJ repair.
 */
public class CrisprEngine {
    
    // PAM sequences for different Cas variants
    public static final String SPCAS9_PAM = "NGG";      // Streptococcus pyogenes Cas9
    public static final String SACAS9_PAM = "NNGRRT";   // Staphylococcus aureus Cas9
    public static final String CPF1_PAM = "TTTN";       // Cas12a (Cpf1)
    
    // Protospacer (guide RNA target) length
    public static final int PROTOSPACER_LENGTH = 20;
    
    // Editing outcome probabilities (based on literature)
    private static final double NHEJ_PROBABILITY = 0.85;  // Non-homologous end joining
    private static final double HDR_PROBABILITY = 0.10;   // Homology-directed repair (with template)
    private static final double NO_EDIT_PROBABILITY = 0.05;
    
    // Indel size distribution parameters
    private static final int MAX_DELETION_SIZE = 30;
    private static final int MAX_INSERTION_SIZE = 10;
    
    private final Random random;
    private final long seed;
    
    public CrisprEngine(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Represents a potential CRISPR target site.
     */
    public record TargetSite(
        long position,          // Position of PAM site
        String protospacer,     // 20bp target sequence
        String pamSequence,     // PAM sequence (e.g., "NGG")
        double onTargetScore,   // Predicted on-target efficiency (0-1)
        double offTargetRisk    // Risk of off-target effects (0-1)
    ) {}
    
    /**
     * Represents the result of a CRISPR editing attempt.
     */
    public record EditingResult(
        EditOutcome primaryOutcome,
        List<EditOutcome> byproducts,
        double editingEfficiency,
        boolean hasOffTargetEffects,
        String repairPathway,       // "NHEJ", "HDR", or "NONE"
        QualityMetrics qualityMetrics
    ) {}
    
    /**
     * Quality control metrics for the editing result.
     */
    public record QualityMetrics(
        double indelFrequency,
        double frameshiftProbability,
        int averageIndelSize,
        double mosaicismLevel,
        Map<String, Double> outcomeDistribution
    ) {}
    
    /**
     * Find all valid PAM sites in a genome region.
     * 
     * @param genome The target genome
     * @param start Start position for search
     * @param length Length of region to search
     * @param pamSequence PAM motif to search for (e.g., "NGG")
     * @return List of valid target sites
     */
    public List<TargetSite> findPamSites(Genome genome, long start, int length, String pamSequence) {
        List<TargetSite> sites = new ArrayList<>();
        String sequence = genome.getSequence(start, length);
        
        // Search for PAM sites
        for (int i = PROTOSPACER_LENGTH; i < sequence.length() - pamSequence.length(); i++) {
            String potentialPam = sequence.substring(i, i + pamSequence.length());
            
            if (matchesPam(potentialPam, pamSequence)) {
                // Get protospacer (20bp upstream of PAM)
                String protospacer = sequence.substring(i - PROTOSPACER_LENGTH, i);
                
                // Calculate scores
                double onTargetScore = calculateOnTargetScore(protospacer, potentialPam);
                double offTargetRisk = calculateOffTargetRisk(protospacer);
                
                sites.add(new TargetSite(
                    start + i,
                    protospacer,
                    potentialPam,
                    onTargetScore,
                    offTargetRisk
                ));
            }
        }
        
        return sites;
    }
    
    /**
     * Perform CRISPR editing at a specified target site.
     * 
     * @param genome The genome to edit
     * @param targetSite The target site for editing
     * @param hdrTemplate Optional HDR template for precise editing (null for knockout)
     * @return EditingResult with outcomes and statistics
     */
    public EditingResult performEditing(Genome genome, TargetSite targetSite, String hdrTemplate) {
        List<EditOutcome> allOutcomes = new ArrayList<>();
        Map<String, Double> outcomeDistribution = new HashMap<>();
        
        // Determine repair pathway and outcome
        double roll = random.nextDouble();
        String repairPathway;
        EditOutcome primaryOutcome;
        
        if (roll < NO_EDIT_PROBABILITY) {
            // No editing occurred
            repairPathway = "NONE";
            primaryOutcome = new EditOutcome(
                EditOutcome.Type.NO_CHANGE,
                targetSite.position(),
                0,
                "",
                "No double-strand break induced"
            );
            outcomeDistribution.put("no_edit", 1.0);
            
        } else if (hdrTemplate != null && roll < NO_EDIT_PROBABILITY + HDR_PROBABILITY) {
            // HDR - precise editing with template
            repairPathway = "HDR";
            primaryOutcome = performHdrRepair(targetSite, hdrTemplate);
            outcomeDistribution.put("hdr_success", 0.10);
            outcomeDistribution.put("partial_hdr", 0.05);
            outcomeDistribution.put("nhej_background", 0.85);
            
        } else {
            // NHEJ - error-prone repair leading to indels
            repairPathway = "NHEJ";
            primaryOutcome = performNhejRepair(targetSite);
            
            // Generate outcome distribution (typical for NHEJ)
            outcomeDistribution.put("deletion_1bp", 0.25);
            outcomeDistribution.put("deletion_2-5bp", 0.30);
            outcomeDistribution.put("deletion_6-20bp", 0.20);
            outcomeDistribution.put("insertion_1bp", 0.15);
            outcomeDistribution.put("insertion_2-5bp", 0.05);
            outcomeDistribution.put("complex_indel", 0.05);
        }
        
        allOutcomes.add(primaryOutcome);
        
        // Generate byproducts (mosaic outcomes in a cell population)
        List<EditOutcome> byproducts = generateByproducts(targetSite, 5);
        
        // Check for off-target effects
        boolean hasOffTargetEffects = random.nextDouble() < targetSite.offTargetRisk();
        
        // Calculate quality metrics
        QualityMetrics metrics = calculateQualityMetrics(
            primaryOutcome, byproducts, outcomeDistribution
        );
        
        return new EditingResult(
            primaryOutcome,
            byproducts,
            targetSite.onTargetScore(),
            hasOffTargetEffects,
            repairPathway,
            metrics
        );
    }
    
    /**
     * Simulate NHEJ repair - produces insertions or deletions.
     */
    private EditOutcome performNhejRepair(TargetSite targetSite) {
        // NHEJ typically cuts 3bp upstream of PAM
        long cutSite = targetSite.position() - 3;
        
        // Determine if deletion or insertion
        boolean isDeletion = random.nextDouble() < 0.75; // Deletions are more common
        
        if (isDeletion) {
            // Generate deletion with realistic size distribution
            int deletionSize = generateIndelSize(true);
            return new EditOutcome(
                EditOutcome.Type.DELETION,
                cutSite,
                deletionSize,
                "",
                "NHEJ-mediated deletion of " + deletionSize + "bp"
            );
        } else {
            // Generate insertion
            int insertionSize = generateIndelSize(false);
            String insertedSequence = generateRandomSequence(insertionSize);
            return new EditOutcome(
                EditOutcome.Type.INSERTION,
                cutSite,
                insertionSize,
                insertedSequence,
                "NHEJ-mediated insertion of " + insertionSize + "bp"
            );
        }
    }
    
    /**
     * Simulate HDR repair - produces precise edits using template.
     */
    private EditOutcome performHdrRepair(TargetSite targetSite, String hdrTemplate) {
        long cutSite = targetSite.position() - 3;
        
        return new EditOutcome(
            EditOutcome.Type.REPLACEMENT,
            cutSite,
            hdrTemplate.length(),
            hdrTemplate,
            "HDR-mediated precise integration"
        );
    }
    
    /**
     * Generate byproduct outcomes representing population heterogeneity.
     */
    private List<EditOutcome> generateByproducts(TargetSite targetSite, int count) {
        List<EditOutcome> byproducts = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            EditOutcome byproduct = performNhejRepair(targetSite);
            byproducts.add(byproduct);
        }
        
        return byproducts;
    }
    
    /**
     * Generate indel size following realistic distribution.
     * Based on empirical CRISPR data - most indels are small (1-5bp).
     */
    private int generateIndelSize(boolean isDeletion) {
        double roll = random.nextDouble();
        int maxSize = isDeletion ? MAX_DELETION_SIZE : MAX_INSERTION_SIZE;
        
        if (roll < 0.4) {
            return 1; // 40% are 1bp
        } else if (roll < 0.7) {
            return random.nextInt(4) + 2; // 30% are 2-5bp
        } else if (roll < 0.9) {
            return random.nextInt(10) + 6; // 20% are 6-15bp
        } else {
            return random.nextInt(maxSize - 15) + 16; // 10% are larger
        }
    }
    
    /**
     * Check if a sequence matches a PAM pattern.
     * Supports IUPAC ambiguity codes: N=any, R=A/G, Y=C/T, etc.
     */
    private boolean matchesPam(String sequence, String pattern) {
        if (sequence.length() != pattern.length()) return false;
        
        for (int i = 0; i < pattern.length(); i++) {
            char patternChar = pattern.charAt(i);
            char seqChar = Character.toUpperCase(sequence.charAt(i));
            
            if (!matchesIupac(seqChar, patternChar)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Match a base against an IUPAC ambiguity code.
     */
    private boolean matchesIupac(char base, char code) {
        return switch (code) {
            case 'N' -> true;
            case 'R' -> base == 'A' || base == 'G';
            case 'Y' -> base == 'C' || base == 'T';
            case 'S' -> base == 'G' || base == 'C';
            case 'W' -> base == 'A' || base == 'T';
            case 'K' -> base == 'G' || base == 'T';
            case 'M' -> base == 'A' || base == 'C';
            default -> base == code;
        };
    }
    
    /**
     * Calculate on-target efficiency score based on sequence features.
     * Simplified model based on Doench et al. scoring.
     */
    private double calculateOnTargetScore(String protospacer, String pam) {
        double score = 0.5; // Base score
        
        // G at position -1 (adjacent to PAM) increases efficiency
        if (protospacer.charAt(protospacer.length() - 1) == 'G') {
            score += 0.1;
        }
        
        // High GC content in seed region (positions 1-12) improves binding
        String seedRegion = protospacer.substring(0, 12);
        double gcContent = calculateGcContent(seedRegion);
        if (gcContent >= 0.4 && gcContent <= 0.7) {
            score += 0.15;
        }
        
        // Avoid poly-T runs (can terminate transcription)
        if (!protospacer.contains("TTTT")) {
            score += 0.1;
        }
        
        // Perfect PAM match bonus
        if (pam.equals("AGG") || pam.equals("TGG")) {
            score += 0.05;
        }
        
        return Math.min(1.0, Math.max(0.0, score + (random.nextDouble() * 0.1)));
    }
    
    /**
     * Calculate off-target risk based on sequence features.
     */
    private double calculateOffTargetRisk(String protospacer) {
        double risk = 0.1; // Base risk
        
        // High similarity sequences increase off-target risk
        // (In real implementations, this would search against a reference genome)
        
        // Low complexity sequences have higher off-target risk
        if (hasLowComplexity(protospacer)) {
            risk += 0.2;
        }
        
        // Sequences with common motifs have higher risk
        String[] commonMotifs = {"AAAA", "TTTT", "GGGG", "CCCC", "ATAT", "GCGC"};
        for (String motif : commonMotifs) {
            if (protospacer.contains(motif)) {
                risk += 0.05;
            }
        }
        
        return Math.min(1.0, risk);
    }
    
    /**
     * Calculate GC content of a sequence.
     */
    private double calculateGcContent(String sequence) {
        long gcCount = sequence.chars()
            .filter(c -> c == 'G' || c == 'C' || c == 'g' || c == 'c')
            .count();
        return (double) gcCount / sequence.length();
    }
    
    /**
     * Check if sequence has low complexity (repetitive).
     */
    private boolean hasLowComplexity(String sequence) {
        Set<String> dimers = new HashSet<>();
        for (int i = 0; i < sequence.length() - 1; i++) {
            dimers.add(sequence.substring(i, i + 2));
        }
        // If fewer than 8 unique dimers in a 20bp sequence, it's low complexity
        return dimers.size() < 8;
    }
    
    /**
     * Generate a random DNA sequence.
     */
    private String generateRandomSequence(int length) {
        char[] bases = {'A', 'C', 'G', 'T'};
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(bases[random.nextInt(4)]);
        }
        return sb.toString();
    }
    
    /**
     * Calculate quality metrics for the editing result.
     */
    private QualityMetrics calculateQualityMetrics(
            EditOutcome primary,
            List<EditOutcome> byproducts,
            Map<String, Double> distribution) {
        
        // Calculate average indel size
        int totalSize = primary.size();
        for (EditOutcome bp : byproducts) {
            totalSize += bp.size();
        }
        int avgSize = totalSize / (byproducts.size() + 1);
        
        // Calculate frameshift probability (non-3n indels cause frameshifts)
        long frameshiftCount = byproducts.stream()
            .filter(o -> o.size() % 3 != 0)
            .count();
        if (primary.size() % 3 != 0) frameshiftCount++;
        double frameshiftProb = (double) frameshiftCount / (byproducts.size() + 1);
        
        // Indel frequency
        double indelFreq = byproducts.isEmpty() ? 1.0 : 0.85;
        
        // Mosaicism level (heterogeneity of outcomes)
        Set<Integer> uniqueSizes = new HashSet<>();
        uniqueSizes.add(primary.size());
        byproducts.forEach(bp -> uniqueSizes.add(bp.size()));
        double mosaicism = (double) uniqueSizes.size() / (byproducts.size() + 1);
        
        return new QualityMetrics(
            indelFreq,
            frameshiftProb,
            avgSize,
            mosaicism,
            distribution
        );
    }
}
