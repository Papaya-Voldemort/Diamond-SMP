package io.github.diamondsmp.platform.paper.beacon;

import java.util.Arrays;
import java.util.Locale;

public enum GodBeaconTier {
    VANILLA(0, "vanilla", "Vanilla Beacon", 1.0D, 80, 0, false, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
    INFUSED(1, "infused", "Infused Beacon", 1.10D, 60, 0, false, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
    RESONANT(2, "resonant", "Resonant Beacon", 1.20D, 40, 0, false, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
    ASCENDED(3, "ascended", "Ascended Beacon", 1.30D, 40, 5, false, 0.20D, 0.10D, 0.0D, 0.0D, 0.0D),
    GOD(4, "god", "God Beacon", 1.40D, 40, 8, true, 0.25D, 0.20D, 0.15D, 0.10D, 0.10D);

    private final int level;
    private final String key;
    private final String displayName;
    private final double radiusMultiplier;
    private final int reapplyTicks;
    private final int lingerSeconds;
    private final boolean requiresFullPyramid;
    private final double regenerationBonus;
    private final double hungerReduction;
    private final double durabilityEfficiency;
    private final double hasteBonus;
    private final double smeltingChance;

    GodBeaconTier(
        int level,
        String key,
        String displayName,
        double radiusMultiplier,
        int reapplyTicks,
        int lingerSeconds,
        boolean requiresFullPyramid,
        double regenerationBonus,
        double hungerReduction,
        double durabilityEfficiency,
        double hasteBonus,
        double smeltingChance
    ) {
        this.level = level;
        this.key = key;
        this.displayName = displayName;
        this.radiusMultiplier = radiusMultiplier;
        this.reapplyTicks = reapplyTicks;
        this.lingerSeconds = lingerSeconds;
        this.requiresFullPyramid = requiresFullPyramid;
        this.regenerationBonus = regenerationBonus;
        this.hungerReduction = hungerReduction;
        this.durabilityEfficiency = durabilityEfficiency;
        this.hasteBonus = hasteBonus;
        this.smeltingChance = smeltingChance;
    }

    public int level() {
        return level;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public double radiusMultiplier() {
        return radiusMultiplier;
    }

    public int reapplyTicks() {
        return reapplyTicks;
    }

    public int lingerSeconds() {
        return lingerSeconds;
    }

    public boolean requiresFullPyramid() {
        return requiresFullPyramid;
    }

    public double regenerationBonus() {
        return regenerationBonus;
    }

    public double hungerReduction() {
        return hungerReduction;
    }

    public double durabilityEfficiency() {
        return durabilityEfficiency;
    }

    public double hasteBonus() {
        return hasteBonus;
    }

    public double smeltingChance() {
        return smeltingChance;
    }

    public boolean supportsSecondarySelection() {
        return level >= 2;
    }

    public GodBeaconTier next() {
        return switch (this) {
            case VANILLA -> INFUSED;
            case INFUSED -> RESONANT;
            case RESONANT -> ASCENDED;
            case ASCENDED -> GOD;
            case GOD -> null;
        };
    }

    public static GodBeaconTier fromLevel(int level) {
        return Arrays.stream(values())
            .filter(value -> value.level == level)
            .findFirst()
            .orElse(VANILLA);
    }

    public static GodBeaconTier fromKey(String key) {
        if (key == null || key.isBlank()) {
            return VANILLA;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(value -> value.key.equals(normalized))
            .findFirst()
            .orElse(VANILLA);
    }
}
