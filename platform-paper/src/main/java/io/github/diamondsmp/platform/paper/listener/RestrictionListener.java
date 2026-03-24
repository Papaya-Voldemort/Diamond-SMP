package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.config.CraftingSettings;
import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.item.DiamondPerkRegistry;
import io.github.diamondsmp.platform.paper.item.DiamondPerkType;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.item.GodItemType;
import io.github.diamondsmp.platform.paper.villager.DiamondMasterTradeService;
import io.github.diamondsmp.platform.paper.villager.GodVillagerService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
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
    private static final Map<Enchantment, Integer> RESTRICTED_ENCHANT_CAPS = Map.of(
        Enchantment.PROTECTION, 3,
        Enchantment.SHARPNESS, 4
    );

    private final PluginSettings settings;
    private final CraftingSettings craftingSettings;
    private final GodItemRegistry godItems;
    private final DiamondPerkRegistry perks;
    private final GodVillagerService godVillagers;
    private final DiamondMasterTradeService diamondMasterTrades;
    private final NamespacedKey prestigeGappleKey;

    public RestrictionListener(
        JavaPlugin plugin,
        PluginSettings settings,
        CraftingSettings craftingSettings,
        GodItemRegistry godItems,
        DiamondPerkRegistry perks,
        GodVillagerService godVillagers,
        DiamondMasterTradeService diamondMasterTrades
    ) {
        this.settings = settings;
        this.craftingSettings = craftingSettings;
        this.godItems = godItems;
        this.perks = perks;
        this.godVillagers = godVillagers;
        this.diamondMasterTrades = diamondMasterTrades;
        this.prestigeGappleKey = new NamespacedKey(plugin, "prestige-god-apple-result");
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack prestigeGapple = preparePrestigeGapple(inventory);
        if (prestigeGapple != null) {
            inventory.setResult(prestigeGapple);
            return;
        }
        ItemStack result = inventory.getResult();
        if (result == null) {
            return;
        }
        if (craftingSettings.easyGaps() && result.getType() == Material.GOLDEN_APPLE) {
            result = result.clone();
            result.setAmount(8);
            inventory.setResult(result);
            return;
        }
        if (craftingSettings.easyCobs() && result.getType() == Material.COBWEB) {
            result = result.clone();
            result.setAmount(5);
            inventory.setResult(result);
            return;
        }
        if (isRestrictedNetherite(result)) {
            inventory.setResult(null);
            return;
        }
        inventory.setResult(sanitizeRestrictedEnchants(result));
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (isPrestigeGappleRecipe(event.getInventory().getMatrix())) {
            handlePrestigeGappleCraft(event);
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        if (isRestrictedNetherite(current)) {
            event.setCancelled(true);
            return;
        }
        event.setCurrentItem(sanitizeRestrictedEnchants(current));
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
        ItemStack left = event.getInventory().getFirstItem();
        ItemStack right = event.getInventory().getSecondItem();
        DiamondPerkType perk = perks.resolveBook(right);
        if (perk != null) {
            event.setResult(perks.apply(perk, left));
            return;
        }
        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }
        if (isRestrictedNetherite(result)) {
            event.setResult(null);
            return;
        }
        event.setResult(sanitizeRestrictedEnchants(result));
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (!settings.worldRules().disableRestrictedEnchants()) {
            return;
        }
        for (Map.Entry<Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet()) {
            entry.setValue(capLevel(entry.getKey(), entry.getValue()));
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
        List<ItemStack> loot = event.getLoot();
        for (int index = 0; index < loot.size(); ) {
            ItemStack item = loot.get(index);
            if (item.getType() == Material.ANCIENT_DEBRIS) {
                loot.remove(index);
                continue;
            }
            ItemStack sanitized = sanitizeRestrictedEnchants(item);
            if (isRestrictedNetherite(sanitized) || hasRestrictedEnchant(sanitized)) {
                loot.remove(index);
                continue;
            }
            loot.set(index, sanitized);
            index++;
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
            rebalanceOverworldOres(event);
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
    public void onJoin(PlayerJoinEvent event) {
        sanitizeInventory(event.getPlayer().getInventory());
        sanitizeInventory(event.getPlayer().getEnderChest());
    }

    @EventHandler
    public void onRestrictedInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        ItemStack sanitizedCurrent = sanitizeRestrictedEnchants(current);
        if (!sanitizedCurrent.equals(current)) {
            event.setCurrentItem(sanitizedCurrent);
            current = sanitizedCurrent;
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            ItemStack sanitizedCursor = sanitizeRestrictedEnchants(cursor);
            if (!sanitizedCursor.equals(cursor)) {
                event.getView().setCursor(sanitizedCursor);
            }
        }
        InventoryView view = event.getView();
        if (!(view.getTopInventory() instanceof AnvilInventory || view.getTopInventory() instanceof SmithingInventory)) {
            return;
        }
        if (event.getRawSlot() != 2) {
            return;
        }
        if (isRestrictedNetherite(current)) {
            event.setCancelled(true);
            return;
        }
        ItemStack sanitized = sanitizeRestrictedEnchants(current);
        if (hasRestrictedEnchant(sanitized)) {
            event.setCancelled(true);
            return;
        }
        event.setCurrentItem(sanitized);
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (isRestrictedNetherite(result)) {
            event.setCancelled(true);
            return;
        }
        ItemStack sanitized = sanitizeRestrictedEnchants(result);
        if (hasRestrictedEnchant(sanitized)) {
            event.setCancelled(true);
            return;
        }
        if (!sanitized.equals(result)) {
            event.setRecipe(copyRecipeWithResult(event.getRecipe(), sanitized));
        }
        if (event.getEntity() instanceof Villager villager) {
            diamondMasterTrades.ensureTrades(villager);
        }
    }

    @EventHandler
    public void onMerchantOpen(InventoryOpenEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory inventory)) {
            return;
        }
        if (!(inventory.getMerchant() instanceof Villager villager)) {
            return;
        }
        diamondMasterTrades.ensureTrades(villager);
        sanitizeMerchantRecipes(villager);
    }

    private void stripAncientDebris(ChunkLoadEvent event) {
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

    private void rebalanceOverworldOres(ChunkLoadEvent event) {
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
                    PluginSettings.OreBalance balance = oreBalance(block.getType());
                    if (balance == null || !balance.enabled() || !processed.add(block.getBlockKey())) {
                        continue;
                    }
                    List<Block> vein = collectVein(block, processed, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                    expandVein(vein, block.getType(), balance, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                    addNearbyClusters(vein, block.getType(), balance, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                    if (balance.exposedMaxAdditions() > 0 && isExposed(block, minBlockX, maxBlockX, minBlockZ, maxBlockZ)) {
                        multiplyExposed(block, block.getType(), balance.exposedMaxAdditions(), minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                    }
                }
            }
        }
    }

    private PluginSettings.OreBalance oreBalance(Material material) {
        return switch (material) {
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> settings.worldRules().diamondOre();
            case IRON_ORE, DEEPSLATE_IRON_ORE -> settings.worldRules().ironOre();
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> settings.worldRules().goldOre();
            case COAL_ORE, DEEPSLATE_COAL_ORE -> settings.worldRules().coalOre();
            default -> null;
        };
    }

    private List<Block> collectVein(Block start, Set<Long> processed, int minBlockX, int maxBlockX, int minBlockZ, int maxBlockZ) {
        List<Block> vein = new ArrayList<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        Material target = start.getType();
        while (!queue.isEmpty()) {
            Block block = queue.removeFirst();
            if (block.getType() != target) {
                continue;
            }
            vein.add(block);
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

    private void expandVein(
        List<Block> vein,
        Material oreType,
        PluginSettings.OreBalance balance,
        int minBlockX,
        int maxBlockX,
        int minBlockZ,
        int maxBlockZ
    ) {
        int extra = Math.min(
            balance.maxAddedBlocksPerVein(),
            Math.max(0, (int) Math.ceil(vein.size() * (balance.veinSizeMultiplier() - 1.0D)))
        );
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
                        if (candidate == null || existing.contains(candidate.getBlockKey()) || !converted.add(candidate.getBlockKey())) {
                            continue;
                        }
                        if (!isReplaceableOreHost(candidate, oreType)) {
                            continue;
                        }
                        candidate.setType(oreType, false);
                        extra--;
                    }
                }
            }
        }
    }

    private void addNearbyClusters(
        List<Block> vein,
        Material oreType,
        PluginSettings.OreBalance balance,
        int minBlockX,
        int maxBlockX,
        int minBlockZ,
        int maxBlockZ
    ) {
        if (balance.extraClusterAttempts() <= 0 || balance.maxAddedBlocksPerVein() <= 0 || vein.isEmpty()) {
            return;
        }
        Set<Long> converted = new HashSet<>();
        int extra = balance.maxAddedBlocksPerVein();
        for (int attempt = 0; attempt < balance.extraClusterAttempts() && extra > 0; attempt++) {
            Block anchor = vein.get(ThreadLocalRandom.current().nextInt(vein.size()));
            int dx = ThreadLocalRandom.current().nextInt(-3, 4);
            int dy = ThreadLocalRandom.current().nextInt(-1, 2);
            int dz = ThreadLocalRandom.current().nextInt(-3, 4);
            Block candidate = getRelativeIfInsideChunk(anchor, dx, dy, dz, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
            if (candidate == null || !converted.add(candidate.getBlockKey()) || !isReplaceableOreHost(candidate, oreType)) {
                continue;
            }
            candidate.setType(oreType, false);
            extra--;
        }
    }

    private void multiplyExposed(
        Block source,
        Material oreType,
        int extra,
        int minBlockX,
        int maxBlockX,
        int minBlockZ,
        int maxBlockZ
    ) {
        Set<Long> converted = new HashSet<>();
        for (int distance = 1; distance <= 2 && extra > 0; distance++) {
            for (int dx = -distance; dx <= distance && extra > 0; dx++) {
                for (int dy = -1; dy <= 1 && extra > 0; dy++) {
                    for (int dz = -distance; dz <= distance && extra > 0; dz++) {
                        Block candidate = getRelativeIfInsideChunk(source, dx, dy, dz, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
                        if (candidate == null) {
                            continue;
                        }
                        if (!converted.add(candidate.getBlockKey()) || !isReplaceableOreHost(candidate, oreType)) {
                            continue;
                        }
                        if (!isExposedCandidate(candidate, minBlockX, maxBlockX, minBlockZ, maxBlockZ)) {
                            continue;
                        }
                        candidate.setType(oreType, false);
                        extra--;
                    }
                }
            }
        }
    }

    private boolean isReplaceableOreHost(Block block, Material oreType) {
        return switch (oreType) {
            case DIAMOND_ORE, IRON_ORE, GOLD_ORE, COAL_ORE -> block.getType() == Material.STONE || block.getType() == Material.TUFF;
            case DEEPSLATE_DIAMOND_ORE, DEEPSLATE_IRON_ORE, DEEPSLATE_GOLD_ORE, DEEPSLATE_COAL_ORE -> block.getType() == Material.DEEPSLATE || block.getType() == Material.TUFF;
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

    private boolean isRestrictedNetherite(ItemStack stack) {
        return stack != null
            && settings.worldRules().disableNetheriteProgression()
            && NETHERITE_ITEMS.contains(stack.getType())
            && !godItems.isAnyGodItem(stack);
    }

    private boolean hasRestrictedEnchant(ItemStack stack) {
        if (stack == null || !settings.worldRules().disableRestrictedEnchants() || godItems.isAnyGodItem(stack) || perks.isPerkItem(stack)) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> entry : stack.getEnchantments().entrySet()) {
            if (isRestricted(entry.getKey(), entry.getValue())) {
                return true;
            }
        }
        if (!(stack.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()) {
            if (isRestricted(entry.getKey(), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private ItemStack sanitizeRestrictedEnchants(ItemStack stack) {
        if (stack == null || !settings.worldRules().disableRestrictedEnchants() || godItems.isAnyGodItem(stack) || perks.isPerkItem(stack)) {
            return stack;
        }
        ItemStack sanitized = stack;
        for (Map.Entry<Enchantment, Integer> entry : new HashMap<>(stack.getEnchantments()).entrySet()) {
            int cappedLevel = capLevel(entry.getKey(), entry.getValue());
            if (cappedLevel == entry.getValue()) {
                continue;
            }
            if (sanitized == stack) {
                sanitized = stack.clone();
            }
            sanitized.removeEnchantment(entry.getKey());
            sanitized.addUnsafeEnchantment(entry.getKey(), cappedLevel);
        }
        ItemMeta meta = sanitized.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta storageMeta)) {
            return sanitized;
        }
        boolean updatedMeta = false;
        for (Map.Entry<Enchantment, Integer> entry : new HashMap<>(storageMeta.getStoredEnchants()).entrySet()) {
            int cappedLevel = capLevel(entry.getKey(), entry.getValue());
            if (cappedLevel == entry.getValue()) {
                continue;
            }
            storageMeta.removeStoredEnchant(entry.getKey());
            storageMeta.addStoredEnchant(entry.getKey(), cappedLevel, true);
            updatedMeta = true;
        }
        if (updatedMeta) {
            if (sanitized == stack) {
                sanitized = stack.clone();
            }
            sanitized.setItemMeta(storageMeta);
        }
        return sanitized;
    }

    private MerchantRecipe copyRecipeWithResult(MerchantRecipe recipe, ItemStack result) {
        MerchantRecipe copy = new MerchantRecipe(
            result,
            recipe.getUses(),
            recipe.getMaxUses(),
            recipe.hasExperienceReward(),
            recipe.getVillagerExperience(),
            recipe.getPriceMultiplier()
        );
        copy.setDemand(recipe.getDemand());
        copy.setSpecialPrice(recipe.getSpecialPrice());
        copy.setIngredients(recipe.getIngredients());
        return copy;
    }

    private void sanitizeInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        boolean changed = false;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) {
                continue;
            }
            ItemStack sanitized = sanitizeRestrictedEnchants(item);
            if (!sanitized.equals(item)) {
                contents[slot] = sanitized;
                changed = true;
            }
        }
        if (changed) {
            inventory.setContents(contents);
        }
    }

    private void sanitizeMerchantRecipes(Villager villager) {
        List<MerchantRecipe> recipes = villager.getRecipes();
        List<MerchantRecipe> sanitizedRecipes = new ArrayList<>(recipes.size());
        boolean changed = false;
        for (MerchantRecipe recipe : recipes) {
            ItemStack result = recipe.getResult();
            if (isRestrictedNetherite(result)) {
                changed = true;
                continue;
            }
            ItemStack sanitized = sanitizeRestrictedEnchants(result);
            if (hasRestrictedEnchant(sanitized)) {
                changed = true;
                continue;
            }
            if (!sanitized.equals(result)) {
                changed = true;
                recipe = copyRecipeWithResult(recipe, sanitized);
            }
            sanitizedRecipes.add(recipe);
        }
        if (changed) {
            villager.setRecipes(sanitizedRecipes);
        }
    }

    private ItemStack preparePrestigeGapple(CraftingInventory inventory) {
        if (!isPrestigeGappleRecipe(inventory.getMatrix())) {
            return null;
        }
        ItemStack result = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, craftingSettings.prestigeGodApple().resultAmount());
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(prestigeGappleKey, PersistentDataType.BYTE, (byte) 1);
        result.setItemMeta(meta);
        return result;
    }

    private boolean isPrestigeGappleRecipe(ItemStack[] matrix) {
        CraftingSettings.PrestigeGodApple recipe = craftingSettings.prestigeGodApple();
        if (!recipe.enabled() || matrix.length < 9) {
            return false;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = matrix[slot];
            if (slot == 4) {
                if (item == null || item.getType() != recipe.centerItem() || item.getAmount() < 1) {
                    return false;
                }
                continue;
            }
            if (item == null || item.getType() != recipe.surroundItem()) {
                return false;
            }
            if (recipe.exactStacksRequired() && item.getAmount() != recipe.surroundStackSize()) {
                return false;
            }
            if (!recipe.exactStacksRequired() && item.getAmount() < recipe.surroundStackSize()) {
                return false;
            }
        }
        return true;
    }

    private void handlePrestigeGappleCraft(CraftItemEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
            return;
        }
        CraftingInventory inventory = event.getInventory();
        if (!isPrestigeGappleRecipe(inventory.getMatrix())) {
            return;
        }
        consumePrestigeGappleIngredients(inventory);
        ItemStack result = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, craftingSettings.prestigeGodApple().resultAmount());
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(result);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.updateInventory();
    }

    private void consumePrestigeGappleIngredients(CraftingInventory inventory) {
        ItemStack[] matrix = inventory.getMatrix();
        for (int slot = 0; slot < matrix.length; slot++) {
            ItemStack item = matrix[slot];
            if (item == null) {
                continue;
            }
            if (slot == 4) {
                item.setAmount(item.getAmount() - 1);
            } else {
                item.setAmount(item.getAmount() - craftingSettings.prestigeGodApple().surroundStackSize());
            }
            if (item.getAmount() <= 0) {
                matrix[slot] = null;
            } else {
                matrix[slot] = item;
            }
        }
        inventory.setMatrix(matrix);
        inventory.setResult(null);
    }

    private int capLevel(Enchantment enchantment, int level) {
        Integer cap = RESTRICTED_ENCHANT_CAPS.get(enchantment);
        if (cap == null) {
            return level;
        }
        return Math.min(level, cap);
    }

    private boolean isRestrictedStrengthPotion(PotionMeta meta) {
        if (meta.getBasePotionType() == PotionType.STRONG_STRENGTH) {
            return true;
        }
        return meta.hasCustomEffects() && meta.getCustomEffects().stream()
            .anyMatch(effect -> effect.getType().equals(PotionEffectType.STRENGTH) && effect.getAmplifier() >= 1);
    }

    private boolean isRestricted(Enchantment enchantment, int level) {
        Integer cap = RESTRICTED_ENCHANT_CAPS.get(enchantment);
        return cap != null && level > cap;
    }
}
