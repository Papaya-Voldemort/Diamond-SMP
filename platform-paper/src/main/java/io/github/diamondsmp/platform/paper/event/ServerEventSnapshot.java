package io.github.diamondsmp.platform.paper.event;

public record ServerEventSnapshot(
    String key,
    String displayName,
    String rewardVillager,
    int participants,
    int remaining,
    String summary
) {}
