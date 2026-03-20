package io.github.diamondsmp.platform.paper.gui;

import io.github.diamondsmp.platform.paper.config.RulesBook;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RulesGui {
    private final RulesBook rulesBook;

    public RulesGui(RulesBook rulesBook) {
        this.rulesBook = rulesBook;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, rulesBook.title());
        ItemStack filler = create(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        int[] slots = {10, 12, 14, 16, 28, 30, 32, 34};
        for (int index = 0; index < Math.min(slots.length, rulesBook.sections().size()); index++) {
            RulesBook.Section section = rulesBook.sections().get(index);
            inventory.setItem(slots[index], create(Material.BOOK, section.icon() + " " + section.title(), section.lines()));
        }
        player.openInventory(inventory);
    }

    public boolean isRulesTitle(String title) {
        return ChatColor.stripColor(title).equalsIgnoreCase(ChatColor.stripColor(rulesBook.title()));
    }

    private ItemStack create(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
