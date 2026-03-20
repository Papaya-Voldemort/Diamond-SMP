package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.gui.PvpGui;
import io.github.diamondsmp.platform.paper.service.PvpService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PvpListener implements Listener {
    private final PvpGui gui;
    private final PvpService pvpService;

    public PvpListener(PvpGui gui, PvpService pvpService) {
        this.gui = gui;
        this.pvpService = pvpService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        pvpService.recordDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvpDeath(PlayerDeathEvent event) {
        if (!pvpService.isManagedPlayer(event.getEntity().getUniqueId())) {
            return;
        }
        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        pvpService.handleDeath(event.getEntity(), event.getEntity().getKiller());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onManagedEntityDeath(EntityDeathEvent event) {
        pvpService.handleManagedEntityDeath(event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        pvpService.handleRespawn(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pvpService.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        pvpService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        pvpService.handleWorldChange(event.getPlayer());
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null) {
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof PvpGui.HubMenuHolder holder) {
            event.setCancelled(true);
            if (!player.getUniqueId().equals(holder.ownerId())) {
                return;
            }
            String modeKey = holder.modeAt(event.getRawSlot());
            if (modeKey != null) {
                pvpService.openKitMenu(player, modeKey);
                return;
            }
            if (holder.isRandomSlot(event.getRawSlot())) {
                pvpService.openRandomModeMenu(player);
                return;
            }
            if (holder.isReturnSlot(event.getRawSlot())) {
                pvpService.leaveSession(player);
                return;
            }
            if (holder.isRematchSlot(event.getRawSlot())) {
                pvpService.rematch(player);
            }
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof PvpGui.SetupMenuHolder holder) {
            event.setCancelled(true);
            if (!player.getUniqueId().equals(holder.ownerId())) {
                return;
            }
            if (holder.isBackSlot(event.getRawSlot())) {
                pvpService.openModeMenu(player);
                return;
            }
            if (holder.isSelectKitSlot(event.getRawSlot())) {
                pvpService.openKitPickerMenu(player, holder.modeKey(), holder.selectedKitName(), 0);
                return;
            }
            if (holder.isStartSlot(event.getRawSlot())) {
                pvpService.startMatch(player, holder.modeKey(), holder.selectedKitName());
            }
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof PvpGui.KitMenuHolder holder) {
            event.setCancelled(true);
            if (!player.getUniqueId().equals(holder.ownerId())) {
                return;
            }
            if (holder.isBackSlot(event.getRawSlot())) {
                pvpService.openKitMenu(player, holder.modeKey(), holder.selectedKitName());
                return;
            }
            if (holder.isPreviousPageSlot(event.getRawSlot())) {
                pvpService.openKitPickerMenu(player, holder.modeKey(), holder.selectedKitName(), holder.page() - 1);
                return;
            }
            if (holder.isNextPageSlot(event.getRawSlot())) {
                pvpService.openKitPickerMenu(player, holder.modeKey(), holder.selectedKitName(), holder.page() + 1);
                return;
            }
            String selectedKit = holder.kitNameAt(event.getRawSlot());
            if (selectedKit != null) {
                pvpService.openKitMenu(player, holder.modeKey(), selectedKit);
            }
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof PvpGui.PostMatchMenuHolder holder) {
            event.setCancelled(true);
            if (!player.getUniqueId().equals(holder.ownerId())) {
                return;
            }
            if (holder.isReturnSlot(event.getRawSlot())) {
                pvpService.returnToSmp(player);
            } else if (holder.isRematchSlot(event.getRawSlot())) {
                pvpService.rematch(player);
            } else if (holder.isNewMatchSlot(event.getRawSlot())) {
                pvpService.openModeMenu(player);
            }
        }
    }
}
