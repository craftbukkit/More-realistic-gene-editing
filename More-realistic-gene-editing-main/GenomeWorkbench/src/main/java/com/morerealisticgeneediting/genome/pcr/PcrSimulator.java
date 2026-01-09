package com.morerealisticgeneediting.genome.pcr;

import com.morerealisticgeneediting.genome.Genome;

import java.util.*;

/**
 * PcrSimulator - Simulates Polymerase Chain Reaction with realistic outcomes.
 * 
 * This simulator models key aspects of real PCR:
 * - Primer binding and specificity
 * - Amplification efficiency
 * - Error rates (Taq polymerase ~1 error per 10,000 bases)
 * - Cycle-dependent exponential amplification
 * - Non-specific amplification and primer dimers
 * 
 * Educational note: Real PCR efficiency is typically 90-100% per cycle,
 * with amplification following 2^n kinetics where n is the number of cycles.
 */
public class PcrSimulator {
    
    // Default PCR parameters
    public static final int DEFAULT_CYCLES = 30;
    public static final double TAQ_ERROR_RATE = 0.0001;  // 1 in 10,000
    public static final double HF_POLYMERASE_ERROR_RATE = 0.00001;  // High fidelity
    public static final int MIN_PRIMER_LENGTH = 18;
    public static final int MAX_PRIMER_LENGTH = 30;
    public static final int OPTIMAL_PRIMER_LENGTH = 20;
    
    // Melting temperature constants (simplified)
    private static final double TM_BASE = 64.9;
    private static final double TM_GC_CONTRIB = 0.41;
    private static final double TM_LENGTH_PENALTY = 600.0;
    
    private final Random random;
    private final long seed;
    
    public PcrSimulator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Represents a PCR primer with its properties.
     */
    public record Primer(
        String sequence,
        boolean isForward,
        double meltingTemperature,
        double gcContent,
        double specificity,
        boolean hasSelfComplementarity,
        boolean hasPrimerDimerRisk
    ) {
        public static Primer design(String sequence, boolean isForward) {
            double tm = calculateMeltingTemperature(sequence);
            double gc = calculateGcContent(sequence);
            double specificity = calculateSpecificity(sequence);
            boolean selfComp = checkSelfComplementarity(sequence);
            boolean dimerRisk = checkPrimerDimerRisk(sequence);
            
            return new Primer(sequence, isForward, tm, gc, specificity, selfComp, dimerRisk);
        }
        
        private static double calculateMeltingTemperature(String seq) {
            // Simplified Tm calculation (Wallace rule + salt adjustment)
            int gc = 0, at = 0;
            for (char c : seq.toUpperCase().toCharArray()) {
                if (c == 'G' || c == 'C') gc++;
                else if (c == 'A' || c == 'T') at++;
            }
            // Basic formula: Tm = 4(G+C) + 2(A+T) for short oligos
            // More accurate: Tm = 64.9 + 41*(G+C-16.4)/N
            return TM_BASE + TM_GC_CONTRIB * (gc - 16.4) * 100 / seq.length() - TM_LENGTH_PENALTY / seq.length();
        }
        
        private static double calculateGcContent(String seq) {
            long gc = seq.toUpperCase().chars()
                .filter(c -> c == 'G' || c == 'C')
                .count();
            return (double) gc / seq.length();
        }
        
        private static double calculateSpecificity(String seq) {
            // Simplified specificity score based on sequence uniqueness
            double score = 0.8;
            
            // Penalize repetitive sequences
            if (hasRepeats(seq)) score -= 0.2;
            
            // Penalize low complexity
            Set<Character> uniqueBases = new HashSet<>();
            for (char c : seq.toCharArray()) uniqueBases.add(c);
            if (uniqueBases.size() < 4) score -= 0.1;
            
            return Math.max(0, Math.min(1, score));
        }
        
        private static boolean hasRepeats(String seq) {
            for (int len = 3; len <= seq.length() / 2; len++) {
                String pattern = seq.substring(0, len);
                if (seq.indexOf(pattern, len) != -1) return true;
            }
            return false;
        }
        
        private static boolean checkSelfComplementarity(String seq) {
            String revComp = reverseComplement(seq);
            // Check for significant self-annealing (3' end is critical)
            String threeEnd = seq.substring(seq.length() - 6);
            return revComp.contains(threeEnd);
        }
        
        private static boolean checkPrimerDimerRisk(String seq) {
            // Check 3' end for complementarity
            String threeEnd = seq.substring(seq.length() - 4);
            String threeEndComp = reverseComplement(threeEnd);
            return seq.contains(threeEndComp);
        }
        
        private static String reverseComplement(String seq) {
            StringBuilder sb = new StringBuilder();
            for (int i = seq.length() - 1; i >= 0; i--) {
                char c = seq.charAt(i);
                sb.append(switch (Character.toUpperCase(c)) {
                    case 'A' -> 'T';
                    case 'T' -> 'A';
                    case 'G' -> 'C';
                    case 'C' -> 'G';
                    default -> 'N';
                });
            }
            return sb.toString();
        }
    }
    
