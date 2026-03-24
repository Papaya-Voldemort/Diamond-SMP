package io.github.diamondsmp.platform.paper.villager;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.item.DiamondPerkRegistry;
import io.github.diamondsmp.platform.paper.item.DiamondPerkType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiamondMasterTradeService {
    private final PluginSettings settings;
    private final DiamondPerkRegistry perks;
    private final GodVillagerService godVillagers;
    private final NamespacedKey initializedKey;
    private final List<ConfiguredTrade> configuredTrades;

    public DiamondMasterTradeService(
        JavaPlugin plugin,
        PluginSettings settings,
        DiamondPerkRegistry perks,
        GodVillagerService godVillagers,
        FileConfiguration villagersConfig
    ) {
        this.settings = settings;
        this.perks = perks;
        this.godVillagers = godVillagers;
        this.initializedKey = new NamespacedKey(plugin, "master-diamond-trades-initialized");
        this.configuredTrades = loadConfiguredTrades(villagersConfig.getConfigurationSection("master-diamond-trades"));
    }

    public void ensureTrades(Villager villager) {
        if (villager.getVillagerLevel() < 5 || godVillagers.isManagedVillager(villager)) {
            return;
        }
        if (villager.getPersistentDataContainer().has(initializedKey, PersistentDataType.BYTE)) {
            return;
        }
        villager.getPersistentDataContainer().set(initializedKey, PersistentDataType.BYTE, (byte) 1);
        if (configuredTrades.isEmpty() || ThreadLocalRandom.current().nextDouble() > settings.villagers().masterDiamondTradeChance()) {
            return;
        }
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        ConfiguredTrade selected = configuredTrades.get(ThreadLocalRandom.current().nextInt(configuredTrades.size()));
        recipes.add(selected.toRecipe(perks));
        villager.setRecipes(recipes);
    }

    private List<ConfiguredTrade> loadConfiguredTrades(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<ConfiguredTrade> trades = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection trade = section.getConfigurationSection(key);
            if (trade == null) {
                continue;
            }
            Material currency = materialOrFallback(trade.getString("currency"), Material.DIAMOND);
            int currencyAmount = Math.max(1, trade.getInt("currency-amount", 1));
            int maxUses = Math.max(1, trade.getInt("max-uses", 1));
            int villagerExperience = Math.max(0, trade.getInt("villager-experience", 0));
            ConfigurationSection result = trade.getConfigurationSection("result");
            if (result == null) {
                continue;
            }
            trades.add(new ConfiguredTrade(
                key.toLowerCase(Locale.ROOT),
                currency,
                currencyAmount,
                maxUses,
                villagerExperience,
                result.getString("perk-book"),
                materialOrFallback(result.getString("material"), Material.AIR),
                Math.max(1, result.getInt("amount", 1)),
                result.getString("name"),
                result.getStringList("lore")
            ));
        }
        return List.copyOf(trades);
    }

    private Material materialOrFallback(String value, Material fallback) {
        Material material = Material.matchMaterial(value == null ? "" : value);
        return material == null ? fallback : material;
    }

    private record ConfiguredTrade(
        String key,
        Material currency,
        int currencyAmount,
        int maxUses,
        int villagerExperience,
        String perkBookKey,
        Material material,
        int amount,
        String name,
        List<String> lore
    ) {
        MerchantRecipe toRecipe(DiamondPerkRegistry perks) {
            ItemStack result = buildResult(perks);
            MerchantRecipe recipe = new MerchantRecipe(result, 0, maxUses, true, villagerExperience, 0.0F);
            recipe.addIngredient(new ItemStack(currency, currencyAmount));
            return recipe;
        }

        private ItemStack buildResult(DiamondPerkRegistry perks) {
            DiamondPerkType perk = DiamondPerkType.fromKey(perkBookKey);
            ItemStack result = perk != null ? perks.createBook(perk) : new ItemStack(material, amount);
            if ((name == null || name.isBlank()) && lore.isEmpty()) {
                return result;
            }
            ItemMeta meta = result.getItemMeta();
            if (name != null && !name.isBlank()) {
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
            }
            if (!lore.isEmpty()) {
                meta.lore(lore.stream()
                    .map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                    .map(component -> component == null ? Component.empty() : component)
                    .toList());
            }
            result.setItemMeta(meta);
            return result;
        }
    }
}
