package io.github.diamondsmp.platform.paper.event;

import io.github.diamondsmp.platform.paper.villager.VillagerType;
import java.util.UUID;

@FunctionalInterface
public interface EventRewardDispatcher {
    void rewardWinner(UUID winnerId, VillagerType rewardVillager, String eventKey);
}
