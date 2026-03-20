package io.github.diamondsmp.platform.paper.villager;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.item.GodItemType;
import io.github.diamondsmp.platform.paper.service.PurchaseHistoryStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class GodVillagerService {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final GodItemRegistry items;
    private final PurchaseHistoryStore purchaseHistory;
    private final NamespacedKey villagerTypeKey;
    private final NamespacedKey eventKey;
    private final NamespacedKey expiresKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey ownerNameKey;
    private final NamespacedKey tradeItemKey;
    private final Map<VillagerType, List<TradeDefinition>> definitions;

    public GodVillagerService(
        JavaPlugin plugin,
        PluginSettings settings,
        GodItemRegistry items,
        PurchaseHistoryStore purchaseHistory,
        FileConfiguration villagersConfig
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.items = items;
        this.purchaseHistory = purchaseHistory;
        this.villagerTypeKey = new NamespacedKey(plugin, "god-villager-type");
        this.eventKey = new NamespacedKey(plugin, "god-villager-event");
        this.expiresKey = new NamespacedKey(plugin, "god-villager-expires-at");
        this.ownerKey = new NamespacedKey(plugin, "god-villager-owner");
        this.ownerNameKey = new NamespacedKey(plugin, "god-villager-owner-name");
        this.tradeItemKey = new NamespacedKey(plugin, "god-villager-trade-item");
        this.definitions = loadDefinitions(villagersConfig);
        startFollowTask();
    }

    public Villager spawnRewardVillager(Player winner, VillagerType type, String eventName) {
        return spawn(winner, type, eventName);
    }

    public Villager spawn(Player player, VillagerType type, String eventName) {
        Villager villager = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
        villager.setAI(true);
        villager.setInvulnerable(true);
        villager.setCustomNameVisible(true);
        villager.setRemoveWhenFarAway(false);
        villager.setCollidable(false);
        villager.setCanPickupItems(false);
        villager.setSilent(true);
        villager.setBreed(false);
        villager.setAware(true);
        villager.getPersistentDataContainer().set(villagerTypeKey, PersistentDataType.STRING, type.key());
        villager.getPersistentDataContainer().set(eventKey, PersistentDataType.STRING, eventName.toLowerCase(Locale.ROOT));
        villager.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        villager.getPersistentDataContainer().set(ownerNameKey, PersistentDataType.STRING, player.getName());
        long expiresAt = Instant.now().plus(settings.villagers().despawnAfter()).toEpochMilli();
        villager.getPersistentDataContainer().set(expiresKey, PersistentDataType.LONG, expiresAt);
        villager.customName(net.kyori.adventure.text.Component.text(compactTitle(player.getName(), type)));
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerLevel(5);
        villager.setVillagerExperience(0);
        villager.setRecipes(buildRecipes(type));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (villager.isValid()) {
                villager.remove();
            }
        }, settings.villagers().despawnAfter().toSeconds() * 20L);
        return villager;
    }

    public void resetPurchases() {
        purchaseHistory.reset();
    }

    public int despawnActiveVillagers() {
        int removed = 0;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (!isManagedVillager(villager)) {
                    continue;
                }
                villager.remove();
                removed++;
            }
        }
        return removed;
    }

    public boolean isManagedVillager(Villager villager) {
        return villager.getPersistentDataContainer().has(villagerTypeKey, PersistentDataType.STRING);
    }

    public void refreshTrades(Villager villager) {
        VillagerType type = VillagerType.fromKey(villager.getPersistentDataContainer().get(villagerTypeKey, PersistentDataType.STRING));
        if (type != null) {
            villager.setRecipes(buildRecipes(type));
        }
    }

    public Long expiryFor(Villager villager) {
        return villager.getPersistentDataContainer().get(expiresKey, PersistentDataType.LONG);
    }

    public String ownerNameFor(Villager villager) {
        return villager.getPersistentDataContainer().get(ownerNameKey, PersistentDataType.STRING);
    }

    public UUID ownerIdFor(Villager villager) {
        String value = villager.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public boolean canUse(Player player, Villager villager) {
        UUID ownerId = ownerIdFor(villager);
        return ownerId == null || ownerId.equals(player.getUniqueId()) || player.hasPermission("diamondsmp.admin.godvillager");
    }

    public String itemKeyForResult(ItemStack result) {
        if (result == null || !result.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = result.getItemMeta();
        return meta.getPersistentDataContainer().get(tradeItemKey, PersistentDataType.STRING);
    }

    public void markPurchased(String itemKey) {
        purchaseHistory.markPurchased(itemKey);
    }

    private List<MerchantRecipe> buildRecipes(VillagerType type) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (TradeDefinition trade : availableTrades(type)) {
            GodItemType itemType = GodItemType.fromKey(trade.itemKey());
            if (itemType == null) {
                continue;
            }
            ItemStack result = items.createItem(itemType);
            ItemMeta meta = result.getItemMeta();
            meta.getPersistentDataContainer().set(tradeItemKey, PersistentDataType.STRING, trade.itemKey());
            result.setItemMeta(meta);
            MerchantRecipe recipe = new MerchantRecipe(result, 0, 1, false);
            recipe.addIngredient(new ItemStack(trade.currency(), trade.currencyAmount()));
            if (trade.emeraldCost() > 0 && trade.currency() != Material.EMERALD) {
                recipe.addIngredient(new ItemStack(Material.EMERALD, trade.emeraldCost()));
            } else if (trade.emeraldCost() > 0) {
                recipe.setIngredients(List.of(new ItemStack(Material.EMERALD, trade.emeraldCost())));
            }
            recipes.add(recipe);
        }
        return recipes;
    }

    private List<TradeDefinition> availableTrades(VillagerType type) {
        return definitions.getOrDefault(type, List.of()).stream()
            .filter(definition -> !purchaseHistory.wasPurchased(definition.itemKey()))
            .toList();
    }

    private Map<VillagerType, List<TradeDefinition>> loadDefinitions(FileConfiguration config) {
        return java.util.Arrays.stream(VillagerType.values()).collect(
            java.util.stream.Collectors.toUnmodifiableMap(type -> type, type -> {
                ConfigurationSection section = config.getConfigurationSection("villagers." + type.key() + ".trades");
                if (section == null) {
                    return List.of();
                }
                return section.getKeys(false).stream()
                    .map(key -> {
                        ConfigurationSection trade = section.getConfigurationSection(key);
                        if (trade == null) {
                            return null;
                        }
                        PluginSettings.TradeEntry entry = PluginSettings.TradeEntry.fromSection(trade, settings.costFor(key, 64));
                        Material currency = Objects.requireNonNullElse(entry.currency(), Material.EMERALD);
                        return new TradeDefinition(entry.itemKey(), entry.emeraldCost(), currency, entry.currencyAmount());
                    })
                    .filter(Objects::nonNull)
                    .toList();
            })
        );
    }

    private void startFollowTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                    if (!isManagedVillager(villager)) {
                        continue;
                    }
                    Long expiry = expiryFor(villager);
                    if (expiry != null && Instant.now().toEpochMilli() > expiry) {
                        villager.remove();
                        continue;
                    }
                    UUID ownerId = ownerIdFor(villager);
                    if (ownerId == null) {
                        continue;
                    }
                    Player owner = Bukkit.getPlayer(ownerId);
                    if (owner == null || !owner.isOnline() || owner.isDead() || !owner.getWorld().equals(villager.getWorld())) {
                        continue;
                    }
                    double distance = villager.getLocation().distance(owner.getLocation());
                    if (distance > 20.0D) {
                        villager.teleport(owner.getLocation().clone().add(1.0D, 0.0D, 1.0D));
                        continue;
                    }
                    if (distance > 3.5D) {
                        villager.getPathfinder().moveTo(owner, 1.1D);
                    }
                }
            }
        }, 20L, 20L);
    }

    private String compactTitle(String ownerName, VillagerType type) {
        String shortOwner = ownerName.length() > 10 ? ownerName.substring(0, 10) : ownerName;
        String label = switch (type) {
            case TOP_ARMOR -> "Top";
            case BOTTOM_ARMOR -> "Bottom";
            case TOOLS -> "Tools";
        };
        return shortOwner + " " + label;
    }

    private record TradeDefinition(String itemKey, int emeraldCost, Material currency, int currencyAmount) {}
}
