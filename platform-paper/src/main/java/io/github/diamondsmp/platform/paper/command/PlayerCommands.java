package io.github.diamondsmp.platform.paper.command;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.event.ServerEventManager;
import io.github.diamondsmp.platform.paper.event.ServerEventSnapshot;
import io.github.diamondsmp.platform.paper.gui.RulesGui;
import io.github.diamondsmp.platform.paper.service.CombatStateService;
import io.github.diamondsmp.platform.paper.service.CooldownService;
import io.github.diamondsmp.platform.paper.service.KitService;
import io.github.diamondsmp.platform.paper.service.PartyService;
import io.github.diamondsmp.platform.paper.service.PvpService;
import io.github.diamondsmp.platform.paper.service.TeleportRequestService;
import io.github.diamondsmp.platform.paper.service.TrustService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerCommands implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MessageBundle messages;
    private final CooldownService cooldowns;
    private final CombatStateService combatState;
    private final KitService kitService;
    private final TeleportRequestService teleportRequests;
    private final TrustService trustService;
    private final PartyService partyService;
    private final PvpService pvpService;
    private final RulesGui rulesGui;
    private final ServerEventManager eventManager;

    public PlayerCommands(
        JavaPlugin plugin,
        PluginSettings settings,
        MessageBundle messages,
        CooldownService cooldowns,
        CombatStateService combatState,
        KitService kitService,
        TeleportRequestService teleportRequests,
        TrustService trustService,
        PartyService partyService,
        PvpService pvpService,
        RulesGui rulesGui,
        ServerEventManager eventManager
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.cooldowns = cooldowns;
        this.combatState = combatState;
        this.kitService = kitService;
        this.teleportRequests = teleportRequests;
        this.trustService = trustService;
        this.partyService = partyService;
        this.pvpService = pvpService;
        this.rulesGui = rulesGui;
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        return switch (command.getName().toLowerCase()) {
            case "string" -> handleString(player);
            case "tpa" -> handleTpa(player, args);
            case "tpaccept" -> handleTpAccept(player);
            case "tpdeny" -> handleTpDeny(player);
            case "kit" -> handleKit(player, args);
            case "trust" -> handleTrust(player, args);
            case "untrust" -> handleUntrust(player, args);
            case "spawn" -> handleSpawn(player);
            case "rules" -> handleRules(player);
            case "event" -> handleEvent(player);
            case "p" -> handleParty(player, args);
            case "pvp" -> pvpService.handlePvpCommand(player, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ((command.getName().equalsIgnoreCase("p") || command.getName().equalsIgnoreCase("pvp")) && !settings.pvp().enabled()) {
            return List.of();
        }
        if ((command.getName().equalsIgnoreCase("tpa")
            || command.getName().equalsIgnoreCase("kit")
            || command.getName().equalsIgnoreCase("trust")
            || command.getName().equalsIgnoreCase("untrust")
            || command.getName().equalsIgnoreCase("p")) && args.length == 1) {
            if (command.getName().equalsIgnoreCase("kit")) {
                return kitService.names().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
            }
            if (command.getName().equalsIgnoreCase("p")) {
                java.util.ArrayList<String> completions = new java.util.ArrayList<>(List.of("accept", "leave", "list", "kick", "disband"));
                Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .forEach(completions::add);
                return completions.stream().distinct().filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).toList();
            }
            List<String> completions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
            if (command.getName().equalsIgnoreCase("tpa") && "spawn".startsWith(args[0].toLowerCase())) {
                java.util.ArrayList<String> withSpawn = new java.util.ArrayList<>(completions);
                withSpawn.add("spawn");
                return withSpawn;
            }
            return completions;
        }
        if (command.getName().equalsIgnoreCase("p") && args.length == 2 && args[0].equalsIgnoreCase("kick")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        }
        if (command.getName().equalsIgnoreCase("pvp")) {
            if (args.length == 1) {
                java.util.ArrayList<String> completions = new java.util.ArrayList<>(settings.pvp().modes().keySet());
                completions.addAll(List.of("menu", "status", "modes", "kits", "leave", "return", "rematch", "start", "test"));
                return completions.stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .distinct()
                    .toList();
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
                return List.of("spawn", "list", "clear").stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("test") && args[1].equalsIgnoreCase("spawn")) {
                return List.of("1", "2", "3", "4").stream()
                    .filter(name -> name.startsWith(args[2]))
                    .toList();
            }
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("start")) {
                    return settings.pvp().modes().keySet().stream()
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .toList();
                }
                List<String> kitNames = settings.pvp().kits().isEmpty() ? kitService.names() : settings.pvp().kits();
                return kitNames.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
                List<String> kitNames = settings.pvp().kits().isEmpty() ? kitService.names() : settings.pvp().kits();
                return kitNames.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
            }
        }
        return List.of();
    }

    private boolean handleString(Player player) {
        if (!cooldowns.isReady(player.getUniqueId(), "string")) {
            player.sendMessage(messages.format(
                "string.cooldown",
                "&c/string is on cooldown for {time}s.",
                Map.of("time", Long.toString(cooldowns.remaining(player.getUniqueId(), "string").toSeconds()))
            ));
            return true;
        }
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                player.getInventory().setItem(slot, new ItemStack(Material.STRING, 64));
            }
        }
        cooldowns.apply(player.getUniqueId(), "string", settings.cooldowns().stringCommand());
        player.sendMessage(messages.prefixed("string.success", "&aFilled empty slots with string."));
        return true;
    }

    private boolean handleTpa(Player player, String[] args) {
        if (!settings.toggles().teleport()) {
            player.sendMessage(messages.prefixed("teleport.disabled", "&cTeleport requests are disabled."));
            return true;
        }
        if (combatState.isTagged(player.getUniqueId())) {
            player.sendMessage(messages.prefixed("teleport.combat", "&cYou cannot use teleport requests during combat."));
            return true;
        }
        if (pvpService.blocksTeleport(player.getUniqueId())) {
            player.sendMessage(messages.prefixed("teleport.pvp", "&cYou cannot use teleport requests while inside PvP."));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(messages.prefixed("teleport.usage", "&cUsage: /tpa <player>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("spawn")) {
            return handleSpawn(player);
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(messages.prefixed("teleport.invalid-target", "&cInvalid target."));
            return true;
        }
        if (pvpService.blocksTeleport(target.getUniqueId())) {
            player.sendMessage(messages.format("teleport.target-pvp", "&c{player} is in PvP and cannot accept teleports.", Map.of("player", target.getName())));
            return true;
        }
        if (combatState.isTagged(target.getUniqueId())) {
            player.sendMessage(messages.format("teleport.target-combat", "&c{player} is in combat and cannot accept teleports.", Map.of("player", target.getName())));
            return true;
        }
        teleportRequests.create(player, target, settings.cooldowns().teleportRequest());
        player.sendMessage(messages.format("teleport.sent", "&aSent request to {player}.", Map.of("player", target.getName())));
        target.sendMessage(messages.format("teleport.received", "&e{player} wants to teleport to you.", Map.of("player", player.getName())));
        target.sendMessage(
            Component.text("Click here to accept ", NamedTextColor.YELLOW)
                .append(Component.text("[/tpaccept]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/tpaccept"))
                    .hoverEvent(HoverEvent.showText(Component.text("Accept teleport request", NamedTextColor.GREEN))))
                .append(Component.text(" or use /tpdeny.", NamedTextColor.YELLOW))
        );
        Bukkit.getScheduler().runTaskLater(
            plugin,
            () -> teleportRequests.expire(target.getUniqueId(), player.getUniqueId()).ifPresent(request -> {
                Player requester = Bukkit.getPlayer(request.requesterId());
                Player currentTarget = Bukkit.getPlayer(request.targetId());
                if (requester != null) {
                    requester.sendMessage(messages.format("teleport.timeout", "&cTeleport request to {player} timed out.", Map.of("player", target.getName())));
                }
                if (currentTarget != null) {
                    currentTarget.sendMessage(messages.format("teleport.timeout-target", "&cTeleport request from {player} expired.", Map.of("player", player.getName())));
                }
            }),
            settings.cooldowns().teleportRequest().toSeconds() * 20L
        );
        return true;
    }

    private boolean handleTpAccept(Player player) {
        return teleportRequests.remove(player).map(request -> {
            Player requester = Bukkit.getPlayer(request.requesterId());
            if (requester == null) {
                player.sendMessage(messages.prefixed("teleport.missing", "&cRequester went offline."));
                return true;
            }
            if (combatState.isTagged(player.getUniqueId())) {
                player.sendMessage(messages.prefixed("teleport.combat", "&cYou cannot use teleport requests during combat."));
                return true;
            }
            if (pvpService.blocksTeleport(player.getUniqueId())) {
                player.sendMessage(messages.prefixed("teleport.pvp", "&cYou cannot use teleport requests while inside PvP."));
                return true;
            }
            if (combatState.isTagged(requester.getUniqueId())) {
                player.sendMessage(messages.format("teleport.requester-combat", "&c{player} is in combat and cannot teleport right now.", Map.of("player", requester.getName())));
                return true;
            }
            if (pvpService.blocksTeleport(requester.getUniqueId())) {
                player.sendMessage(messages.format("teleport.requester-pvp", "&c{player} is in PvP and cannot teleport right now.", Map.of("player", requester.getName())));
                return true;
            }
            requester.teleport(player.getLocation());
            requester.sendMessage(messages.format("teleport.accepted", "&aTeleport accepted by {player}.", Map.of("player", player.getName())));
            player.sendMessage(messages.prefixed("teleport.accept", "&aTeleport request accepted."));
            return true;
        }).orElseGet(() -> {
            player.sendMessage(messages.prefixed("teleport.none", "&cNo pending teleport request."));
            return true;
        });
    }

    private boolean handleEvent(Player player) {
        return eventManager.activeSnapshot().map(snapshot -> {
            player.sendMessage(formatEventSnapshot(snapshot));
            return true;
        }).orElseGet(() -> {
            player.sendMessage(messages.prefixed("events.none", "&eThere is no active server event right now."));
            return true;
        });
    }

    private Component formatEventSnapshot(ServerEventSnapshot snapshot) {
        return Component.text()
            .append(Component.text("Current event: ", NamedTextColor.YELLOW))
            .append(Component.text(snapshot.displayName(), NamedTextColor.GOLD))
            .append(Component.text(" | Remaining: ", NamedTextColor.YELLOW))
            .append(Component.text(snapshot.remaining() + "/" + snapshot.participants(), NamedTextColor.GREEN))
            .append(Component.text(" | Reward: ", NamedTextColor.YELLOW))
            .append(Component.text(snapshot.rewardVillager(), NamedTextColor.GOLD))
            .append(Component.text(" | " + snapshot.summary(), NamedTextColor.GRAY))
            .build();
    }

    private boolean handleTrust(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(messages.prefixed("trust.usage", "&cUsage: /trust <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(messages.prefixed("trust.invalid-target", "&cInvalid target."));
            return true;
        }
        if (trustService.isTrusted(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(messages.format("trust.already", "&eYou already trust-hit with {player}.", Map.of("player", target.getName())));
            return true;
        }
        if (trustService.accept(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(messages.format("trust.accepted", "&aYou are now trusted with {player}.", Map.of("player", target.getName())));
            target.sendMessage(messages.format("trust.accepted-other", "&a{player} accepted your trust request.", Map.of("player", player.getName())));
            return true;
        }
        if (trustService.hasPendingRequest(player.getUniqueId(), target.getUniqueId())
            || trustService.hasIncomingRequest(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(messages.format("trust.pending", "&eA trust request is already pending with {player}.", Map.of("player", target.getName())));
            return true;
        }
        trustService.request(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(messages.format("trust.sent", "&aSent a trust request to {player}.", Map.of("player", target.getName())));
        target.sendMessage(messages.format("trust.received", "&e{player} wants to trust-hit with you.", Map.of("player", player.getName())));
        target.sendMessage(
            Component.text("Click here to accept ", NamedTextColor.YELLOW)
                .append(Component.text("[/trust " + player.getName() + "]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/trust " + player.getName()))
                    .hoverEvent(HoverEvent.showText(Component.text("Accept trust request", NamedTextColor.GREEN))))
        );
        return true;
    }

    private boolean handleUntrust(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(messages.prefixed("trust.untrust-usage", "&cUsage: /untrust <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(messages.prefixed("trust.invalid-target", "&cInvalid target."));
            return true;
        }
        if (!trustService.remove(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(messages.format("trust.none", "&cNo trust or request existed with {player}.", Map.of("player", target.getName())));
            return true;
        }
        player.sendMessage(messages.format("trust.removed", "&cTrust removed with {player}.", Map.of("player", target.getName())));
        target.sendMessage(messages.format("trust.removed-other", "&c{player} removed trust with you.", Map.of("player", player.getName())));
        return true;
    }

    private boolean handleTpDeny(Player player) {
        return teleportRequests.remove(player).map(request -> {
            Player requester = Bukkit.getPlayer(request.requesterId());
            if (requester != null) {
                requester.sendMessage(messages.format("teleport.denied", "&c{player} denied your teleport request.", Map.of("player", player.getName())));
            }
            player.sendMessage(messages.prefixed("teleport.deny", "&cTeleport request denied."));
            return true;
        }).orElseGet(() -> {
            player.sendMessage(messages.prefixed("teleport.none", "&cNo pending teleport request."));
            return true;
        });
    }

    private boolean handleKit(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(messages.prefixed("kit.usage", "&cUsage: /kit <name>"));
            return true;
        }
        if (!settings.kits().allowRegularUsers() && !player.hasPermission("diamondsmp.admin.kit")) {
            player.sendMessage(messages.prefixed("kit.disabled", "&cOnly admins can use kits right now."));
            return true;
        }
        return kitService.find(args[0]).map(kit -> {
            if (kit.permission() != null && !kit.permission().isBlank() && !player.hasPermission(kit.permission())) {
                player.sendMessage(messages.prefixed("kit.no-permission", "&cYou do not have permission to use that kit."));
                return true;
            }
            if (!kitService.giveKit(player, kit.name())) {
                player.sendMessage(messages.prefixed("kit.not-found", "&cThat kit does not exist."));
                return true;
            }
            player.sendMessage(messages.format("kit.received", "&aReceived kit &e{name}&a.", Map.of("name", kit.name())));
            return true;
        }).orElseGet(() -> {
            player.sendMessage(messages.prefixed("kit.not-found", "&cThat kit does not exist."));
            return true;
        });
    }

    private boolean handleSpawn(Player player) {
        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage(messages.prefixed("spawn.teleport", "&aTeleported to spawn."));
        return true;
    }

    private boolean handleRules(Player player) {
        rulesGui.open(player);
        return true;
    }

    private boolean handleParty(Player player, String[] args) {
        if (!settings.pvp().enabled()) {
            player.sendMessage(messages.prefixed("party.disabled", "&cParty PvP beta is disabled right now."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            return showParty(player);
        }
        if (args[0].equalsIgnoreCase("accept")) {
            return handlePartyAccept(player, args);
        }
        if (args[0].equalsIgnoreCase("leave")) {
            if (!partyService.leave(player.getUniqueId())) {
                player.sendMessage(messages.prefixed("party.none", "&eYou are not in a party."));
                return true;
            }
            player.sendMessage(messages.prefixed("party.left", "&eYou left the party."));
            return true;
        }
        if (args[0].equalsIgnoreCase("disband")) {
            if (!partyService.disband(player.getUniqueId())) {
                player.sendMessage(messages.prefixed("party.not-leader", "&cOnly the party leader can disband the party."));
                return true;
            }
            player.sendMessage(messages.prefixed("party.disbanded", "&cYour party was disbanded."));
            return true;
        }
        if (args[0].equalsIgnoreCase("kick")) {
            if (args.length != 2) {
                player.sendMessage(messages.prefixed("party.kick-usage", "&cUsage: /p kick <player>"));
                return true;
            }
            Optional<UUID> targetId = resolvePartyTarget(player, args[1]);
            if (targetId.isEmpty() || !partyService.kick(player.getUniqueId(), targetId.get())) {
                player.sendMessage(messages.prefixed("party.kick-failed", "&cYou cannot kick that player from the party."));
                return true;
            }
            String targetName = pvpService.displayName(targetId.get());
            player.sendMessage(messages.format("party.kicked", "&eRemoved {player} from the party.", Map.of("player", targetName)));
            Player onlineTarget = Bukkit.getPlayer(targetId.get());
            if (onlineTarget != null) {
                onlineTarget.sendMessage(messages.format("party.kicked-other", "&cYou were removed from {player}'s party.", Map.of("player", player.getName())));
            }
            return true;
        }
        return handlePartyInvite(player, args[0]);
    }

    private boolean showParty(Player player) {
        Optional<PartyService.PartyView> party = partyService.partyOf(player.getUniqueId());
        if (party.isEmpty()) {
            player.sendMessage(messages.prefixed("party.none", "&eYou are not in a party."));
            return true;
        }
        String members = party.get().members().stream()
            .map(pvpService::displayName)
            .reduce((left, right) -> left + ", " + right)
            .orElse(player.getName());
        player.sendMessage(messages.format("party.list", "&6Party members: &e{members}", Map.of("members", members)));
        return true;
    }

    private boolean handlePartyAccept(Player player, String[] args) {
        Optional<PartyService.PartyInvite> accepted = args.length == 2
            ? resolvePlayerId(args[1]).flatMap(inviterId -> partyService.accept(player.getUniqueId(), inviterId))
            : partyService.acceptLatest(player.getUniqueId());
        return accepted.map(invite -> {
            Player inviter = Bukkit.getPlayer(invite.inviterId());
            String inviterName = inviter == null ? "that party" : inviter.getName();
            player.sendMessage(messages.format("party.accepted", "&aJoined {player}'s party.", Map.of("player", inviterName)));
            if (inviter != null) {
                inviter.sendMessage(messages.format("party.accepted-other", "&a{player} joined your party.", Map.of("player", player.getName())));
            }
            return true;
        }).orElseGet(() -> {
            player.sendMessage(messages.prefixed("party.no-invite", "&cYou do not have a matching party invite."));
            return true;
        });
    }

    private boolean handlePartyInvite(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || target.equals(player)) {
            player.sendMessage(messages.prefixed("party.invalid-target", "&cThat player is not available."));
            return true;
        }
        PartyService.InviteResult result = partyService.invite(player.getUniqueId(), target.getUniqueId(), settings.pvp().partyInviteTimeout());
        if (result == PartyService.InviteResult.ALREADY_IN_PARTY) {
            player.sendMessage(messages.format("party.already", "&e{player} is already in your party.", Map.of("player", target.getName())));
            return true;
        }
        if (result == PartyService.InviteResult.ALREADY_PENDING) {
            player.sendMessage(messages.format("party.pending", "&eA party invite is already pending for {player}.", Map.of("player", target.getName())));
            return true;
        }
        if (result == PartyService.InviteResult.TARGET_IN_OTHER_PARTY) {
            player.sendMessage(messages.format("party.target-busy", "&c{player} is already in another party.", Map.of("player", target.getName())));
            return true;
        }
        if (result == PartyService.InviteResult.INVALID) {
            player.sendMessage(messages.prefixed("party.invalid-target", "&cThat player is not available."));
            return true;
        }
        player.sendMessage(messages.format("party.sent", "&aSent a party invite to {player}.", Map.of("player", target.getName())));
        target.sendMessage(messages.format("party.received", "&e{player} invited you to a PvP party.", Map.of("player", player.getName())));
        target.sendMessage(pvpService.inviteAcceptComponent(player.getName()));
        return true;
    }

    private Optional<UUID> resolvePlayerId(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player == null ? Optional.empty() : Optional.of(player.getUniqueId());
    }

    private Optional<UUID> resolvePartyTarget(Player actor, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online.getUniqueId());
        }
        return Optional.empty();
    }
}
