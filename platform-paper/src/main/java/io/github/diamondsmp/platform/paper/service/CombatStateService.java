package io.github.diamondsmp.platform.paper.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatStateService {
    private final Map<UUID, Instant> taggedPlayers = new HashMap<>();

    public void tag(UUID playerId, Duration duration) {
        taggedPlayers.put(playerId, Instant.now().plus(duration));
    }

    public void clear(UUID playerId) {
        taggedPlayers.remove(playerId);
    }

    public boolean isTagged(UUID playerId) {
        return remaining(playerId).compareTo(Duration.ZERO) > 0;
    }

    public Duration remaining(UUID playerId) {
        Instant expiresAt = taggedPlayers.get(playerId);
        if (expiresAt == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            taggedPlayers.remove(playerId);
            return Duration.ZERO;
        }
        return remaining;
    }
}
