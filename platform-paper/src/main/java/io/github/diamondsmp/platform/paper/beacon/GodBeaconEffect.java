package io.github.diamondsmp.platform.paper.beacon;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.potion.PotionEffectType;

public enum GodBeaconEffect {
    SPEED("speed", "Speed", PotionEffectType.SPEED),
    HASTE("haste", "Haste", PotionEffectType.HASTE),
    RESISTANCE("resistance", "Resistance", PotionEffectType.RESISTANCE),
    JUMP_BOOST("jump_boost", "Jump Boost", PotionEffectType.JUMP_BOOST),
    STRENGTH("strength", "Strength", PotionEffectType.STRENGTH),
    REGENERATION("regeneration", "Regeneration", PotionEffectType.REGENERATION);

    private final String key;
    private final String displayName;
    private final PotionEffectType effectType;

    GodBeaconEffect(String key, String displayName, PotionEffectType effectType) {
        this.key = key;
        this.displayName = displayName;
        this.effectType = effectType;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public PotionEffectType effectType() {
        return effectType;
    }

    public static GodBeaconEffect fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(value -> value.key.equals(normalized))
            .findFirst()
            .orElse(null);
    }

    public static GodBeaconEffect fromPotion(PotionEffectType type) {
        if (type == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(value -> value.effectType.equals(type))
            .findFirst()
            .orElse(null);
    }
}
