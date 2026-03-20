package io.github.diamondsmp.platform.paper.config;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record RulesBook(String title, List<Section> sections) {
    public static RulesBook load(FileConfiguration config) {
        String title = color(config.getString("title", "&6Diamond SMP Rules"));
        List<Section> sections = new ArrayList<>();
        ConfigurationSection root = config.getConfigurationSection("sections");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                sections.add(new Section(
                    color(section.getString("title", key)),
                    color(section.getString("icon", "&f")),
                    section.getStringList("lines").stream().map(RulesBook::color).toList()
                ));
            }
        }
        return new RulesBook(title, sections);
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public record Section(String title, String icon, List<String> lines) {}
}
