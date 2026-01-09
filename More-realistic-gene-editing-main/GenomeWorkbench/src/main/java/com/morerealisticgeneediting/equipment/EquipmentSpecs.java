package com.morerealisticgeneediting.equipment;

import java.util.HashMap;
import java.util.Map;

/**
 * Equipment Specifications based on real laboratory equipment.
 * 
 * This class contains detailed specifications derived from actual equipment manuals:
 * - DLAB D1008 Palm Micro Centrifuge
 * - Haier LX-165T2R Refrigerated Centrifuge
 * - Beckman Coulter Optima XPN-100 Ultracentrifuge
 * - Bio-Rad T100 Thermal Cycler
 * - Roche LightCycler 480 II
 * - Oxford Nanopore MinION Mk1C
 * - Thermo NanoDrop One
 * - Bio-Rad Gene Pulser Xcell
 * - And more...
 */
public final class EquipmentSpecs {

    private EquipmentSpecs() {}

    // ==================== CENTRIFUGE SPECIFICATIONS ====================
    
    /**
     * DLAB D1008 Palm Micro Centrifuge
     * - Max Speed: 7000 rpm
     * - Max RCF: 2680 × g
     * - Capacity: 8 × 0.2ml or 2 × 1.5ml
     * - No temperature control
     */
    public static final CentrifugeSpec PALM_CENTRIFUGE = new CentrifugeSpec(
        "D1008 Palm Micro Centrifuge",
        7000,      // max RPM
        2680,      // max RCF (× g)
        8,         // capacity (tubes)
        0.2f,      // max tube volume (ml)
        false,     // has temperature control
        20, 20,    // temp range (fixed at room temp)
        15,        // processing time (seconds per cycle)
        0.7f       // base success rate
    );

    /**
     * Haier LX-165T2R Refrigerated Centrifuge
     * - Max Speed: 16500 rpm
     * - Max RCF: 24140 × g
     * - Capacity: 24 × 1.5ml or 4 × 50ml
     * - Temperature: -20°C to +40°C
     */
    public static final CentrifugeSpec REFRIGERATED_CENTRIFUGE = new CentrifugeSpec(
        "LX-165T2R Refrigerated Centrifuge",
        16500,     // max RPM
        24140,     // max RCF
        24,        // capacity
        1.5f,      // max tube volume
        true,      // has temperature control
        -20, 40,   // temp range
        10,        // faster processing
        0.85f      // higher success rate
    );

    /**
     * Beckman Coulter Optima XPN-100 Ultracentrifuge
     * - Max Speed: 100000 rpm
     * - Max RCF: 802000 × g
     * - Capacity: varies by rotor
     * - Temperature: 0°C to +40°C
     * - Vacuum operation
     */
    public static final CentrifugeSpec ULTRACENTRIFUGE = new CentrifugeSpec(
        "Optima XPN-100 Ultracentrifuge",
        100000,    // max RPM
        802000,    // max RCF
        6,         // typical capacity
        5.0f,      // max tube volume
        true,      // has temperature control
        0, 40,     // temp range
        8,         // very fast
        0.95f      // excellent success rate
    );

    // ==================== THERMAL CYCLER SPECIFICATIONS ====================

    /**
     * OpenPCR / Chai Open qPCR
     * - Block capacity: 16 × 0.2ml
     * - Temperature range: 4-99°C
     * - Ramp rate: ~1°C/s
     * - No gradient
     */
    public static final ThermalCyclerSpec OPEN_PCR = new ThermalCyclerSpec(
        "OpenPCR",
        16,        // well count
        4, 99,     // temp range
        1.0f,      // ramp rate °C/s
        false,     // no gradient
        false,     // no real-time
        0.1f       // basic precision
    );

