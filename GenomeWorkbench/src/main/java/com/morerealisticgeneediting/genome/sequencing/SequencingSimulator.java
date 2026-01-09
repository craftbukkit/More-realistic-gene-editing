package com.morerealisticgeneediting.genome.sequencing;

import com.morerealisticgeneediting.genome.Genome;

import java.util.*;

/**
 * SequencingSimulator - Simulates next-generation DNA sequencing with realistic outcomes.
 * 
 * This simulator models key aspects of real NGS sequencing:
 * - Read generation with configurable length
 * - Quality score simulation (Phred scores)
 * - Coverage calculation
 * - Error modeling (substitution, insertion, deletion)
 * - Variant calling basics
 * 
 * Educational note: Illumina sequencing has ~0.1% error rate, while
 * long-read technologies (PacBio, Nanopore) have ~5-15% raw error rate
 * but can achieve high accuracy through consensus.
 */
public class SequencingSimulator {
    
    // Sequencing technology presets
    public enum Technology {
        ILLUMINA_SE50(50, false, 0.001, 40, "Illumina Short-read 50bp SE"),
        ILLUMINA_SE150(150, false, 0.001, 35, "Illumina Short-read 150bp SE"),
        ILLUMINA_PE150(150, true, 0.001, 35, "Illumina Short-read 150bp PE"),
        ILLUMINA_PE300(300, true, 0.002, 30, "Illumina MiSeq 300bp PE"),
        PACBIO_HIFI(15000, false, 0.001, 30, "PacBio HiFi Long-read"),
        PACBIO_CLR(20000, false, 0.10, 10, "PacBio CLR Long-read"),
        NANOPORE_R10(30000, false, 0.05, 15, "Oxford Nanopore R10"),
        SANGER(800, false, 0.0001, 50, "Sanger Sequencing");
        
        public final int readLength;
        public final boolean pairedEnd;
        public final double errorRate;
        public final int avgQuality;
        public final String description;
        
        Technology(int readLength, boolean pairedEnd, double errorRate, int avgQuality, String description) {
            this.readLength = readLength;
            this.pairedEnd = pairedEnd;
            this.errorRate = errorRate;
            this.avgQuality = avgQuality;
            this.description = description;
        }
    }
    
    /**
     * Represents a single sequencing read.
     */
    public record SequencingRead(
        String id,
        String sequence,
        int[] qualityScores,  // Phred scores (0-40+)
        long referencePosition,
        boolean isReversed,
        String mateId  // For paired-end reads
    ) {
        /**
         * Get quality string in FASTQ format.
         */
        public String getQualityString() {
            StringBuilder sb = new StringBuilder(qualityScores.length);
            for (int q : qualityScores) {
                sb.append((char) (q + 33));  // Phred+33 encoding
            }
            return sb.toString();
        }
        
        /**
         * Get average quality score.
         */
        public double getAverageQuality() {
            return Arrays.stream(qualityScores).average().orElse(0);
        }
        
        /**
         * Get FASTQ formatted string.
         */
        public String toFastq() {
            return String.format("@%s\n%s\n+\n%s", id, sequence, getQualityString());
        }
    }
    
    /**
     * Summary statistics for a sequencing run.
     */
    public record SequencingStats(
        int totalReads,
        long totalBases,
        double meanReadLength,
        double meanQuality,
        double q30Percentage,  // % of bases with Q>=30
        double coverageDepth,
        double gcContent,
        int estimatedVariants,
        Map<String, Object> additionalMetrics
    ) {}
    
    /**
     * Result of a sequencing run.
     */
    public record SequencingResult(
        List<SequencingRead> reads,
        SequencingStats stats,
        Technology technology,
        List<String> qualityWarnings,
        String reportSummary
    ) {}
    
    /**
     * Represents a detected variant.
     */
    public record Variant(
        long position,
        String referenceAllele,
        String alternateAllele,
        VariantType type,
        int depth,
        double alleleFrequency,
        double quality
    ) {
        public enum VariantType {
            SNP, INSERTION, DELETION, COMPLEX
        }
    }
    
    private final Random random;
    private final long seed;
    
