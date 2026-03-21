package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.item.GodItemType;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public final class RestrictionListener implements Listener {
    private static final Set<Material> NETHERITE_ITEMS = EnumSet.of(
        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS,
        Material.NETHERITE_SWORD,
        Material.NETHERITE_AXE,
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_INGOT,
        Material.NETHERITE_BLOCK
    );

    private final PluginSettings settings;
    private final GodItemRegistry godItems;

    public RestrictionListener(PluginSettings settings, GodItemRegistry godItems) {
        this.settings = settings;
        this.godItems = godItems;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        if (result == null) {
            return;
        }
        if (result.getType() == Material.GOLDEN_APPLE) {
            result = result.clone();
            result.setAmount(8);
            inventory.setResult(result);
            return;
        }
        if (result.getType() == Material.COBWEB) {
            result = result.clone();
            result.setAmount(5);
            inventory.setResult(result);
            return;
        }
        if (isRestrictedNetherite(result) || hasRestrictedEnchant(result)) {
            inventory.setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        if (isRestrictedNetherite(current) || hasRestrictedEnchant(current)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        if (result != null && isRestrictedNetherite(result)) {
            inventory.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result != null && (isRestrictedNetherite(result) || hasRestrictedEnchant(result))) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (event.getEnchantsToAdd().entrySet().stream().anyMatch(entry -> isRestricted(entry.getKey(), entry.getValue()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (!settings.worldRules().disableStrengthTwo()) {
            return;
        }
        for (int slot = 0; slot < event.getContents().getSize(); slot++) {
            ItemStack item = event.getContents().getItem(slot);
            if (item == null || !(item.getItemMeta() instanceof PotionMeta meta)) {
                continue;
            }
            if (isRestrictedStrengthPotion(meta)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        Iterator<ItemStack> iterator = event.getLoot().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item.getType() == Material.ANCIENT_DEBRIS || isRestrictedNetherite(item) || hasRestrictedEnchant(item)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }
        if (event.getWorld().getEnvironment() == World.Environment.NETHER
            && settings.worldRules().removeAncientDebrisFromNewChunks()) {
            stripAncientDebris(event);
        }
        if (event.getWorld().getEnvironment() == World.Environment.NORMAL) {
            boostDiamonds(event);
        }
    }

    private void stripAncientDebris(ChunkLoadEvent event) {
        for (BlockState state : event.getChunk().getTileEntities()) {
            // nothing to do for tile entities
        }
        int minY = event.getWorld().getMinHeight();
        int maxY = event.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = event.getChunk().getBlock(x, y, z);
                    if (block.getType() == Material.ANCIENT_DEBRIS) {
                        block.setType(Material.NETHERRACK, false);
                    }
                }
            }
        }
    }

    private void boostDiamonds(ChunkLoadEvent event) {
        Set<Long> processed = new HashSet<>();
        int minBlockX = event.getChunk().getX() << 4;
        int maxBlockX = minBlockX + 15;
        int minBlockZ = event.getChunk().getZ() << 4;
        int maxBlockZ = minBlockZ + 15;
        int minY = event.getWorld().getMinHeight();
        int maxY = event.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = event.getChunk().getBlock(x, y, z);
                    if (!isDiamondOre(block.getType()) || !processed.add(block.getBlockKey())) {
                        continue;
                    }
                    List<Block> vein = collectVein(block, processed, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                    expandVein(vein, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                    if (isExposed(block, minBlockX, maxBlockX, minBlockZ, maxBlockZ)) {
                        multiplyExposed(block, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDiamondBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type != Material.DIAMOND_ORE && type != Material.DEEPSLATE_DIAMOND_ORE) {
            return;
        }
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (godItems.isGodItem(tool, GodItemType.PICKAXE)) {
            return;
        }
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }
        int total = Math.max(1, (int) Math.round(settings.worldRules().diamondDropMultiplier()));
        if (total <= 1) {
            return;
        }
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.DIAMOND, total - 1));
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!(item.getItemMeta() instanceof PotionMeta meta)) {
            return;
        }
        if (settings.worldRules().disableStrengthTwo() && isRestrictedStrengthPotion(meta)
            && !godItems.isGodItem(item, GodItemType.INFINITE_TOTEM)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRestrictedInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        InventoryView view = event.getView();
        if (view.getTopInventory() instanceof AnvilInventory || view.getTopInventory() instanceof SmithingInventory) {
            if (event.getRawSlot() == 2 && (isRestrictedNetherite(current) || hasRestrictedEnchant(current))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (isRestrictedNetherite(result) || hasRestrictedEnchant(result)) {
            event.setCancelled(true);
        }
    }

    private boolean isRestrictedNetherite(ItemStack stack) {
        return settings.worldRules().disableNetheriteProgression()
            && NETHERITE_ITEMS.contains(stack.getType())
            && !godItems.isAnyGodItem(stack);
    }

    private List<Block> collectVein(Block start, Set<Long> processed, int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        List<Block> vein = new java.util.ArrayList<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        Material target = start.getType();
        while (!queue.isEmpty()) {
            Block block = queue.removeFirst();
            if (!isDiamondOre(block.getType()) || block.getType() != target) {
                continue;
            }
            if (!vein.contains(block)) {
                vein.add(block);
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = getRelativeIfInsideChunk(block, dx, dy, dz, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                        if (next == null) {
                            continue;
                        }
                        if (next.getType() == target && processed.add(next.getBlockKey())) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return vein;
    }

    private void expandVein(List<Block> vein, int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        int extra = Math.max(0, (int) Math.ceil(vein.size() * (settings.worldRules().diamondVeinSizeMultiplier() - 1.0D)));
        if (extra == 0) {
            return;
        }
        Set<Long> existing = vein.stream().map(Block::getBlockKey).collect(java.util.stream.Collectors.toSet());
        Set<Long> converted = new HashSet<>();
        for (Block ore : vein) {
            for (int dx = -1; dx <= 1 && extra > 0; dx++) {
                for (int dy = -1; dy <= 1 && extra > 0; dy++) {
                    for (int dz = -1; dz <= 1 && extra > 0; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block candidate = getRelativeIfInsideChunk(ore, dx, dy, dz, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                        if (candidate == null) {
                            continue;
                        }
                        if (existing.contains(candidate.getBlockKey()) || !converted.add(candidate.getBlockKey())) {
                            continue;
                        }
                        if (!isReplaceableDiamondHost(candidate, ore.getType())) {
                            continue;
                        }
                        candidate.setType(ore.getType(), false);
                        extra--;
                    }
                }
            }
        }
    }

    private void multiplyExposed(Block source, int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        int extra = Math.max(0, (int) Math.ceil(settings.worldRules().exposedDiamondMultiplier() - 1.0D));
        Set<Long> converted = new HashSet<>();
        for (int distance = 1; distance <= 2 && extra > 0; distance++) {
            for (int dx = -distance; dx <= distance && extra > 0; dx++) {
                for (int dy = -1; dy <= 1 && extra > 0; dy++) {
                    for (int dz = -distance; dz <= distance && extra > 0; dz++) {
                        Block candidate = getRelativeIfInsideChunk(source, dx, dy, dz, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                        if (candidate == null) {
                            continue;
                        }
                        if (!converted.add(candidate.getBlockKey()) || !isReplaceableDiamondHost(candidate, source.getType())) {
                            continue;
                        }
                        if (!isExposedCandidate(candidate, minBlockX, maxBlockX, minBlockZ, maxBlockZ)) {
                            continue;
                        }
                        candidate.setType(source.getType(), false);
                        extra--;
                    }
                }
            }
        }
    }

    private boolean isDiamondOre(Material material) {
        return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE;
    }

    private boolean isReplaceableDiamondHost(Block block, Material oreType) {
        return switch (oreType) {
            case DIAMOND_ORE -> block.getType() == Material.STONE || block.getType() == Material.TUFF;
            case DEEPSLATE_DIAMOND_ORE -> block.getType() == Material.DEEPSLATE || block.getType() == Material.TUFF;
            default -> false;
        };
    }

    private boolean isExposed(Block block, int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        return isExposedCandidate(block, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
    }

    private boolean isExposedCandidate(Block block, int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        return isOpenFace(getRelativeIfInsideChunk(block, 1, 0, 0, minBlockX, maxBlockX, minBlockZ, maxBlockZ))
            || isOpenFace(getRelativeIfInsideChunk(block, -1, 0, 0, minBlockX, maxBlockX, minBlockZ, maxBlockZ))
            || isOpenFace(getRelativeIfInsideChunk(block, 0, 1, 0, minBlockX, maxBlockX, minBlockZ, maxBlockZ))
            || isOpenFace(getRelativeIfInsideChunk(block, 0, -1, 0, minBlockX, maxBlockX, minBlockZ, maxBlockZ))
            || isOpenFace(getRelativeIfInsideChunk(block, 0, 0, 1, minBlockX, maxBlockX, minBlockZ, maxBlockZ))
            || isOpenFace(getRelativeIfInsideChunk(block, 0, 0, -1, minBlockX, maxBlockX, minBlockZ, maxBlockZ));
    }

    private Block getRelativeIfInsideChunk(Block block, int dx, int dy, int dz, int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        int nextX = block.getX() + dx;
        int nextZ = block.getZ() + dz;
        if (nextX < minBlockX || nextX > maxBlockX || nextZ < minBlockZ || nextZ > maxBlockZ) {
            return null;
        }
        return block.getRelative(dx, dy, dz);
    }

    private boolean isOpenFace(Block block) {
        return block != null && (block.getType().isAir() || block.isLiquid());
    }

    private boolean hasRestrictedEnchant(ItemStack stack) {
        return settings.worldRules().disableRestrictedEnchants()
            && stack.getEnchantments().entrySet().stream().anyMatch(entry -> isRestricted(entry.getKey(), entry.getValue()))
            && !godItems.isAnyGodItem(stack);
    }

    private boolean isRestrictedStrengthPotion(PotionMeta meta) {
        if (meta.getBasePotionType() == PotionType.STRONG_STRENGTH) {
            return true;
        }
        return meta.hasCustomEffects() && meta.getCustomEffects().stream()
            .anyMatch(effect -> effect.getType().equals(PotionEffectType.STRENGTH) && effect.getAmplifier() >= 1);
    }

    private boolean isRestricted(Enchantment enchantment, int level) {
        return (enchantment == Enchantment.PROTECTION && level >= 4)
            || (enchantment == Enchantment.SHARPNESS && level >= 5);
    }
}