    /**
     * Bio-Rad T100 Thermal Cycler
     * - Block: 96 × 0.2ml
     * - Temperature range: 0-100°C
     * - Ramp rate: 3°C/s
     * - Gradient: 1-25°C across block
     */
    public static final ThermalCyclerSpec T100_THERMAL_CYCLER = new ThermalCyclerSpec(
        "T100 Thermal Cycler",
        96,        // well count
        0, 100,    // temp range
        3.0f,      // ramp rate
        true,      // has gradient
        false,     // no real-time
        0.05f      // good precision
    );

    /**
     * Roche LightCycler 480 II
     * - Block: 96 or 384 wells
     * - Temperature range: 37-95°C
     * - Ramp rate: 4.8°C/s
     * - Real-time detection with 6 channels
     */
    public static final ThermalCyclerSpec LIGHTCYCLER_480 = new ThermalCyclerSpec(
        "LightCycler 480 II",
        384,       // well count (max)
        37, 95,    // temp range
        4.8f,      // ramp rate
        true,      // has gradient
        true,      // real-time detection
        0.02f      // excellent precision
    );

    // ==================== ELECTROPHORESIS SPECIFICATIONS ====================

    /**
     * blueGel Portable Electrophoresis
     * - Gel size: Mini (5 × 5 cm)
     * - Voltage: 48V fixed
     * - Run time: 20-30 min
     * - Built-in transilluminator
     */
    public static final ElectrophoresisSpec BLUEGEL_PORTABLE = new ElectrophoresisSpec(
        "blueGel Electrophoresis",
        8,         // well count
        48,        // max voltage
        false,     // not adjustable
        true,      // has transilluminator
        30,        // run time minutes
        500        // max DNA size (bp) for good resolution
    );

    /**
     * DYCZ-24DN Vertical Electrophoresis
     * - Gel size: Standard (10 × 10 cm)
     * - Voltage: up to 300V
     * - Requires separate power supply
     */
    public static final ElectrophoresisSpec VERTICAL_ELECTROPHORESIS = new ElectrophoresisSpec(
        "DYCZ-24DN Vertical",
        24,        // well count
        300,       // max voltage
        true,      // adjustable
        false,     // external transilluminator
        60,        // run time
        10000      // max DNA size
    );

    /**
     * Agilent 4200 TapeStation
     * - Fully automated
     * - ScreenTape technology
     * - 96 samples per run
     * - Digital results
     */
    public static final ElectrophoresisSpec TAPESTATION_4200 = new ElectrophoresisSpec(
        "4200 TapeStation",
        96,        // samples per run
        0,         // automated (no manual voltage)
        false,     // fully automated
        true,      // integrated detection
        3,         // very fast (minutes per sample)
        60000      // excellent range
    );

    // ==================== SEQUENCER SPECIFICATIONS ====================

    /**
     * Oxford Nanopore MinION Mk1C
     * - Flow cells: 1
     * - Read length: Up to 2Mb+
     * - Throughput: ~50 Gb per flow cell
     * - Real-time sequencing
     * - Portable (handheld)
     */
    public static final SequencerSpec MINION_MK1C = new SequencerSpec(
        "MinION Mk1C",
        1,         // flow cells
        2_000_000, // max read length
        50,        // throughput Gb
        true,      // real-time
        true,      // portable
        "nanopore" // technology
    );

    /**
     * MGI DNBSEQ-G99
     * - Flow cells: 2
     * - Read length: 50-150bp
     * - Throughput: 720 Gb per run
     * - DNB sequencing technology
     */
    public static final SequencerSpec DNBSEQ_G99 = new SequencerSpec(
        "DNBSEQ-G99",
        2,         // flow cells
        150,       // max read length
        720,       // throughput Gb
        false,     // batch mode
        false,     // benchtop
        "dnbseq"   // technology
    );

    // ==================== SPECTROPHOTOMETER SPECIFICATIONS ====================

    /**
     * 721 Visible Spectrophotometer
     * - Wavelength: 340-1000 nm
     * - Sample volume: 3 ml cuvette
     * - Basic absorbance readings
     */
    public static final SpectrophotometerSpec VISIBLE_721 = new SpectrophotometerSpec(
        "721 Visible Spectrophotometer",
        340, 1000, // wavelength range
        3.0f,      // sample volume ml
        false,     // not micro-volume
        0.01f      // precision
    );

