package com.morerealisticgeneediting.genome.sequencing;

import com.morerealisticgeneediting.genome.Genome;
import java.util.*;

/**
 * Bounded teaching model of reference-positioned reads. Technology presets are
 * illustrative game settings, not specifications of current instruments.
 * Only substitutions are simulated/called; indels need an explicit alignment model.
 */
public class SequencingSimulator {
    public static final long MAX_GENERATED_BASES = 10_000_000;
    public static final int MAX_READS = 200_000;
    public static final long MAX_CALL_BASES = 1_000_000;
    public enum Technology {
        ILLUMINA_SE50(50, false, 0.001, 40, "Illustrative short-read 50bp SE"),
        ILLUMINA_SE150(150, false, 0.001, 35, "Illustrative short-read 150bp SE"),
        ILLUMINA_PE150(150, true, 0.001, 35, "Illustrative short-read 150bp PE"),
        ILLUMINA_PE300(300, true, 0.002, 30, "Illustrative short-read 300bp PE"),
        PACBIO_HIFI(15000, false, 0.001, 30, "Illustrative HiFi-like long-read"),
        PACBIO_CLR(20000, false, 0.10, 10, "Illustrative legacy noisy long-read"),
        NANOPORE_R10(30000, false, 0.05, 15, "Illustrative nanopore-like long-read"),
        SANGER(800, false, 0.0001, 40, "Illustrative Sanger-like read");
        public final int readLength;
        public final boolean pairedEnd;
        /** Legacy display hint; actual errors are sampled from each base's Phred score. */
        public final double errorRate;
        public final int avgQuality;
        public final String description;
        Technology(int length, boolean paired, double error, int quality, String description) {
            readLength = length; pairedEnd = paired; errorRate = error; avgQuality = quality; this.description = description;
        }
    }
    public record SequencingRead(String id, String sequence, int[] qualityScores,
                                 long referencePosition, boolean isReversed, String mateId) {
        public SequencingRead {
            Objects.requireNonNull(id); Objects.requireNonNull(sequence); Objects.requireNonNull(qualityScores);
            if (sequence.length() != qualityScores.length || referencePosition < 0
                    || id.contains("\n") || id.contains("\r") || !sequence.matches("[ACGTNacgtn]*"))
                throw new IllegalArgumentException("Invalid read or unequal sequence/quality lengths");
            qualityScores = qualityScores.clone();
            for (int q : qualityScores) if (q < 0 || q > 93) throw new IllegalArgumentException("Invalid Phred+33 value");
            sequence = sequence.toUpperCase(Locale.ROOT);
        }
        @Override public int[] qualityScores() { return qualityScores.clone(); }
        public String getQualityString() {
            StringBuilder out = new StringBuilder(qualityScores.length);
            for (int q : qualityScores) out.append((char) (q + 33));
            return out.toString();
        }
        public double getAverageQuality() { return Arrays.stream(qualityScores).average().orElse(0); }
        public String toFastq() { return "@" + id + "\n" + sequence + "\n+\n" + getQualityString(); }
    }
    /** estimatedVariants=-1 means no independent-reference comparison has been performed. */
    public record SequencingStats(int totalReads, long totalBases, double meanReadLength,
                                  double meanQuality, double q30Percentage, double coverageDepth,
                                  double gcContent, int estimatedVariants, Map<String, Object> additionalMetrics) {}
    public record SequencingResult(List<SequencingRead> reads, SequencingStats stats, Technology technology,
                                   List<String> qualityWarnings, String reportSummary) {}
    public record Variant(long position, String referenceAllele, String alternateAllele,
                          VariantType type, int depth, double alleleFrequency, double quality) {
        public enum VariantType { SNP, INSERTION, DELETION, COMPLEX }
    }
    private final Random random;
    public SequencingSimulator(long seed) { random = new Random(seed); }

