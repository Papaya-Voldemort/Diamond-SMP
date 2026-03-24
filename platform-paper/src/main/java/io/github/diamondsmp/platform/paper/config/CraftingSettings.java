package io.github.diamondsmp.platform.paper.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record CraftingSettings(
    boolean easyGaps,
    boolean easyCobs,
    PrestigeGodApple prestigeGodApple
) {
    public static CraftingSettings load(FileConfiguration config) {
        ConfigurationSection recipes = config.getConfigurationSection("recipes");
        if (recipes == null) {
            return defaults();
        }
        PrestigeGodApple defaults = defaults().prestigeGodApple();
        ConfigurationSection prestige = recipes.getConfigurationSection("prestige-god-apple");
        return new CraftingSettings(
            recipes.getBoolean("easy-gaps", true),
            recipes.getBoolean("easy-cobs", true),
            new PrestigeGodApple(
                prestige == null || prestige.getBoolean("enabled", defaults.enabled()),
                materialOrFallback(prestige == null ? null : prestige.getString("center-item"), defaults.centerItem()),
                materialOrFallback(prestige == null ? null : prestige.getString("surround-item"), defaults.surroundItem()),
                Math.max(1, prestige == null ? defaults.surroundStackSize() : prestige.getInt("surround-stack-size", defaults.surroundStackSize())),
                Math.max(1, prestige == null ? defaults.resultAmount() : prestige.getInt("result-amount", defaults.resultAmount())),
                prestige == null || prestige.getBoolean("exact-stacks-required", defaults.exactStacksRequired())
            )
        );
    }

    public static CraftingSettings defaults() {
        return new CraftingSettings(
            true,
            true,
            new PrestigeGodApple(true, Material.GOLDEN_APPLE, Material.DIAMOND_BLOCK, 64, 1, true)
        );
    }

    private static Material materialOrFallback(String value, Material fallback) {
        Material material = Material.matchMaterial(value == null ? "" : value);
        return material == null ? fallback : material;
    }

    public record PrestigeGodApple(
        boolean enabled,
        Material centerItem,
        Material surroundItem,
        int surroundStackSize,
        int resultAmount,
        boolean exactStacksRequired
    ) {}
}
