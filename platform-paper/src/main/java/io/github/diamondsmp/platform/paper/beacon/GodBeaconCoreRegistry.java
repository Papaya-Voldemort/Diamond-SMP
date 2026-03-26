package io.github.diamondsmp.platform.paper.beacon;

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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class GodBeaconCoreRegistry {
    private final NamespacedKey coreTypeKey;
    private final Map<GodBeaconCoreType, ItemStack> templates = new EnumMap<>(GodBeaconCoreType.class);

    public GodBeaconCoreRegistry(JavaPlugin plugin) {
        this.coreTypeKey = new NamespacedKey(plugin, "god-beacon-core-type");
        for (GodBeaconCoreType type : GodBeaconCoreType.values()) {
            templates.put(type, create(type));
        }
    }

    public ItemStack createItem(GodBeaconCoreType type) {
        return templates.get(type).clone();
    }

    public GodBeaconCoreType resolve(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return null;
        }
        String stored = stack.getItemMeta().getPersistentDataContainer().get(coreTypeKey, PersistentDataType.STRING);
        return GodBeaconCoreType.fromKey(stored);
    }

    public boolean isCore(ItemStack stack, GodBeaconCoreType type) {
        return resolve(stack) == type;
    }

    private ItemStack create(GodBeaconCoreType type) {
        ItemStack stack = new ItemStack(type.displayMaterial());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(type.displayName(), NamedTextColor.AQUA));
        meta.lore(List.of(
            Component.text("Beacon upgrade catalyst", NamedTextColor.DARK_GRAY),
            Component.text("Use on a " + type.requiredTier().displayName(), NamedTextColor.GRAY),
            Component.text("Consumes " + type.diamondBlockCost() + " Diamond Blocks", NamedTextColor.BLUE),
            Component.text("Consumes " + type.netherStarCost() + " Nether Star" + (type.netherStarCost() == 1 ? "" : "s"), NamedTextColor.BLUE)
        ));
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(coreTypeKey, PersistentDataType.STRING, type.key());
        stack.setItemMeta(meta);
        return stack;
    }
}
