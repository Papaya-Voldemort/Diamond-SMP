package io.github.diamondsmp.platform.paper.villager;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.item.GodItemType;
import io.github.diamondsmp.platform.paper.service.PurchaseHistoryStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    private final NamespacedKey ownerKey;
    private final NamespacedKey ownerNameKey;
    private final NamespacedKey tradeItemKey;
    private final NamespacedKey eggMarkerKey;
    private final NamespacedKey eggDroppedKey;
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
        this.ownerKey = new NamespacedKey(plugin, "god-villager-owner");
        this.ownerNameKey = new NamespacedKey(plugin, "god-villager-owner-name");
        this.tradeItemKey = new NamespacedKey(plugin, "god-villager-trade-item");
        this.eggMarkerKey = new NamespacedKey(plugin, "god-villager-egg");
        this.eggDroppedKey = new NamespacedKey(plugin, "god-villager-egg-dropped");
        this.definitions = loadDefinitions(villagersConfig);
        startFollowTask();
    }

    public Villager spawnRewardVillager(Player winner, VillagerType type, String eventName) {
        return spawn(winner, type, eventName);
    }

    public Villager spawn(Player player, VillagerType type, String eventName) {
        return spawnManagedVillager(player.getLocation(), type, eventName, player.getUniqueId(), player.getName());
    }

    public Villager spawnFromEgg(Location location, ItemStack egg) {
        if (!isManagedVillagerEgg(egg)) {
            return null;
        }
        ItemMeta meta = egg.getItemMeta();
        VillagerType type = VillagerType.fromKey(meta.getPersistentDataContainer().get(villagerTypeKey, PersistentDataType.STRING));
        if (type == null) {
            return null;
        }
        UUID ownerId = null;
        String owner = meta.getPersistentDataContainer().get(ownerNameKey, PersistentDataType.STRING);
        String ownerValue = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (ownerValue != null && !ownerValue.isBlank()) {
            try {
                ownerId = UUID.fromString(ownerValue);
            } catch (IllegalArgumentException ignored) {
                ownerId = null;
            }
        }
        String eventName = meta.getPersistentDataContainer().get(eventKey, PersistentDataType.STRING);
        return spawnManagedVillager(location, type, eventName == null ? "relocated" : eventName, ownerId, owner);
    }

    public ItemStack createRelocationEgg(Villager villager) {
        ItemStack egg = new ItemStack(Material.VILLAGER_SPAWN_EGG);
        ItemMeta meta = egg.getItemMeta();
        meta.displayName(Component.text(compactTitle(ownerNameFor(villager), typeFor(villager))));
        meta.getPersistentDataContainer().set(eggMarkerKey, PersistentDataType.BYTE, (byte) 1);
        copyIdentity(villager, meta);
        egg.setItemMeta(meta);
        return egg;
    }

    public boolean shouldDropEgg(Villager villager, Player killer) {
        if (!settings.villagers().dropEggOnKill() || hasDroppedEgg(villager)) {
            return false;
        }
        if (settings.villagers().requirePlayerKillForEgg()) {
            return killer != null;
        }
        return killer != null || settings.villagers().allowEnvironmentalEggDrop();
    }

    public void markEggDropped(Villager villager) {
        villager.getPersistentDataContainer().set(eggDroppedKey, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean hasDroppedEgg(Villager villager) {
        return villager.getPersistentDataContainer().has(eggDroppedKey, PersistentDataType.BYTE);
    }

    public boolean isManagedVillagerEgg(ItemStack stack) {
        if (stack == null || stack.getType() != Material.VILLAGER_SPAWN_EGG || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(eggMarkerKey, PersistentDataType.BYTE);
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
        VillagerType type = typeFor(villager);
        if (type != null) {
            villager.setRecipes(buildRecipes(type));
        }
    }

    public String ownerNameFor(Villager villager) {
        String owner = villager.getPersistentDataContainer().get(ownerNameKey, PersistentDataType.STRING);
        return owner == null || owner.isBlank() ? "Unknown" : owner;
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

    public VillagerType typeFor(Villager villager) {
        return VillagerType.fromKey(villager.getPersistentDataContainer().get(villagerTypeKey, PersistentDataType.STRING));
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

    private Villager spawnManagedVillager(Location location, VillagerType type, String eventName, UUID ownerId, String ownerName) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(true);
        villager.setCustomNameVisible(true);
        villager.setRemoveWhenFarAway(false);
        villager.setCollidable(false);
        villager.setCanPickupItems(false);
        villager.setSilent(true);
        villager.setBreed(false);
        villager.setAware(true);
        villager.setPersistent(settings.villagers().persistManaged());
        villager.getPersistentDataContainer().set(villagerTypeKey, PersistentDataType.STRING, type.key());
        villager.getPersistentDataContainer().set(eventKey, PersistentDataType.STRING, eventName.toLowerCase(Locale.ROOT));
        if (ownerId != null) {
            villager.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, ownerId.toString());
        }
        if (ownerName != null && !ownerName.isBlank()) {
            villager.getPersistentDataContainer().set(ownerNameKey, PersistentDataType.STRING, ownerName);
        }
        villager.customName(Component.text(compactTitle(ownerName == null ? "Server" : ownerName, type)));
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerLevel(5);
        villager.setVillagerExperience(0);
        villager.setRecipes(buildRecipes(type));
        return villager;
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
            MerchantRecipe recipe = new MerchantRecipe(result, 0, trade.maxUses(), false, trade.villagerExperience(), 0.0F);
            if (trade.currencyAmount() > 0) {
                recipe.addIngredient(new ItemStack(trade.currency(), trade.currencyAmount()));
            }
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
            java.util.stream.Collectors.toUnmodifiableMap(type -> type, value -> {
                ConfigurationSection section = config.getConfigurationSection("villagers." + value.key() + ".trades");
                if (section == null) {
                    return List.of();
                }
                return PluginSettings.loadTrades(section, settings).stream()
                    .map(entry -> new TradeDefinition(
                        entry.itemKey(),
                        entry.emeraldCost(),
                        Objects.requireNonNullElse(entry.currency(), Material.EMERALD),
                        entry.currencyAmount(),
                        entry.maxUses(),
                        entry.villagerExperience()
                    ))
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

    private void copyIdentity(Villager villager, ItemMeta meta) {
        String type = villager.getPersistentDataContainer().get(villagerTypeKey, PersistentDataType.STRING);
        String event = villager.getPersistentDataContainer().get(eventKey, PersistentDataType.STRING);
        String owner = villager.getPersistentDataContainer().get(ownerNameKey, PersistentDataType.STRING);
        String ownerId = villager.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (type != null) {
            meta.getPersistentDataContainer().set(villagerTypeKey, PersistentDataType.STRING, type);
        }
        if (event != null) {
            meta.getPersistentDataContainer().set(eventKey, PersistentDataType.STRING, event);
        }
        if (owner != null) {
            meta.getPersistentDataContainer().set(ownerNameKey, PersistentDataType.STRING, owner);
        }
        if (ownerId != null) {
            meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, ownerId);
        }
    }

    private String compactTitle(String ownerName, VillagerType type) {
        String shortOwner = ownerName.length() > 10 ? ownerName.substring(0, 10) : ownerName;
        String label = switch (type) {
            case TOP_ARMOR -> "Top";
            case BOTTOM_ARMOR -> "Bottom";
            case TOOLS -> "Tools";
        };
        return shortOwner + " " + label + " Villager";
    }

    private record TradeDefinition(
        String itemKey,
        int emeraldCost,
        Material currency,
        int currencyAmount,
        int maxUses,
        int villagerExperience
    ) {}
}