    public SequencingSimulator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Run a sequencing simulation.
     * 
     * @param genome Target genome to sequence
     * @param technology Sequencing technology to simulate
     * @param targetCoverage Desired average coverage depth
     * @param regionStart Start position (0 for whole genome)
     * @param regionLength Length of region (0 for whole genome)
     * @return SequencingResult with reads and statistics
     */
    public SequencingResult runSequencing(Genome genome, Technology technology, 
                                          int targetCoverage, long regionStart, long regionLength) {
        
        List<SequencingRead> allReads = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Calculate region
        long genomeLength = genome.getTotalLength();
        if (regionLength <= 0) {
            regionStart = 0;
            regionLength = genomeLength;
        }
        regionLength = Math.min(regionLength, genomeLength - regionStart);
        
        // Calculate number of reads needed
        int readLength = technology.readLength;
        int numReads = (int) ((regionLength * targetCoverage) / readLength);
        if (technology.pairedEnd) {
            numReads /= 2;  // Each pair covers 2x
        }
        
        // Generate reads
        int readCounter = 0;
        for (int i = 0; i < numReads; i++) {
            // Random position within region
            long position = regionStart + (long) (random.nextDouble() * (regionLength - readLength));
            position = Math.max(regionStart, Math.min(position, regionStart + regionLength - readLength));
            
            if (technology.pairedEnd) {
                // Generate paired-end reads
                int insertSize = 300 + random.nextInt(200);  // 300-500bp insert
                
                String readId1 = String.format("READ_%08d/1", readCounter);
                String readId2 = String.format("READ_%08d/2", readCounter);
                
                SequencingRead read1 = generateRead(genome, position, readLength, 
                    technology, readId1, false, readId2);
                allReads.add(read1);
                
                long mate2Pos = position + insertSize - readLength;
                if (mate2Pos > 0 && mate2Pos + readLength <= genomeLength) {
                    SequencingRead read2 = generateRead(genome, mate2Pos, readLength,
                        technology, readId2, true, readId1);
                    allReads.add(read2);
                }
            } else {
                // Single-end read
                String readId = String.format("READ_%08d", readCounter);
                boolean reversed = random.nextBoolean();
                SequencingRead read = generateRead(genome, position, readLength,
                    technology, readId, reversed, null);
                allReads.add(read);
            }
            readCounter++;
        }
        
        // Calculate statistics
        SequencingStats stats = calculateStats(allReads, regionLength, technology);
        
        // Generate warnings
        if (stats.coverageDepth() < 10) {
            warnings.add("Low coverage: <10x may miss variants");
        }
        if (stats.q30Percentage() < 80) {
            warnings.add("Low quality: <80% bases at Q30");
        }
        if (stats.gcContent() < 0.35 || stats.gcContent() > 0.65) {
            warnings.add("Unusual GC content may indicate contamination or bias");
        }
        
        // Generate report summary
        String report = generateReport(stats, technology, warnings);
        
        return new SequencingResult(allReads, stats, technology, warnings, report);
    }
    
    /**
     * Call variants from sequencing data.
     * 
     * @param reads List of sequencing reads
     * @param reference Reference genome
     * @param minDepth Minimum read depth for calling
     * @param minQuality Minimum quality score
     * @return List of called variants
     */
    public List<Variant> callVariants(List<SequencingRead> reads, Genome reference,
                                      int minDepth, double minQuality) {
        
        List<Variant> variants = new ArrayList<>();
        
        // Build pileup (simplified)
        Map<Long, List<Character>> pileup = new HashMap<>();
        Map<Long, List<Integer>> qualityPileup = new HashMap<>();
        
        for (SequencingRead read : reads) {
            for (int i = 0; i < read.sequence().length(); i++) {
                long pos = read.referencePosition() + i;
                pileup.computeIfAbsent(pos, k -> new ArrayList<>()).add(read.sequence().charAt(i));
                qualityPileup.computeIfAbsent(pos, k -> new ArrayList<>()).add(read.qualityScores()[i]);
            }
        }
        
        // Call variants at each position
        for (Map.Entry<Long, List<Character>> entry : pileup.entrySet()) {
            long pos = entry.getKey();
            List<Character> bases = entry.getValue();
            List<Integer> qualities = qualityPileup.get(pos);
            
            if (bases.size() < minDepth) continue;
            
            // Get reference base
            String refSeq = reference.getSequence(pos, 1);
            if (refSeq.isEmpty()) continue;
            char refBase = refSeq.charAt(0);
            
            // Count alleles
            Map<Character, Integer> alleleCounts = new HashMap<>();
            for (char base : bases) {
                alleleCounts.merge(base, 1, Integer::sum);
            }
            
            // Check for non-reference alleles
            for (Map.Entry<Character, Integer> allele : alleleCounts.entrySet()) {
                char altBase = allele.getKey();
                int count = allele.getValue();
                
                if (altBase != refBase && count >= minDepth / 3) {
                    double af = (double) count / bases.size();
                    double avgQual = qualities.stream().mapToInt(Integer::intValue).average().orElse(0);
                    
                    if (avgQual >= minQuality && af >= 0.1) {
                        variants.add(new Variant(
                            pos,
                            String.valueOf(refBase),
                            String.valueOf(altBase),
                            Variant.VariantType.SNP,
                            bases.size(),
                            af,
                            avgQual
                        ));
                    }
                }
            }
        }
        
        return variants;
    }
    
