package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.advancement.MythicalAdvancementService;
import io.github.diamondsmp.platform.paper.advancement.MythicalAdvancementService.MythicalAdvancement;
import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.event.ServerEventManager;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.item.GodItemType;
import io.github.diamondsmp.platform.paper.service.CombatStateService;
import io.github.diamondsmp.platform.paper.service.CooldownService;
import io.github.diamondsmp.platform.paper.service.EndAccessService;
import io.github.diamondsmp.platform.paper.service.PvpService;
import io.github.diamondsmp.platform.paper.service.TrustService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class GameplayListener implements Listener {
    private static final int MAX_VEIN_MINE = 128;
    private static final int GOD_BOW_COOLDOWN_TICKS = 5;
    private static final double GOD_BOW_HEADSHOT_Y_TOLERANCE = 0.25D;
    private static final double GOD_BOW_SHORT_PLAYER_BONUS_DAMAGE = 1.0D;
    private static final double GOD_BOW_SHORT_NON_PLAYER_BONUS_DAMAGE = 3.0D;
    private static final double GOD_BOW_NORMAL_DAMAGE = 6.0D;
    private static final double GOD_BOW_NORMAL_VELOCITY = 3.0D;

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MessageBundle messages;
    private final GodItemRegistry godItems;
    private final CooldownService cooldowns;
    private final CombatStateService combatState;
    private final TrustService trustService;
    private final EndAccessService endAccessService;
    private final ServerEventManager eventManager;
    private final MythicalAdvancementService mythicalAdvancements;
    private final PvpService pvpService;
    private final Map<UUID, GodBowShot> godBowShots = new HashMap<>();
    private final Map<UUID, BedrockBreakSession> bedrockSessions = new HashMap<>();

    public GameplayListener(
        JavaPlugin plugin,
        PluginSettings settings,
        MessageBundle messages,
        GodItemRegistry godItems,
        CooldownService cooldowns,
        CombatStateService combatState,
        TrustService trustService,
        EndAccessService endAccessService,
        ServerEventManager eventManager,
        MythicalAdvancementService mythicalAdvancements,
        PvpService pvpService
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.godItems = godItems;
        this.cooldowns = cooldowns;
        this.combatState = combatState;
        this.trustService = trustService;
        this.endAccessService = endAccessService;
        this.eventManager = eventManager;
        this.mythicalAdvancements = mythicalAdvancements;
        this.pvpService = pvpService;
        startStatusTask();
        startGodBowTask();
    }

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack totem = godItems.isGodItem(hand, GodItemType.INFINITE_TOTEM) ? hand : offHand;
        if (!godItems.isGodItem(totem, GodItemType.INFINITE_TOTEM)) {
            return;
        }
        if (!cooldowns.isReady(player.getUniqueId(), "totem")) {
            int cooldownTicks = (int) (cooldowns.remaining(player.getUniqueId(), "totem").toMillis() / 50L);
            player.setCooldown(Material.TOTEM_OF_UNDYING, Math.max(1, cooldownTicks));
            event.setCancelled(true);
            player.sendMessage(messages.format(
                "totem.cooldown",
                "&cInfinite Totem cooldown: {time}s remaining.",
                Map.of("time", Long.toString(cooldowns.remaining(player.getUniqueId(), "totem").toSeconds()))
            ));
            return;
        }
        ItemStack restoredTotem = totem.clone();
        EquipmentSlot usedHand = event.getHand();
        cooldowns.apply(player.getUniqueId(), "totem", settings.cooldowns().infiniteTotem());
        player.setCooldown(Material.TOTEM_OF_UNDYING, (int) settings.cooldowns().infiniteTotem().toSeconds() * 20);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (usedHand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(restoredTotem);
            } else if (usedHand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(restoredTotem);
            } else if (!godItems.isGodItem(player.getInventory().getItemInOffHand(), GodItemType.INFINITE_TOTEM)) {
                player.getInventory().setItemInOffHand(restoredTotem);
            } else {
                player.getInventory().setItemInMainHand(restoredTotem);
            }
            player.updateInventory();
        });
    }

    @EventHandler
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        Player attacker = extractPlayerDamager(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (victim instanceof Player playerVictim
            && trustService.isTrusted(attacker.getUniqueId(), playerVictim.getUniqueId())
            && !pvpService.isMatchOpponents(attacker.getUniqueId(), playerVictim.getUniqueId())) {
            event.setCancelled(true);
            event.setDamage(0.0D);
            return;
        }
        if (victim instanceof Player playerVictim) {
            if (!pvpService.isInActiveMatch(attacker.getUniqueId()) && !pvpService.isInActiveMatch(playerVictim.getUniqueId())) {
                combatState.tag(attacker.getUniqueId(), settings.combat().combatTagTime());
                combatState.tag(playerVictim.getUniqueId(), settings.combat().combatTagTime());
            }
        }
        ItemStack held = attacker.getInventory().getItemInMainHand();
        if (godItems.isGodItem(held, GodItemType.AXE) && victim instanceof Player playerVictim) {
            shredArmor(playerVictim);
        }
        if (event.getDamager() instanceof AbstractArrow) {
            processGodBowShot(victim, event);
        }
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !godItems.isGodItem(event.getBow(), GodItemType.BOW)) {
            return;
        }
        event.setCancelled(true);
        event.setConsumeItem(false);
        event.setConsumeArrow(false);
    }

    @EventHandler
    public void onProjectileMiss(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || event.getHitEntity() != null) {
            return;
        }
        godBowShots.remove(arrow.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
            && event.getHand() == EquipmentSlot.HAND
            && godItems.isGodItem(player.getInventory().getItemInMainHand(), GodItemType.BOW)) {
            event.setCancelled(true);
            manualLaunchGodBow(player, GodBowMode.SHORT);
            return;
        }
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            && event.getHand() == EquipmentSlot.HAND
            && godItems.isGodItem(player.getInventory().getItemInMainHand(), GodItemType.BOW)) {
            event.setCancelled(true);
            manualLaunchGodBow(player, GodBowMode.NORMAL);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
            && event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.END_PORTAL_FRAME
            && player.getInventory().getItemInMainHand().getType() == Material.ENDER_EYE) {
            if (!endAccessService.isOpen()) {
                event.setCancelled(true);
                player.sendMessage(messages.prefixed("end.blocked", "&cThe End is currently closed."));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> endAccessService.activateNearbyPortal(event.getClickedBlock()));
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEDROCK) {
            return;
        }
        if (!godItems.isGodItem(player.getInventory().getItemInMainHand(), GodItemType.PICKAXE)) {
            return;
        }
        startBedrockBreak(player, block);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerPortalEvent.TeleportCause.END_PORTAL) {
            return;
        }
        if (!endAccessService.isOpen()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.prefixed("end.blocked", "&cThe End is currently closed."));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        endAccessService.enforcePlayerAccess(event.getPlayer());
        eventManager.handleJoin(event.getPlayer());
        checkMythicalProgress(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        endAccessService.enforcePlayerAccess(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (pvpService.isManagedPlayer(player.getUniqueId())) {
            bedrockSessions.remove(player.getUniqueId());
            clearGodBowShots(player.getUniqueId());
            return;
        }
        if (combatState.isTagged(player.getUniqueId()) && !player.isDead()) {
            player.setHealth(0.0D);
            combatState.clear(player.getUniqueId());
        }
        bedrockSessions.remove(player.getUniqueId());
        clearGodBowShots(player.getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (pvpService.isManagedPlayer(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!godItems.isGodItem(tool, GodItemType.PICKAXE)) {
            return;
        }
        if (!isVeinMineTarget(event.getBlock().getType())) {
            return;
        }
        event.setCancelled(true);
        int totalExp = 0;
        for (Block block : collectVeinBlocks(event.getBlock())) {
            totalExp += expFor(block.getType());
            for (ItemStack drop : block.getDrops(tool, player)) {
                event.getBlock().getWorld().dropItemNaturally(block.getLocation(), smelt(drop));
            }
            block.setType(Material.AIR, false);
        }
        if (totalExp > 0) {
            ExperienceOrb orb = event.getBlock().getWorld().spawn(event.getBlock().getLocation(), ExperienceOrb.class);
            orb.setExperience(totalExp);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (pvpService.isManagedPlayer(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (pvpService.isManagedPlayer(player.getUniqueId())) {
                bedrockSessions.remove(player.getUniqueId());
                clearGodBowShots(player.getUniqueId());
                combatState.clear(player.getUniqueId());
                return;
            }
            Player killer = player.getKiller();
            eventManager.handlePlayerDeath(player, killer);
            bedrockSessions.remove(player.getUniqueId());
            clearGodBowShots(player.getUniqueId());
            combatState.clear(player.getUniqueId());
            return;
        }
        Player killer = event.getEntity().getKiller();
        eventManager.handleMobDeath(event, killer);
    }

    private void startStatusTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(player -> {
            refreshEffects(player);
            updateCombatStatus(player);
            endAccessService.enforcePlayerAccess(player);
            checkMythicalProgress(player);
        }), 20L, 20L);
    }

    private void startGodBowTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, GodBowShot>> iterator = godBowShots.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, GodBowShot> entry = iterator.next();
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (!(entity instanceof AbstractArrow arrow) || !arrow.isValid() || arrow.isInBlock() || arrow.isOnGround()) {
                    iterator.remove();
                    continue;
                }
                GodBowShot shot = entry.getValue();
                if (!arrow.getWorld().equals(shot.origin().getWorld())
                    || arrow.getLocation().distanceSquared(shot.origin()) > shot.maxRange() * shot.maxRange()) {
                    arrow.remove();
                    iterator.remove();
                }
            }
        }, 1L, 1L);
    }

    private void refreshEffects(Player player) {
        PlayerInventory inventory = player.getInventory();
        applyArmorEffect(player, inventory.getHelmet(), GodItemType.HELMET, PotionEffectType.FIRE_RESISTANCE, 0);
        applyArmorEffect(player, inventory.getHelmet(), GodItemType.HELMET, PotionEffectType.DOLPHINS_GRACE, 0);
        applyArmorEffect(player, inventory.getHelmet(), GodItemType.HELMET, PotionEffectType.WATER_BREATHING, 0);
        applyArmorEffect(player, inventory.getChestplate(), GodItemType.CHESTPLATE, PotionEffectType.HEALTH_BOOST, 4);
        applyArmorEffect(player, inventory.getLeggings(), GodItemType.LEGGINGS, PotionEffectType.RESISTANCE, 1);
        applyArmorEffect(player, inventory.getBoots(), GodItemType.BOOTS, PotionEffectType.SPEED, 1);
        applyHeldEffect(player, inventory.getItemInMainHand(), GodItemType.SWORD, PotionEffectType.STRENGTH, 1);
    }

    private void updateCombatStatus(Player player) {
        Duration remaining = combatState.remaining(player.getUniqueId());
        if (remaining.isZero()) {
            return;
        }
        player.sendActionBar(Component.text(
            ChatColor.RED + "Combat tagged for " + Math.max(1L, remaining.toSeconds()) + "s",
            NamedTextColor.RED
        ));
    }

    private void applyArmorEffect(Player player, ItemStack item, GodItemType type, PotionEffectType effect, int amplifier) {
        if (godItems.isGodItem(item, type)) {
            player.addPotionEffect(new PotionEffect(effect, 60, amplifier, true, false, true));
        } else if (player.hasPotionEffect(effect)) {
            PotionEffect active = player.getPotionEffect(effect);
            if (active != null && active.getDuration() <= 70 && active.getAmplifier() == amplifier) {
                player.removePotionEffect(effect);
            }
        }
    }

    private void applyHeldEffect(Player player, ItemStack item, GodItemType type, PotionEffectType effect, int amplifier) {
        if (godItems.isGodItem(item, type)) {
            player.addPotionEffect(new PotionEffect(effect, 60, amplifier, true, false, true));
        }
    }

    private void shredArmor(Player victim) {
        double multiplier = Math.max(1.0D, settings.combat().axeArmorDurabilityMultiplier());
        ItemStack[] armor = victim.getInventory().getArmorContents();
        for (int index = 0; index < armor.length; index++) {
            ItemStack piece = armor[index];
            if (piece == null || !piece.hasItemMeta() || piece.getItemMeta().isUnbreakable()) {
                continue;
            }
            if (piece.getItemMeta() instanceof Damageable damageable) {
                damageable.setDamage(damageable.getDamage() + (int) Math.ceil(multiplier));
                piece.setItemMeta(damageable);
                armor[index] = piece;
            }
        }
        victim.getInventory().setArmorContents(armor);
    }

    private void processGodBowShot(LivingEntity victim, EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) {
            return;
        }
        GodBowShot shot = godBowShots.remove(arrow.getUniqueId());
        if (shot == null) {
            return;
        }
        event.setDamage(victim instanceof Player ? shot.playerDamage() : shot.nonPlayerDamage());
        if (shot.headshotBonus() > 0.0D && isHeadshot(arrow, victim)) {
            event.setDamage(event.getDamage() + shot.headshotBonus());
        }
    }

    private boolean isHeadshot(AbstractArrow arrow, LivingEntity victim) {
        return arrow.getLocation().getY() >= victim.getEyeLocation().getY() - GOD_BOW_HEADSHOT_Y_TOLERANCE;
    }

    private void manualLaunchGodBow(Player player, GodBowMode mode) {
        if (player.hasCooldown(Material.BOW)) {
            return;
        }
        SpecialArrowAmmo ammo = findSpecialArrowAmmo(player);
        Vector velocity = player.getEyeLocation().getDirection().normalize().multiply(mode.velocity(this.settings));
        AbstractArrow projectile = ammo != null && ammo.material() == Material.SPECTRAL_ARROW
            ? player.launchProjectile(SpectralArrow.class, velocity)
            : player.launchProjectile(Arrow.class, velocity);
        configureGodBowProjectile(projectile, ammo, mode);
        godBowShots.put(projectile.getUniqueId(), new GodBowShot(
            player.getUniqueId(),
            player.getEyeLocation(),
            mode.playerDamage(this.settings),
            mode.nonPlayerDamage(this.settings),
            mode.headshotBonus(this.settings),
            mode.range(this.settings)
        ));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
        player.setCooldown(Material.BOW, GOD_BOW_COOLDOWN_TICKS);
    }

    private void configureGodBowProjectile(AbstractArrow projectile, SpecialArrowAmmo ammo, GodBowMode mode) {
        projectile.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        projectile.setCritical(false);
        projectile.setDamage(mode.playerDamage(this.settings));
        if (projectile instanceof Arrow arrow && ammo != null && ammo.material() == Material.TIPPED_ARROW) {
            applyTippedArrowMeta(arrow, ammo.stack());
        }
    }

    private void applyTippedArrowMeta(Arrow arrow, ItemStack ammo) {
        if (!(ammo.getItemMeta() instanceof PotionMeta potionMeta)) {
            return;
        }
        if (potionMeta.getBasePotionType() != null) {
            arrow.setBasePotionType(potionMeta.getBasePotionType());
        }
        for (PotionEffect effect : potionMeta.getCustomEffects()) {
            arrow.addCustomEffect(effect, true);
        }
        Color color = potionMeta.getColor();
        if (color != null) {
            arrow.setColor(color);
        }
    }

    private SpecialArrowAmmo findSpecialArrowAmmo(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isConsumableSpecialArrow(offHand)) {
            return new SpecialArrowAmmo(true, -1, offHand.getType(), offHand);
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isConsumableSpecialArrow(item)) {
                return new SpecialArrowAmmo(false, slot, item.getType(), item);
            }
        }
        return null;
    }

    private boolean isConsumableSpecialArrow(ItemStack item) {
        return item != null && (item.getType() == Material.TIPPED_ARROW || item.getType() == Material.SPECTRAL_ARROW);
    }

    private void startBedrockBreak(Player player, Block block) {
        BedrockBreakSession current = bedrockSessions.get(player.getUniqueId());
        if (current != null && current.location().equals(block.getLocation())) {
            return;
        }
        BedrockBreakSession session = new BedrockBreakSession(block.getLocation(), Instant.now());
        bedrockSessions.put(player.getUniqueId(), session);
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            BedrockBreakSession active = bedrockSessions.get(player.getUniqueId());
            if (active == null || !active.location().equals(block.getLocation()) || !player.isOnline()) {
                task.cancel();
                return;
            }
            if (!godItems.isGodItem(player.getInventory().getItemInMainHand(), GodItemType.PICKAXE) || block.getType() != Material.BEDROCK) {
                bedrockSessions.remove(player.getUniqueId());
                task.cancel();
                return;
            }
            Duration elapsed = Duration.between(active.startedAt(), Instant.now());
            long total = settings.combat().bedrockBreakTime().toSeconds();
            long remaining = Math.max(0, total - elapsed.toSeconds());
            player.sendActionBar(Component.text(ChatColor.GOLD + "Breaking bedrock: " + remaining + "s"));
            if (!elapsed.minus(settings.combat().bedrockBreakTime()).isNegative()) {
                block.setType(Material.AIR, false);
                bedrockSessions.remove(player.getUniqueId());
                player.sendMessage(messages.prefixed("bedrock.complete", "&6Bedrock broken."));
                task.cancel();
            }
        }, 0L, 20L);
    }

    private void clearGodBowShots(UUID playerId) {
        godBowShots.entrySet().removeIf(entry -> entry.getValue().shooterId().equals(playerId));
    }

    private List<Block> collectVeinBlocks(Block start) {
        List<Block> blocks = new java.util.ArrayList<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        Material target = start.getType();
        Map<Long, Boolean> seen = new HashMap<>();
        while (!queue.isEmpty() && blocks.size() < MAX_VEIN_MINE) {
            Block block = queue.removeFirst();
            if (block.getType() != target || seen.putIfAbsent(block.getBlockKey(), Boolean.TRUE) != null) {
                continue;
            }
            blocks.add(block);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = block.getRelative(dx, dy, dz);
                        if (next.getType() == target) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return blocks;
    }

    private boolean isVeinMineTarget(Material material) {
        return material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
    }

    private ItemStack smelt(ItemStack stack) {
        Material result = switch (stack.getType()) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            default -> null;
        };
        if (result == null) {
            return stack;
        }
        return new ItemStack(result, stack.getAmount());
    }

    private int expFor(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> 2;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> 5;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, NETHER_QUARTZ_ORE -> 4;
            case NETHER_GOLD_ORE -> 3;
            default -> 1;
        };
    }

    private void checkMythicalProgress(Player player) {
        unlockMythical(player, MythicalAdvancement.DEMI_GOD, hasAny(player, GodItemType.HELMET, GodItemType.CHESTPLATE, GodItemType.LEGGINGS, GodItemType.BOOTS));
        unlockMythical(player, MythicalAdvancement.IS_HE_A_GOD, hasAll(player, GodItemType.HELMET, GodItemType.CHESTPLATE, GodItemType.LEGGINGS, GodItemType.BOOTS));
        unlockMythical(player, MythicalAdvancement.DIVINE_ARSENAL, hasAll(player, GodItemType.SWORD, GodItemType.AXE, GodItemType.PICKAXE, GodItemType.BOW));
        unlockMythical(player, MythicalAdvancement.IMMOVABLE_OBJECT, hasAll(player, GodItemType.INFINITE_TOTEM, GodItemType.ENCHANTED_GAPPLE));
        unlockMythical(player, MythicalAdvancement.MYTHIC_INCARNATE, hasAll(player, GodItemType.values()));
    }

    private void unlockMythical(Player player, MythicalAdvancement advancement, boolean condition) {
        if (!condition || mythicalAdvancements.isUnlocked(player, advancement)) {
            return;
        }
        mythicalAdvancements.unlock(player, advancement);
    }

    private boolean hasAny(Player player, GodItemType... types) {
        for (GodItemType type : types) {
            if (hasItem(player, type)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAll(Player player, GodItemType... types) {
        for (GodItemType type : types) {
            if (!hasItem(player, type)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasItem(Player player, GodItemType type) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (godItems.isGodItem(item, type)) {
                return true;
            }
        }
        return godItems.isGodItem(player.getInventory().getItemInOffHand(), type);
    }

    private Player extractPlayerDamager(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private record GodBowShot(UUID shooterId, Location origin, double playerDamage, double nonPlayerDamage, double headshotBonus, double maxRange) {}

    private enum GodBowMode {
        SHORT,
        NORMAL;

        private double playerDamage(PluginSettings settings) {
            return this == SHORT ? settings.combat().godBowBaseDamage() + GOD_BOW_SHORT_PLAYER_BONUS_DAMAGE : GOD_BOW_NORMAL_DAMAGE;
        }

        private double nonPlayerDamage(PluginSettings settings) {
            return this == SHORT ? settings.combat().godBowBaseDamage() + GOD_BOW_SHORT_NON_PLAYER_BONUS_DAMAGE : GOD_BOW_NORMAL_DAMAGE;
        }

        private double headshotBonus(PluginSettings settings) {
            return this == SHORT ? settings.combat().godBowHeadshotBonus() : 0.0D;
        }

        private double velocity(PluginSettings settings) {
            return this == SHORT ? settings.combat().godBowVelocity() : GOD_BOW_NORMAL_VELOCITY;
        }

        private double range(PluginSettings settings) {
            return this == SHORT ? settings.combat().godBowRange() : Double.MAX_VALUE;
        }
    }

    private record SpecialArrowAmmo(boolean offHand, int slot, Material material, ItemStack stack) {}

    private record BedrockBreakSession(Location location, Instant startedAt) {}
}
