package io.github.diamondsmp.platform.paper.listener;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import io.github.diamondsmp.platform.paper.beacon.GodBeaconCoreType;
import io.github.diamondsmp.platform.paper.beacon.GodBeaconRecord;
import io.github.diamondsmp.platform.paper.beacon.GodBeaconService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.BeaconView;
import org.bukkit.plugin.java.JavaPlugin;

public final class GodBeaconListener implements Listener {
    private final JavaPlugin plugin;
    private final GodBeaconService beacons;

    public GodBeaconListener(JavaPlugin plugin, GodBeaconService beacons) {
        this.plugin = plugin;
        this.beacons = beacons;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBeaconInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.BEACON) {
            return;
        }
        GodBeaconCoreType type = beacons.cores().resolve(event.getPlayer().getInventory().getItemInMainHand());
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        GodBeaconService.UpgradeAttemptResult result = beacons.upgrade(event.getPlayer(), block, type);
        event.getPlayer().sendMessage(color(result.message()));
        if (result.success()) {
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.0F);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.BEACON) {
            return;
        }
        beacons.restorePlacedBeacon(event.getBlockPlaced(), event.getItemInHand(), event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BEACON || !beacons.shouldPreservePortableDrop(block)) {
            return;
        }
        GodBeaconRecord removed = beacons.removeForPortableDrop(block);
        if (removed == null) {
            return;
        }
        event.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation(), beacons.createPortableBeaconItem(removed));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        preserveExplodedBeacons(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        preserveExplodedBeacons(event.blockList());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (event.getDamage() <= 0) {
            return;
        }
        if (beacons.hasDurabilityProtection(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        event.setExhaustion((float) (event.getExhaustion() * beacons.exhaustionMultiplier(player)));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBeaconEffect(BeaconEffectEvent event) {
        beacons.handleBeaconEffectEvent(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView() instanceof BeaconView beaconView) {
            beacons.syncEffects(beaconView);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSmeltingBreak(BlockBreakEvent event) {
        if (!event.isDropItems()) {
            return;
        }
        if (!beacons.isSmeltEligible(event.getBlock().getType())) {
            return;
        }
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }
        if (!beacons.shouldProcSmelting(event.getPlayer())) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>(event.getBlock().getDrops(tool, event.getPlayer()));
        if (drops.isEmpty()) {
            return;
        }
        event.setDropItems(false);
        for (ItemStack drop : drops) {
            ItemStack converted = convertDrop(drop);
            if (converted != null) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), converted);
            }
        }
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            beacons.shutdown();
        }
    }

    private void preserveExplodedBeacons(List<Block> blocks) {
        List<Block> processed = new ArrayList<>();
        for (Block block : blocks) {
            if (block.getType() != Material.BEACON || !beacons.shouldPreservePortableDrop(block)) {
                continue;
            }
            GodBeaconRecord removed = beacons.removeForPortableDrop(block);
            if (removed != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), beacons.createPortableBeaconItem(removed));
                processed.add(block);
            }
        }
        blocks.removeAll(processed);
        for (Block block : processed) {
            block.setType(Material.AIR, false);
        }
    }

    private ItemStack convertDrop(ItemStack drop) {
        if (drop == null) {
            return null;
        }
        return switch (drop.getType()) {
            case RAW_IRON -> new ItemStack(Material.IRON_INGOT, drop.getAmount());
            case RAW_GOLD -> new ItemStack(Material.GOLD_INGOT, drop.getAmount());
            case RAW_COPPER -> new ItemStack(Material.COPPER_INGOT, drop.getAmount());
            default -> drop;
        };
    }

    private String color(String message) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
