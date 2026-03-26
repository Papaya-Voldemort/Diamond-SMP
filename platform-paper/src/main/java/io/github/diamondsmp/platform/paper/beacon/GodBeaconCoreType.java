package io.github.diamondsmp.platform.paper.beacon;

import org.bukkit.Material;

public enum GodBeaconCoreType {
    INFUSION("infusion", "Infusion Core", GodBeaconTier.VANILLA, GodBeaconTier.INFUSED, 128, 1),
    RESONANCE("resonance", "Resonance Core", GodBeaconTier.INFUSED, GodBeaconTier.RESONANT, 256, 2),
    ASCENSION("ascension", "Ascension Core", GodBeaconTier.RESONANT, GodBeaconTier.ASCENDED, 512, 4),
    GOD("god", "God Core", GodBeaconTier.ASCENDED, GodBeaconTier.GOD, 1024, 8);

    private final String key;
    private final String displayName;
    private final GodBeaconTier requiredTier;
    private final GodBeaconTier resultTier;
    private final int diamondBlockCost;
    private final int netherStarCost;

    GodBeaconCoreType(String key, String displayName, GodBeaconTier requiredTier, GodBeaconTier resultTier, int diamondBlockCost, int netherStarCost) {
        this.key = key;
        this.displayName = displayName;
        this.requiredTier = requiredTier;
        this.resultTier = resultTier;
        this.diamondBlockCost = diamondBlockCost;
        this.netherStarCost = netherStarCost;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public GodBeaconTier requiredTier() {
        return requiredTier;
    }

    public GodBeaconTier resultTier() {
        return resultTier;
    }

    public int diamondBlockCost() {
        return diamondBlockCost;
    }

    public int netherStarCost() {
        return netherStarCost;
    }

    public Material displayMaterial() {
        return switch (this) {
            case INFUSION, RESONANCE, ASCENSION -> Material.NETHER_STAR;
            case GOD -> Material.BEACON;
        };
    }

    public static GodBeaconCoreType fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (GodBeaconCoreType value : values()) {
            if (value.key.equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
