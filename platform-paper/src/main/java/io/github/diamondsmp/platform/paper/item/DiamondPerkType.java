package io.github.diamondsmp.platform.paper.item;

import java.util.Locale;
import org.bukkit.Material;

public enum DiamondPerkType {
    PROSPECTOR("prospector", "Prospector", Material.ENCHANTED_BOOK, "Mining-focused late-game utility"),
    BULWARK("bulwark", "Bulwark", Material.ENCHANTED_BOOK, "Survival defense without full god-item power"),
    MOMENTUM("momentum", "Momentum", Material.ENCHANTED_BOOK, "Movement utility for rich late-game players");

    private final String key;
    private final String displayName;
    private final Material material;
    private final String description;

    DiamondPerkType(String key, String displayName, Material material, String description) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }

    public String description() {
        return description;
    }

    public static DiamondPerkType fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        for (DiamondPerkType value : values()) {
            if (value.key.replace("_", "").equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
