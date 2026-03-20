package io.github.diamondsmp.platform.paper.config;

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public final class MessageBundle {
    private final String prefix;
    private final Map<String, String> values;

    private MessageBundle(String prefix, Map<String, String> values) {
        this.prefix = color(prefix);
        this.values = values;
    }

    public static MessageBundle load(FileConfiguration config) {
        Map<String, String> entries = config.getKeys(true).stream()
            .filter(key -> !config.isConfigurationSection(key))
            .collect(java.util.stream.Collectors.toUnmodifiableMap(key -> key, key -> color(config.getString(key, ""))));
        return new MessageBundle(config.getString("prefix", "&b[DiamondSMP]&r "), entries);
    }

    public String get(String key, String fallback) {
        return values.getOrDefault(key, color(fallback));
    }

    public String format(String key, String fallback, Map<String, String> replacements) {
        String message = prefix + get(key, fallback);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String prefixed(String key, String fallback) {
        return prefix + get(key, fallback);
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
