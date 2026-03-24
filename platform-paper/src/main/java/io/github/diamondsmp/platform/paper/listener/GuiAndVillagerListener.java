package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.gui.RulesGui;
import io.github.diamondsmp.platform.paper.villager.DiamondMasterTradeService;
import io.github.diamondsmp.platform.paper.villager.GodVillagerService;
import java.util.Map;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Merchant;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantInventory;

public final class GuiAndVillagerListener implements Listener {
    private final RulesGui rulesGui;
    private final GodVillagerService villagerService;
    private final DiamondMasterTradeService diamondTrades;
    private final MessageBundle messages;

    public GuiAndVillagerListener(RulesGui rulesGui, GodVillagerService villagerService, DiamondMasterTradeService diamondTrades, MessageBundle messages) {
        this.rulesGui = rulesGui;
        this.villagerService = villagerService;
        this.diamondTrades = diamondTrades;
        this.messages = messages;
    }

    @EventHandler
    public void onRulesClick(InventoryClickEvent event) {
        if (rulesGui.isRulesTitle(event.getView().getTitle())) {
            event.setCancelled(true);
        }
        if (!(event.getView().getTopInventory() instanceof MerchantInventory inventory)) {
            return;
        }
        Merchant merchant = inventory.getMerchant();
        if (!(merchant instanceof Villager villager) || !villagerService.isManagedVillager(villager)) {
            return;
        }
        if (!villagerService.canUse((org.bukkit.entity.Player) event.getWhoClicked(), villager)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(messages.format(
                "villager.not-owner",
                "&cThis villager belongs to {player}.",
                Map.of("player", villagerService.ownerNameFor(villager))
            ));
            return;
        }
        if (event.getRawSlot() != 2) {
            return;
        }
        String itemKey = villagerService.itemKeyForResult(event.getCurrentItem());
        if (itemKey != null) {
            villagerService.markPurchased(itemKey);
            event.getWhoClicked().sendMessage(messages.format(
                "villager.purchase",
                "&6Purchased {item}. This trade is now globally retired.",
                Map.of("item", itemKey)
            ));
            villagerService.refreshTrades(villager);
        }
    }

    @EventHandler
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager) || !villagerService.isManagedVillager(villager)) {
            return;
        }
        if (!villagerService.canUse(event.getPlayer(), villager)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.format(
                "villager.not-owner",
                "&cThis villager belongs to {player}.",
                Map.of("player", villagerService.ownerNameFor(villager))
            ));
        }
    }

    @EventHandler
    public void onMerchantOpen(InventoryOpenEvent event) {
        HumanEntity player = event.getPlayer();
        if (!(event.getInventory() instanceof MerchantInventory inventory)) {
            return;
        }
        Merchant merchant = inventory.getMerchant();
        if (!(merchant instanceof Villager villager)) {
            return;
        }
        diamondTrades.ensureTrades(villager);
        if (!villagerService.isManagedVillager(villager)) {
            return;
        }
        if (player instanceof org.bukkit.entity.Player bukkitPlayer && !villagerService.canUse(bukkitPlayer, villager)) {
            event.setCancelled(true);
            player.sendMessage(messages.format(
                "villager.not-owner",
                "&cThis villager belongs to {player}.",
                Map.of("player", villagerService.ownerNameFor(villager))
            ));
            return;
        }
        villagerService.refreshTrades(villager);
        player.sendMessage(messages.prefixed("villager.open", "&eLimited event stock. Bought items disappear globally."));
    }

    @EventHandler
    public void onManagedVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !villagerService.isManagedVillager(villager)) {
            return;
        }
        if (!villagerService.shouldDropEgg(villager, villager.getKiller())) {
            return;
        }
        villagerService.markEggDropped(villager);
        event.getDrops().add(villagerService.createRelocationEgg(villager));
    }

    @EventHandler
    public void onRelocationEggUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!villagerService.isManagedVillagerEgg(event.getItem())) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        org.bukkit.Location spawnLocation = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5D, 0.0D, 0.5D);
        Villager villager = villagerService.spawnFromEgg(spawnLocation, event.getItem());
        if (villager == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getItem() != null) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        }
    }
}