    public SequencingResult runSequencing(Genome genome, Technology technology, int targetCoverage,
                                         long regionStart, long regionLength) {
        Objects.requireNonNull(technology);
        return simulate(genome, technology, regionStart, regionLength, targetCoverage, technology.readLength, technology.pairedEnd);
    }
    public SequencingResult runSequencing(Genome genome, Technology technology, long regionStart,
                                         int regionLength, int targetCoverage, int readLength, boolean pairedEnd) {
        return simulate(genome, technology, regionStart, regionLength, targetCoverage, readLength, pairedEnd);
    }
    private SequencingResult simulate(Genome genome, Technology technology, long start, long length,
                                     int coverage, int requestedReadLength, boolean pairedEnd) {
        Objects.requireNonNull(genome); Objects.requireNonNull(technology);
        if (start < 0 || length < 0 || coverage < 1 || coverage > 1000 || requestedReadLength < 1 || requestedReadLength > 100_000)
            throw new IllegalArgumentException("Invalid sequencing parameters");
        if (length == 0) {
            if (start != 0) throw new IllegalArgumentException("Whole-genome mode requires start=0");
            length = genome.getTotalLength();
        }
        if (length < 1 || start > genome.getTotalLength() || length > genome.getTotalLength() - start)
            throw new IllegalArgumentException("Invalid sequencing region");
        if (length > MAX_GENERATED_BASES / coverage) throw new IllegalArgumentException("Sequencing base budget exceeded");
        int readLength = (int) Math.min(length, requestedReadLength);
        int readsPerTemplate = pairedEnd ? 2 : 1;
        long basesPerTemplate = (long) readLength * readsPerTemplate;
        long desiredBases = length * coverage;
        long templates = (desiredBases + basesPerTemplate - 1) / basesPerTemplate;
        if (templates * readsPerTemplate > MAX_READS || templates * basesPerTemplate > MAX_GENERATED_BASES)
            throw new IllegalArgumentException("Sequencing read/base budget exceeded");
        List<SequencingRead> reads = new ArrayList<>((int) templates * readsPerTemplate);
        List<String> warnings = new ArrayList<>();
        if (readLength != requestedReadLength) warnings.add("Read length shortened to fit the requested region.");
        // A toy fragment span, bounded to this requested region; both mates always exist.
        int fragmentLength = (int) Math.min(length, pairedEnd ? 2L * readLength : readLength);
        long possibleStarts = length - fragmentLength + 1;
        for (int i = 0; i < templates; i++) {
            long position = start + random.nextLong(possibleStarts);
            String id = String.format(Locale.ROOT, "READ_%08d", i);
            if (pairedEnd) {
                reads.add(generateRead(genome, position, readLength, technology, id + "/1", false, id + "/2"));
                reads.add(generateRead(genome, position + fragmentLength - readLength, readLength,
                        technology, id + "/2", true, id + "/1"));
            } else reads.add(generateRead(genome, position, readLength, technology, id, random.nextBoolean(), null));
        }
        SequencingStats stats = calculateStats(reads, length, technology, pairedEnd);
        if (stats.coverageDepth() < 10) warnings.add("Low simulated depth; absence of a call is not absence of a variant.");
        if (stats.q30Percentage() < 80) warnings.add("Many simulated bases have Q<30.");
        warnings.add("Variant count not evaluated; comparison with an independent reference is required.");
        String report = String.format(Locale.ROOT,
                "SIMULATED READ REPORT\nPreset: %s\nReads: %d\nBases: %d\nMean read length: %.1f\nMean quality: %.1f\nQ30: %.1f%%\nDepth: %.1fx\nRead N50: %d\nVariants: not evaluated\nSubstitution-only teaching model.",
                technology.description, stats.totalReads(), stats.totalBases(), stats.meanReadLength(), stats.meanQuality(),
                stats.q30Percentage(), stats.coverageDepth(), calculateN50(reads));
        return new SequencingResult(List.copyOf(reads), stats, technology, List.copyOf(warnings), report);
    }

    /** SNP-only pileup over already known reference positions; not an aligner. */
    public List<Variant> callVariants(List<SequencingRead> reads, Genome reference, int minDepth, double minQuality) {
        Objects.requireNonNull(reads); Objects.requireNonNull(reference);
        if (minDepth < 1 || !Double.isFinite(minQuality) || minQuality < 0 || minQuality > 93 || reads.size() > MAX_READS)
            throw new IllegalArgumentException("Invalid variant-calling parameters");
        long baseCount = 0;
        for (SequencingRead read : reads) {
            baseCount += read.sequence().length();
            if (baseCount > MAX_CALL_BASES || read.referencePosition() > reference.getTotalLength()
                    || read.sequence().length() > reference.getTotalLength() - read.referencePosition())
                throw new IllegalArgumentException("Read outside reference or pileup budget exceeded");
        }
        // Each allele stores its own count and quality sum; reference quality cannot rescue a poor alt.
        Map<Long, long[]> pileup = new TreeMap<>();
        for (SequencingRead read : reads) {
            int[] qualities = read.qualityScores();
            int n = read.sequence().length();
            for (int i = 0; i < n; i++) {
                int readIndex = read.isReversed() ? n - 1 - i : i;
                int q = qualities[readIndex];
                if (q < minQuality) continue;
                char base = read.sequence().charAt(readIndex);
                if (read.isReversed()) base = complement(base);
                int allele = "ACGT".indexOf(base);
                if (allele < 0) continue;
                long[] counts = pileup.computeIfAbsent(read.referencePosition() + i, p -> new long[8]);
                counts[allele]++; counts[allele + 4] += q;
            }
        }
        List<Variant> variants = new ArrayList<>();
        for (var entry : pileup.entrySet()) {
            long[] counts = entry.getValue();
            int depth = (int) (counts[0] + counts[1] + counts[2] + counts[3]);
            if (depth < minDepth) continue;
            String ref = reference.getSequence(entry.getKey(), 1).toUpperCase(Locale.ROOT);
            if (ref.length() != 1 || "ACGT".indexOf(ref.charAt(0)) < 0) continue;
            for (int allele = 0; allele < 4; allele++) {
                char alt = "ACGT".charAt(allele);
                double fraction = (double) counts[allele] / depth;
                if (alt != ref.charAt(0) && counts[allele] >= Math.max(1, (minDepth + 2) / 3) && fraction >= 0.1)
                    variants.add(new Variant(entry.getKey(), ref, String.valueOf(alt), Variant.VariantType.SNP,
                            depth, fraction, (double) counts[allele + 4] / counts[allele]));
            }
        }
        return List.copyOf(variants);
    }

