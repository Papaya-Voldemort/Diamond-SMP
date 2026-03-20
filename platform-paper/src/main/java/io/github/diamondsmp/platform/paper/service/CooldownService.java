package io.github.diamondsmp.platform.paper.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {
    private final Map<String, Instant> cooldowns = new HashMap<>();

    public boolean isReady(UUID playerId, String key) {
        Instant expiry = cooldowns.get(compose(playerId, key));
        return expiry == null || Instant.now().isAfter(expiry);
    }

    public Duration remaining(UUID playerId, String key) {
        Instant expiry = cooldowns.get(compose(playerId, key));
        if (expiry == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(Instant.now(), expiry);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public void apply(UUID playerId, String key, Duration duration) {
        cooldowns.put(compose(playerId, key), Instant.now().plus(duration));
    }

    private String compose(UUID playerId, String key) {
        return playerId + ":" + key;
    }
}
