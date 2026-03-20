package io.github.diamondsmp.platform.paper.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class GodItemRegistry {
    private final NamespacedKey itemKey;
    private final Map<GodItemType, ItemStack> templates = new EnumMap<>(GodItemType.class);

    public GodItemRegistry(JavaPlugin plugin) {
        this.itemKey = new NamespacedKey(plugin, "god-item-type");
        for (GodItemType type : GodItemType.values()) {
            templates.put(type, create(type));
        }
    }

    public ItemStack createItem(GodItemType type) {
        return templates.get(type).clone();
    }

    public boolean isGodItem(ItemStack stack, GodItemType type) {
        return type == resolve(stack);
    }

    public boolean isAnyGodItem(ItemStack stack) {
        return resolve(stack) != null;
    }

    public GodItemType resolve(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return null;
        }
        String stored = stack.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return stored == null ? null : GodItemType.fromKey(stored);
    }

    private ItemStack create(GodItemType type) {
        ItemStack stack = new ItemStack(type.material());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(type.displayName(), NamedTextColor.GOLD));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, type.key());
        switch (type) {
            case HELMET -> {
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.RESPIRATION, 3, true);
                meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("Permanent Fire Resistance", NamedTextColor.GRAY),
                    Component.text("Permanent Dolphin's Grace", NamedTextColor.GRAY),
                    Component.text("Permanent Water Breathing", NamedTextColor.GRAY)
                ));
            }
            case CHESTPLATE -> {
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("+10 hearts while worn", NamedTextColor.GRAY)
                ));
            }
            case LEGGINGS -> {
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("Permanent Resistance II", NamedTextColor.GRAY)
                ));
            }
            case BOOTS -> {
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                meta.addEnchant(Enchantment.DEPTH_STRIDER, 5, true);
                meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
                meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("Permanent Speed II", NamedTextColor.GRAY)
                ));
            }
            case SWORD -> {
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.LOOTING, 5, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 5, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("Strength II while held", NamedTextColor.GRAY)
                ));
            }
            case AXE -> {
                meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("Shreds enemy armor durability", NamedTextColor.GRAY)
                ));
            }
            case PICKAXE -> {
                meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
                meta.addEnchant(Enchantment.FORTUNE, 7, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("Can slowly break bedrock", NamedTextColor.GRAY),
                    Component.text("Fortune VII, auto-smelting, and vein mining", NamedTextColor.GRAY)
                ));
            }
            case BOW -> {
                meta.addEnchant(Enchantment.POWER, 5, true);
                meta.addEnchant(Enchantment.INFINITY, 1, true);
                meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                meta.lore(List.of(
                    Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                    Component.text("Left click: short-bow burst with headshot bonus", NamedTextColor.GRAY),
                    Component.text("Right click: normal buffed bow shot", NamedTextColor.GRAY),
                    Component.text("Neither mode consumes arrows", NamedTextColor.GRAY)
                ));
            }
            case INFINITE_TOTEM -> meta.lore(List.of(
                Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                Component.text("Reusable with a cooldown", NamedTextColor.GRAY)
            ));
            case ENCHANTED_GAPPLE -> meta.lore(List.of(
                Component.text("Event-exclusive god item", NamedTextColor.DARK_GRAY),
                Component.text("Event-exclusive consumable", NamedTextColor.GRAY)
            ));
        }
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