    private SequencingRead generateRead(Genome genome, long position, int length, Technology technology,
                                        String id, boolean reversed, String mateId) {
        if (position < 0 || position >= genome.getTotalLength() || length < 1)
            throw new IllegalArgumentException("Invalid read interval");
        int actualLength = (int) Math.min(length, genome.getTotalLength() - position);
        String sequence = genome.getSequence(position, actualLength).toUpperCase(Locale.ROOT);
        if (reversed) sequence = reverseComplement(sequence);
        char[] bases = sequence.toCharArray();
        int[] qualities = new int[bases.length];
        for (int i = 0; i < bases.length; i++) {
            double decay = 1.0 - 0.3 * i / Math.max(1.0, bases.length - 1);
            int q = Math.max(2, Math.min(50, (int) Math.round(technology.avgQuality * decay + random.nextGaussian() * 3)));
            qualities[i] = q;
            if ("ACGT".indexOf(bases[i]) >= 0 && random.nextDouble() < Math.pow(10, -q / 10.0)) bases[i] = substitute(bases[i]);
        }
        return new SequencingRead(id, new String(bases), qualities, position, reversed, mateId);
    }
    private SequencingStats calculateStats(List<SequencingRead> reads, long regionLength, Technology technology, boolean pairedEnd) {
        long bases = 0, q30 = 0, gc = 0, quality = 0;
        for (SequencingRead read : reads) {
            int[] qualities = read.qualityScores(); bases += qualities.length;
            for (int i = 0; i < qualities.length; i++) {
                quality += qualities[i]; if (qualities[i] >= 30) q30++;
                if (read.sequence().charAt(i) == 'G' || read.sequence().charAt(i) == 'C') gc++;
            }
        }
        Map<String, Object> additional = Map.of("technology", technology.description, "pairedEnd", pairedEnd,
                "readN50", calculateN50(reads), "variantCountEvaluated", false, "model", "substitution-only teaching simulation");
        return new SequencingStats(reads.size(), bases, reads.isEmpty() ? 0 : (double) bases / reads.size(),
                bases == 0 ? 0 : (double) quality / bases, bases == 0 ? 0 : q30 * 100.0 / bases,
                (double) bases / regionLength, bases == 0 ? 0 : (double) gc / bases, -1, additional);
    }
    public int calculateN50(List<SequencingRead> reads) {
        List<Integer> lengths = reads.stream().map(r -> r.sequence().length()).sorted(Comparator.reverseOrder()).toList();
        long total = lengths.stream().mapToLong(Integer::longValue).sum();
        if (total == 0) return 0;
        long threshold = total / 2 + total % 2;
        long cumulative = 0;
        for (int length : lengths) { cumulative += length; if (cumulative >= threshold) return length; }
        return 0;
    }
    private char substitute(char original) {
        int index = "ACGT".indexOf(original);
        return "ACGT".charAt((index + 1 + random.nextInt(3)) % 4);
    }
    private static char complement(char c) {
        return switch (c) { case 'A' -> 'T'; case 'T' -> 'A'; case 'G' -> 'C'; case 'C' -> 'G'; default -> 'N'; };
    }
    private static String reverseComplement(String sequence) {
        StringBuilder out = new StringBuilder(sequence.length());
        for (int i = sequence.length() - 1; i >= 0; i--) out.append(complement(sequence.charAt(i)));
        return out.toString();
    }
}
