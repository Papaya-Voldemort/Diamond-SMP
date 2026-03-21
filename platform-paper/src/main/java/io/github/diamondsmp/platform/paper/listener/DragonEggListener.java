package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.service.DragonEggService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class DragonEggListener implements Listener {
    private final DragonEggService dragonEggs;
    private final MessageBundle messages;

    public DragonEggListener(DragonEggService dragonEggs, MessageBundle messages) {
        this.dragonEggs = dragonEggs;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!dragonEggs.specialRulesEnabled()) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!isRestrictedContainer(top)) {
            return;
        }
        if (event.getClickedInventory() != null
            && event.getClickedInventory().equals(event.getView().getBottomInventory())
            && event.isShiftClick()
            && isDragonEgg(event.getCurrentItem())) {
            event.setCancelled(true);
            notify(event.getWhoClicked());
            return;
        }
        if (event.getClickedInventory() != null
            && event.getClickedInventory().equals(top)
            && (isDragonEgg(event.getCursor()) || isDragonEgg(hotbarItem(event)))) {
            event.setCancelled(true);
            notify(event.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!dragonEggs.specialRulesEnabled() || !isDragonEgg(event.getOldCursor())) {
            return;
        }
        if (!isRestrictedContainer(event.getView().getTopInventory())) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
            notify(event.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (dragonEggs.specialRulesEnabled()
            && isDragonEgg(event.getItem())
            && isRestrictedContainer(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (dragonEggs.specialRulesEnabled()
            && isDragonEgg(event.getItem().getItemStack())
            && isRestrictedContainer(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (!dragonEggs.specialRulesEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        int removed = dragonEggs.removeDragonEggs(player);
        if (removed <= 0) {
            return;
        }
        player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.DRAGON_EGG, removed));
        player.sendMessage(messages.format(
            "dragon-egg.logout-drop",
            "&6Your dragon egg was dropped at logout location: &f{count}",
            java.util.Map.of("count", Integer.toString(removed))
        ));
    }

    private void notify(org.bukkit.entity.HumanEntity human) {
        human.sendMessage(messages.prefixed(
            "dragon-egg.restricted",
            "&cThe dragon egg must stay in a player inventory or on the ground."
        ));
    }

    private boolean isRestrictedContainer(Inventory inventory) {
        return inventory != null
            && inventory.getType() != org.bukkit.event.inventory.InventoryType.CRAFTING
            && inventory.getType() != org.bukkit.event.inventory.InventoryType.CREATIVE
            && inventory.getType() != org.bukkit.event.inventory.InventoryType.PLAYER;
    }

    private boolean isDragonEgg(ItemStack item) {
        return item != null && item.getType() == Material.DRAGON_EGG;
    }

    private ItemStack hotbarItem(InventoryClickEvent event) {
        if (event.getHotbarButton() < 0 || !(event.getWhoClicked() instanceof Player player)) {
            return null;
        }
        return player.getInventory().getItem(event.getHotbarButton());
    }
}
