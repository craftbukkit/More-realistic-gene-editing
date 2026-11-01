package com.morerealisticgeneediting.genome.crispr;

import com.morerealisticgeneediting.data.EditOutcome;
import com.morerealisticgeneediting.genome.Genome;
import java.util.*;

/**
 * Bounded, single-allele teaching simulation. The scores and random repair
 * weights are game rules, not validated efficiency or clinical predictions.
 * It does not model a cell population, off-target loci or coding annotations.
 */
public class CrisprEngine {
    public static final String SPCAS9_PAM = "NGG";
    public static final String SACAS9_PAM = "NNGRRT";
    public static final String CPF1_PAM = "TTTV";
    public static final int PROTOSPACER_LENGTH = 20;
    public static final int MAX_SEARCH_BASES = 1_000_000;
    private static final int MAX_DELETION_SIZE = 30;
    private static final int MAX_INSERTION_SIZE = 10;
    private final Random random;

    public enum Strand { FORWARD, REVERSE }
    /** Schematic cleavage geometry; actual cleavage can vary by enzyme/context. */
    public enum Nuclease {
        SP_CAS9(SPCAS9_PAM, 20, false, 3),
        SA_CAS9(SACAS9_PAM, 21, false, 3),
        CAS12A(CPF1_PAM, 23, true, 18);
        public final String pam;
        public final int guideLength;
        public final boolean pamBeforeGuide;
        private final int cutOffset;
        Nuclease(String pam, int length, boolean before, int cutOffset) {
            this.pam = pam; this.guideLength = length;
            this.pamBeforeGuide = before; this.cutOffset = cutOffset;
        }
    }

    public CrisprEngine(long seed) { random = new Random(seed); }

    /** Coordinates are zero-based in the current edited snapshot, forward orientation; cuts are boundaries. */
    public record TargetSite(long position, String protospacer, String pamSequence,
                             double onTargetScore, double offTargetRisk, Strand strand,
                             Nuclease nuclease, long protospacerStart, long cutPosition) {
        public TargetSite(long position, String protospacer, String pamSequence,
                          double onTargetScore, double offTargetRisk) {
            this(position, protospacer, pamSequence, onTargetScore, offTargetRisk,
                 Strand.FORWARD, Nuclease.SP_CAS9, position - PROTOSPACER_LENGTH, position - 3);
        }
        public TargetSite {
            Objects.requireNonNull(protospacer); Objects.requireNonNull(pamSequence);
            Objects.requireNonNull(strand); Objects.requireNonNull(nuclease);
            if (position < 0 || protospacerStart < 0 || cutPosition < 0)
                throw new IllegalArgumentException("Negative target coordinate");
            if (!Double.isFinite(onTargetScore) || onTargetScore < 0 || onTargetScore > 1)
                throw new IllegalArgumentException("Invalid game score");
        }
    }
    /** hasOffTargetEffects means a simulated off-target event, not proof of absence. */
    public record EditingResult(EditOutcome primaryOutcome, List<EditOutcome> byproducts,
                                double editingEfficiency, boolean hasOffTargetEffects,
                                String repairPathway, QualityMetrics qualityMetrics) {}
    /** Fractions describe the one simulated allele; NaN means not evaluated. */
    public record QualityMetrics(double indelFrequency, double frameshiftProbability,
                                 int averageIndelSize, double mosaicismLevel,
                                 Map<String, Double> outcomeDistribution) {}

