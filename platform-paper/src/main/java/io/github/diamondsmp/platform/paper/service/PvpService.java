package io.github.diamondsmp.platform.paper.service;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.gui.PvpGui;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public final class PvpService {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MessageBundle messages;
    private final KitService kitService;
    private final PartyService partyService;
    private final CombatStateService combatState;
    private final PvpTestService testService;
    private final PvpGui gui;
    private final Random random = new Random();
    private final Set<Integer> occupiedArenas = new HashSet<>();
    private final Map<UUID, ActiveMatch> activeMatchByPlayer = new HashMap<>();
    private final Map<UUID, UUID> participantIdByEntityId = new HashMap<>();
    private final Map<UUID, UUID> entityIdByParticipantId = new HashMap<>();
    private final Map<UUID, Snapshot> snapshots = new HashMap<>();
    private final Map<UUID, PostMatchState> postMatchStates = new HashMap<>();
    private World cachedWorld;

    public PvpService(
        JavaPlugin plugin,
        PluginSettings settings,
        MessageBundle messages,
        KitService kitService,
        PartyService partyService,
        CombatStateService combatState,
        PvpTestService testService,
        PvpGui gui
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.kitService = kitService;
        this.partyService = partyService;
        this.combatState = combatState;
        this.testService = testService;
        this.gui = gui;
    }

    public boolean handlePvpCommand(Player player, String[] args) {
        if (!settings.pvp().enabled()) {
            player.sendMessage(messages.prefixed("pvp.disabled", "&cPvP beta is disabled right now."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (hasPostMatchMenu(player.getUniqueId())) {
                return openPostMatchMenu(player);
            }
            return openModeMenu(player);
        }
        PluginSettings.PvpMode directMode = settings.pvp().modes().get(args[0].toLowerCase(Locale.ROOT));
        if (directMode != null) {
            if (args.length == 1) {
                return openKitMenu(player, directMode.key());
            }
            return startMatch(player, directMode.key(), args[1]);
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (args.length < 3) {
                    player.sendMessage(messages.prefixed("pvp.usage", "&cUsage: /pvp [menu|status|modes|kits|leave|rematch|start <mode> <kit>]"));
                    yield true;
                }
                yield startMatch(player, args[1], args[2]);
            }
            case "test" -> handleTestCommand(player, args);
            case "status" -> showStatus(player);
            case "modes" -> showModes(player);
            case "kits" -> showKits(player);
            case "leave", "return" -> leaveSession(player);
            case "rematch" -> rematch(player);
            default -> {
                player.sendMessage(messages.prefixed("pvp.usage", "&cUsage: /pvp [menu|status|modes|kits|leave|rematch|start <mode> <kit>]"));
                yield true;
            }
        };
    }

    public boolean openModeMenu(Player player) {
        List<PvpGui.ModeOption> options = settings.pvp().modes().values().stream()
            .map(mode -> new PvpGui.ModeOption(
                mode.key(),
                mode.displayName(),
                mode.teamSizes().toString(),
                describePartyFit(resolveParty(player).map(PartyService.PartyView::size).orElse(1), mode),
                mode.icon()
            ))
            .toList();
        gui.openHubMenu(player, new PvpGui.HubView(
            options,
            partyLore(resolveParty(player)),
            postMatchStates.containsKey(player.getUniqueId()),
            snapshots.containsKey(player.getUniqueId()) && !activeMatchByPlayer.containsKey(player.getUniqueId()),
            availableArenaCount(),
            occupiedArenas.size(),
            randomCompatibleModeKey(player)
        ));
        return true;
    }

    public boolean openPostMatchMenu(Player player) {
        PostMatchState state = postMatchStates.get(player.getUniqueId());
        if (state == null) {
            return openModeMenu(player);
        }
        gui.openPostMatchMenu(player, state.winnerLine());
        return true;
    }

    public boolean openKitMenu(Player player, String modeKey) {
        PluginSettings.PvpMode mode = settings.pvp().modes().get(modeKey.toLowerCase(Locale.ROOT));
        if (mode == null) {
            player.sendMessage(messages.prefixed("pvp.mode-not-found", "&cThat PvP mode is not configured."));
            return true;
        }
        List<KitService.KitDefinition> kits = availableKits();
        if (kits.isEmpty()) {
            player.sendMessage(messages.prefixed("pvp.no-kits", "&cNo PvP kits are configured."));
            return true;
        }
        openSetupMenu(player, mode, kits.getFirst().name(), kits);
        return true;
    }

    public boolean openKitMenu(Player player, String modeKey, String selectedKitName) {
        PluginSettings.PvpMode mode = settings.pvp().modes().get(modeKey.toLowerCase(Locale.ROOT));
        if (mode == null) {
            player.sendMessage(messages.prefixed("pvp.mode-not-found", "&cThat PvP mode is not configured."));
            return true;
        }
        List<KitService.KitDefinition> kits = availableKits();
        if (kits.isEmpty()) {
            player.sendMessage(messages.prefixed("pvp.no-kits", "&cNo PvP kits are configured."));
            return true;
        }
        String kitName = findAvailableKit(selectedKitName).map(KitService.KitDefinition::name).orElseGet(() -> kits.getFirst().name());
        openSetupMenu(player, mode, kitName, kits);
        return true;
    }

    public boolean openKitPickerMenu(Player player, String modeKey) {
        List<KitService.KitDefinition> kits = availableKits();
        if (kits.isEmpty()) {
            player.sendMessage(messages.prefixed("pvp.no-kits", "&cNo PvP kits are configured."));
            return true;
        }
        return openKitPickerMenu(player, modeKey, kits.getFirst().name(), 0);
    }

    public boolean openKitPickerMenu(Player player, String modeKey, String selectedKitName, int page) {
        PluginSettings.PvpMode mode = settings.pvp().modes().get(modeKey.toLowerCase(Locale.ROOT));
        if (mode == null) {
            player.sendMessage(messages.prefixed("pvp.mode-not-found", "&cThat PvP mode is not configured."));
            return true;
        }
        List<KitService.KitDefinition> kits = availableKits();
        if (kits.isEmpty()) {
            player.sendMessage(messages.prefixed("pvp.no-kits", "&cNo PvP kits are configured."));
            return true;
        }
        int pageSize = gui.kitPageSize();
        int totalPages = Math.max(1, (int) Math.ceil((double) kits.size() / pageSize));
        int safePage = Math.floorMod(page, totalPages);
        String safeSelectedKit = findAvailableKit(selectedKitName).map(KitService.KitDefinition::name).orElseGet(() -> kits.getFirst().name());
        gui.openKitMenu(player, new PvpGui.KitPickerView(
            mode.key(),
            mode.displayName(),
            safeSelectedKit,
            safePage,
            totalPages,
            kits.stream()
                .skip((long) safePage * pageSize)
                .limit(pageSize)
                .map(kit -> new PvpGui.KitOption(kit.name(), kitLore(kit), safeSelectedKit.equalsIgnoreCase(kit.name())))
                .toList()
        ));
        return true;
    }

    public boolean openRandomModeMenu(Player player) {
        String modeKey = randomCompatibleModeKey(player);
        if (modeKey == null) {
            player.sendMessage(messages.prefixed("pvp.no-compatible-modes", "&cNo configured PvP mode matches your current party size."));
            return true;
        }
        return openKitMenu(player, modeKey);
    }

    public boolean openRandomKitMenu(Player player, String modeKey) {
        List<KitService.KitDefinition> kits = availableKits();
        if (kits.isEmpty()) {
            player.sendMessage(messages.prefixed("pvp.no-kits", "&cNo PvP kits are configured."));
            return true;
        }
        return openKitMenu(player, modeKey, kits.get(random.nextInt(kits.size())).name());
    }

    public boolean startMatch(Player initiator, String modeKey, String kitName) {
        PluginSettings.PvpMode mode = settings.pvp().modes().get(modeKey.toLowerCase(Locale.ROOT));
        if (mode == null) {
            initiator.sendMessage(messages.prefixed("pvp.mode-not-found", "&cThat PvP mode is not configured."));
            return true;
        }
        Optional<PartyService.PartyView> partyView = resolveParty(initiator);
        if (partyView.isEmpty()) {
            initiator.sendMessage(messages.prefixed("party.none", "&eYou need a party before you can launch PvP."));
            return true;
        }
        PartyService.PartyView party = partyView.get();
        if (party.size() != mode.totalPlayers()) {
            initiator.sendMessage(messages.format(
                "pvp.invalid-party-size",
                "&c{mode} needs exactly {size} party members.",
                Map.of("mode", mode.displayName(), "size", Integer.toString(mode.totalPlayers()))
            ));
            return true;
        }
        Optional<KitService.KitDefinition> kit = findAvailableKit(kitName);
        if (kit.isEmpty()) {
            initiator.sendMessage(messages.prefixed("pvp.kit-not-found", "&cThat PvP kit is not available."));
            return true;
        }
        if (!supportsTestPartyLayout(party, mode)) {
            initiator.sendMessage(messages.prefixed(
                "pvp.test-layout-invalid",
                "&cTest players only support solo-side layouts right now, like 1v1 or 1v2."
            ));
            return true;
        }
        List<Player> liveParticipants = new ArrayList<>();
        List<UUID> participantIds = new ArrayList<>();
        for (UUID memberId : party.members()) {
            if (testService.isTestPlayer(memberId)) {
                participantIds.add(memberId);
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                initiator.sendMessage(messages.prefixed("pvp.offline-party-member", "&cEveryone in the party must be online."));
                return true;
            }
            if (isBusyForMatchStart(memberId)) {
                initiator.sendMessage(messages.format(
                    "pvp.member-in-session",
                    "&c{player} is already busy with another PvP session.",
                    Map.of("player", member.getName())
                ));
                return true;
            }
            if (combatState.isTagged(memberId)) {
                initiator.sendMessage(messages.format(
                    "pvp.member-combat",
                    "&c{player} is combat tagged and cannot queue PvP yet.",
                    Map.of("player", member.getName())
                ));
                return true;
            }
            liveParticipants.add(member);
            participantIds.add(memberId);
        }
        Integer arenaIndex = nextArenaIndex();
        if (arenaIndex == null) {
            initiator.sendMessage(messages.prefixed("pvp.no-arena", "&cAll PvP arenas are currently busy."));
            return true;
        }
        World world = ensureWorld();
        buildArena(world, arenaIndex);
        Match match = createMatch(participantIds, mode, kit.get(), arenaIndex);
        occupiedArenas.add(arenaIndex);
        for (Player participant : liveParticipants) {
            snapshots.computeIfAbsent(participant.getUniqueId(), key -> Snapshot.capture(participant));
        }
        launch(match, world, kit.get(), liveParticipants);
        return true;
    }

    public void recordDamage(EntityDamageByEntityEvent event) {
        UUID victimId = resolveParticipantId(event.getEntity());
        if (victimId == null) {
            return;
        }
        UUID attackerId = resolveParticipantId(event.getDamager());
        if (attackerId == null) {
            return;
        }
        ActiveMatch activeMatch = activeMatchByParticipant(victimId);
        if (activeMatch == null) {
            return;
        }
        Match match = activeMatch.match();
        ParticipantState attackerState = match.participants().get(attackerId);
        ParticipantState victimState = match.participants().get(victimId);
        if (attackerState == null || victimState == null) {
            event.setCancelled(true);
            event.setDamage(0.0D);
            return;
        }
        if (attackerState.teamIndex() == victimState.teamIndex()) {
            event.setCancelled(true);
            event.setDamage(0.0D);
            Player attacker = Bukkit.getPlayer(attackerId);
            if (attacker != null) {
                attacker.sendMessage(messages.prefixed("pvp.friendly-fire", "&eFriendly fire is disabled inside party PvP."));
            }
            return;
        }
        attackerState.stats().damageDealt += event.getFinalDamage();
        victimState.stats().damageTaken += event.getFinalDamage();
    }

    public void handleDeath(Player player, Player killer) {
        handleParticipantDeath(player.getUniqueId(), killer == null ? null : killer.getUniqueId());
    }

    public void handleRespawn(Player player) {
        if (!snapshots.containsKey(player.getUniqueId())) {
            return;
        }
        ActiveMatch activeMatch = activeMatchByPlayer.get(player.getUniqueId());
        if (activeMatch != null) {
            ParticipantState state = activeMatch.match().participants().get(player.getUniqueId());
            if (state != null && !state.alive) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.teleport(arenaCenter(activeMatch.match().arenaIndex()));
                    player.setGameMode(GameMode.ADVENTURE);
                    player.getInventory().clear();
                });
            }
            return;
        }
        PostMatchState postMatch = postMatchStates.get(player.getUniqueId());
        if (postMatch == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(arenaCenter(postMatch.arenaIndex()));
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            openPostMatchMenu(player);
        });
    }

    public void handleQuit(Player player) {
        ActiveMatch activeMatch = activeMatchByPlayer.get(player.getUniqueId());
        if (activeMatch != null) {
            forfeitParticipant(player.getUniqueId(), player.getName());
            return;
        }
        if (snapshots.containsKey(player.getUniqueId())) {
            postMatchStates.remove(player.getUniqueId());
        }
    }

    public void handleWorldChange(Player player) {
        ActiveMatch activeMatch = activeMatchByPlayer.get(player.getUniqueId());
        if (activeMatch == null) {
            return;
        }
        World pvpWorld = ensureWorld();
        if (player.getWorld().equals(pvpWorld)) {
            return;
        }
        if (forfeitParticipant(player.getUniqueId(), player.getName())) {
            returnToSmp(player);
        }
    }

    private void handleParticipantDeath(UUID defeatedId, UUID killerId) {
        ActiveMatch activeMatch = activeMatchByParticipant(defeatedId);
        if (activeMatch == null) {
            return;
        }
        Match match = activeMatch.match();
        ParticipantState defeated = match.participants().get(defeatedId);
        if (defeated == null || !defeated.alive) {
            return;
        }
        defeated.alive = false;
        defeated.stats().deaths += 1;
        if (killerId != null) {
            ParticipantState killerState = match.participants().get(killerId);
            if (killerState != null) {
                killerState.stats().kills += 1;
            }
        }
        int winningTeam = remainingWinningTeam(match);
        if (winningTeam >= 0) {
            finishMatch(match, winningTeam);
        }
    }

    private ActiveMatch activeMatchByParticipant(UUID participantId) {
        ActiveMatch activeMatch = activeMatchByPlayer.get(participantId);
        if (activeMatch != null) {
            return activeMatch;
        }
        return activeMatchByPlayer.values().stream()
            .filter(match -> match.match().participants().containsKey(participantId))
            .findFirst()
            .orElse(null);
    }

    public void handleJoin(Player player) {
        if (!snapshots.containsKey(player.getUniqueId())) {
            return;
        }
        if (activeMatchByPlayer.containsKey(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> returnToSmp(player));
    }

    public boolean returnToSmp(Player player) {
        Snapshot snapshot = snapshots.remove(player.getUniqueId());
        postMatchStates.remove(player.getUniqueId());
        if (snapshot == null) {
            player.sendMessage(messages.prefixed("pvp.not-in-session", "&cYou are not inside PvP right now."));
            return true;
        }
        restoreSnapshot(player, snapshot);
        combatState.clear(player.getUniqueId());
        player.sendMessage(messages.prefixed("pvp.returned", "&aYou have been returned to the SMP."));
        return true;
    }

    public boolean rematch(Player player) {
        PostMatchState state = postMatchStates.get(player.getUniqueId());
        if (state == null) {
            player.sendMessage(messages.prefixed("pvp.no-rematch", "&cThere is no completed match to rematch."));
            return true;
        }
        return startMatch(player, state.modeKey(), state.kitName());
    }

    public boolean isManagedPlayer(UUID playerId) {
        return snapshots.containsKey(playerId) || activeMatchByPlayer.containsKey(playerId);
    }

    public boolean hasPostMatchMenu(UUID playerId) {
        return postMatchStates.containsKey(playerId) && snapshots.containsKey(playerId) && !activeMatchByPlayer.containsKey(playerId);
    }

    public boolean isInActiveMatch(UUID playerId) {
        return activeMatchByPlayer.containsKey(playerId);
    }

    public boolean isInPvpSession(UUID playerId) {
        return activeMatchByPlayer.containsKey(playerId) || snapshots.containsKey(playerId) || postMatchStates.containsKey(playerId);
    }

    public boolean blocksTeleport(UUID playerId) {
        return isInPvpSession(playerId);
    }

    public boolean isMatchOpponents(UUID attackerId, UUID victimId) {
        ActiveMatch attackerMatch = activeMatchByParticipant(attackerId);
        ActiveMatch victimMatch = activeMatchByParticipant(victimId);
        if (attackerMatch == null || victimMatch == null || !attackerMatch.match().id().equals(victimMatch.match().id())) {
            return false;
        }
        ParticipantState attacker = attackerMatch.match().participants().get(attackerId);
        ParticipantState victim = victimMatch.match().participants().get(victimId);
        return attacker != null && victim != null && attacker.teamIndex() != victim.teamIndex();
    }

    public boolean handleManagedEntityDeath(EntityDeathEvent event) {
        UUID participantId = participantIdByEntityId.get(event.getEntity().getUniqueId());
        if (participantId == null) {
            return false;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        UUID killerId = resolveParticipantId(event.getEntity().getKiller());
        handleParticipantDeath(participantId, killerId);
        cleanupTestEntity(participantId);
        return true;
    }

    public String displayName(UUID playerId) {
        return testService.displayName(playerId).orElseGet(() -> Optional.ofNullable(Bukkit.getPlayer(playerId)).map(Player::getName).orElse("Offline"));
    }

    public boolean showStatus(Player player) {
        Optional<PartyService.PartyView> party = resolveParty(player);
        String partyStatus = party.map(view -> view.size() + " players").orElse("No eligible party");
        String session = activeMatchByPlayer.containsKey(player.getUniqueId())
            ? "Active match"
            : snapshots.containsKey(player.getUniqueId()) ? "Post-match session" : "In SMP";
        player.sendMessage(messages.format(
            "pvp.status",
            "&7[&cPvP Beta&7] &fState: &e{state}&f | Party: &e{party}&f | Arenas: &e{open}&f open / &e{busy}&f busy",
            Map.of(
                "state", session,
                "party", partyStatus,
                "open", Integer.toString(availableArenaCount()),
                "busy", Integer.toString(occupiedArenas.size())
            )
        ));
        return true;
    }

    public boolean showModes(Player player) {
        player.sendMessage(messages.prefixed("pvp.mode-list-header", "&6Configured PvP modes:"));
        int partySize = resolveParty(player).map(PartyService.PartyView::size).orElse(1);
        settings.pvp().modes().values().forEach(mode -> player.sendMessage(messages.format(
            "pvp.mode-list-entry",
            "&e{mode}&7 -> teams {sizes}&7 | needs &f{players}&7 players | your party: &f{fit}",
            Map.of(
                "mode", ChatColor.stripColor(mode.displayName()),
                "sizes", mode.teamSizes().toString(),
                "players", Integer.toString(mode.totalPlayers()),
                "fit", describePartyFit(partySize, mode)
            )
        )));
        return true;
    }

    public boolean showKits(Player player) {
        List<KitService.KitDefinition> kits = availableKits();
        if (kits.isEmpty()) {
            player.sendMessage(messages.prefixed("pvp.no-kits", "&cNo PvP kits are configured."));
            return true;
        }
        player.sendMessage(messages.prefixed("pvp.kit-list-header", "&6Configured PvP kits:"));
        kits.forEach(kit -> player.sendMessage(messages.format(
            "pvp.kit-list-entry",
            "&e{kit}&7 -> &f{items}&7 items",
            Map.of("kit", kit.name(), "items", Integer.toString(kit.items().size()))
        )));
        return true;
    }

    public boolean leaveSession(Player player) {
        if (activeMatchByPlayer.containsKey(player.getUniqueId())) {
            player.sendMessage(messages.prefixed("pvp.leave-blocked", "&cYou cannot leave an active PvP match. Finish it or die out first."));
            return true;
        }
        return returnToSmp(player);
    }

    private Optional<PartyService.PartyView> resolveParty(Player player) {
        return partyService.partyOf(player.getUniqueId()).filter(party -> party.size() >= 2);
    }

    private Optional<KitService.KitDefinition> findAvailableKit(String kitName) {
        return availableKits().stream()
            .filter(kit -> kit.name().equalsIgnoreCase(kitName))
            .findFirst();
    }

    private List<KitService.KitDefinition> availableKits() {
        List<String> allowed = settings.pvp().kits();
        if (allowed.isEmpty()) {
            return kitService.names().stream().map(kitService::find).flatMap(Optional::stream).toList();
        }
        return allowed.stream().map(kitService::find).flatMap(Optional::stream).toList();
    }

    private void openSetupMenu(Player player, PluginSettings.PvpMode mode, String selectedKitName, List<KitService.KitDefinition> kits) {
        KitService.KitDefinition selectedKit = findAvailableKit(selectedKitName).orElseGet(kits::getFirst);
        gui.openSetupMenu(player, new PvpGui.SetupView(
            mode.key(),
            mode.displayName(),
            mode.teamSizes().toString(),
            mode.totalPlayers(),
            mode.icon(),
            availableArenaCount(),
            partyLore(resolveParty(player)),
            requirementLore(player, mode),
            selectedKit.name(),
            kitLore(selectedKit)
        ));
    }

    private List<String> partyLore(Optional<PartyService.PartyView> party) {
        if (party.isEmpty()) {
            return List.of(
                ChatColor.RED + "No eligible party yet",
                ChatColor.GRAY + "Use /p <player> to invite players",
                ChatColor.GRAY + "You need 2 or more players for PvP"
            );
        }
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Leader: " + ChatColor.WHITE + displayName(party.get().leaderId()));
        lore.add(ChatColor.GRAY + "Members:");
        party.get().members().forEach(memberId -> lore.add(ChatColor.WHITE + "- " + displayName(memberId)));
        return lore;
    }

    private List<String> requirementLore(Player player, PluginSettings.PvpMode mode) {
        Optional<PartyService.PartyView> party = resolveParty(player);
        List<String> lore = new ArrayList<>();
        lore.add(requirementLine("Party size", party.map(view -> view.size() == mode.totalPlayers()).orElse(false), mode.totalPlayers() + " players required"));
        lore.add(requirementLine("Arena free", availableArenaCount() > 0, availableArenaCount() + " open"));
        lore.add(requirementLine("Combat clear", party.map(this::isPartyCombatClear).orElse(true), "No member tagged"));
        lore.add(requirementLine("Roster ready", party.map(this::isPartyReady).orElse(false), "All real players online"));
        return lore;
    }

    private String requirementLine(String label, boolean ok, String detail) {
        return (ok ? ChatColor.GREEN : ChatColor.RED) + label + ": " + ChatColor.WHITE + detail;
    }

    private List<String> kitLore(KitService.KitDefinition kit) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Items: " + ChatColor.WHITE + kit.items().size());
        if (kit.permission() != null && !kit.permission().isBlank()) {
            lore.add(ChatColor.GRAY + "Permission: " + ChatColor.WHITE + kit.permission());
        } else {
            lore.add(ChatColor.GRAY + "Permission: " + ChatColor.WHITE + "None");
        }
        lore.add(ChatColor.YELLOW + "Click to choose this kit");
        return lore;
    }

    private boolean isPartyCombatClear(PartyService.PartyView party) {
        return party.members().stream().noneMatch(combatState::isTagged);
    }

    private boolean isBusyForMatchStart(UUID playerId) {
        if (activeMatchByPlayer.containsKey(playerId)) {
            return true;
        }
        return snapshots.containsKey(playerId) && !postMatchStates.containsKey(playerId);
    }

    private int availableArenaCount() {
        return Math.max(0, settings.pvp().arenaCount() - occupiedArenas.size());
    }

    private String randomCompatibleModeKey(Player player) {
        int partySize = resolveParty(player).map(PartyService.PartyView::size).orElse(-1);
        List<PluginSettings.PvpMode> compatible = settings.pvp().modes().values().stream()
            .filter(mode -> mode.totalPlayers() == partySize)
            .toList();
        if (compatible.isEmpty()) {
            return null;
        }
        return compatible.get(random.nextInt(compatible.size())).key();
    }

    private String describePartyFit(int partySize, PluginSettings.PvpMode mode) {
        return partySize == mode.totalPlayers()
            ? ChatColor.GREEN + "Ready"
            : ChatColor.RED + "Need " + mode.totalPlayers();
    }

    private boolean isPartyReady(PartyService.PartyView party) {
        return party.members().stream()
            .filter(memberId -> !testService.isTestPlayer(memberId))
            .allMatch(memberId -> Bukkit.getPlayer(memberId) != null);
    }

    private Integer nextArenaIndex() {
        for (int index = 0; index < settings.pvp().arenaCount(); index++) {
            if (!occupiedArenas.contains(index)) {
                return index;
            }
        }
        return null;
    }

    private World ensureWorld() {
        if (cachedWorld != null) {
            return cachedWorld;
        }
        World world = Bukkit.getWorld(settings.pvp().worldName());
        if (world == null) {
            WorldCreator creator = new WorldCreator(settings.pvp().worldName());
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("Unable to create PvP world");
        }
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setStorm(false);
        world.setTime(6000L);
        cachedWorld = world;
        return world;
    }

    private void buildArena(World world, int arenaIndex) {
        PluginSettings.PvpArena arena = settings.pvp().arena();
        Location center = arenaCenter(arenaIndex);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int half = arena.halfSize();
        int height = arena.height();
        Material floor = arena.floorMaterial();
        Material wall = arena.wallMaterial();
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(world, x, cy - 1, z, floor);
                for (int y = cy; y <= cy + height; y++) {
                    boolean boundary = x == cx - half || x == cx + half || z == cz - half || z == cz + half;
                    setBlock(world, x, y, z, boundary ? wall : Material.AIR);
                }
            }
        }
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) {
            block.setType(material, false);
        }
    }

    private Match createMatch(List<UUID> orderedMembers, PluginSettings.PvpMode mode, KitService.KitDefinition kit, int arenaIndex) {
        Map<UUID, ParticipantState> participantStates = new LinkedHashMap<>();
        int split = mode.teamSizes().get(0);
        for (int index = 0; index < orderedMembers.size(); index++) {
            UUID memberId = orderedMembers.get(index);
            int teamIndex = index < split ? 0 : 1;
            participantStates.put(memberId, new ParticipantState(memberId, teamIndex, true, new Stats()));
        }
        Match match = new Match(UUID.randomUUID(), mode.key(), mode.displayName(), kit.name(), arenaIndex, participantStates, Instant.now());
        orderedMembers.stream()
            .filter(memberId -> !testService.isTestPlayer(memberId))
            .forEach(memberId -> activeMatchByPlayer.put(memberId, new ActiveMatch(match)));
        postMatchStates.keySet().removeIf(participantStates::containsKey);
        return match;
    }

    private void launch(Match match, World world, KitService.KitDefinition kit, List<Player> liveParticipants) {
        List<Player> participants = liveParticipants.stream()
            .sorted(Comparator.comparing(Player::getName))
            .toList();
        int team0Count = 0;
        int team1Count = 0;
        for (Player participant : participants) {
            ParticipantState state = match.participants().get(participant.getUniqueId());
            participant.closeInventory();
            clearForMatch(participant);
            combatState.clear(participant.getUniqueId());
            if (state.teamIndex() == 0) {
                participant.teleport(teamSpawn(world, match.arenaIndex(), 0, team0Count++));
            } else {
                participant.teleport(teamSpawn(world, match.arenaIndex(), 1, team1Count++));
            }
            kitService.giveKit(participant, match.kitName());
            participant.sendMessage(messages.format(
                "pvp.started",
                "&cPvP beta started: &e{mode}&c with kit &e{kit}&c.",
                Map.of("mode", match.modeDisplayName(), "kit", match.kitName())
            ));
        }
        for (UUID participantId : match.participants().keySet()) {
            if (!testService.isTestPlayer(participantId)) {
                continue;
            }
            ParticipantState state = match.participants().get(participantId);
            spawnTestParticipant(participantId, state, match, world, kit, state.teamIndex() == 0 ? team0Count++ : team1Count++);
        }
    }

    private void spawnTestParticipant(
        UUID participantId,
        ParticipantState state,
        Match match,
        World world,
        KitService.KitDefinition kit,
        int teamOffset
    ) {
        String displayName = displayName(participantId);
        Husk husk = (Husk) world.spawnEntity(teamSpawn(world, match.arenaIndex(), state.teamIndex(), teamOffset), EntityType.HUSK);
        husk.setCustomName(ChatColor.RED + displayName);
        husk.setCustomNameVisible(true);
        husk.setCanPickupItems(false);
        husk.setRemoveWhenFarAway(false);
        husk.setShouldBurnInDay(false);
        husk.setPersistent(false);
        prepareTestEntityLoadout(husk, kit);
        participantIdByEntityId.put(husk.getUniqueId(), participantId);
        entityIdByParticipantId.put(participantId, husk.getUniqueId());
    }

    private void prepareTestEntityLoadout(LivingEntity entity, KitService.KitDefinition kit) {
        if (entity.getEquipment() == null) {
            return;
        }
        entity.getEquipment().clear();
        entity.getEquipment().setHelmetDropChance(0.0F);
        entity.getEquipment().setChestplateDropChance(0.0F);
        entity.getEquipment().setLeggingsDropChance(0.0F);
        entity.getEquipment().setBootsDropChance(0.0F);
        entity.getEquipment().setItemInMainHandDropChance(0.0F);
        entity.getEquipment().setItemInOffHandDropChance(0.0F);
        for (ItemStack item : kit.items()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            switch (item.getType()) {
                case NETHERITE_HELMET, DIAMOND_HELMET, IRON_HELMET, GOLDEN_HELMET, CHAINMAIL_HELMET, LEATHER_HELMET ->
                    entity.getEquipment().setHelmet(item.clone());
                case NETHERITE_CHESTPLATE, DIAMOND_CHESTPLATE, IRON_CHESTPLATE, GOLDEN_CHESTPLATE, CHAINMAIL_CHESTPLATE, LEATHER_CHESTPLATE ->
                    entity.getEquipment().setChestplate(item.clone());
                case NETHERITE_LEGGINGS, DIAMOND_LEGGINGS, IRON_LEGGINGS, GOLDEN_LEGGINGS, CHAINMAIL_LEGGINGS, LEATHER_LEGGINGS ->
                    entity.getEquipment().setLeggings(item.clone());
                case NETHERITE_BOOTS, DIAMOND_BOOTS, IRON_BOOTS, GOLDEN_BOOTS, CHAINMAIL_BOOTS, LEATHER_BOOTS ->
                    entity.getEquipment().setBoots(item.clone());
                case SHIELD -> entity.getEquipment().setItemInOffHand(item.clone());
                default -> {
                    if (item.getType().name().endsWith("_SWORD")
                        || item.getType().name().endsWith("_AXE")
                        || item.getType() == Material.BOW
                        || item.getType() == Material.CROSSBOW
                        || item.getType() == Material.TRIDENT) {
                        if (entity.getEquipment().getItemInMainHand().getType() == Material.AIR) {
                            entity.getEquipment().setItemInMainHand(item.clone());
                        }
                    }
                }
            }
        }
    }

    private void clearForMatch(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setExtraContents(new ItemStack[0]);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
        player.setFireTicks(0);
        player.setVelocity(new Vector());
    }

    private int remainingWinningTeam(Match match) {
        Set<Integer> aliveTeams = new HashSet<>();
        for (ParticipantState state : match.participants().values()) {
            if (state.alive) {
                aliveTeams.add(state.teamIndex());
            }
        }
        return aliveTeams.size() == 1 ? aliveTeams.iterator().next() : -1;
    }

    private boolean forfeitParticipant(UUID participantId, String playerName) {
        ActiveMatch activeMatch = activeMatchByParticipant(participantId);
        if (activeMatch == null) {
            return false;
        }
        Match match = activeMatch.match();
        ParticipantState participant = match.participants().get(participantId);
        if (participant == null || !participant.alive) {
            return false;
        }
        participant.alive = false;
        participant.stats().deaths += 1;
        activeMatchByPlayer.remove(participantId);
        notifyMatchQuit(match, playerName);
        int winningTeam = remainingWinningTeam(match);
        if (winningTeam >= 0) {
            finishMatch(match, winningTeam, Set.of(participantId));
        }
        return true;
    }

    private void finishMatch(Match match, int winningTeam) {
        finishMatch(match, winningTeam, Set.of());
    }

    private void finishMatch(Match match, int winningTeam, Set<UUID> excludedPlayers) {
        occupiedArenas.remove(match.arenaIndex());
        String winners = match.participants().values().stream()
            .filter(state -> state.teamIndex() == winningTeam)
            .map(state -> displayName(state.playerId()))
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElse("Unknown");
        String winnerLine = "Winners: " + winners;
        for (ParticipantState state : match.participants().values()) {
            activeMatchByPlayer.remove(state.playerId());
            cleanupTestEntity(state.playerId());
            if (testService.isTestPlayer(state.playerId())) {
                continue;
            }
            if (excludedPlayers.contains(state.playerId())) {
                postMatchStates.remove(state.playerId());
                continue;
            }
            postMatchStates.put(state.playerId(), new PostMatchState(match.modeKey(), match.kitName(), match.arenaIndex(), winnerLine));
            Player player = Bukkit.getPlayer(state.playerId());
            if (player == null) {
                continue;
            }
            combatState.clear(state.playerId());
            player.sendMessage(messages.format(
                "pvp.finished",
                "&6Match complete. Winners: &e{winners}&6.",
                Map.of("winners", winners)
            ));
            sendStats(player, match, winningTeam);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(arenaCenter(match.arenaIndex()));
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
                openPostMatchMenu(player);
            });
        }
    }

    private void notifyMatchQuit(Match match, String playerName) {
        Map<String, String> placeholders = Map.of("player", playerName);
        for (ParticipantState state : match.participants().values()) {
            if (testService.isTestPlayer(state.playerId())) {
                continue;
            }
            Player player = Bukkit.getPlayer(state.playerId());
            if (player == null) {
                continue;
            }
            player.sendMessage(messages.format(
                "pvp.player-quit",
                "&c{player} quit the PvP match.",
                placeholders
            ));
        }
    }

    private void sendStats(Player player, Match match, int winningTeam) {
        ParticipantState state = match.participants().get(player.getUniqueId());
        if (state == null) {
            return;
        }
        Duration duration = Duration.between(match.startedAt(), Instant.now());
        player.sendMessage(messages.format(
            "pvp.summary-header",
            "&7[&cPvP Beta&7] &fWinner: &e{winner}&f | Duration: &e{time}s",
            Map.of("winner", winningTeam == state.teamIndex() ? "Your team" : "Enemy team", "time", Long.toString(duration.toSeconds()))
        ));
        player.sendMessage(messages.format(
            "pvp.summary-body",
            "&fKills: &e{kills}&f | Deaths: &e{deaths}&f | Damage dealt: &e{dealt}&f | Damage taken: &e{taken}",
            Map.of(
                "kills", Integer.toString(state.stats().kills),
                "deaths", Integer.toString(state.stats().deaths),
                "dealt", String.format(Locale.US, "%.1f", state.stats().damageDealt),
                "taken", String.format(Locale.US, "%.1f", state.stats().damageTaken)
            )
        ));
    }

    private void restoreSnapshot(Player player, Snapshot snapshot) {
        player.closeInventory();
        player.teleport(snapshot.location());
        player.setGameMode(snapshot.gameMode());
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
        player.setCanPickupItems(true);
        player.setCollidable(true);
        player.setInvulnerable(false);
        player.getInventory().clear();
        player.getInventory().setContents(cloneItems(snapshot.contents()));
        player.getInventory().setArmorContents(cloneItems(snapshot.armor()));
        player.getInventory().setExtraContents(cloneItems(snapshot.extra()));
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        snapshot.effects().forEach(player::addPotionEffect);
        player.setExp(snapshot.exp());
        player.setLevel(snapshot.level());
        player.setTotalExperience(snapshot.totalExperience());
        player.setFoodLevel(snapshot.foodLevel());
        player.setSaturation(snapshot.saturation());
        player.setExhaustion(snapshot.exhaustion());
        player.setFireTicks(snapshot.fireTicks());
        player.setHealth(Math.min(snapshot.health(), player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
        player.updateInventory();
    }

    private ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int index = 0; index < source.length; index++) {
            copy[index] = source[index] == null ? null : source[index].clone();
        }
        return copy;
    }

    private Location arenaCenter(int arenaIndex) {
        World world = ensureWorld();
        int spacing = settings.pvp().arena().spacing();
        int yLevel = settings.pvp().arena().yLevel();
        return new Location(world, arenaIndex * spacing + 0.5D, yLevel, 0.5D);
    }

    private Location teamSpawn(World world, int arenaIndex, int teamIndex, int teamOffset) {
        int half = settings.pvp().arena().halfSize();
        int spread = Math.max(2, settings.pvp().arena().spawnSpread());
        double x = arenaIndex * settings.pvp().arena().spacing() + 0.5D + (teamIndex == 0 ? -half + 3 : half - 3);
        double z = 0.5D + ((teamOffset * spread) - ((spread * Math.max(0, settings.pvp().modes().values().stream().mapToInt(mode -> mode.teamSizes().get(teamIndex)).max().orElse(1) - 1)) / 2.0D));
        Location location = new Location(world, x, settings.pvp().arena().yLevel(), z);
        location.setYaw(teamIndex == 0 ? -90.0F : 90.0F);
        return location;
    }

    private Player extractPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof org.bukkit.projectiles.ProjectileSource) {
            return null;
        }
        if (damager instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private UUID resolveParticipantId(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof Player player) {
            return player.getUniqueId();
        }
        UUID participantId = participantIdByEntityId.get(entity.getUniqueId());
        if (participantId != null) {
            return participantId;
        }
        if (entity instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player.getUniqueId();
            }
            if (projectile.getShooter() instanceof Entity shooter) {
                return participantIdByEntityId.get(shooter.getUniqueId());
            }
        }
        return null;
    }

    private void cleanupTestEntity(UUID participantId) {
        UUID entityId = entityIdByParticipantId.remove(participantId);
        if (entityId == null) {
            return;
        }
        participantIdByEntityId.remove(entityId);
        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    private boolean supportsTestPartyLayout(PartyService.PartyView party, PluginSettings.PvpMode mode) {
        List<UUID> members = party.members();
        boolean hasTestPlayers = members.stream().anyMatch(testService::isTestPlayer);
        if (!hasTestPlayers) {
            return true;
        }
        int split = mode.teamSizes().getFirst();
        List<UUID> teamOne = members.subList(0, Math.min(split, members.size()));
        List<UUID> teamTwo = members.subList(Math.min(split, members.size()), members.size());
        return allReal(teamOne) && allTests(teamTwo);
    }

    private boolean allReal(List<UUID> members) {
        return members.stream().noneMatch(testService::isTestPlayer);
    }

    private boolean allTests(List<UUID> members) {
        return !members.isEmpty() && members.stream().allMatch(testService::isTestPlayer);
    }

    private boolean handleTestCommand(Player player, String[] args) {
        if (!player.hasPermission("diamondsmp.admin.pvp-test")) {
            player.sendMessage(messages.prefixed("pvp.test.no-permission", "&cYou do not have permission to manage PvP test players."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(messages.prefixed("pvp.test.usage", "&cUsage: /pvp test <spawn|clear|list> [count]"));
            return true;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "spawn", "add" -> handleSpawnTestPlayers(player, args);
            case "clear", "remove" -> handleClearTestPlayers(player);
            case "list" -> handleListTestPlayers(player);
            default -> {
                player.sendMessage(messages.prefixed("pvp.test.usage", "&cUsage: /pvp test <spawn|clear|list> [count]"));
                yield true;
            }
        };
    }

    private boolean handleSpawnTestPlayers(Player player, String[] args) {
        int count = 1;
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                player.sendMessage(messages.prefixed("pvp.test.invalid-count", "&cTest player count must be a number."));
                return true;
            }
        }
        List<PvpTestService.TestPlayerProfile> created = testService.create(player.getUniqueId(), count);
        PartyService.AddMemberResult firstFailure = null;
        for (PvpTestService.TestPlayerProfile profile : created) {
            PartyService.AddMemberResult result = partyService.addMember(player.getUniqueId(), profile.id());
            if (result != PartyService.AddMemberResult.ADDED && result != PartyService.AddMemberResult.ALREADY_IN_PARTY) {
                firstFailure = result;
                break;
            }
        }
        if (firstFailure == PartyService.AddMemberResult.NOT_LEADER) {
            player.sendMessage(messages.prefixed("pvp.test.not-leader", "&cOnly party leaders can add test players to the party."));
            return true;
        }
        if (firstFailure == PartyService.AddMemberResult.TARGET_IN_OTHER_PARTY) {
            player.sendMessage(messages.prefixed("pvp.test.busy", "&cA test player is already attached to another party."));
            return true;
        }
        player.sendMessage(messages.format(
            "pvp.test.spawned",
            "&aAdded &e{count}&a test player(s): &e{names}&a.",
            Map.of(
                "count", Integer.toString(created.size()),
                "names", created.stream().map(PvpTestService.TestPlayerProfile::name).reduce((left, right) -> left + ", " + right).orElse("none")
            )
        ));
        return true;
    }

    private boolean handleClearTestPlayers(Player player) {
        List<PvpTestService.TestPlayerProfile> profiles = testService.profilesOwnedBy(player.getUniqueId());
        if (profiles.stream().map(PvpTestService.TestPlayerProfile::id).anyMatch(this::isInPvpSession)) {
            player.sendMessage(messages.prefixed("pvp.test.busy", "&cYou cannot clear test players while one is in an active PvP session."));
            return true;
        }
        profiles.stream().map(PvpTestService.TestPlayerProfile::id).forEach(partyService::removeMemberCompletely);
        List<UUID> cleared = testService.clearOwnedBy(player.getUniqueId());
        cleared.forEach(this::cleanupTestEntity);
        player.sendMessage(messages.format(
            "pvp.test.cleared",
            "&aCleared &e{count}&a test player(s) from your party tools.",
            Map.of("count", Integer.toString(cleared.size()))
        ));
        return true;
    }

    private boolean handleListTestPlayers(Player player) {
        List<PvpTestService.TestPlayerProfile> profiles = testService.profilesOwnedBy(player.getUniqueId());
        if (profiles.isEmpty()) {
            player.sendMessage(messages.prefixed("pvp.test.none", "&eYou do not have any PvP test players yet."));
            return true;
        }
        player.sendMessage(messages.format(
            "pvp.test.list",
            "&6Your test players: &e{names}",
            Map.of("names", profiles.stream().map(PvpTestService.TestPlayerProfile::name).reduce((left, right) -> left + ", " + right).orElse("none"))
        ));
        return true;
    }

    public Component inviteAcceptComponent(String inviterName) {
        return Component.text("Click here to accept ", NamedTextColor.YELLOW)
            .append(Component.text("[/p accept " + inviterName + "]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/p accept " + inviterName))
                .hoverEvent(HoverEvent.showText(Component.text("Join " + inviterName + "'s party", NamedTextColor.GREEN))))
            .append(Component.text(" or run /p accept.", NamedTextColor.YELLOW));
    }

    private record ActiveMatch(Match match) {}

    private record Match(
        UUID id,
        String modeKey,
        String modeDisplayName,
        String kitName,
        int arenaIndex,
        Map<UUID, ParticipantState> participants,
        Instant startedAt
    ) {}

    private static final class ParticipantState {
        private final UUID playerId;
        private final int teamIndex;
        private boolean alive;
        private final Stats stats;

        private ParticipantState(UUID playerId, int teamIndex, boolean alive, Stats stats) {
            this.playerId = playerId;
            this.teamIndex = teamIndex;
            this.alive = alive;
            this.stats = stats;
        }

        public UUID playerId() {
            return playerId;
        }

        public int teamIndex() {
            return teamIndex;
        }

        public Stats stats() {
            return stats;
        }
    }

    private static final class Stats {
        private int kills;
        private int deaths;
        private double damageDealt;
        private double damageTaken;
    }

    private record PostMatchState(String modeKey, String kitName, int arenaIndex, String winnerLine) {}

    private record Snapshot(
        Location location,
        GameMode gameMode,
        ItemStack[] contents,
        ItemStack[] armor,
        ItemStack[] extra,
        Collection<PotionEffect> effects,
        float exp,
        int level,
        int totalExperience,
        int foodLevel,
        float saturation,
        float exhaustion,
        int fireTicks,
        double health
    ) {
        private static Snapshot capture(Player player) {
            PlayerInventory inventory = player.getInventory();
            Collection<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
            return new Snapshot(
                player.getLocation().clone(),
                player.getGameMode(),
                inventory.getContents().clone(),
                inventory.getArmorContents().clone(),
                inventory.getExtraContents().clone(),
                effects,
                player.getExp(),
                player.getLevel(),
                player.getTotalExperience(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getFireTicks(),
                player.getHealth()
            );
        }
    }
}
