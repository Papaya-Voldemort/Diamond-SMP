package io.github.diamondsmp.platform.paper.item;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiamondPerkRegistry {
    private final NamespacedKey perkBookKey;
    private final NamespacedKey appliedPerkKey;

    public DiamondPerkRegistry(JavaPlugin plugin) {
        this.perkBookKey = new NamespacedKey(plugin, "diamond-perk-book");
        this.appliedPerkKey = new NamespacedKey(plugin, "diamond-perk-applied");
    }

    public ItemStack createBook(DiamondPerkType type) {
        ItemStack stack = new ItemStack(type.material());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(type.displayName() + " Book", NamedTextColor.AQUA));
        meta.lore(List.of(
            Component.text("Diamond sink enchant", NamedTextColor.DARK_GRAY),
            Component.text(type.description(), NamedTextColor.GRAY),
            Component.text(targetLine(type), NamedTextColor.BLUE)
        ));
        meta.getPersistentDataContainer().set(perkBookKey, PersistentDataType.STRING, type.key());
        stack.setItemMeta(meta);
        return stack;
    }

    public DiamondPerkType resolveBook(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return null;
        }
        String stored = stack.getItemMeta().getPersistentDataContainer().get(perkBookKey, PersistentDataType.STRING);
        return DiamondPerkType.fromKey(stored);
    }

    public DiamondPerkType resolveApplied(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return null;
        }
        String stored = stack.getItemMeta().getPersistentDataContainer().get(appliedPerkKey, PersistentDataType.STRING);
        return DiamondPerkType.fromKey(stored);
    }

    public boolean canApply(DiamondPerkType type, ItemStack stack) {
        if (type == null || stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        if (resolveApplied(stack) != null) {
            return false;
        }
        return switch (type) {
            case PROSPECTOR -> stack.getType().name().endsWith("_PICKAXE");
            case BULWARK -> stack.getType().name().endsWith("_CHESTPLATE");
            case MOMENTUM -> stack.getType().name().endsWith("_BOOTS");
        };
    }

    public ItemStack apply(DiamondPerkType type, ItemStack target) {
        if (!canApply(type, target)) {
            return null;
        }
        ItemStack result = target.clone();
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(appliedPerkKey, PersistentDataType.STRING, type.key());
        List<Component> lore = meta.lore();
        java.util.ArrayList<Component> updatedLore = lore == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(lore);
        updatedLore.add(Component.text(type.displayName() + " Applied", NamedTextColor.AQUA));
        meta.lore(updatedLore);
        result.setItemMeta(meta);
        return result;
    }

    public boolean isPerkItem(ItemStack stack) {
        return resolveBook(stack) != null || resolveApplied(stack) != null;
    }

    private String targetLine(DiamondPerkType type) {
        return switch (type) {
            case PROSPECTOR -> "Applies to pickaxes";
            case BULWARK -> "Applies to chestplates";
            case MOMENTUM -> "Applies to boots";
        };
    }
}
