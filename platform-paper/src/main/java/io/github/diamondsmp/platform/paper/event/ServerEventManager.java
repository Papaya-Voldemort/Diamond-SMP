package io.github.diamondsmp.platform.paper.event;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.villager.GodVillagerService;
import io.github.diamondsmp.platform.paper.villager.VillagerType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerEventManager {
    private final MessageBundle messages;
    private final GodVillagerService villagerService;
    private final Predicate<UUID> excludedParticipant;
    private final Map<ServerEventType, ServerEvent> events = new EnumMap<>(ServerEventType.class);
    private final Map<UUID, PendingReward> pendingRewards = new HashMap<>();
    private ServerEvent activeEvent;

    public ServerEventManager(JavaPlugin plugin, MessageBundle messages, GodVillagerService villagerService, Predicate<UUID> excludedParticipant) {
        this.messages = messages;
        this.villagerService = villagerService;
        this.excludedParticipant = excludedParticipant;
        events.put(ServerEventType.NAME_TAG, new NameTagServerEvent(plugin, messages, this::rewardWinner));
        events.put(ServerEventType.CAT_HUNT, new CatHuntServerEvent(messages, this::rewardWinner));
    }

    public boolean start(ServerEventType type, VillagerType rewardVillager) {
        if (activeEvent != null && activeEvent.isRunning()) {
            return false;
        }
        ServerEvent event = events.get(type);
        if (event == null) {
            return false;
        }
        Set<UUID> participants = Bukkit.getOnlinePlayers().stream()
            .map(Player::getUniqueId)
            .filter(playerId -> !excludedParticipant.test(playerId))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (participants.isEmpty()) {
            return false;
        }
        activeEvent = event;
        event.start(participants, rewardVillager);
        Bukkit.broadcastMessage(messages.format(
            "events.started-broadcast",
            "&6{name}&e started with &6{participants}&e participants. Reward villager: &6{reward}",
            Map.of(
                "name", type.displayName(),
                "participants", Integer.toString(participants.size()),
                "reward", rewardVillager.key()
            )
        ));
        return true;
    }

    public void stop(String reason) {
        if (activeEvent != null) {
            activeEvent.stop(reason);
            activeEvent = null;
        }
    }

    public Optional<ServerEvent> activeEvent() {
        return Optional.ofNullable(activeEvent).filter(ServerEvent::isRunning);
    }

    public Optional<ServerEventSnapshot> activeSnapshot() {
        return activeEvent().map(ServerEvent::snapshot);
    }

    public void handlePlayerDeath(Player victim, Player killer) {
        activeEvent().filter(ServerEvent::handlesDeaths).ifPresent(event -> event.handlePlayerDeath(victim, killer));
    }

    public void handleMobDeath(EntityDeathEvent event, Player killer) {
        activeEvent().ifPresent(active -> active.handleMobDeath(event, killer));
    }

    public void handleJoin(Player player) {
        PendingReward reward = pendingRewards.remove(player.getUniqueId());
        if (reward != null) {
            villagerService.spawnRewardVillager(player, reward.rewardVillager(), reward.eventKey());
            player.sendMessage(messages.format(
                "events.reward-ready",
                "&aYour &e{event}&a reward villager has been spawned now that you are online.",
                Map.of("event", reward.eventKey())
            ));
        }
        activeEvent().ifPresent(active -> active.handlePlayerJoin(player));
        activeSnapshot().ifPresent(snapshot -> player.sendMessage(messages.format(
            "events.status",
            "&eCurrent event: &6{name}&e | Remaining: &6{remaining}&e/&6{participants}&e | Reward: &6{reward}",
            Map.of(
                "name", snapshot.displayName(),
                "remaining", Integer.toString(snapshot.remaining()),
                "participants", Integer.toString(snapshot.participants()),
                "reward", snapshot.rewardVillager()
            )
        )));
    }

    public String describeActiveEvent() {
        return activeSnapshot().map(ServerEventSnapshot::displayName).orElse("none");
    }

    public String currentEventKey() {
        return activeSnapshot().map(ServerEventSnapshot::key).orElse("none");
    }

    public String currentEventStatus() {
        return activeSnapshot().map(ServerEventSnapshot::summary).orElse("No active event");
    }

    public String currentEventReward() {
        return activeSnapshot().map(ServerEventSnapshot::rewardVillager).orElse("none");
    }

    public int currentEventParticipants() {
        return activeSnapshot().map(ServerEventSnapshot::participants).orElse(0);
    }

    public int currentEventRemaining() {
        return activeSnapshot().map(ServerEventSnapshot::remaining).orElse(0);
    }

    public MessageBundle messages() {
        return messages;
    }

    private void rewardWinner(UUID winnerId, VillagerType rewardVillager, String eventKey) {
        Player winner = Bukkit.getPlayer(winnerId);
        if (winner != null) {
            villagerService.spawnRewardVillager(winner, rewardVillager, eventKey);
            return;
        }
        pendingRewards.put(winnerId, new PendingReward(rewardVillager, eventKey));
    }

    private record PendingReward(VillagerType rewardVillager, String eventKey) {}
}
