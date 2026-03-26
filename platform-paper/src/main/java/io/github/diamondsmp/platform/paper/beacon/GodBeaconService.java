package io.github.diamondsmp.platform.paper.beacon;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Barrel;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.BeaconView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

public final class GodBeaconService {
    private static final int TARGET_DISTANCE = 10;
    private static final int STORAGE_RADIUS = 6;
    private static final int VISUAL_RANGE = 32;
    private static final String MINING_SPEED_MODIFIER_NAME = "god-beacon-haste-bonus";
    private static final Set<Material> SMELT_ELIGIBLE = EnumSet.of(
        Material.IRON_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.GOLD_ORE,
        Material.DEEPSLATE_GOLD_ORE,
        Material.COPPER_ORE,
        Material.DEEPSLATE_COPPER_ORE,
        Material.NETHER_GOLD_ORE
    );

    private final JavaPlugin plugin;
    private final GodBeaconStore store;
    private final GodBeaconCoreRegistry cores;
    private final NamespacedKey itemTierKey;
    private final NamespacedKey itemOwnerKey;
    private final NamespacedKey itemCreatedKey;
    private final NamespacedKey itemPrimaryKey;
    private final NamespacedKey itemSecondaryKey;
    private final Map<BeaconPosition, GodBeaconRecord> records;
    private final Map<UUID, ActiveContext> activeContexts = new HashMap<>();
    private final Map<UUID, LingerContext> lingerContexts = new HashMap<>();
    private final Map<UUID, Double> regenerationAccumulators = new HashMap<>();
    private final Map<String, Long> ambientCooldowns = new HashMap<>();
    private final Map<String, Long> entryCooldowns = new HashMap<>();
    private final Set<String> insidePairs = new HashSet<>();
    private BukkitTask tickerTask;
    private BukkitTask visualsTask;
    private BukkitTask flushTask;
    private boolean dirty;
    private long dirtyAtTick;
    private final AttributeModifier hasteModifier;

    public GodBeaconService(JavaPlugin plugin, GodBeaconStore store, GodBeaconCoreRegistry cores) {
        this.plugin = plugin;
        this.store = store;
        this.cores = cores;
        this.itemTierKey = new NamespacedKey(plugin, "god-beacon-tier");
        this.itemOwnerKey = new NamespacedKey(plugin, "god-beacon-owner");
        this.itemCreatedKey = new NamespacedKey(plugin, "god-beacon-created");
        this.itemPrimaryKey = new NamespacedKey(plugin, "god-beacon-primary");
        this.itemSecondaryKey = new NamespacedKey(plugin, "god-beacon-secondary");
        this.records = new HashMap<>(store.load());
        this.hasteModifier = new AttributeModifier(
            NamespacedKey.minecraft(MINING_SPEED_MODIFIER_NAME),
            0.10D,
            AttributeModifier.Operation.ADD_SCALAR
        );
    }

