package io.github.diamondsmp.platform.paper.villager;

public enum VillagerType {
    TOP_ARMOR("top"),
    BOTTOM_ARMOR("bottom"),
    TOOLS("tools");

    private final String key;

    VillagerType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static VillagerType fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.replace("-", "").replace("_", "").toLowerCase();
        for (VillagerType value : values()) {
            String enumKey = value.name().replace("_", "").toLowerCase();
            String shortKey = value.key.replace("-", "").replace("_", "").toLowerCase();
            if (shortKey.equals(normalized) || enumKey.equals(normalized)) {
                return value;
            }
        }
        return switch (normalized) {
            case "toparmor", "helmetchest", "toparmour" -> TOP_ARMOR;
            case "bottomarmor", "leggingsboots", "bottomarmour" -> BOTTOM_ARMOR;
            case "toolsweapons", "weapons", "toolweapon" -> TOOLS;
            default -> null;
        };
    }
}
