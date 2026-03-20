package io.github.diamondsmp.platform.paper.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class TeleportRequestService {
    private final Map<UUID, TeleportRequest> requestsByTarget = new HashMap<>();

    public void create(Player requester, Player target, Duration timeout) {
        requestsByTarget.put(target.getUniqueId(), new TeleportRequest(requester.getUniqueId(), target.getUniqueId(), Instant.now().plus(timeout)));
    }

    public Optional<TeleportRequest> find(Player target) {
        TeleportRequest request = requestsByTarget.get(target.getUniqueId());
        if (request == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(request.expiresAt())) {
            requestsByTarget.remove(target.getUniqueId());
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public Optional<TeleportRequest> remove(Player target) {
        return Optional.ofNullable(requestsByTarget.remove(target.getUniqueId()));
    }

    public Optional<TeleportRequest> expire(UUID targetId, UUID requesterId) {
        TeleportRequest request = requestsByTarget.get(targetId);
        if (request == null || !request.requesterId().equals(requesterId)) {
            return Optional.empty();
        }
        requestsByTarget.remove(targetId);
        return Optional.of(request);
    }

    public record TeleportRequest(UUID requesterId, UUID targetId, Instant expiresAt) {}
}