    public List<TargetSite> findPamSites(Genome genome, long start, int length, String pam) {
        Objects.requireNonNull(pam);
        Nuclease nuclease = switch (pam.toUpperCase(Locale.ROOT)) {
            case SPCAS9_PAM -> Nuclease.SP_CAS9;
            case SACAS9_PAM -> Nuclease.SA_CAS9;
            case CPF1_PAM, "TTTN" -> Nuclease.CAS12A;
            default -> throw new IllegalArgumentException("Unsupported nuclease/PAM geometry");
        };
        return findPamSites(genome, start, length, nuclease);
    }
    public List<TargetSite> findPamSites(Genome genome, long start, int length, Nuclease nuclease) {
        Objects.requireNonNull(genome); Objects.requireNonNull(nuclease);
        if (start < 0 || length < 0 || length > MAX_SEARCH_BASES || start > genome.getTotalLength()
                || length > genome.getTotalLength() - start)
            throw new IllegalArgumentException("Invalid or excessive target-search interval");
        String forward = genome.getSequence(start, length).toUpperCase(Locale.ROOT);
        List<TargetSite> sites = new ArrayList<>();
        for (Strand strand : Strand.values()) {
            String sequence = strand == Strand.FORWARD ? forward : reverseComplement(forward);
            for (int i = 0; i <= sequence.length() - nuclease.pam.length(); i++) {
                int gs = nuclease.pamBeforeGuide ? i + nuclease.pam.length() : i - nuclease.guideLength;
                if (gs < 0 || gs + nuclease.guideLength > sequence.length()) continue;
                String actualPam = sequence.substring(i, i + nuclease.pam.length());
                if (!matchesPam(actualPam, nuclease.pam)) continue;
                String guide = sequence.substring(gs, gs + nuclease.guideLength);
                if (!guide.matches("[ACGT]+")) continue;
                int orientedCut = nuclease.pamBeforeGuide ? gs + nuclease.cutOffset : i - nuclease.cutOffset;
                long pamPosition = start + (strand == Strand.FORWARD ? i : sequence.length() - i - actualPam.length());
                long guideStart = start + (strand == Strand.FORWARD ? gs : sequence.length() - gs - guide.length());
                long cut = start + (strand == Strand.FORWARD ? orientedCut : sequence.length() - orientedCut);
                sites.add(new TargetSite(pamPosition, guide, actualPam, gameScore(guide, nuclease),
                        Double.NaN, strand, nuclease, guideStart, cut));
            }
        }
        sites.sort(Comparator.comparingLong(TargetSite::position).thenComparing(TargetSite::strand));
        return List.copyOf(sites);
    }

    /** Produces an outcome; the caller applies it atomically to the authorized sample. */
    public EditingResult performEditing(Genome genome, TargetSite target, String hdrTemplate) {
        validateTarget(genome, target);
        if (hdrTemplate != null) {
            hdrTemplate = hdrTemplate.toUpperCase(Locale.ROOT);
            if (hdrTemplate.isEmpty() || hdrTemplate.length() > 4096 || !hdrTemplate.matches("[ACGT]+")
                    || hdrTemplate.length() > genome.getTotalLength() - target.cutPosition())
                throw new IllegalArgumentException("Invalid replacement template");
        }
        // Explicit gameplay weights: they do not express a probability in an organism.
        double roll = random.nextDouble();
        String pathway;
        EditOutcome outcome;
        if (roll < 0.05) {
            pathway = "NONE";
            outcome = new EditOutcome(EditOutcome.Type.NO_CHANGE, target.cutPosition(), 0, "", "No simulated edit");
        } else if (hdrTemplate != null && roll < 0.15) {
            pathway = "HDR";
            outcome = new EditOutcome(EditOutcome.Type.REPLACEMENT, target.cutPosition(), hdrTemplate.length(),
                    hdrTemplate, "Schematic donor-directed replacement; homology/repair not mechanistically modeled");
        } else {
            pathway = "NHEJ";
            long remaining = genome.getTotalLength() - target.cutPosition();
            if (remaining > 0 && random.nextDouble() < 0.75) {
                int size = (int) Math.min(generateIndelSize(true), remaining);
                outcome = new EditOutcome(EditOutcome.Type.DELETION, target.cutPosition(), size, "", "Simulated end-joining deletion");
            } else {
                int size = generateIndelSize(false);
                outcome = new EditOutcome(EditOutcome.Type.INSERTION, target.cutPosition(), size,
                        randomSequence(size), "Simulated end-joining insertion");
            }
        }
        boolean edited = outcome.getType() != EditOutcome.Type.NO_CHANGE;
        boolean indel = outcome.getType() == EditOutcome.Type.DELETION || outcome.getType() == EditOutcome.Type.INSERTION;
        Map<String, Double> distribution = Map.of(outcome.getType().name().toLowerCase(Locale.ROOT), 1.0);
        QualityMetrics metrics = new QualityMetrics(indel ? 1 : 0, Double.NaN, indel ? outcome.size() : 0,
                Double.NaN, distribution);
        return new EditingResult(outcome, List.of(), edited ? 1 : 0, false, pathway, metrics);
    }

