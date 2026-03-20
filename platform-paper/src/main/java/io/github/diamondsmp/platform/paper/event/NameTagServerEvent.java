package io.github.diamondsmp.platform.paper.event;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.villager.VillagerType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class NameTagServerEvent implements ServerEvent {
    private static final String GREEN_TEAM = "dsmp_event_green";
    private static final String RED_TEAM = "dsmp_event_red";

    private final JavaPlugin plugin;
    private final MessageBundle messages;
    private final EventRewardDispatcher rewardDispatcher;
    private final Set<UUID> greenPlayers = new HashSet<>();
    private final Set<UUID> allParticipants = new HashSet<>();
    private VillagerType rewardVillager;
    private boolean running;
    private BukkitTask syncTask;

    public NameTagServerEvent(JavaPlugin plugin, MessageBundle messages, EventRewardDispatcher rewardDispatcher) {
        this.plugin = plugin;
        this.messages = messages;
        this.rewardDispatcher = rewardDispatcher;
    }

    @Override
    public ServerEventType type() {
        return ServerEventType.NAME_TAG;
    }

    @Override
    public void start(Set<UUID> participants, VillagerType rewardVillager) {
        this.rewardVillager = rewardVillager;
        this.greenPlayers.clear();
        this.allParticipants.clear();
        for (UUID id : participants) {
            greenPlayers.add(id);
            allParticipants.add(id);
        }
        for (Player participant : Bukkit.getOnlinePlayers()) {
            if (!allParticipants.contains(participant.getUniqueId())) {
                continue;
            }
            participant.sendMessage(messages.prefixed("events.start", "&aName Tag event started. Stay green to survive."));
        }
        this.running = true;
        syncAllScoreboards();
        startSyncTask();
    }

    @Override
    public void stop(String reason) {
        cancelSyncTask();
        clearAllScoreboards();
        this.greenPlayers.clear();
        this.allParticipants.clear();
        this.running = false;
        Bukkit.broadcastMessage(messages.format("events.stop", "&cEvent ended: {reason}", java.util.Map.of("reason", reason)));
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean handlesDeaths() {
        return true;
    }

    @Override
    public void handlePlayerDeath(Player victim, Player killer) {
        if (!running || killer == null) {
            return;
        }
        if (!greenPlayers.contains(victim.getUniqueId()) || !greenPlayers.contains(killer.getUniqueId())) {
            return;
        }
        greenPlayers.remove(victim.getUniqueId());
        syncAllScoreboards();
        victim.sendMessage(messages.prefixed("events.nametag.eliminated", "&cYou were turned red and eliminated."));
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(Component.text(
            victim.getName() + " was turned red. " + greenPlayers.size() + " green remaining.",
            NamedTextColor.RED
        )));

        if (greenPlayers.size() == 1) {
            UUID winnerId = greenPlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                winner.sendMessage(messages.prefixed("events.win", "&6You won the event."));
                rewardDispatcher.rewardWinner(winnerId, rewardVillager, type().key());
                stop("winner: " + winner.getName());
            } else {
                rewardDispatcher.rewardWinner(winnerId, rewardVillager, type().key());
                stop("winner: " + offlineName(winnerId));
            }
        } else if (greenPlayers.isEmpty()) {
            stop("no players left");
        }
    }

    @Override
    public void handleMobDeath(EntityDeathEvent event, Player killer) {}

    @Override
    public Set<UUID> participants() {
        return Set.copyOf(allParticipants);
    }

    @Override
    public void handlePlayerJoin(Player player) {
        if (!running) {
            return;
        }
        syncScoreboard(player.getScoreboard());
        if (allParticipants.contains(player.getUniqueId())) {
            player.sendMessage(messages.format(
                "events.status-participant",
                "&e{name}&7 is running. Green remaining: &a{remaining}&7/&a{participants}&7. Reward: &6{reward}",
                java.util.Map.of(
                    "name", type().displayName(),
                    "remaining", Integer.toString(greenPlayers.size()),
                    "participants", Integer.toString(allParticipants.size()),
                    "reward", rewardVillager == null ? "none" : rewardVillager.key()
                )
            ));
        }
    }

    @Override
    public ServerEventSnapshot snapshot() {
        return new ServerEventSnapshot(
            type().key(),
            type().displayName(),
            rewardVillager == null ? "none" : rewardVillager.key(),
            allParticipants.size(),
            greenPlayers.size(),
            greenPlayers.size() + " green remaining"
        );
    }

    private void startSyncTask() {
        cancelSyncTask();
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncAllScoreboards, 20L, 20L);
    }

    private void cancelSyncTask() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
    }

    private void syncAllScoreboards() {
        if (!running) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> syncScoreboard(player.getScoreboard()));
    }

    private void clearAllScoreboards() {
        Bukkit.getOnlinePlayers().forEach(player -> clearScoreboard(player.getScoreboard()));
    }

    private void syncScoreboard(Scoreboard scoreboard) {
        Team green = getOrCreate(scoreboard, GREEN_TEAM, NamedTextColor.GREEN);
        Team red = getOrCreate(scoreboard, RED_TEAM, NamedTextColor.RED);
        Set<String> greenEntries = new HashSet<>();
        Set<String> redEntries = new HashSet<>();
        allParticipants.forEach(participantId -> {
            String name = offlineName(participantId);
            if (name == null || name.isBlank()) {
                return;
            }
            if (greenPlayers.contains(participantId)) {
                greenEntries.add(name);
            } else {
                redEntries.add(name);
            }
        });

        reconcileEntries(green, greenEntries);
        reconcileEntries(red, redEntries);
    }

    private void clearScoreboard(Scoreboard scoreboard) {
        Team green = scoreboard.getTeam(GREEN_TEAM);
        Team red = scoreboard.getTeam(RED_TEAM);
        if (green != null) {
            green.unregister();
        }
        if (red != null) {
            red.unregister();
        }
    }

    private void reconcileEntries(Team team, Set<String> expectedEntries) {
        for (String entry : Set.copyOf(team.getEntries())) {
            if (!expectedEntries.contains(entry)) {
                team.removeEntry(entry);
            }
        }
        for (String entry : expectedEntries) {
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
        }
    }

    private Team getOrCreate(Scoreboard scoreboard, String name, NamedTextColor color) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.color(color);
        return team;
    }

    private String offlineName(UUID playerId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return offlinePlayer.getName() == null ? playerId.toString() : offlinePlayer.getName();
    }
}
