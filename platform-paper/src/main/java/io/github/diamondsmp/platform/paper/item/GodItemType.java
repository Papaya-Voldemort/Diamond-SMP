package io.github.diamondsmp.platform.paper.item;

import org.bukkit.Material;

public enum GodItemType {
    HELMET("helmet", Material.NETHERITE_HELMET, "God Helmet"),
    CHESTPLATE("chestplate", Material.NETHERITE_CHESTPLATE, "God Chestplate"),
    LEGGINGS("leggings", Material.NETHERITE_LEGGINGS, "God Leggings"),
    BOOTS("boots", Material.NETHERITE_BOOTS, "God Boots"),
    SWORD("sword", Material.NETHERITE_SWORD, "God Sword"),
    AXE("axe", Material.NETHERITE_AXE, "God Axe"),
    PICKAXE("pickaxe", Material.NETHERITE_PICKAXE, "God Pickaxe"),
    BOW("bow", Material.BOW, "God Bow"),
    INFINITE_TOTEM("infinite_totem", Material.TOTEM_OF_UNDYING, "Infinite Totem"),
    ENCHANTED_GAPPLE("enchanted_gapple", Material.ENCHANTED_GOLDEN_APPLE, "Event Gapple");

    private final String key;
    private final Material material;
    private final String displayName;

    GodItemType(String key, Material material, String displayName) {
        this.key = key;
        this.material = material;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public Material material() {
        return material;
    }

    public String displayName() {
        return displayName;
    }

    public static GodItemType fromKey(String key) {
        for (GodItemType value : values()) {
            if (value.key.equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