    private void validateTarget(Genome genome, TargetSite t) {
        Objects.requireNonNull(genome); Objects.requireNonNull(t);
        if (t.protospacer().length() != t.nuclease().guideLength || t.pamSequence().length() != t.nuclease().pam.length()
                || t.position() > genome.getTotalLength() - t.pamSequence().length()
                || t.protospacerStart() > genome.getTotalLength() - t.protospacer().length()
                || t.cutPosition() > genome.getTotalLength()) throw new IllegalArgumentException("Target outside sample");
        String guide = genome.getSequence(t.protospacerStart(), t.protospacer().length()).toUpperCase(Locale.ROOT);
        String pam = genome.getSequence(t.position(), t.pamSequence().length()).toUpperCase(Locale.ROOT);
        if (t.strand() == Strand.REVERSE) { guide = reverseComplement(guide); pam = reverseComplement(pam); }
        if (!guide.equals(t.protospacer()) || !pam.equals(t.pamSequence()) || !matchesPam(pam, t.nuclease().pam))
            throw new IllegalArgumentException("Target no longer matches sample");
        long expectedGuide = t.strand() == Strand.FORWARD
                ? (t.nuclease().pamBeforeGuide ? t.position() + pam.length() : t.position() - guide.length())
                : (t.nuclease().pamBeforeGuide ? t.position() - guide.length() : t.position() + pam.length());
        long expectedCut = t.nuclease().pamBeforeGuide
                ? (t.strand() == Strand.FORWARD ? expectedGuide + t.nuclease().cutOffset : expectedGuide + guide.length() - t.nuclease().cutOffset)
                : (t.strand() == Strand.FORWARD ? t.position() - t.nuclease().cutOffset : t.position() + pam.length() + t.nuclease().cutOffset);
        if (expectedGuide != t.protospacerStart() || expectedCut != t.cutPosition())
            throw new IllegalArgumentException("Inconsistent target geometry");
    }

    private int generateIndelSize(boolean deletion) {
        int max = deletion ? MAX_DELETION_SIZE : MAX_INSERTION_SIZE;
        // Truncated geometric gameplay distribution: every branch is bounded and nonempty.
        int size = 1;
        while (size < max && random.nextDouble() < 0.65) size++;
        return size;
    }
    private static boolean matchesPam(String sequence, String pattern) {
        if (sequence.length() != pattern.length()) return false;
        for (int i = 0; i < pattern.length(); i++) {
            char base = sequence.charAt(i);
            if ("ACGT".indexOf(base) < 0) return false;
            boolean matches = switch (pattern.charAt(i)) {
                case 'N' -> true;
                case 'R' -> base == 'A' || base == 'G';
                case 'V' -> base == 'A' || base == 'C' || base == 'G';
                default -> base == pattern.charAt(i);
            };
            if (!matches) return false;
        }
        return true;
    }
    private static double gameScore(String guide, Nuclease nuclease) {
        // The PAM-proximal end is the guide start for Cas12a, end for these Cas9s.
        String proximal = nuclease.pamBeforeGuide ? guide.substring(0, 12) : guide.substring(guide.length() - 12);
        double gc = proximal.chars().filter(c -> c == 'G' || c == 'C').count() / 12.0;
        return Math.max(0, Math.min(1, 0.5 + (0.5 - Math.abs(gc - 0.5)) * 0.5 - (guide.contains("TTTT") ? 0.1 : 0)));
    }
    private String randomSequence(int length) {
        StringBuilder s = new StringBuilder(length);
        for (int i = 0; i < length; i++) s.append("ACGT".charAt(random.nextInt(4)));
        return s.toString();
    }
    private static String reverseComplement(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = s.length() - 1; i >= 0; i--) out.append(switch (s.charAt(i)) {
            case 'A' -> 'T'; case 'T' -> 'A'; case 'G' -> 'C'; case 'C' -> 'G'; default -> 'N';
        });
        return out.toString();
    }
}
