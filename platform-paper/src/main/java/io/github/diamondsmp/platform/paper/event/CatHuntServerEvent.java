package io.github.diamondsmp.platform.paper.event;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.villager.VillagerType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public final class CatHuntServerEvent implements ServerEvent {
    private final MessageBundle messages;
    private final EventRewardDispatcher rewardDispatcher;
    private final Set<UUID> participants = new HashSet<>();
    private VillagerType rewardVillager;
    private boolean running;

    public CatHuntServerEvent(MessageBundle messages, EventRewardDispatcher rewardDispatcher) {
        this.messages = messages;
        this.rewardDispatcher = rewardDispatcher;
    }

    @Override
    public ServerEventType type() {
        return ServerEventType.CAT_HUNT;
    }

    @Override
    public void start(Set<UUID> participants, VillagerType rewardVillager) {
        this.participants.clear();
        this.participants.addAll(participants);
        this.rewardVillager = rewardVillager;
        this.running = true;
        org.bukkit.Bukkit.getOnlinePlayers().stream()
            .filter(player -> this.participants.contains(player.getUniqueId()))
            .forEach(player -> player.sendMessage(messages.prefixed("events.cat.start", "&eCat Hunt started. First qualifying cat kill wins.")));
    }

    @Override
    public void stop(String reason) {
        this.participants.clear();
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean handlesDeaths() {
        return false;
    }

    @Override
    public void handleMobDeath(EntityDeathEvent event, Player killer) {
        if (!running || killer == null || event.getEntityType() != EntityType.CAT) {
            return;
        }
        if (!participants.contains(killer.getUniqueId())) {
            return;
        }
        killer.sendMessage(messages.prefixed("events.win", "&6You won the event."));
        rewardDispatcher.rewardWinner(killer.getUniqueId(), rewardVillager, type().key());
        stop("winner: " + killer.getName());
    }

    @Override
    public Set<UUID> participants() {
        return Set.copyOf(participants);
    }

    @Override
    public ServerEventSnapshot snapshot() {
        return new ServerEventSnapshot(
            type().key(),
            type().displayName(),
            rewardVillager == null ? "none" : rewardVillager.key(),
            participants.size(),
            participants.size(),
            "First qualifying cat kill wins"
        );
    }
}
