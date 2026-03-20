package io.github.diamondsmp.platform.paper.advancement;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MythicalAdvancementService {
    private final JavaPlugin plugin;
    private final MessageBundle messages;
    private final NamespacedKey rootKey;
    private final Map<MythicalAdvancement, NamespacedKey> advancementKeys = new EnumMap<>(MythicalAdvancement.class);

    public MythicalAdvancementService(JavaPlugin plugin, MessageBundle messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.rootKey = new NamespacedKey(plugin, "mythical/root");
        for (MythicalAdvancement advancement : MythicalAdvancement.values()) {
            advancementKeys.put(advancement, new NamespacedKey(plugin, "mythical/" + advancement.key()));
        }
        ensureLoaded();
    }

    public void unlock(Player player, MythicalAdvancement advancement) {
        award(player, rootKey);
        award(player, advancementKeys.get(advancement));
        player.sendMessage(messages.format("mythical.unlock", "&dMYTHICAL UNLOCKED: &f{title}", Map.of("title", advancement.title())));
    }

    public boolean isUnlocked(Player player, MythicalAdvancement advancement) {
        Advancement loaded = Bukkit.getAdvancement(advancementKeys.get(advancement));
        if (loaded == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancementProgress(loaded);
        return progress.isDone();
    }

    private void ensureLoaded() {
        loadIfMissing(rootKey, rootJson());
        for (MythicalAdvancement advancement : MythicalAdvancement.values()) {
            loadIfMissing(advancementKeys.get(advancement), advancement.json(plugin.getName().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private void loadIfMissing(NamespacedKey key, String json) {
        if (Bukkit.getAdvancement(key) != null) {
            return;
        }
        Bukkit.getUnsafe().loadAdvancement(key, json);
    }

    private void award(Player player, NamespacedKey key) {
        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            progress.awardCriteria(criterion);
        }
    }

    private String rootJson() {
        return """
            {
              "display": {
                "icon": { "id": "minecraft:nether_star" },
                "title": "Mythical",
                "description": "Collect mythical Diamond SMP gear.",
                "background": "minecraft:textures/gui/advancements/backgrounds/adventure.png",
                "frame": "task",
                "show_toast": false,
                "announce_to_chat": false,
                "hidden": true
              },
              "criteria": {
                "trigger": {
                  "trigger": "minecraft:impossible"
                }
              }
            }
            """;
    }

    public enum MythicalAdvancement {
        DEMI_GOD("demi-god", "Demi-God", "Own any piece of god armor.", Material.DIAMOND_HELMET, "task"),
        IS_HE_A_GOD("is-he-a-god", "Is He a God", "Own the full god armor set.", Material.DIAMOND_CHESTPLATE, "goal"),
        DIVINE_ARSENAL("divine-arsenal", "Divine Arsenal", "Own every god weapon and tool.", Material.BOW, "goal"),
        IMMOVABLE_OBJECT("immovable-object", "Immortal Problems", "Own the Infinite Totem and enchanted gapple.", Material.TOTEM_OF_UNDYING, "challenge"),
        MYTHIC_INCARNATE("mythic-incarnate", "Mythic Incarnate", "Own every mythical item.", Material.ENCHANTED_GOLDEN_APPLE, "challenge");

        private final String key;
        private final String title;
        private final String description;
        private final Material icon;
        private final String frame;

        MythicalAdvancement(String key, String title, String description, Material icon, String frame) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.frame = frame;
        }

        public String key() {
            return key;
        }

        public String title() {
            return title;
        }

        private String json(String namespace) {
            return """
                {
                  "parent": "%s:mythical/root",
                  "display": {
                    "icon": { "id": "minecraft:%s" },
                    "title": "%s",
                    "description": "%s",
                    "frame": "%s",
                    "show_toast": true,
                    "announce_to_chat": true,
                    "hidden": false
                  },
                  "criteria": {
                    "trigger": {
                      "trigger": "minecraft:impossible"
                    }
                  }
                }
                """.formatted(namespace, icon.getKey().getKey(), escape(title), escape(description), frame);
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