    /**
     * PCR reaction parameters.
     */
    public record ReactionParameters(
        int cycles,
        double annealingTemp,
        double extensionTime,  // seconds per kb
        boolean useHighFidelityPolymerase,
        double mgConcentration,  // mM
        double dntpConcentration  // uM
    ) {
        public static ReactionParameters standard() {
            return new ReactionParameters(30, 55.0, 60.0, false, 1.5, 200.0);
        }
        
        public static ReactionParameters highFidelity() {
            return new ReactionParameters(30, 58.0, 30.0, true, 2.0, 200.0);
        }
    }
    
    /**
     * Result of a PCR reaction.
     */
    public record PcrResult(
        boolean success,
        String amplicon,
        int ampliconLength,
        double yield,           // Relative yield (0-1)
        int totalCopies,        // Estimated copy number
        double errorRate,
        List<String> mutations, // List of introduced mutations
        List<String> warnings,
        PcrQualityMetrics quality
    ) {}
    
    /**
     * Quality metrics for PCR result.
     */
    public record PcrQualityMetrics(
        double efficiency,      // Per-cycle efficiency
        double specificity,     // Single band vs multiple bands
        boolean hasPrimerDimers,
        boolean hasNonSpecificBands,
        double estimatedPurity
    ) {}
    
    /**
     * Design primers for a target region.
     * 
     * @param genome Target genome
     * @param targetStart Start position of region to amplify
     * @param targetEnd End position of region to amplify
     * @param primerLength Desired primer length
     * @return Pair of forward and reverse primers
     */
    public List<Primer> designPrimers(Genome genome, long targetStart, long targetEnd, int primerLength) {
        // Get flanking sequences for primer design
        int flankingSize = primerLength + 50;  // Extra for optimization
        
        String upstreamRegion = genome.getSequence(targetStart - flankingSize, flankingSize);
        String downstreamRegion = genome.getSequence(targetEnd, flankingSize);
        
        List<Primer> candidates = new ArrayList<>();
        
        // Design forward primers from upstream region
        for (int i = 0; i <= upstreamRegion.length() - primerLength; i++) {
            String seq = upstreamRegion.substring(i, i + primerLength);
            Primer primer = Primer.design(seq, true);
            if (isPrimerAcceptable(primer)) {
                candidates.add(primer);
            }
        }
        
        // Design reverse primers from downstream region (reverse complement)
        for (int i = 0; i <= downstreamRegion.length() - primerLength; i++) {
            String seq = reverseComplement(downstreamRegion.substring(i, i + primerLength));
            Primer primer = Primer.design(seq, false);
            if (isPrimerAcceptable(primer)) {
                candidates.add(primer);
            }
        }
        
        // Select best primer pair
        return selectBestPrimerPair(candidates);
    }
    
    /**
     * Run a PCR simulation.
     * 
     * @param genome Template genome
     * @param forwardPrimer Forward primer
     * @param reversePrimer Reverse primer
     * @param params Reaction parameters
     * @return PCR result
     */
    public PcrResult runPcr(Genome genome, Primer forwardPrimer, Primer reversePrimer, ReactionParameters params) {
        List<String> warnings = new ArrayList<>();
        
        // Validate primers
        if (!validatePrimerPair(forwardPrimer, reversePrimer, warnings)) {
            return new PcrResult(false, "", 0, 0, 0, 0, List.of(), warnings,
                new PcrQualityMetrics(0, 0, false, false, 0));
        }
        
        // Find primer binding sites
        long forwardSite = findPrimerBindingSite(genome, forwardPrimer, 0, genome.getTotalLength());
        if (forwardSite < 0) {
            warnings.add("Forward primer binding site not found");
            return failedResult(warnings);
        }
        
        long reverseSite = findPrimerBindingSite(genome, reversePrimer, forwardSite, genome.getTotalLength());
        if (reverseSite < 0) {
            warnings.add("Reverse primer binding site not found");
            return failedResult(warnings);
        }
        
        // Calculate amplicon length
        int ampliconLength = (int)(reverseSite - forwardSite + reversePrimer.sequence().length());
        if (ampliconLength > 10000) {
            warnings.add("Amplicon too long for standard PCR: " + ampliconLength + "bp");
        }
        
        // Get template sequence
        String template = genome.getSequence(forwardSite, ampliconLength);
        
        // Simulate amplification
        double efficiency = calculateEfficiency(forwardPrimer, reversePrimer, params);
        int copies = simulateAmplification(params.cycles(), efficiency);
        
        // Introduce PCR errors
        double errorRate = params.useHighFidelityPolymerase() ? 
            HF_POLYMERASE_ERROR_RATE : TAQ_ERROR_RATE;
        List<String> mutations = new ArrayList<>();
        String amplicon = introduceErrors(template, errorRate, params.cycles(), mutations);
        
        // Calculate quality metrics
        double specificity = calculateSpecificityScore(forwardPrimer, reversePrimer);
        boolean hasDimers = forwardPrimer.hasPrimerDimerRisk() || reversePrimer.hasPrimerDimerRisk();
        boolean hasNonSpecific = random.nextDouble() < (1 - specificity);
        
        PcrQualityMetrics quality = new PcrQualityMetrics(
            efficiency,
            specificity,
            hasDimers,
            hasNonSpecific,
            hasDimers || hasNonSpecific ? 0.7 : 0.95
        );
        
        // Check for warnings
        if (hasDimers) warnings.add("Primer dimers detected");
        if (hasNonSpecific) warnings.add("Non-specific amplification detected");
        if (!mutations.isEmpty()) warnings.add("PCR errors introduced: " + mutations.size());
        
        return new PcrResult(
            true,
            amplicon,
            ampliconLength,
            efficiency,
            copies,
            errorRate,
            mutations,
            warnings,
            quality
        );
    }
    