    /**
     * Thermo NanoDrop One
     * - Wavelength: 190-850 nm
     * - Sample volume: 0.5-2 μl
     * - Direct measurement (no cuvette)
     * - Auto-blanking
     */
    public static final SpectrophotometerSpec NANODROP_ONE = new SpectrophotometerSpec(
        "NanoDrop One",
        190, 850,  // wavelength range
        0.002f,    // sample volume ml (2 μl)
        true,      // micro-volume
        0.001f     // excellent precision
    );

    /**
     * Unchained Labs Lunatic
     * - 96-well plate format
     * - Sample volume: 2 μl
     * - Full spectrum in seconds
     * - High throughput
     */
    public static final SpectrophotometerSpec LUNATIC = new SpectrophotometerSpec(
        "Lunatic",
        230, 750,  // wavelength range
        0.002f,    // sample volume
        true,      // micro-volume
        0.0005f    // best precision
    );

    // ==================== ELECTROPORATOR SPECIFICATIONS ====================

    /**
     * Bio-Rad Gene Pulser Xcell
     * - Voltage: 10-3000V
     * - Capacitance: 0.5-200 μF
     * - Pulse types: Exponential, Square wave
     * - Cuvettes: 1mm, 2mm, 4mm
     */
    public static final ElectroporatorSpec GENE_PULSER_XCELL = new ElectroporatorSpec(
        "Gene Pulser Xcell",
        10, 3000,  // voltage range
        500,       // max capacitance μF
        true,      // has square wave
        new int[]{1, 2, 4}  // cuvette sizes mm
    );

    // ==================== INCUBATOR SPECIFICATIONS ====================

    /**
     * DIY Styrofoam Incubator
     * - Temperature: Room temp to ~40°C
     * - No CO2 control
     * - No humidity control
     * - Basic monitoring
     */
    public static final IncubatorSpec DIY_INCUBATOR = new IncubatorSpec(
        "DIY Incubator",
        25, 42,    // temp range
        false,     // no CO2
        false,     // no humidity
        false,     // no shaking
        0          // no shaking speed
    );

    /**
     * Yiheng THZ-300C Shaking Incubator
     * - Temperature: 4-60°C
     * - Shaking: 30-300 rpm
     * - Timer: 0-9999 min
     */
    public static final IncubatorSpec SHAKING_INCUBATOR = new IncubatorSpec(
        "THZ-300C Shaking Incubator",
        4, 60,     // temp range
        false,     // no CO2
        false,     // no humidity
        true,      // has shaking
        300        // max shaking rpm
    );

    /**
     * Sartorius Ambr 250 Modular
     * - Precise temperature control
     * - pH, DO monitoring
     * - Automated sampling
     * - 24 parallel bioreactors
     */
    public static final IncubatorSpec AMBR_250 = new IncubatorSpec(
        "Ambr 250 Modular",
        15, 45,    // temp range
        true,      // has CO2/gas control
        true,      // has humidity
        true,      // has stirring
        2000       // max stirring rpm
    );

    // ==================== BIOSAFETY CABINET SPECIFICATIONS ====================

    /**
     * Simple Laminar Flow Hood
     * - HEPA filtered
     * - Product protection only
     * - No recirculation
     */
    public static final BiosafetySpec LAMINAR_FLOW_HOOD = new BiosafetySpec(
        "Laminar Flow Hood",
        0,         // class (not a BSC)
        true,      // HEPA filter
        false,     // no UV
        0.3f       // low protection factor
    );

    /**
     * Esco Airstream Class II BSC
     * - Class II Type A2
     * - 70% recirculation
     * - UV lamp
     * - Personnel and product protection
     */
    public static final BiosafetySpec BIOSAFETY_CABINET_II = new BiosafetySpec(
        "Class II Biosafety Cabinet",
        2,         // Class II
        true,      // HEPA
        true,      // has UV
        0.7f       // good protection
    );

