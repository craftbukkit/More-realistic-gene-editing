package com.morerealisticgeneediting.genome.analysis;

import java.util.*;

/**
 * GelElectrophoresisSimulator - Simulates agarose gel electrophoresis for DNA analysis.
 * 
 * This simulator models the key aspects of gel electrophoresis:
 * - DNA migration based on molecular weight (size)
 * - Agarose concentration effects on resolution
 * - Band visualization and interpretation
 * - Molecular weight estimation using DNA ladder
 * 
 * Educational model: migration is approximated as affine in log10(fragment length)
 * over a limited range. Time and field drive displacement; no drive means no
 * migration. Coefficients are gameplay settings, not instrument calibration.
 */
public class GelElectrophoresisSimulator {
    
    // Standard DNA ladders (sizes in bp)
    public static final int[] LADDER_100BP = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1200, 1500};
    public static final int[] LADDER_1KB = {250, 500, 750, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 8000, 10000};
    public static final int[] LADDER_1KB_PLUS = {100, 200, 300, 400, 500, 650, 850, 1000, 1650, 2000, 3000, 4000, 5000, 6000, 8000, 10000, 12000};
    
    // Gel concentrations and their optimal resolution ranges
    public enum GelConcentration {
        LOW_0_5(0.5, 1000, 30000),      // 0.5% - large fragments
        STANDARD_1_0(1.0, 500, 10000),   // 1.0% - standard range
        MEDIUM_1_5(1.5, 200, 3000),      // 1.5% - medium fragments
        HIGH_2_0(2.0, 100, 2000),        // 2.0% - small fragments
        VERY_HIGH_3_0(3.0, 50, 1000);    // 3.0% - very small fragments
        
        public final double concentration;
        public final int minOptimalSize;
        public final int maxOptimalSize;
        
        GelConcentration(double concentration, int minSize, int maxSize) {
            this.concentration = concentration;
            this.minOptimalSize = minSize;
            this.maxOptimalSize = maxSize;
        }
    }
    
    /**
     * Represents a DNA sample loaded into a gel lane.
     */
    public record DnaSample(
        String name,
        List<DnaFragment> fragments,
        double concentration, // illustrative abundance scale
        boolean isLadder
    ) {
        public DnaSample(String name, List<DnaFragment> fragments, double concentration) {
            this(name, fragments, concentration, false);
        }
        public DnaSample {
            Objects.requireNonNull(name); Objects.requireNonNull(fragments);
            fragments = List.copyOf(fragments);
            if (fragments.size() > 1000 || !Double.isFinite(concentration) || concentration < 0)
                throw new IllegalArgumentException("Invalid sample");
        }
        public static DnaSample single(String name, int size, double concentration) {
            return new DnaSample(name, List.of(new DnaFragment(size, 1.0)), concentration);
        }
        
        public static DnaSample ladder(String name, int[] sizes) {
            List<DnaFragment> fragments = new ArrayList<>();
            for (int size : sizes) {
                fragments.add(new DnaFragment(size, 1.0 / sizes.length));
            }
            return new DnaSample(name, fragments, 100.0, true);
        }
    }
    
    /**
     * Represents a DNA fragment with size and relative abundance.
     */
    public record DnaFragment(
        int sizeInBp,
        double relativeAbundance  // 0-1, relative to other fragments
    ) {
        public DnaFragment {
            if (sizeInBp < 1 || !Double.isFinite(relativeAbundance) || relativeAbundance < 0 || relativeAbundance > 1)
                throw new IllegalArgumentException("Invalid fragment");
        }
    }
    
    /**
     * Represents a band visible on the gel.
     */
    public record GelBand(
        double migrationDistance,  // 0-1, from top of gel
        int estimatedSize,         // Estimated bp based on ladder
        double intensity,          // Band intensity (0-1)
        boolean isSharp,           // Sharp band vs smeared
        String annotation          // Optional annotation
    ) {}
    
    /**
     * Result of gel electrophoresis analysis.
     */
    public record GelResult(
        List<List<GelBand>> lanes,      // Bands for each lane
        int gelLengthMm,                 // Physical gel length
        GelConcentration concentration,
        double runTimeMinutes,
        List<String> interpretations,   // Analysis interpretations
        GelQualityMetrics quality
    ) {}
    
    /**
     * Quality metrics for the gel.
     */
    public record GelQualityMetrics(
        double resolution,        // How well-separated bands are
        double bandSharpness,     // Average band sharpness
        boolean hasSmearing,      // Appearance flag; does not identify a cause
        boolean hasOverloading,   // Too much DNA
        double overallQuality     // 0-1 overall score
    ) {}
    
    private final Random random;
    private final long seed;
    
    public GelElectrophoresisSimulator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    /**
     * Run gel electrophoresis simulation.
     * 
     * @param samples List of DNA samples (first should typically be ladder)
     * @param gelConcentration Agarose concentration
     * @param runTimeMinutes Duration of electrophoresis
     * @param voltage Running voltage (V)
     * @return GelResult with band patterns
     */
    public GelResult runGel(List<DnaSample> samples, GelConcentration gelConcentration, 
                           double runTimeMinutes, int voltage) {
        
        Objects.requireNonNull(samples); Objects.requireNonNull(gelConcentration);
        if (samples.size() > 96 || !Double.isFinite(runTimeMinutes) || runTimeMinutes < 0 || runTimeMinutes > 10_000
                || voltage < 0 || voltage > 10_000)
            throw new IllegalArgumentException("Invalid or excessive gel parameters");
        List<List<GelBand>> allLanes = new ArrayList<>();
        List<String> interpretations = new ArrayList<>();
        
        // Determine gel characteristics
        int gelLength = 100; // mm
        double maxMigration = calculateMaxMigration(runTimeMinutes, voltage, gelConcentration);
        
        // Process each sample
        for (int laneIndex = 0; laneIndex < samples.size(); laneIndex++) {
            DnaSample sample = samples.get(laneIndex);
            List<GelBand> laneBands = new ArrayList<>();
            
            for (DnaFragment fragment : sample.fragments()) {
                // Calculate migration distance
                double migration = calculateMigrationDistance(
                    fragment.sizeInBp(), gelConcentration, maxMigration
                );
                
                // Check if fragment is in optimal resolution range
                boolean isOptimal = fragment.sizeInBp() >= gelConcentration.minOptimalSize &&
                                   fragment.sizeInBp() <= gelConcentration.maxOptimalSize;
                
                // Calculate band properties
                double intensity = calculateBandIntensity(
                    fragment.relativeAbundance(), sample.concentration(), fragment.sizeInBp()
                );
                boolean isSharp = isOptimal && sample.concentration() < 500;
                
                // Add some noise
                migration *= 0.99 + random.nextDouble() * 0.02;
                intensity *= 0.9 + random.nextDouble() * 0.2;
                
                if (migration >= 0 && migration < 1 && intensity > 0) {  // Within gel boundaries
                    laneBands.add(new GelBand(
                        migration,
                        sample.isLadder() ? fragment.sizeInBp() : -1,
                        Math.min(1.0, intensity),
                        isSharp,
                        sample.isLadder() ? fragment.sizeInBp() + " bp" : null
                    ));
                }
            }
            
            // Sort bands by migration (top to bottom)
            laneBands.sort(Comparator.comparingDouble(GelBand::migrationDistance));
            allLanes.add(laneBands);
        }
        
        // Only an explicitly designated ladder calibrates unknown lanes.
        List<GelBand> ladder = List.of();
        for (int i = 0; i < samples.size(); i++) if (samples.get(i).isLadder()) { ladder = allLanes.get(i); break; }
        for (int i = 0; i < samples.size(); i++) {
            if (samples.get(i).isLadder()) continue;
            List<GelBand> calibrated = new ArrayList<>();
            for (GelBand band : allLanes.get(i)) calibrated.add(new GelBand(band.migrationDistance(),
                    estimateSize(band.migrationDistance(), ladder), band.intensity(), band.isSharp(), null));
            allLanes.set(i, List.copyOf(calibrated));
        }
        if (runTimeMinutes == 0 || voltage == 0) interpretations.add("No migration: sample remains at the loading origin.");
        interpretations.addAll(generateInterpretations(samples, allLanes, gelConcentration));
        
        // Calculate quality metrics
        GelQualityMetrics quality = assessQuality(samples, allLanes, gelConcentration);
        
        return new GelResult(
            allLanes.stream().map(List::copyOf).toList(),
            gelLength,
            gelConcentration,
            runTimeMinutes,
            interpretations,
            quality
        );
    }
    
    /**
     * Estimate size of unknown band using ladder.
     * 
     * @param unknownMigration Migration distance of unknown band
     * @param ladderBands Bands from ladder lane
     * @return Estimated size in bp
     */
    public int estimateSize(double unknownMigration, List<GelBand> ladderBands) {
        if (!Double.isFinite(unknownMigration) || unknownMigration < 0 || unknownMigration >= 1
                || ladderBands.size() < 2 || ladderBands.stream().anyMatch(b -> b.estimatedSize() <= 0)
                || ladderBands.stream().mapToDouble(GelBand::migrationDistance).max().orElse(0)
                - ladderBands.stream().mapToDouble(GelBand::migrationDistance).min().orElse(0) < 1e-9) return -1;
        
        // Find flanking ladder bands
        GelBand upper = null, lower = null;
        
        for (GelBand band : ladderBands) {
            if (band.migrationDistance() <= unknownMigration) {
                if (upper == null || band.migrationDistance() > upper.migrationDistance()) {
                    upper = band;
                }
            }
            if (band.migrationDistance() >= unknownMigration) {
                if (lower == null || band.migrationDistance() < lower.migrationDistance()) {
                    lower = band;
                }
            }
        }
        
        if (upper == null || lower == null) {
            // Extrapolate
            return extrapolateSize(unknownMigration, ladderBands);
        }
        
        if (upper.equals(lower)) {
            return upper.estimatedSize();
        }
        
        // Linear interpolation in log space
        double logUpper = Math.log10(upper.estimatedSize());
        double logLower = Math.log10(lower.estimatedSize());
        double migrationRange = lower.migrationDistance() - upper.migrationDistance();
        double fraction = (unknownMigration - upper.migrationDistance()) / migrationRange;
        double logEstimate = logUpper + fraction * (logLower - logUpper);
        
        return (int) Math.pow(10, logEstimate);
    }
    
    /**
     * Calculate migration distance based on fragment size.
     * This display approximation decreases affinely with log10 of size.
     */
    private double calculateMigrationDistance(int sizeInBp, GelConcentration gel, double drive) {
        // Log-linear display approximation, limited to the simulated size range.
        double mobility = Math.max(0.02, 0.6 - 0.15 * (Math.log10(sizeInBp) - 2));
        return mobility * drive / gel.concentration;
    }
    private double calculateMaxMigration(double timeMinutes, int voltage, GelConcentration gel) {
        return (timeMinutes / 60.0) * (voltage / 100.0);
    }
    
    /**
     * Calculate band intensity.
     */
    private double calculateBandIntensity(double abundance, double concentration, int size) {
        // Intensity depends on amount of DNA and visualization
        // Larger fragments bind more dye per molecule
        double sizeEffect = Math.log10(size) / 4.0;  // Normalize
        return abundance * (concentration / 100.0) * sizeEffect;
    }
    
    /**
     * Extrapolate size for bands outside ladder range.
     */
    private int extrapolateSize(double migration, List<GelBand> ladderBands) {
        if (ladderBands.size() < 2) return -1;
        
        // Use linear regression in log-migration space
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = ladderBands.size();
        
        for (GelBand band : ladderBands) {
            double x = band.migrationDistance();
            double y = Math.log10(band.estimatedSize());
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-12) return -1;
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        double logSize = slope * migration + intercept;
        return (int) Math.pow(10, logSize);
    }
    
    /**
     * Generate interpretation text for the gel results.
     */
    private List<String> generateInterpretations(List<DnaSample> samples, List<List<GelBand>> lanes,
                                                 GelConcentration gel) {
        List<String> text = new ArrayList<>();
        boolean hasLadder = samples.stream().anyMatch(DnaSample::isLadder);
        if (!hasLadder) text.add("No designated DNA ladder; unknown fragment sizes are not calibrated.");
        for (int i = 0; i < lanes.size(); i++) {
            if (samples.get(i).isLadder()) continue;
            List<GelBand> bands = lanes.get(i);
            String description = bands.isEmpty() ? "No visible band in the simulated gel."
                    : bands.size() + " visible simulated band(s); a band alone does not establish editing success or purity.";
            text.add("Lane " + (i + 1) + " (" + samples.get(i).name() + "): " + description);
            if (bands.stream().anyMatch(b -> !b.isSharp())) text.add("Lane " + (i + 1) + ": Poor simulated resolution; cause is not identified by appearance alone.");
        }
        return List.copyOf(text);
    }
    
    /**
     * Assess overall gel quality.
     */
    private GelQualityMetrics assessQuality(List<DnaSample> samples,
                                            List<List<GelBand>> lanes,
                                            GelConcentration gel) {
        // Calculate resolution
        double resolution = 0;
        int separations = 0;
        for (List<GelBand> lane : lanes) for (int i = 1; i < lane.size(); i++) {
            resolution += Math.min(1, Math.max(0, lane.get(i).migrationDistance() - lane.get(i - 1).migrationDistance()) / 0.05);
            separations++;
        }
        resolution = separations == 0 ? 0 : resolution / separations;
        
        // Calculate average band sharpness
        double totalSharpness = 0;
        int bandCount = 0;
        for (List<GelBand> lane : lanes) {
            for (GelBand band : lane) {
                totalSharpness += band.isSharp() ? 1.0 : 0.5;
                bandCount++;
            }
        }
        double avgSharpness = bandCount > 0 ? totalSharpness / bandCount : 0;
        
        // Check for issues
        boolean hasSmearing = lanes.stream().anyMatch(lane -> 
            lane.stream().anyMatch(b -> !b.isSharp()));
        boolean hasOverloading = samples.stream().anyMatch(s -> s.concentration() > 500);
        
        // Calculate overall quality
        double quality = 0.7;
        if (!hasSmearing) quality += 0.15;
        if (!hasOverloading) quality += 0.1;
        quality *= avgSharpness;
        
        return new GelQualityMetrics(
            resolution,
            avgSharpness,
            hasSmearing,
            hasOverloading,
            Math.min(1.0, quality)
        );
    }
    
    /**
     * Generate a visual representation of the gel (ASCII art for debugging).
     */
    public String visualizeGel(GelResult result) {
        StringBuilder sb = new StringBuilder();
        int width = result.lanes().size() * 8 + 2;
        int height = 20;
        
        // Header
        sb.append("=".repeat(width)).append("\n");
        sb.append("| ");
        for (int i = 0; i < result.lanes().size(); i++) {
            sb.append(String.format("L%-5d ", i + 1));
        }
        sb.append("|\n");
        sb.append("-".repeat(width)).append("\n");
        
        // Gel visualization
        for (int row = 0; row < height; row++) {
            double rowPosition = (double) row / height;
            sb.append("| ");
            
            for (List<GelBand> lane : result.lanes()) {
                boolean hasBand = false;
                for (GelBand band : lane) {
                    if (Math.abs(band.migrationDistance() - rowPosition) < 0.05) {
                        hasBand = true;
                        // Intensity visualization
                        if (band.intensity() > 0.7) sb.append("██████ ");
                        else if (band.intensity() > 0.4) sb.append("▓▓▓▓▓▓ ");
                        else sb.append("░░░░░░ ");
                        break;
                    }
                }
                if (!hasBand) {
                    sb.append("       ");
                }
            }
            sb.append("|\n");
        }
        
        sb.append("=".repeat(width)).append("\n");
        
        // Legend
        sb.append("Bands: ██=strong, ▓▓=medium, ░░=weak\n");
        
        return sb.toString();
    }
}
