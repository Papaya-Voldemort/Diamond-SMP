package io.github.diamondsmp.platform.paper.gui;

import io.github.diamondsmp.platform.paper.service.EndAccessService;
import io.github.diamondsmp.platform.paper.service.OwnerControlService;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GodMenuGui {
    public static final String TITLE = ChatColor.DARK_RED + "Owner Control Matrix";

    public void open(Player player, OwnerControlService ownerControl, EndAccessService endAccessService) {
        MenuHolder holder = new MenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE);
        fill(inventory, Material.BLACK_STAINED_GLASS_PANE, " ");

        inventory.setItem(4, create(Material.NETHER_STAR, ChatColor.GOLD + "Owner Control Matrix", List.of(
            ChatColor.GRAY + "Live controls for server-side systems.",
            ChatColor.DARK_GRAY + "Owner-scoped. Runtime only."
        )));

        inventory.setItem(10, create(
            ownerControl.globalPvpEnabled() ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD,
            statusName("Global PvP", ownerControl.globalPvpEnabled()),
            List.of(
                statusLine(ownerControl.globalPvpEnabled()),
                ChatColor.GRAY + "Applies across loaded worlds.",
                ChatColor.YELLOW + "Click to toggle live combat."
            )
        ));
        inventory.setItem(12, create(
            ownerControl.partyEnabled() ? Material.PLAYER_HEAD : Material.SKELETON_SKULL,
            statusName("/p Party Command", ownerControl.partyEnabled()),
            List.of(
                statusLine(ownerControl.partyEnabled()),
                ChatColor.GRAY + "Controls party invites and management.",
                ChatColor.YELLOW + "Click to toggle /p."
            )
        ));
        inventory.setItem(14, create(
            ownerControl.pvpEnabled() ? Material.IRON_SWORD : Material.BARRIER,
            statusName("/pvp Match System", ownerControl.pvpEnabled()),
            List.of(
                statusLine(ownerControl.pvpEnabled()),
                ChatColor.GRAY + "Controls the custom PvP feature set.",
                ChatColor.YELLOW + "Click to toggle /pvp."
            )
        ));
        inventory.setItem(16, create(
            endAccessService.isOpen() ? Material.END_PORTAL_FRAME : Material.OBSIDIAN,
            statusName("End Access", endAccessService.isOpen()),
            List.of(
                statusLine(endAccessService.isOpen()),
                ChatColor.GRAY + "Opens or seals stronghold portals.",
                ChatColor.YELLOW + "Click to toggle The End."
            )
        ));

        inventory.setItem(28, create(Material.NAME_TAG, ChatColor.AQUA + "Start Nametag Event", List.of(
            ChatColor.GRAY + "Reward villager: " + ChatColor.WHITE + "top",
            ChatColor.YELLOW + "Click to launch."
        )));
        inventory.setItem(29, create(Material.CAT_SPAWN_EGG, ChatColor.AQUA + "Start Cat Hunt Event", List.of(
            ChatColor.GRAY + "Reward villager: " + ChatColor.WHITE + "tools",
            ChatColor.YELLOW + "Click to launch."
        )));
        inventory.setItem(31, create(Material.BARRIER, ChatColor.RED + "Stop Active Event", List.of(
            ChatColor.GRAY + "Immediately stops the current plugin event."
        )));
        inventory.setItem(33, create(Material.CHEST, ChatColor.GOLD + "Give God Test Kit", List.of(
            ChatColor.GRAY + "Loads the full custom item set into your inventory."
        )));
        inventory.setItem(35, create(Material.MAP, ChatColor.YELLOW + "Reset World Border", List.of(
            ChatColor.GRAY + "Returns border center, size, and warnings",
            ChatColor.GRAY + "to the configured defaults."
        )));
        inventory.setItem(40, create(Material.WRITABLE_BOOK, ChatColor.LIGHT_PURPLE + "Create Recovery Export", List.of(
            ChatColor.GRAY + "Builds a timestamped archive with",
            ChatColor.GRAY + "server configs, plugins, and worlds.",
            ChatColor.YELLOW + "Action is logged to owner-audit.log."
        )));
        inventory.setItem(42, create(
            dragonEggRulesMaterial(ownerControl),
            dragonEggRulesName(ownerControl),
            List.of(
                ChatColor.GRAY + "Dragon egg gets +5 hearts in inventory.",
                ChatColor.GRAY + "Egg cannot stay in containers.",
                ChatColor.GRAY + "Logout drops it at your location.",
                ChatColor.YELLOW + "Click to toggle special rules."
            )
        ));

        inventory.setItem(49, create(Material.CLOCK, ChatColor.GREEN + "Refresh", List.of(
            ChatColor.GRAY + "Rebuild this control panel."
        )));
        inventory.setItem(53, create(Material.SPRUCE_DOOR, ChatColor.RED + "Close", List.of(
            ChatColor.GRAY + "Exit the menu."
        )));
        player.openInventory(inventory);
    }

    public boolean isMenu(InventoryHolder holder) {
        return holder instanceof MenuHolder;
    }

    private String statusName(String label, boolean enabled) {
        return (enabled ? ChatColor.GREEN : ChatColor.RED) + label;
    }

    private String statusLine(boolean enabled) {
        return ChatColor.GRAY + "State: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled");
    }

    private Material dragonEggRulesMaterial(OwnerControlService ownerControl) {
        return ownerControl.dragonEggRulesEnabled() ? Material.DRAGON_EGG : Material.EGG;
    }

    private String dragonEggRulesName(OwnerControlService ownerControl) {
        return statusName("Dragon Egg Rules", ownerControl.dragonEggRulesEnabled());
    }

    private void fill(Inventory inventory, Material material, String name) {
        ItemStack filler = create(material, name, List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
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

    private static final class MenuHolder implements InventoryHolder {
        private final UUID viewerId;

        private MenuHolder(UUID viewerId) {
            this.viewerId = viewerId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
