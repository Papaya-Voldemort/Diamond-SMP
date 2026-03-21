package io.github.diamondsmp.platform.paper.service;

import java.io.File;
import java.io.IOException;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class DragonEggService {
    private final JavaPlugin plugin;
    private final File stateFile;
    private boolean specialRulesEnabled;

    public DragonEggService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "dragon-egg.yml");
        this.specialRulesEnabled = loadState();
    }

    public boolean specialRulesEnabled() {
        return specialRulesEnabled;
    }

    public void setSpecialRulesEnabled(boolean specialRulesEnabled) {
        this.specialRulesEnabled = specialRulesEnabled;
        saveState();
    }

    public boolean hasDragonEgg(Player player) {
        return countDragonEggs(player) > 0;
    }

    public int countDragonEggs(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public int removeDragonEggs(Player player) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int index = 0; index < contents.length; index++) {
            ItemStack item = contents[index];
            if (item == null || item.getType() != Material.DRAGON_EGG) {
                continue;
            }
            removed += item.getAmount();
            contents[index] = null;
        }
        player.getInventory().setContents(contents);
        return removed;
    }

    private boolean loadState() {
        if (!stateFile.exists()) {
            return true;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);
        return config.getBoolean("special-rules-enabled", true);
    }

    private void saveState() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("special-rules-enabled", specialRulesEnabled);
        try {
            config.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save dragon-egg.yml: " + exception.getMessage());
        }
    }
}