    /**
     * Generate a single sequencing read.
     */
    private SequencingRead generateRead(Genome genome, long position, int length,
                                        Technology tech, String id, boolean reversed, String mateId) {
        
        // Get sequence from genome
        String sequence = genome.getSequence(position, length);
        
        // Reverse complement if needed
        if (reversed) {
            sequence = reverseComplement(sequence);
        }
        
        // Add sequencing errors
        char[] chars = sequence.toCharArray();
        int[] qualities = new int[length];
        
        for (int i = 0; i < chars.length; i++) {
            // Quality decreases towards end of read (typical for Illumina)
            double positionEffect = 1.0 - (double) i / length * 0.3;
            int baseQuality = (int) (tech.avgQuality * positionEffect);
            baseQuality += random.nextGaussian() * 5;  // Add noise
            baseQuality = Math.max(2, Math.min(41, baseQuality));
            qualities[i] = baseQuality;
            
            // Introduce errors based on technology error rate
            if (random.nextDouble() < tech.errorRate) {
                // Error type: mostly substitutions
                double errorType = random.nextDouble();
                if (errorType < 0.9) {
                    // Substitution
                    chars[i] = substituteBase(chars[i]);
                    qualities[i] = Math.min(qualities[i], 10);  // Low quality for errors
                }
                // Could add insertions/deletions for long-read tech
            }
        }
        
        return new SequencingRead(id, new String(chars), qualities, position, reversed, mateId);
    }
    
    /**
     * Calculate statistics from reads.
     */
    private SequencingStats calculateStats(List<SequencingRead> reads, long regionLength, Technology tech) {
        if (reads.isEmpty()) {
            return new SequencingStats(0, 0, 0, 0, 0, 0, 0, 0, Map.of());
        }
        
        long totalBases = 0;
        long q30Bases = 0;
        long gcBases = 0;
        double totalQuality = 0;
        
        for (SequencingRead read : reads) {
            totalBases += read.sequence().length();
            
            for (int i = 0; i < read.sequence().length(); i++) {
                if (read.qualityScores()[i] >= 30) q30Bases++;
                char base = read.sequence().charAt(i);
                if (base == 'G' || base == 'C' || base == 'g' || base == 'c') gcBases++;
                totalQuality += read.qualityScores()[i];
            }
        }
        
        double meanLength = (double) totalBases / reads.size();
        double meanQuality = totalQuality / totalBases;
        double q30Pct = (double) q30Bases / totalBases * 100;
        double coverage = (double) totalBases / regionLength;
        double gcContent = (double) gcBases / totalBases;
        
        // Estimate variants (rough heuristic)
        int estimatedVariants = (int) (regionLength * 0.001 * (coverage / 30));
        
        Map<String, Object> additional = new HashMap<>();
        additional.put("technology", tech.description);
        additional.put("readLength", tech.readLength);
        additional.put("pairedEnd", tech.pairedEnd);
        
        return new SequencingStats(
            reads.size(),
            totalBases,
            meanLength,
            meanQuality,
            q30Pct,
            coverage,
            gcContent,
            estimatedVariants,
            additional
        );
    }
    
    /**
     * Generate a quality report.
     */
    private String generateReport(SequencingStats stats, Technology tech, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== SEQUENCING QUALITY REPORT ===\n\n");
        sb.append(String.format("Technology: %s\n", tech.description));
        sb.append(String.format("Total Reads: %,d\n", stats.totalReads()));
        sb.append(String.format("Total Bases: %,d\n", stats.totalBases()));
        sb.append(String.format("Mean Read Length: %.1f bp\n", stats.meanReadLength()));
        sb.append(String.format("Mean Quality Score: Q%.1f\n", stats.meanQuality()));
        sb.append(String.format("Q30 Bases: %.1f%%\n", stats.q30Percentage()));
        sb.append(String.format("Coverage Depth: %.1fx\n", stats.coverageDepth()));
        sb.append(String.format("GC Content: %.1f%%\n", stats.gcContent() * 100));
        sb.append(String.format("Estimated Variants: ~%d\n", stats.estimatedVariants()));
        
        if (!warnings.isEmpty()) {
            sb.append("\n--- WARNINGS ---\n");
            for (String warning : warnings) {
                sb.append("⚠ ").append(warning).append("\n");
            }
        }
        
        sb.append("\n--- QUALITY ASSESSMENT ---\n");
        if (stats.q30Percentage() >= 90 && stats.coverageDepth() >= 30) {
            sb.append("✓ High quality data suitable for variant calling\n");
        } else if (stats.q30Percentage() >= 80 && stats.coverageDepth() >= 15) {
            sb.append("◐ Moderate quality data - may miss some variants\n");
        } else {
            sb.append("✗ Low quality data - consider re-sequencing\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Substitute a base with a random different base.
     */
    private char substituteBase(char original) {
        char[] bases = {'A', 'C', 'G', 'T'};
        char result;
        do {
            result = bases[random.nextInt(4)];
        } while (result == Character.toUpperCase(original));
        return result;
    }
    
    /**
     * Get reverse complement of a sequence.
     */
    private String reverseComplement(String seq) {
        StringBuilder sb = new StringBuilder(seq.length());
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
     * Calculate N50 (common quality metric for assemblies).
     */
    public int calculateN50(List<SequencingRead> reads) {
        List<Integer> lengths = reads.stream()
            .map(r -> r.sequence().length())
            .sorted(Comparator.reverseOrder())
            .toList();
        
        long totalLength = lengths.stream().mapToLong(Integer::longValue).sum();
        long halfLength = totalLength / 2;
        
        long cumulative = 0;
        for (int length : lengths) {
            cumulative += length;
            if (cumulative >= halfLength) {
                return length;
            }
        }
        return 0;
    }
}
