package io.github.diamondsmp.platform.paper.service;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PurchaseHistoryStore {
    private final File file;
    private final Set<String> purchasedItems = new HashSet<>();

    public PurchaseHistoryStore(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "purchase-history.yml");
        load();
    }

    public boolean wasPurchased(String itemKey) {
        return purchasedItems.contains(itemKey.toLowerCase());
    }

    public void markPurchased(String itemKey) {
        purchasedItems.add(itemKey.toLowerCase());
        save();
    }

    public void reset() {
        purchasedItems.clear();
        save();
    }

    public Set<String> snapshot() {
        return Set.copyOf(purchasedItems);
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        purchasedItems.clear();
        purchasedItems.addAll(yaml.getStringList("purchased"));
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("purchased", purchasedItems.stream().sorted().toList());
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save purchase history", exception);
        }
    }
}
