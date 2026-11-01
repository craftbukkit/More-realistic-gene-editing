package com.morerealisticgeneediting.equipment;

/** Pure teaching-model calculations. None of these scores is a clinical prediction. */
public final class MeasurementModels {
    private MeasurementModels() { }
    public record OpticalReading(float a260, float a280, float a230, float concentration,
                                 float ratio260280, float ratio260230) { }
    public record DeliveryOutcome(float deliveryFraction, float survivingFraction) { }
    public static boolean supportsWavelength(int minimum, int maximum, int wavelength) {
        return minimum <= wavelength && wavelength <= maximum;
    }
    private static float observation(float sample, float blank, boolean supported) {
        return supported && Float.isFinite(sample) && Float.isFinite(blank)
                ? Math.max(0, sample - blank) : Float.NaN;
    }
    private static float ratio(float numerator, float denominator) {
        return Float.isFinite(numerator) && Float.isFinite(denominator) && denominator > 0
                ? numerator / denominator : Float.NaN;
    }
    public static OpticalReading optical(String type, float a260, float a280, float a230,
                                         float blank260, float blank280, float blank230,
                                         int minimum, int maximum) {
        float x = observation(a260, blank260, supportsWavelength(minimum, maximum, 260));
        float y = observation(a280, blank280, supportsWavelength(minimum, maximum, 280));
        float z = observation(a230, blank230, supportsWavelength(minimum, maximum, 230));
        float coefficient = switch (type) {
            case "dsDNA", "virtual_dna_aliquot" -> 50;
            case "ssDNA" -> 33;
            case "RNA" -> 40;
            default -> Float.NaN; // Protein requires its own extinction coefficient.
        };
        return new OpticalReading(x, y, z, x * coefficient, ratio(x,y), ratio(x,z));
    }
    /** Unitless game dose; separate delivery and survival, never infer editing or colonies. */
    public static DeliveryOutcome delivery(float driveFraction, int pulseCount) {
        if (!Float.isFinite(driveFraction) || driveFraction < 0 || driveFraction > 1
                || pulseCount < 1 || pulseCount > 10) throw new IllegalArgumentException("Invalid simulated pulse");
        float dose = driveFraction * pulseCount;
        return new DeliveryOutcome((float) (1 - Math.exp(-dose)), (float) Math.exp(-dose * .35));
    }
    public static int cycleCount(int readLength, boolean pairedEnd) {
        if (readLength < 1 || readLength > 150) throw new IllegalArgumentException("Invalid read length");
        return readLength * (pairedEnd ? 2 : 1);
    }
    public static float[] cycleQuality(java.util.List<int[]> reads, int readLength, boolean pairedEnd) {
        int cycles = cycleCount(readLength, pairedEnd);
        double[] sum = new double[cycles];
        int[] counts = new int[cycles];
        for (int read = 0; read < reads.size(); read++) {
            int offset = pairedEnd && read % 2 == 1 ? readLength : 0;
            int[] qualities = reads.get(read);
            for (int i = 0; i < Math.min(readLength, qualities.length); i++) {
                sum[offset+i] += qualities[i]; counts[offset+i]++;
            }
        }
        float[] result = new float[cycles];
        for (int i = 0; i < cycles; i++) result[i] = counts[i] == 0 ? Float.NaN : (float)(sum[i]/counts[i]);
        return result;
    }
}
