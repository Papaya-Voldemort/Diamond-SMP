package io.github.diamondsmp.platform.paper.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public final class KitService {
    private final JavaPlugin plugin;
    private final Map<String, KitDefinition> kits;

    public KitService(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.kits = loadKits(config);
    }

    public List<String> names() {
        return kits.values().stream().map(KitDefinition::name).toList();
    }

    public Optional<KitDefinition> find(String name) {
        return Optional.ofNullable(kits.get(name.toLowerCase(Locale.ROOT)));
    }

    public boolean giveKit(Player player, String name) {
        KitDefinition kit = kits.get(name.toLowerCase(Locale.ROOT));
        if (kit == null) {
            return false;
        }
        applyLoadout(player.getInventory(), kit.items());
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
        return true;
    }

    private void applyLoadout(PlayerInventory inventory, List<ItemStack> items) {
        LoadoutPlan loadout = buildLoadout(items);
        if (loadout.helmet() != null) {
            inventory.setHelmet(loadout.helmet());
        }
        if (loadout.chestplate() != null) {
            inventory.setChestplate(loadout.chestplate());
        }
        if (loadout.leggings() != null) {
            inventory.setLeggings(loadout.leggings());
        }
        if (loadout.boots() != null) {
            inventory.setBoots(loadout.boots());
        }
        if (loadout.offHand() != null) {
            inventory.setItemInOffHand(loadout.offHand());
        }
        int slot = 0;
        for (ItemStack item : loadout.hotbar()) {
            if (slot >= 9) {
                break;
            }
            inventory.setItem(slot++, item);
        }
        int storageSlot = 9;
        int storageLimit = 36;
        for (ItemStack item : loadout.storage()) {
            while (storageSlot < storageLimit && inventory.getItem(storageSlot) != null && inventory.getItem(storageSlot).getType() != Material.AIR) {
                storageSlot++;
            }
            if (storageSlot >= storageLimit) {
                break;
            }
            inventory.setItem(storageSlot++, item);
        }
    }

    private LoadoutPlan buildLoadout(List<ItemStack> items) {
        ItemStack helmet = null;
        ItemStack chestplate = null;
        ItemStack leggings = null;
        ItemStack boots = null;
        ItemStack offHand = null;
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack source : items) {
            if (source == null || source.getType() == Material.AIR) {
                continue;
            }
            ItemStack item = source.clone();
            switch (armorSlot(item.getType())) {
                case HELMET -> {
                    if (helmet == null) {
                        helmet = item;
                        continue;
                    }
                }
                case CHESTPLATE -> {
                    if (chestplate == null) {
                        chestplate = item;
                        continue;
                    }
                }
                case LEGGINGS -> {
                    if (leggings == null) {
                        leggings = item;
                        continue;
                    }
                }
                case BOOTS -> {
                    if (boots == null) {
                        boots = item;
                        continue;
                    }
                }
                case NONE -> {
                }
            }
            if (offHand == null && isPreferredOffHand(item.getType())) {
                offHand = item;
                continue;
            }
            remaining.add(item);
        }

        List<ItemStack> hotbar = new ArrayList<>();
        List<ItemStack> storage = new ArrayList<>();
        for (ItemStack item : remaining.stream().sorted((left, right) -> Integer.compare(loadoutPriority(left), loadoutPriority(right))).toList()) {
            if (hotbar.size() < 9 && prefersHotbar(item)) {
                hotbar.add(item);
            } else {
                storage.add(item);
            }
        }
        if (hotbar.size() < 9) {
            List<ItemStack> overflow = new ArrayList<>();
            for (ItemStack item : storage) {
                if (hotbar.size() < 9 && shouldBackfillHotbar(item)) {
                    hotbar.add(item);
                } else {
                    overflow.add(item);
                }
            }
            storage = overflow;
        }
        return new LoadoutPlan(helmet, chestplate, leggings, boots, offHand, hotbar, storage);
    }

    private boolean prefersHotbar(ItemStack item) {
        return loadoutPriority(item) <= 7;
    }

    private boolean shouldBackfillHotbar(ItemStack item) {
        return item.getMaxStackSize() > 1 || isWeapon(item.getType());
    }

    private int loadoutPriority(ItemStack item) {
        Material material = item.getType();
        if (isSword(material)) {
            return 0;
        }
        if (isBow(material) || material == Material.TRIDENT) {
            return 1;
        }
        if (isAxe(material)) {
            return 2;
        }
        if (material == Material.ENDER_PEARL || material == Material.SNOWBALL || material == Material.EGG) {
            return 3;
        }
        if (material == Material.GOLDEN_APPLE || material == Material.ENCHANTED_GOLDEN_APPLE) {
            return 4;
        }
        if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION) {
            return 5;
        }
        if (isFood(material)) {
            return 6;
        }
        if (material.isBlock()) {
            return 7;
        }
        if (material == Material.ARROW || material == Material.SPECTRAL_ARROW || material == Material.TIPPED_ARROW) {
            return 8;
        }
        return 9;
    }

    private boolean isPreferredOffHand(Material material) {
        return material == Material.SHIELD || material == Material.TOTEM_OF_UNDYING;
    }

    private boolean isWeapon(Material material) {
        return isSword(material) || isAxe(material) || isBow(material) || material == Material.TRIDENT;
    }

    private boolean isSword(Material material) {
        return material.name().endsWith("_SWORD");
    }

    private boolean isAxe(Material material) {
        return material.name().endsWith("_AXE");
    }

    private boolean isBow(Material material) {
        return material == Material.BOW || material == Material.CROSSBOW;
    }

    private boolean isFood(Material material) {
        return material.isEdible() && material != Material.GOLDEN_APPLE && material != Material.ENCHANTED_GOLDEN_APPLE;
    }

    private ArmorSlot armorSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) {
            return ArmorSlot.HELMET;
        }
        if (name.endsWith("_CHESTPLATE")) {
            return ArmorSlot.CHESTPLATE;
        }
        if (name.endsWith("_LEGGINGS")) {
            return ArmorSlot.LEGGINGS;
        }
        if (name.endsWith("_BOOTS")) {
            return ArmorSlot.BOOTS;
        }
        return ArmorSlot.NONE;
    }

    private Map<String, KitDefinition> loadKits(FileConfiguration config) {
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection == null) {
            return Map.of();
        }
        Map<String, KitDefinition> loaded = new LinkedHashMap<>();
        for (String key : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(key);
            if (kitSection == null) {
                continue;
            }
            List<ItemStack> items = new ArrayList<>();
            ConfigurationSection itemsSection = kitSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection == null) {
                        continue;
                    }
                    ItemStack item = createItem(itemSection);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            loaded.put(key.toLowerCase(Locale.ROOT), new KitDefinition(
                key,
                kitSection.getString("permission"),
                items
            ));
        }
        return Map.copyOf(loaded);
    }

    private ItemStack createItem(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("Invalid kit item material in kits.yml: " + section.getCurrentPath());
            return null;
        }
        ItemStack stack = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        ItemMeta meta = stack.getItemMeta();
        if (section.contains("name")) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(section.getString("name", "")));
        }
        if (section.isList("lore")) {
            List<Component> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(lore);
        }
        if (section.contains("custom-model-data")) {
            meta.setCustomModelData(section.getInt("custom-model-data"));
        }
        if (section.getBoolean("unbreakable", false)) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        if (section.isList("item-flags")) {
            for (String flagName : section.getStringList("item-flags")) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Invalid item flag in kits.yml: " + flagName);
                }
            }
        }
        ConfigurationSection enchants = section.getConfigurationSection("enchants");
        if (enchants != null) {
            for (String enchantKey : enchants.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByName(enchantKey.toUpperCase(Locale.ROOT));
                if (enchantment == null) {
                    plugin.getLogger().warning("Invalid enchantment in kits.yml: " + enchantKey);
                    continue;
                }
                meta.addEnchant(enchantment, enchants.getInt(enchantKey, 1), true);
            }
        }
        applySpecializedMeta(meta, section);
        stack.setItemMeta(meta);
        return stack;
    }

    private void applySpecializedMeta(ItemMeta meta, ConfigurationSection section) {
        if (meta instanceof Damageable damageable && section.contains("damage")) {
            damageable.setDamage(Math.max(0, section.getInt("damage")));
        }
        if (meta instanceof PotionMeta potionMeta) {
            applyPotionMeta(potionMeta, section.getConfigurationSection("potion"));
        }
        if (meta instanceof LeatherArmorMeta leatherArmorMeta) {
            applyLeatherMeta(leatherArmorMeta, section.getString("leather-color"));
        }
        if (meta instanceof SkullMeta skullMeta && section.contains("skull-owner")) {
            String owner = section.getString("skull-owner", "");
            if (!owner.isBlank()) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            }
        }
        if (meta instanceof BookMeta bookMeta) {
            applyBookMeta(bookMeta, section.getConfigurationSection("book"));
        }
    }

    private void applyPotionMeta(PotionMeta meta, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        String baseType = section.getString("type");
        if (baseType != null && !baseType.isBlank()) {
            try {
                meta.setBasePotionType(PotionType.valueOf(baseType.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid potion type in kits.yml: " + baseType);
            }
        }
        Color color = parseColor(section.getString("color"));
        if (color != null) {
            meta.setColor(color);
        }
        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        if (effectsSection == null) {
            return;
        }
        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(key);
            if (effectSection == null) {
                continue;
            }
            String typeName = effectSection.getString("type", key);
            PotionEffectType effectType = PotionEffectType.getByName(typeName.toUpperCase(Locale.ROOT));
            if (effectType == null) {
                plugin.getLogger().warning("Invalid potion effect in kits.yml: " + typeName);
                continue;
            }
            int durationTicks = Math.max(1, effectSection.getInt("duration-seconds", 30) * 20);
            int amplifier = Math.max(0, effectSection.getInt("amplifier", 0));
            boolean ambient = effectSection.getBoolean("ambient", false);
            boolean particles = effectSection.getBoolean("particles", true);
            boolean icon = effectSection.getBoolean("icon", true);
            meta.addCustomEffect(new PotionEffect(effectType, durationTicks, amplifier, ambient, particles, icon), true);
        }
    }

    private void applyLeatherMeta(LeatherArmorMeta meta, String colorValue) {
        Color color = parseColor(colorValue);
        if (color != null) {
            meta.setColor(color);
        }
    }

    private void applyBookMeta(BookMeta meta, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        if (section.contains("title")) {
            meta.setTitle(section.getString("title", ""));
        }
        if (section.contains("author")) {
            meta.setAuthor(section.getString("author", ""));
        }
        if (section.isList("pages")) {
            List<Component> pages = new ArrayList<>();
            for (String page : section.getStringList("pages")) {
                pages.add(LegacyComponentSerializer.legacyAmpersand().deserialize(page));
            }
            meta.pages(pages);
        }
    }

    private Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) {
            plugin.getLogger().warning("Invalid color in kits.yml: " + value);
            return null;
        }
        try {
            return Color.fromRGB(Integer.parseInt(normalized, 16));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid color in kits.yml: " + value);
            return null;
        }
    }

    public record KitDefinition(String name, String permission, List<ItemStack> items) {}

    private enum ArmorSlot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS,
        NONE
    }

    private record LoadoutPlan(
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        ItemStack offHand,
        List<ItemStack> hotbar,
        List<ItemStack> storage
    ) {}
}
