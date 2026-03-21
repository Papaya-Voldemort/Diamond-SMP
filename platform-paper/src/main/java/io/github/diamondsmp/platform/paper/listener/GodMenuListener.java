package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.event.ServerEventManager;
import io.github.diamondsmp.platform.paper.event.ServerEventType;
import io.github.diamondsmp.platform.paper.gui.GodMenuGui;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.item.GodItemType;
import io.github.diamondsmp.platform.paper.service.EndAccessService;
import io.github.diamondsmp.platform.paper.service.OwnerControlService;
import io.github.diamondsmp.platform.paper.service.OwnerRecoveryExportService;
import java.io.File;
import io.github.diamondsmp.platform.paper.villager.VillagerType;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class GodMenuListener implements Listener {
    private final GodMenuGui godMenuGui;
    private final JavaPlugin plugin;
    private final OwnerControlService ownerControl;
    private final OwnerRecoveryExportService exportService;
    private final EndAccessService endAccessService;
    private final ServerEventManager eventManager;
    private final GodItemRegistry godItems;
    private final MessageBundle messages;
    private final double defaultBorderSize;
    private final double defaultCenterX;
    private final double defaultCenterZ;
    private final int defaultWarningDistance;
    private final int defaultWarningTime;

    public GodMenuListener(
        JavaPlugin plugin,
        GodMenuGui godMenuGui,
        OwnerControlService ownerControl,
        OwnerRecoveryExportService exportService,
        EndAccessService endAccessService,
        ServerEventManager eventManager,
        GodItemRegistry godItems,
        MessageBundle messages,
        double defaultBorderSize,
        double defaultCenterX,
        double defaultCenterZ,
        int defaultWarningDistance,
        int defaultWarningTime
    ) {
        this.plugin = plugin;
        this.godMenuGui = godMenuGui;
        this.ownerControl = ownerControl;
        this.exportService = exportService;
        this.endAccessService = endAccessService;
        this.eventManager = eventManager;
        this.godItems = godItems;
        this.messages = messages;
        this.defaultBorderSize = defaultBorderSize;
        this.defaultCenterX = defaultCenterX;
        this.defaultCenterZ = defaultCenterZ;
        this.defaultWarningDistance = defaultWarningDistance;
        this.defaultWarningTime = defaultWarningTime;
    }

    @EventHandler
    public void onGodMenuClick(InventoryClickEvent event) {
        if (!godMenuGui.isMenu(event.getView().getTopInventory().getHolder())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !ownerControl.isOwner(player)) {
            event.getWhoClicked().closeInventory();
            return;
        }
        switch (event.getRawSlot()) {
            case 10 -> {
                ownerControl.audit(player, "toggle-global-pvp");
                ownerControl.setGlobalPvp(!ownerControl.globalPvpEnabled());
                notify(player, "Global PvP updated.");
            }
            case 12 -> {
                if (!ownerControl.pvpEnabled() && !ownerControl.partyEnabled()) {
                    reject(player, "Enable /pvp before turning parties back on.");
                    godMenuGui.open(player, ownerControl, endAccessService);
                    return;
                }
                ownerControl.audit(player, "toggle-party-command");
                ownerControl.setPartyEnabled(!ownerControl.partyEnabled());
                notify(player, "Party command access updated.");
            }
            case 14 -> {
                ownerControl.audit(player, "toggle-pvp-system");
                ownerControl.setPvpEnabled(!ownerControl.pvpEnabled());
                notify(player, "PvP system access updated.");
            }
            case 16 -> {
                ownerControl.audit(player, "toggle-end-access");
                endAccessService.setOpen(!endAccessService.isOpen());
                notify(player, endAccessService.isOpen() ? "The End is now open." : "The End is now closed.");
            }
            case 28 -> {
                ownerControl.audit(player, "start-nametag-event");
                if (eventManager.start(ServerEventType.NAME_TAG, VillagerType.TOP_ARMOR)) {
                    notify(player, "Started nametag event.");
                } else {
                    reject(player, "Could not start the nametag event.");
                }
            }
            case 29 -> {
                ownerControl.audit(player, "start-cat-hunt-event");
                if (eventManager.start(ServerEventType.CAT_HUNT, VillagerType.TOOLS)) {
                    notify(player, "Started cat hunt event.");
                } else {
                    reject(player, "Could not start the cat hunt event.");
                }
            }
            case 31 -> {
                ownerControl.audit(player, "stop-active-event");
                eventManager.stop("stopped from owner control");
                notify(player, "Stopped the active event.");
            }
            case 33 -> {
                ownerControl.audit(player, "give-god-test-kit");
                for (GodItemType type : GodItemType.values()) {
                    player.getInventory().addItem(godItems.createItem(type));
                }
                notify(player, "Loaded the god test kit.");
            }
            case 35 -> {
                ownerControl.audit(player, "reset-world-border");
                WorldBorder border = player.getWorld().getWorldBorder();
                border.setCenter(defaultCenterX, defaultCenterZ);
                border.setSize(defaultBorderSize);
                border.setWarningDistance(defaultWarningDistance);
                border.setWarningTime(defaultWarningTime);
                notify(player, "Reset the world border.");
            }
            case 40 -> {
                ownerControl.audit(player, "create-recovery-export");
                player.closeInventory();
                player.sendMessage(messages.prefixed("owner-control.export", "&eBuilding recovery export archive..."));
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        File archive = exportService.createRecoveryArchive();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7F, 1.0F);
                            player.sendMessage(messages.format(
                                "owner-control.export-ready",
                                "&aRecovery export created: &f{path}",
                                Map.of("path", archive.getAbsolutePath())
                            ));
                        });
                    } catch (Exception exception) {
                        Bukkit.getScheduler().runTask(plugin, () -> reject(player, "Recovery export failed: " + exception.getMessage()));
                    }
                });
                return;
            }
            case 42 -> {
                ownerControl.audit(player, "toggle-dragon-egg-rules");
                ownerControl.setDragonEggRulesEnabled(!ownerControl.dragonEggRulesEnabled());
                notify(player, ownerControl.dragonEggRulesEnabled()
                    ? "Dragon egg rules are now enabled."
                    : "Dragon egg rules are now disabled.");
            }
            case 49 -> notify(player, "Refreshing owner controls.");
            case 53 -> {
                player.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        godMenuGui.open(player, ownerControl, endAccessService);
    }

    private void notify(Player player, String fallback) {
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6F, 1.2F);
        player.sendMessage(messages.prefixed("owner-control.updated", "&a" + fallback));
    }

    private void reject(Player player, String fallback) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.8F);
        player.sendMessage(messages.format("owner-control.failed", "&c{reason}", Map.of("reason", fallback)));
    }
}