    public void start() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        visualsTask = Bukkit.getScheduler().runTaskTimer(plugin, this::emitVisuals, 40L, 40L);
        flushTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirtyIfNeeded, 100L, 100L);
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        if (visualsTask != null) {
            visualsTask.cancel();
        }
        if (flushTask != null) {
            flushTask.cancel();
        }
        flushNow();
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeMiningSpeedModifier(player);
        }
    }

    public GodBeaconCoreRegistry cores() {
        return cores;
    }

    public Optional<GodBeaconRecord> findRecord(Block block) {
        return Optional.ofNullable(records.get(BeaconPosition.fromBlock(block)));
    }

    public GodBeaconTier resolveTier(Block block) {
        return findRecord(block).map(GodBeaconRecord::tier).orElse(GodBeaconTier.VANILLA);
    }

    public boolean isTracked(Block block) {
        return records.containsKey(BeaconPosition.fromBlock(block));
    }

    public Optional<TargetedBeacon> findTarget(Player player, int maxDistance) {
        Block lookedAt = rayTraceBeacon(player, maxDistance);
        if (lookedAt != null) {
            return Optional.of(targetOf(lookedAt));
        }
        Location playerLocation = player.getLocation();
        TargetedBeacon nearest = null;
        double bestDistance = Double.MAX_VALUE;
        int bestYDiff = Integer.MAX_VALUE;
        for (World world : List.of(player.getWorld())) {
            for (GodBeaconRecord record : records.values()) {
                if (!record.position().worldId().equals(world.getUID())) {
                    continue;
                }
                Block block = world.getBlockAt(record.position().x(), record.position().y(), record.position().z());
                if (block.getType() != Material.BEACON) {
                    continue;
                }
                double distance = block.getLocation().add(0.5D, 0.5D, 0.5D).distanceSquared(playerLocation);
                if (distance > (double) maxDistance * maxDistance) {
                    continue;
                }
                int yDiff = Math.abs(block.getY() - playerLocation.getBlockY());
                if (nearest == null
                    || distance < bestDistance
                    || (distance == bestDistance && yDiff < bestYDiff)
                    || (distance == bestDistance && yDiff == bestYDiff && compareBlockOrder(block, nearest.block()) < 0)) {
                    nearest = targetOf(block);
                    bestDistance = distance;
                    bestYDiff = yDiff;
                }
            }
            for (Block nearby : nearbyVanillaBeacons(playerLocation, maxDistance)) {
                double distance = nearby.getLocation().add(0.5D, 0.5D, 0.5D).distanceSquared(playerLocation);
                int yDiff = Math.abs(nearby.getY() - playerLocation.getBlockY());
                if (nearest == null
                    || distance < bestDistance
                    || (distance == bestDistance && yDiff < bestYDiff)
                    || (distance == bestDistance && yDiff == bestYDiff && compareBlockOrder(nearby, nearest.block()) < 0)) {
                    nearest = targetOf(nearby);
                    bestDistance = distance;
                    bestYDiff = yDiff;
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    public UpgradeAttemptResult upgrade(Player player, Block block, GodBeaconCoreType type) {
        if (block.getType() != Material.BEACON) {
            return UpgradeAttemptResult.failure("&cThat block is not a beacon.");
        }
        GodBeaconRecord existing = records.get(BeaconPosition.fromBlock(block));
        GodBeaconTier currentTier = existing == null ? GodBeaconTier.VANILLA : existing.tier();
        if (currentTier != type.requiredTier()) {
            return UpgradeAttemptResult.failure("&cThat beacon must be " + type.requiredTier().displayName() + " before using this core.");
        }
        StructureState structure = structureState(block, type.requiredTier());
        if (!structure.validForTier()) {
            return UpgradeAttemptResult.failure("&cThis beacon structure is inactive. Rebuild the pyramid first.");
        }
        PaymentPlan paymentPlan = buildPaymentPlan(player, block, type, true);
        if (!paymentPlan.canAfford()) {
            return UpgradeAttemptResult.failure("&cMissing: &f" + paymentPlan.missingDiamondBlocks() + " Diamond Blocks&c, &f"
                + paymentPlan.missingNetherStars() + " Nether Star" + (paymentPlan.missingNetherStars() == 1 ? "" : "s") + "&c.");
        }
        if (!consumePayment(player, paymentPlan, type, block)) {
            return UpgradeAttemptResult.failure("&cUpgrade payment changed before it could be consumed. Try again.");
        }
        GodBeaconEffect primary = readPrimaryEffect(block);
        GodBeaconEffect secondary = readSecondaryEffect(block);
        GodBeaconRecord next = new GodBeaconRecord(
            BeaconPosition.fromBlock(block),
            type.resultTier(),
            existing == null ? player.getUniqueId() : existing.ownerId(),
            existing == null ? Instant.now() : existing.createdAt(),
            primary,
            secondary
        );
        records.put(next.position(), next);
        dirtyNow();
        flushNow();
        playUpgradeBurst(block, type.resultTier());
        return UpgradeAttemptResult.success(successMessage(type.resultTier()), paymentPlan);
    }

    public boolean setTier(Block block, GodBeaconTier tier, UUID ownerId) {
        if (block.getType() != Material.BEACON) {
            return false;
        }
        BeaconPosition position = BeaconPosition.fromBlock(block);
        if (tier == GodBeaconTier.VANILLA) {
            if (records.remove(position) != null) {
                dirtyNow();
                flushNow();
            }
            return true;
        }
        GodBeaconRecord current = records.get(position);
        GodBeaconRecord next = new GodBeaconRecord(
            position,
            tier,
            current == null ? ownerId : current.ownerId(),
            current == null ? Instant.now() : current.createdAt(),
            current == null ? readPrimaryEffect(block) : current.primaryEffect(),
            current == null ? readSecondaryEffect(block) : current.secondaryEffect()
        );
        records.put(position, next);
        dirtyNow();
        flushNow();
        applyStoredEffects(block, next);
        return true;
    }

    public void restorePlacedBeacon(Block block, ItemStack placedItem, Player owner) {
        GodBeaconRecord restored = readRecordFromItem(block, placedItem, owner == null ? null : owner.getUniqueId());
        if (restored == null || restored.tier() == GodBeaconTier.VANILLA) {
            return;
        }
        records.put(restored.position(), restored);
        dirtyNow();
        flushNow();
        Bukkit.getScheduler().runTask(plugin, () -> applyStoredEffects(block, restored));
    }

    public ItemStack createPortableBeaconItem(GodBeaconRecord record) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(record.tier().displayName(), NamedTextColor.AQUA));
        meta.lore(List.of(
            Component.text("Portable beacon state", NamedTextColor.DARK_GRAY),
            Component.text("Tier: " + record.tier().displayName(), NamedTextColor.GRAY),
            Component.text("Primary: " + displayEffect(record.primaryEffect()), NamedTextColor.BLUE),
            Component.text("Secondary: " + displayEffect(record.secondaryEffect()), NamedTextColor.BLUE)
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemTierKey, PersistentDataType.STRING, record.tier().key());
        if (record.ownerId() != null) {
            pdc.set(itemOwnerKey, PersistentDataType.STRING, record.ownerId().toString());
        }
        pdc.set(itemCreatedKey, PersistentDataType.STRING, record.createdAt().toString());
        if (record.primaryEffect() != null) {
            pdc.set(itemPrimaryKey, PersistentDataType.STRING, record.primaryEffect().key());
        }
        if (record.secondaryEffect() != null) {
            pdc.set(itemSecondaryKey, PersistentDataType.STRING, record.secondaryEffect().key());
        }
        item.setItemMeta(meta);
        return item;
    }

    public GodBeaconRecord removeForPortableDrop(Block block) {
        GodBeaconRecord removed = records.remove(BeaconPosition.fromBlock(block));
        if (removed != null) {
            dirtyNow();
            flushNow();
        }
        return removed;
    }

    public void handleBeaconEffectEvent(BeaconEffectEvent event) {
        GodBeaconRecord record = records.get(BeaconPosition.fromBlock(event.getBlock()));
        if (record == null) {
            return;
        }
        if (structureState(event.getBlock(), record.tier()).customHandlingActive()) {
            event.setCancelled(true);
        }
    }

    public void syncEffects(BeaconView view) {
        if (!(view.getTopInventory().getLocation() != null && view.getTopInventory().getLocation().getBlock().getType() == Material.BEACON)) {
            return;
        }
        Block block = Objects.requireNonNull(view.getTopInventory().getLocation()).getBlock();
        GodBeaconRecord record = records.get(BeaconPosition.fromBlock(block));
        if (record == null) {
            return;
        }
        GodBeaconEffect primary = GodBeaconEffect.fromPotion(view.getPrimaryEffect());
        GodBeaconEffect secondary = GodBeaconEffect.fromPotion(view.getSecondaryEffect());
        if (secondary == primary && (record.tier() == GodBeaconTier.VANILLA || record.tier() == GodBeaconTier.INFUSED)) {
            secondary = null;
        }
        GodBeaconRecord updated = record.withEffects(primary, secondary);
        records.put(updated.position(), updated);
        dirtyNow();
        applyStoredEffects(block, updated);
    }

    public Optional<ActiveContext> activeContext(Player player) {
        ActiveContext context = activeContexts.get(player.getUniqueId());
        if (context == null || context.expiresAtTick() < Bukkit.getCurrentTick()) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public PaymentPlan buildPaymentPlan(Player player, Block block, GodBeaconCoreType type, boolean requireCoreInHand) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean hasCore = !requireCoreInHand || cores.isCore(hand, type);
        List<InventoryRef> inventories = collectPaymentInventories(player, block);
        int diamonds = countMaterial(player, inventories, Material.DIAMOND_BLOCK, true);
        int stars = countMaterial(player, inventories, Material.NETHER_STAR, true);
        return new PaymentPlan(
            type,
            hasCore,
            diamonds,
            stars,
            Math.max(0, type.diamondBlockCost() - diamonds),
            Math.max(0, type.netherStarCost() - stars),
            inventories.size()
        );
    }

    public Collection<GodBeaconRecord> snapshot() {
        return List.copyOf(records.values());
    }

    public StructureState structureState(Block block, GodBeaconTier tier) {
        BlockState state = block.getState();
        if (!(state instanceof Beacon beacon)) {
            return new StructureState(0, 0.0D, false, false);
        }
        int baseTier = beacon.getTier();
        boolean validVanilla = baseTier > 0;
        boolean active = validVanilla && (!tier.requiresFullPyramid() || baseTier >= 4);
        double baseRadius = validVanilla ? baseTier * 10.0D + 10.0D : 0.0D;
        return new StructureState(baseTier, baseRadius, validVanilla, active);
    }

    public String displayEffect(GodBeaconEffect effect) {
        return effect == null ? "None" : effect.displayName();
    }

    private void tick() {
        long currentTick = Bukkit.getCurrentTick();
        Map<UUID, CandidateContext> best = new HashMap<>();
        Set<String> currentPairs = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            List<GodBeaconRecord> worldRecords = records.values().stream()
                .filter(record -> record.position().worldId().equals(world.getUID()))
                .toList();
            if (worldRecords.isEmpty()) {
                continue;
            }
            for (GodBeaconRecord record : worldRecords) {
                Block block = world.getBlockAt(record.position().x(), record.position().y(), record.position().z());
                if (block.getType() != Material.BEACON) {
                    continue;
                }
                StructureState structure = structureState(block, record.tier());
                if (!structure.customHandlingActive()) {
                    continue;
                }
                double radius = structure.baseRadius() * record.tier().radiusMultiplier();
                double radiusSquared = radius * radius;
                for (Player player : world.getPlayers()) {
                    if (!player.isOnline() || player.isDead()) {
                        continue;
                    }
                    double distanceSquared = player.getLocation().distanceSquared(block.getLocation().add(0.5D, 0.5D, 0.5D));
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }
                    CandidateContext candidate = new CandidateContext(record, block, structure, radius, distanceSquared);
                    best.merge(player.getUniqueId(), candidate, GodBeaconService::chooseBetter);
                    String pairKey = pairKey(player.getUniqueId(), record.position());
                    currentPairs.add(pairKey);
                    maybePlayEntrySound(player, candidate, currentTick, pairKey);
                    maybePlayAmbientSound(player, candidate, currentTick, pairKey);
                    if (currentTick % record.tier().reapplyTicks() == 0) {
                        applyPotionEffects(player, candidate);
                    }
                }
            }
        }

        insidePairs.clear();
        insidePairs.addAll(currentPairs);
        activeContexts.clear();

        for (Map.Entry<UUID, CandidateContext> entry : best.entrySet()) {
            UUID playerId = entry.getKey();
            CandidateContext candidate = entry.getValue();
            long expires = currentTick + candidate.record().tier().lingerSeconds() * 20L;
            activeContexts.put(playerId, new ActiveContext(candidate.record(), candidate.block(), candidate.radius(), candidate.structure(), currentTick));
            if (candidate.record().tier().lingerSeconds() > 0) {
                lingerContexts.put(playerId, new LingerContext(candidate.record(), candidate.block(), expires));
            } else {
                lingerContexts.remove(playerId);
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                syncMiningSpeedModifier(player, candidate.record());
                applyRegenerationBonus(player, candidate.record());
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (activeContexts.containsKey(player.getUniqueId())) {
                continue;
            }
            removeMiningSpeedModifier(player);
            LingerContext linger = lingerContexts.get(player.getUniqueId());
            if (linger != null && linger.expiresAtTick() >= currentTick) {
                applyRegenerationBonus(player, linger.record());
                activeContexts.put(player.getUniqueId(), new ActiveContext(
                    linger.record(),
                    linger.block(),
                    0.0D,
                    new StructureState(0, 0.0D, false, false),
                    linger.expiresAtTick()
                ));
            } else {
                lingerContexts.remove(player.getUniqueId());
                regenerationAccumulators.remove(player.getUniqueId());
            }
        }
    }

    private void emitVisuals() {
        for (GodBeaconRecord record : records.values()) {
            World world = Bukkit.getWorld(record.position().worldId());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(record.position().x(), record.position().y(), record.position().z());
            if (block.getType() != Material.BEACON) {
                continue;
            }
            StructureState structure = structureState(block, record.tier());
            if (!structure.customHandlingActive()) {
                continue;
            }
            boolean nearbyPlayers = world.getPlayers().stream()
                .anyMatch(player -> player.getLocation().distanceSquared(block.getLocation().add(0.5D, 0.5D, 0.5D)) <= VISUAL_RANGE * VISUAL_RANGE);
            if (!nearbyPlayers) {
                continue;
            }
            Location beam = block.getLocation().add(0.5D, 1.2D, 0.5D);
            int beamParticles = switch (record.tier()) {
                case INFUSED -> 4;
                case RESONANT -> 6;
                case ASCENDED -> 8;
                case GOD -> 10;
                default -> 0;
            };
            for (int i = 0; i < beamParticles; i++) {
                world.spawnParticle(Particle.END_ROD, beam, 1, 0.15D, 0.4D, 0.15D, 0.0D);
            }
            if (record.tier() == GodBeaconTier.GOD) {
                world.spawnParticle(Particle.ENCHANT, block.getLocation().add(0.5D, 0.35D, 0.5D), 6, 0.6D, 0.15D, 0.6D, 0.0D);
            }
        }
    }

    private void flushDirtyIfNeeded() {
        if (!dirty) {
            return;
        }
        long currentTick = Bukkit.getCurrentTick();
        if (currentTick - dirtyAtTick >= 100L) {
            flushNow();
        }
    }

    private void flushNow() {
        if (!dirty) {
            return;
        }
        store.save(records.values());
        dirty = false;
    }

    private void dirtyNow() {
        dirty = true;
        dirtyAtTick = Bukkit.getCurrentTick();
    }

    private static int compareBlockOrder(Block left, Block right) {
        int byY = Integer.compare(left.getY(), right.getY());
        if (byY != 0) {
            return byY;
        }
        int byX = Integer.compare(left.getX(), right.getX());
        if (byX != 0) {
            return byX;
        }
        return Integer.compare(left.getZ(), right.getZ());
    }

    private static CandidateContext chooseBetter(CandidateContext left, CandidateContext right) {
        if (left.record().tier().level() != right.record().tier().level()) {
            return left.record().tier().level() > right.record().tier().level() ? left : right;
        }
        return left.distanceSquared() <= right.distanceSquared() ? left : right;
    }

    private Block rayTraceBeacon(Player player, int maxDistance) {
        RayTraceResult trace = player.rayTraceBlocks(maxDistance);
        if (trace == null || trace.getHitBlock() == null) {
            return null;
        }
        return trace.getHitBlock().getType() == Material.BEACON ? trace.getHitBlock() : null;
    }

    private List<Block> nearbyVanillaBeacons(Location center, int maxDistance) {
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }
        List<Block> results = new ArrayList<>();
        int minX = center.getBlockX() - maxDistance;
        int maxX = center.getBlockX() + maxDistance;
        int minY = center.getBlockY() - maxDistance;
        int maxY = center.getBlockY() + maxDistance;
        int minZ = center.getBlockZ() - maxDistance;
        int maxZ = center.getBlockZ() + maxDistance;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.BEACON) {
                        results.add(block);
                    }
                }
            }
        }
        return results;
    }

    private TargetedBeacon targetOf(Block block) {
        GodBeaconRecord record = records.get(BeaconPosition.fromBlock(block));
        GodBeaconTier tier = record == null ? GodBeaconTier.VANILLA : record.tier();
        StructureState structure = structureState(block, tier);
        double radius = structure.baseRadius() * tier.radiusMultiplier();
        return new TargetedBeacon(block, record, tier, structure, radius);
    }

    private List<InventoryRef> collectPaymentInventories(Player player, Block block) {
        List<InventoryRef> refs = new ArrayList<>();
        Set<Inventory> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        World world = block.getWorld();
        for (int x = block.getX() - STORAGE_RADIUS; x <= block.getX() + STORAGE_RADIUS; x++) {
            for (int y = block.getY() - STORAGE_RADIUS; y <= block.getY() + STORAGE_RADIUS; y++) {
                for (int z = block.getZ() - STORAGE_RADIUS; z <= block.getZ() + STORAGE_RADIUS; z++) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    BlockState state = world.getBlockAt(x, y, z).getState();
                    Inventory inventory = resolveInventory(state);
                    if (inventory == null || seen.contains(inventory)) {
                        continue;
                    }
                    seen.add(inventory);
                    refs.add(new InventoryRef(inventory, new Location(world, x, y, z)));
                }
            }
        }
        refs.sort(Comparator
            .comparingDouble((InventoryRef ref) -> ref.location().distanceSquared(block.getLocation()))
            .thenComparingInt(ref -> ref.location().getBlockY())
            .thenComparingInt(ref -> ref.location().getBlockX())
            .thenComparingInt(ref -> ref.location().getBlockZ()));
        return refs;
    }

    private Inventory resolveInventory(BlockState state) {
        if (state instanceof Chest chest) {
            Inventory inventory = chest.getBlockInventory();
            if (inventory instanceof DoubleChestInventory doubleChest) {
                return doubleChest;
            }
            return inventory;
        }
        if (state instanceof Barrel barrel) {
            return barrel.getInventory();
        }
        return null;
    }

    private int countMaterial(Player player, List<InventoryRef> refs, Material material, boolean includeContainers) {
        int total = countPlayerMaterial(player, material, true);
        if (!includeContainers) {
            return total;
        }
        for (InventoryRef ref : refs) {
            total += countInventoryMaterial(ref.inventory(), material);
        }
        return total;
    }

    private int countPlayerMaterial(Player player, Material material, boolean skipCoreSlot) {
        PlayerInventory inventory = player.getInventory();
        int total = 0;
        int heldSlot = inventory.getHeldItemSlot();
        for (int slot = 0; slot <= 35; slot++) {
            if (skipCoreSlot && slot == heldSlot && cores.resolve(inventory.getItem(slot)) != null) {
                continue;
            }
            total += amountOf(inventory.getItem(slot), material);
        }
        ItemStack offHand = inventory.getItemInOffHand();
        if (!(skipCoreSlot && cores.resolve(offHand) != null)) {
            total += amountOf(offHand, material);
        }
        return total;
    }

    private int countInventoryMaterial(Inventory inventory, Material material) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            total += amountOf(inventory.getItem(slot), material);
        }
        return total;
    }

    private boolean consumePayment(Player player, PaymentPlan plan, GodBeaconCoreType type, Block block) {
        if (!plan.hasCoreInHand()) {
            return false;
        }
        List<InventoryRef> inventories = collectPaymentInventories(player, block);
        if (countPlayerMaterial(player, Material.DIAMOND_BLOCK, true) + inventories.stream().mapToInt(ref -> countInventoryMaterial(ref.inventory(), Material.DIAMOND_BLOCK)).sum() < type.diamondBlockCost()) {
            return false;
        }
        if (countPlayerMaterial(player, Material.NETHER_STAR, true) + inventories.stream().mapToInt(ref -> countInventoryMaterial(ref.inventory(), Material.NETHER_STAR)).sum() < type.netherStarCost()) {
            return false;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(hand);
        }
        int remainingDiamonds = removeMaterial(player, Material.DIAMOND_BLOCK, type.diamondBlockCost(), true);
        remainingDiamonds = removeFromContainers(inventories, Material.DIAMOND_BLOCK, remainingDiamonds);
        int remainingStars = removeMaterial(player, Material.NETHER_STAR, type.netherStarCost(), true);
        remainingStars = removeFromContainers(inventories, Material.NETHER_STAR, remainingStars);
        player.updateInventory();
        return remainingDiamonds == 0 && remainingStars == 0;
    }

    private int removeMaterial(Player player, Material material, int amount, boolean skipCoreSlot) {
        int remaining = amount;
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        int heldSlot = inventory.getHeldItemSlot();
        for (int slot = 0; slot <= 35 && remaining > 0; slot++) {
            if (skipCoreSlot && slot == heldSlot && cores.resolve(inventory.getItem(slot)) != null) {
                continue;
            }
            remaining = removeFromStack(inventory, slot, material, remaining);
        }
        if (remaining > 0 && !(skipCoreSlot && cores.resolve(inventory.getItemInOffHand()) != null)) {
            remaining = removeFromOffhand(inventory, material, remaining);
        }
        return remaining;
    }

    private int removeFromContainers(List<InventoryRef> refs, Material material, int remaining) {
        for (InventoryRef ref : refs) {
            Inventory inventory = ref.inventory();
            for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || item.getType() != material) {
                    continue;
                }
                int consumed = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - consumed);
                if (item.getAmount() <= 0) {
                    inventory.setItem(slot, null);
                } else {
                    inventory.setItem(slot, item);
                }
                remaining -= consumed;
            }
        }
        return remaining;
    }

    private int removeFromStack(org.bukkit.inventory.PlayerInventory inventory, int slot, Material material, int remaining) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType() != material) {
            return remaining;
        }
        int consumed = Math.min(item.getAmount(), remaining);
        item.setAmount(item.getAmount() - consumed);
        if (item.getAmount() <= 0) {
            inventory.setItem(slot, null);
        } else {
            inventory.setItem(slot, item);
        }
        return remaining - consumed;
    }

    private int removeFromOffhand(org.bukkit.inventory.PlayerInventory inventory, Material material, int remaining) {
        ItemStack item = inventory.getItemInOffHand();
        if (item == null || item.getType() != material) {
            return remaining;
        }
        int consumed = Math.min(item.getAmount(), remaining);
        item.setAmount(item.getAmount() - consumed);
        if (item.getAmount() <= 0) {
            inventory.setItemInOffHand(null);
        } else {
            inventory.setItemInOffHand(item);
        }
        return remaining - consumed;
    }

    private static int amountOf(ItemStack stack, Material material) {
        return stack != null && stack.getType() == material ? stack.getAmount() : 0;
    }

    private GodBeaconEffect readPrimaryEffect(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof Beacon beacon)) {
            return null;
        }
        return beacon.getPrimaryEffect() == null ? null : GodBeaconEffect.fromPotion(beacon.getPrimaryEffect().getType());
    }

    private GodBeaconEffect readSecondaryEffect(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof Beacon beacon)) {
            return null;
        }
        return beacon.getSecondaryEffect() == null ? null : GodBeaconEffect.fromPotion(beacon.getSecondaryEffect().getType());
    }

    private GodBeaconRecord readRecordFromItem(Block block, ItemStack item, UUID ownerFallback) {
        if (item == null || item.getType() != Material.BEACON || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        GodBeaconTier tier = GodBeaconTier.fromKey(pdc.get(itemTierKey, PersistentDataType.STRING));
        if (tier == GodBeaconTier.VANILLA) {
            return null;
        }
        UUID owner = ownerFallback;
        String storedOwner = pdc.get(itemOwnerKey, PersistentDataType.STRING);
        if (storedOwner != null && !storedOwner.isBlank()) {
            owner = UUID.fromString(storedOwner);
        }
        String created = pdc.get(itemCreatedKey, PersistentDataType.STRING);
        Instant createdAt = created == null ? Instant.now() : Instant.parse(created);
        return new GodBeaconRecord(
            BeaconPosition.fromBlock(block),
            tier,
            owner,
            createdAt,
            GodBeaconEffect.fromKey(pdc.get(itemPrimaryKey, PersistentDataType.STRING)),
            GodBeaconEffect.fromKey(pdc.get(itemSecondaryKey, PersistentDataType.STRING))
        );
    }

    private void applyStoredEffects(Block block, GodBeaconRecord record) {
        BlockState state = block.getState();
        if (!(state instanceof Beacon beacon)) {
            return;
        }
        beacon.setPrimaryEffect(record.primaryEffect() == null ? null : record.primaryEffect().effectType());
        if (record.tier().supportsSecondarySelection()) {
            beacon.setSecondaryEffect(record.secondaryEffect() == null ? null : record.secondaryEffect().effectType());
        } else {
            beacon.setSecondaryEffect(null);
        }
        beacon.update(true, false);
    }

    private void applyPotionEffects(Player player, CandidateContext candidate) {
        GodBeaconRecord record = candidate.record();
        GodBeaconEffect primary = record.primaryEffect();
        GodBeaconEffect secondary = record.tier().supportsSecondarySelection() ? record.secondaryEffect() : null;
        if (primary == null) {
            return;
        }
        int durationTicks = record.tier().reapplyTicks() + Math.max(0, record.tier().lingerSeconds() * 20);
        if (secondary != null && secondary == primary) {
            player.addPotionEffect(new PotionEffect(primary.effectType(), durationTicks, 1, true, false, true));
            return;
        }
        player.addPotionEffect(new PotionEffect(primary.effectType(), durationTicks, 0, true, false, true));
        if (secondary != null) {
            player.addPotionEffect(new PotionEffect(secondary.effectType(), durationTicks, 0, true, false, true));
        }
    }

    private void applyRegenerationBonus(Player player, GodBeaconRecord record) {
        if (record.tier().regenerationBonus() <= 0.0D) {
            regenerationAccumulators.remove(player.getUniqueId());
            return;
        }
        if (record.primaryEffect() != GodBeaconEffect.REGENERATION && record.secondaryEffect() != GodBeaconEffect.REGENERATION) {
            regenerationAccumulators.remove(player.getUniqueId());
            return;
        }
        if (player.isDead() || player.getHealth() >= Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue()) {
            regenerationAccumulators.remove(player.getUniqueId());
            return;
        }
        double total = regenerationAccumulators.getOrDefault(player.getUniqueId(), 0.0D) + record.tier().regenerationBonus();
        if (total >= 1.0D) {
            double heal = Math.floor(total);
            double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            player.setHealth(Math.min(maxHealth, player.getHealth() + heal));
            total -= heal;
        }
        regenerationAccumulators.put(player.getUniqueId(), total);
    }

    private void syncMiningSpeedModifier(Player player, GodBeaconRecord record) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) {
            return;
        }
        boolean shouldApply = record.tier() == GodBeaconTier.GOD
            && (record.primaryEffect() == GodBeaconEffect.HASTE || record.secondaryEffect() == GodBeaconEffect.HASTE);
        boolean present = attribute.getModifiers().stream().anyMatch(modifier -> modifier.getKey().equals(hasteModifier.getKey()));
        if (shouldApply && !present) {
            attribute.addModifier(hasteModifier);
        } else if (!shouldApply && present) {
            removeMiningSpeedModifier(player);
        }
    }

    private void removeMiningSpeedModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.getModifiers().stream()
            .filter(modifier -> modifier.getKey().equals(hasteModifier.getKey()))
            .toList()
            .forEach(attribute::removeModifier);
    }

    private void maybePlayEntrySound(Player player, CandidateContext candidate, long currentTick, String pairKey) {
        if (candidate.record().tier().level() < GodBeaconTier.RESONANT.level()) {
            return;
        }
        if (insidePairs.contains(pairKey)) {
            return;
        }
        long readyAt = entryCooldowns.getOrDefault(pairKey, 0L);
        if (readyAt > currentTick) {
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6F, 1.4F);
        entryCooldowns.put(pairKey, currentTick + 100L);
    }

    private void maybePlayAmbientSound(Player player, CandidateContext candidate, long currentTick, String pairKey) {
        if (candidate.record().tier() != GodBeaconTier.GOD) {
            return;
        }
        long readyAt = ambientCooldowns.getOrDefault(pairKey, 0L);
        if (readyAt > currentTick) {
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.15F, 0.6F);
        ambientCooldowns.put(pairKey, currentTick + 200L);
    }

    private void playUpgradeBurst(Block block, GodBeaconTier tier) {
        World world = block.getWorld();
        Location center = block.getLocation().add(0.5D, 0.9D, 0.5D);
        world.spawnParticle(Particle.END_ROD, center, 24, 0.35D, 0.7D, 0.35D, 0.02D);
        world.spawnParticle(Particle.ENCHANT, center, 32, 0.5D, 0.5D, 0.5D, 0.0D);
        Sound sound = switch (tier) {
            case INFUSED -> Sound.BLOCK_BEACON_POWER_SELECT;
            case RESONANT -> Sound.BLOCK_BEACON_ACTIVATE;
            case ASCENDED -> Sound.BLOCK_RESPAWN_ANCHOR_CHARGE;
            case GOD -> Sound.ITEM_TRIDENT_THUNDER;
            default -> Sound.BLOCK_BEACON_AMBIENT;
        };
        world.playSound(center, sound, 1.0F, 1.0F);
    }

    private String successMessage(GodBeaconTier tier) {
        return switch (tier) {
            case INFUSED -> "&aBeacon infused. Power resonates through the structure.";
            case RESONANT -> "&aBeacon resonated. Its secondary current has awakened.";
            case ASCENDED -> "&aBeacon ascended. Its presence lingers beyond its bounds.";
            case GOD -> "&6God Beacon awakened. This place is now a true stronghold.";
            default -> "&aBeacon updated.";
        };
    }

    private String pairKey(UUID playerId, BeaconPosition position) {
        return playerId + "@" + position.key();
    }

    public boolean shouldPreservePortableDrop(Block block) {
        return resolveTier(block) != GodBeaconTier.VANILLA;
    }

    public boolean hasPortableData(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BEACON || !stack.hasItemMeta()) {
            return false;
        }
        return GodBeaconTier.fromKey(stack.getItemMeta().getPersistentDataContainer().get(itemTierKey, PersistentDataType.STRING)) != GodBeaconTier.VANILLA;
    }

    public boolean isSmeltEligible(Material type) {
        return SMELT_ELIGIBLE.contains(type);
    }

    public boolean shouldProcSmelting(Player player) {
        ActiveContext context = activeContexts.get(player.getUniqueId());
        return context != null
            && context.record().tier() == GodBeaconTier.GOD
            && ThreadLocalRandom.current().nextDouble() < context.record().tier().smeltingChance();
    }

    public boolean hasDurabilityProtection(Player player) {
        ActiveContext context = activeContexts.get(player.getUniqueId());
        return context != null
            && context.record().tier() == GodBeaconTier.GOD
            && ThreadLocalRandom.current().nextDouble() < context.record().tier().durabilityEfficiency();
    }

    public double exhaustionMultiplier(Player player) {
        ActiveContext context = activeContexts.get(player.getUniqueId());
        if (context == null) {
            return 1.0D;
        }
        return 1.0D - context.record().tier().hungerReduction();
    }

    public record StructureState(int vanillaTier, double baseRadius, boolean validVanilla, boolean customHandlingActive) {
        public boolean validForTier() {
            return customHandlingActive;
        }
    }

    public record TargetedBeacon(Block block, GodBeaconRecord record, GodBeaconTier tier, StructureState structure, double effectiveRadius) {}

    public record ActiveContext(GodBeaconRecord record, Block block, double radius, StructureState structure, long expiresAtTick) {}

    private record LingerContext(GodBeaconRecord record, Block block, long expiresAtTick) {}

    private record CandidateContext(GodBeaconRecord record, Block block, StructureState structure, double radius, double distanceSquared) {}

    private record InventoryRef(Inventory inventory, Location location) {}

    public record PaymentPlan(
        GodBeaconCoreType type,
        boolean hasCoreInHand,
        int availableDiamondBlocks,
        int availableNetherStars,
        int missingDiamondBlocks,
        int missingNetherStars,
        int containerCount
    ) {
        public boolean canAfford() {
            return hasCoreInHand && missingDiamondBlocks == 0 && missingNetherStars == 0;
        }
    }

    public record UpgradeAttemptResult(boolean success, String message, PaymentPlan paymentPlan) {
        public static UpgradeAttemptResult success(String message, PaymentPlan paymentPlan) {
            return new UpgradeAttemptResult(true, message, paymentPlan);
        }

        public static UpgradeAttemptResult failure(String message) {
            return new UpgradeAttemptResult(false, message, null);
        }
    }
}
