package com.morerealisticgeneediting.equipment;

/**
 * Equipment tier system for laboratory equipment.
 * 
 * Tier 1 (基础/Basic): Entry-level, DIY or basic equipment
 * Tier 2 (进阶/Advanced): Professional-grade benchtop equipment
 * Tier 3 (尖端/Elite): High-end, automated or multi-block systems
 */
public enum EquipmentTier {
    BASIC(1, "basic", 0x4CAF50, 1.0f),      // Green - 基础
    ADVANCED(2, "advanced", 0x2196F3, 1.5f), // Blue - 进阶
    ELITE(3, "elite", 0x9C27B0, 2.0f);       // Purple - 尖端
    
    private final int level;
    private final String id;
    private final int color;
    private final float efficiencyMultiplier;
    
    EquipmentTier(int level, String id, int color, float efficiencyMultiplier) {
        this.level = level;
        this.id = id;
        this.color = color;
        this.efficiencyMultiplier = efficiencyMultiplier;
    }
    
    public int getLevel() { return level; }
    public String getId() { return id; }
    public int getColor() { return color; }
    public float getEfficiencyMultiplier() { return efficiencyMultiplier; }
    
    /**
     * Get processing time multiplier (higher tier = faster)
     */
    public float getSpeedMultiplier() {
        return switch (this) {
            case BASIC -> 1.0f;
            case ADVANCED -> 0.7f;
            case ELITE -> 0.4f;
        };
    }
    
    /**
     * Get success rate bonus
     */
    public float getSuccessRateBonus() {
        return switch (this) {
            case BASIC -> 0.0f;
            case ADVANCED -> 0.1f;
            case ELITE -> 0.2f;
        };
    }
    
    /**
     * Get energy consumption multiplier
     */
    public float getEnergyMultiplier() {
        return switch (this) {
            case BASIC -> 1.0f;
            case ADVANCED -> 1.5f;
            case ELITE -> 2.5f;
        };
    }
}
