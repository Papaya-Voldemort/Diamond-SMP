package io.github.diamondsmp.platform.paper.event;

import io.github.diamondsmp.platform.paper.villager.VillagerType;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public interface ServerEvent {
    ServerEventType type();

    void start(Set<UUID> participants, VillagerType rewardVillager);

    void stop(String reason);

    boolean isRunning();

    boolean handlesDeaths();

    default void handlePlayerDeath(Player victim, Player killer) {}

    default void handleMobDeath(EntityDeathEvent event, Player killer) {}

    default void handlePlayerJoin(Player player) {}

    Set<UUID> participants();

    ServerEventSnapshot snapshot();
}