    /**
     * Find where a primer binds in the genome.
     */
    private long findPrimerBindingSite(Genome genome, Primer primer, long start, long end) {
        String searchSeq = primer.isForward() ? primer.sequence() : reverseComplement(primer.sequence());
        
        // Search in chunks to avoid loading entire genome
        int chunkSize = 10000;
        for (long pos = start; pos < end; pos += chunkSize - primer.sequence().length()) {
            int len = (int) Math.min(chunkSize, end - pos);
            String chunk = genome.getSequence(pos, len);
            
            int idx = chunk.indexOf(searchSeq);
            if (idx >= 0) {
                return pos + idx;
            }
            
            // Also check for mismatched binding (up to 2 mismatches at 5' end)
            idx = findWithMismatches(chunk, searchSeq, 2);
            if (idx >= 0) {
                return pos + idx;
            }
        }
        
        return -1;  // Not found
    }
    
    /**
     * Find sequence with allowed mismatches.
     */
    private int findWithMismatches(String text, String pattern, int maxMismatches) {
        for (int i = 0; i <= text.length() - pattern.length(); i++) {
            int mismatches = 0;
            for (int j = 0; j < pattern.length(); j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    mismatches++;
                    if (mismatches > maxMismatches) break;
                }
            }
            if (mismatches <= maxMismatches) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Calculate PCR efficiency based on primer properties.
     */
    private double calculateEfficiency(Primer forward, Primer reverse, ReactionParameters params) {
        double baseEfficiency = 0.95;  // Ideal efficiency
        
        // Tm difference penalty
        double tmDiff = Math.abs(forward.meltingTemperature() - reverse.meltingTemperature());
        if (tmDiff > 5) baseEfficiency -= 0.1;
        
        // Annealing temperature optimization
        double optimalAnnealing = (forward.meltingTemperature() + reverse.meltingTemperature()) / 2 - 5;
        double annealDiff = Math.abs(params.annealingTemp() - optimalAnnealing);
        if (annealDiff > 5) baseEfficiency -= 0.15;
        
        // GC content penalty
        if (forward.gcContent() < 0.4 || forward.gcContent() > 0.6) baseEfficiency -= 0.05;
        if (reverse.gcContent() < 0.4 || reverse.gcContent() > 0.6) baseEfficiency -= 0.05;
        
        // Self-complementarity penalty
        if (forward.hasSelfComplementarity()) baseEfficiency -= 0.1;
        if (reverse.hasSelfComplementarity()) baseEfficiency -= 0.1;
        
        return Math.max(0.5, Math.min(1.0, baseEfficiency));
    }
    
    /**
     * Simulate exponential amplification.
     */
    private int simulateAmplification(int cycles, double efficiency) {
        // PCR amplification: N = N0 * (1 + E)^n
        // Where E is efficiency (0-1) and n is number of cycles
        double copies = Math.pow(1 + efficiency, cycles);
        return (int) Math.min(copies, Integer.MAX_VALUE);
    }
    
    /**
     * Introduce polymerase errors into the amplicon.
     */
    private String introduceErrors(String template, double errorRate, int cycles, List<String> mutations) {
        char[] result = template.toCharArray();
        char[] bases = {'A', 'C', 'G', 'T'};
        
        // Error probability increases with cycles (errors accumulate)
        // Simplified model: total error rate = 1 - (1 - errorRate)^(cycles * length)
        for (int i = 0; i < result.length; i++) {
            if (random.nextDouble() < errorRate * cycles) {
                char original = result[i];
                char newBase;
                do {
                    newBase = bases[random.nextInt(4)];
                } while (newBase == original);
                
                result[i] = newBase;
                mutations.add(String.format("%d:%c>%c", i, original, newBase));
            }
        }
        
        return new String(result);
    }
    
    /**
     * Check if a primer meets basic quality criteria.
     */
    private boolean isPrimerAcceptable(Primer primer) {
        // Tm should be between 55-65°C
        if (primer.meltingTemperature() < 55 || primer.meltingTemperature() > 65) return false;
        
        // GC content should be 40-60%
        if (primer.gcContent() < 0.4 || primer.gcContent() > 0.6) return false;
        
        // Avoid severe self-complementarity
        if (primer.hasSelfComplementarity() && primer.specificity() < 0.5) return false;
        
        return true;
    }
    
    /**
     * Validate a primer pair for PCR.
     */
    private boolean validatePrimerPair(Primer forward, Primer reverse, List<String> warnings) {
        // Check Tm compatibility
        double tmDiff = Math.abs(forward.meltingTemperature() - reverse.meltingTemperature());
        if (tmDiff > 5) {
            warnings.add("Primer Tm difference too large: " + String.format("%.1f°C", tmDiff));
        }
        
        // Check for primer-primer complementarity
        if (arePrimersComplementary(forward.sequence(), reverse.sequence())) {
            warnings.add("Primers may form dimers with each other");
        }
        
        return warnings.stream().noneMatch(w -> w.contains("too large"));
    }
    
    /**
     * Check if two primers can form dimers.
     */
    private boolean arePrimersComplementary(String p1, String p2) {
        String p1end = p1.substring(p1.length() - 4);
        String p2end = p2.substring(p2.length() - 4);
        String p2endComp = reverseComplement(p2end);
        return p1.contains(p2endComp) || p1end.equals(p2endComp);
    }
    
    /**
     * Calculate combined specificity score.
     */
    private double calculateSpecificityScore(Primer forward, Primer reverse) {
        return (forward.specificity() + reverse.specificity()) / 2;
    }
    
    /**
     * Select the best primer pair from candidates.
     */
    private List<Primer> selectBestPrimerPair(List<Primer> candidates) {
        List<Primer> forward = candidates.stream().filter(Primer::isForward).toList();
        List<Primer> reverse = candidates.stream().filter(p -> !p.isForward()).toList();
        
        if (forward.isEmpty() || reverse.isEmpty()) {
            return List.of();
        }
        
        // Score each pair and select best
        Primer bestF = forward.get(0);
        Primer bestR = reverse.get(0);
        double bestScore = 0;
        
        for (Primer f : forward) {
            for (Primer r : reverse) {
                double score = scorePrimerPair(f, r);
                if (score > bestScore) {
                    bestScore = score;
                    bestF = f;
                    bestR = r;
                }
            }
        }
        
        return List.of(bestF, bestR);
    }
    
    /**
     * Score a primer pair.
     */
    private double scorePrimerPair(Primer forward, Primer reverse) {
        double score = 0;
        
        // Prefer similar Tm
        double tmDiff = Math.abs(forward.meltingTemperature() - reverse.meltingTemperature());
        score += Math.max(0, 10 - tmDiff);
        
        // Prefer optimal GC content
        score += (1 - Math.abs(forward.gcContent() - 0.5)) * 5;
        score += (1 - Math.abs(reverse.gcContent() - 0.5)) * 5;
        
        // Prefer high specificity
        score += forward.specificity() * 10;
        score += reverse.specificity() * 10;
        
        // Penalize self-complementarity
        if (forward.hasSelfComplementarity()) score -= 5;
        if (reverse.hasSelfComplementarity()) score -= 5;
        
        // Penalize primer dimer risk
        if (forward.hasPrimerDimerRisk()) score -= 3;
        if (reverse.hasPrimerDimerRisk()) score -= 3;
        
        return score;
    }
    
    /**
     * Get reverse complement of a sequence.
     */
    private String reverseComplement(String seq) {
        StringBuilder sb = new StringBuilder();
        for (int i = seq.length() - 1; i >= 0; i--) {
            char c = seq.charAt(i);
            sb.append(switch (Character.toUpperCase(c)) {
                case 'A' -> 'T';
                case 'T' -> 'A';
                case 'G' -> 'C';
                case 'C' -> 'G';
                default -> 'N';
            });
        }
        return sb.toString();
    }
    
    /**
     * Create a failed result.
     */
    private PcrResult failedResult(List<String> warnings) {
        return new PcrResult(false, "", 0, 0, 0, 0, List.of(), warnings,
            new PcrQualityMetrics(0, 0, false, false, 0));
    }
}