    /**
     * Esco Isoclean Isolator
     * - Complete containment
     * - Positive or negative pressure
     * - Sterility assurance
     */
    public static final BiosafetySpec ISOLATOR = new BiosafetySpec(
        "Isoclean Isolator",
        3,         // Class III equivalent
        true,      // HEPA
        true,      // has UV
        0.99f      // excellent protection
    );

    // ==================== RECORD CLASSES ====================

    public record CentrifugeSpec(
        String name,
        int maxRpm,
        int maxRcf,
        int capacity,
        float maxTubeVolume,
        boolean hasTemperatureControl,
        int minTemp,
        int maxTemp,
        int processingTime,
        float successRate
    ) {}

    public record ThermalCyclerSpec(
        String name,
        int wellCount,
        int minTemp,
        int maxTemp,
        float rampRate,
        boolean hasGradient,
        boolean hasRealTimeDetection,
        float precision
    ) {}

    public record ElectrophoresisSpec(
        String name,
        int wellCount,
        int maxVoltage,
        boolean adjustable,
        boolean hasTransilluminator,
        int runTimeMinutes,
        int maxDnaSize
    ) {}

    public record SequencerSpec(
        String name,
        int flowCells,
        int maxReadLength,
        int throughputGb,
        boolean realTime,
        boolean portable,
        String technology
    ) {}

    public record SpectrophotometerSpec(
        String name,
        int minWavelength,
        int maxWavelength,
        float sampleVolumeML,
        boolean microVolume,
        float precision
    ) {}

    public record ElectroporatorSpec(
        String name,
        int minVoltage,
        int maxVoltage,
        int maxCapacitance,
        boolean hasSquareWave,
        int[] cuvetteSizes
    ) {}

    public record IncubatorSpec(
        String name,
        int minTemp,
        int maxTemp,
        boolean hasCO2Control,
        boolean hasHumidityControl,
        boolean hasShaking,
        int maxShakingRpm
    ) {}

    public record BiosafetySpec(
        String name,
        int safetyClass,
        boolean hasHepa,
        boolean hasUV,
        float protectionFactor
    ) {}

    // ==================== SPEC RETRIEVAL ====================

    private static final Map<String, CentrifugeSpec> CENTRIFUGE_SPECS = new HashMap<>();
    private static final Map<String, ThermalCyclerSpec> THERMAL_CYCLER_SPECS = new HashMap<>();
    private static final Map<String, ElectrophoresisSpec> ELECTROPHORESIS_SPECS = new HashMap<>();

    static {
        CENTRIFUGE_SPECS.put("basic", PALM_CENTRIFUGE);
        CENTRIFUGE_SPECS.put("advanced", REFRIGERATED_CENTRIFUGE);
        CENTRIFUGE_SPECS.put("elite", ULTRACENTRIFUGE);

        THERMAL_CYCLER_SPECS.put("basic", OPEN_PCR);
        THERMAL_CYCLER_SPECS.put("advanced", T100_THERMAL_CYCLER);
        THERMAL_CYCLER_SPECS.put("elite", LIGHTCYCLER_480);

        ELECTROPHORESIS_SPECS.put("basic", BLUEGEL_PORTABLE);
        ELECTROPHORESIS_SPECS.put("advanced", VERTICAL_ELECTROPHORESIS);
        ELECTROPHORESIS_SPECS.put("elite", TAPESTATION_4200);
    }

    public static CentrifugeSpec getCentrifugeSpec(EquipmentTier tier) {
        return CENTRIFUGE_SPECS.get(tier.getId());
    }

    public static ThermalCyclerSpec getThermalCyclerSpec(EquipmentTier tier) {
        return THERMAL_CYCLER_SPECS.get(tier.getId());
    }

    public static ElectrophoresisSpec getElectrophoresisSpec(EquipmentTier tier) {
        return ELECTROPHORESIS_SPECS.get(tier.getId());
    }
}
