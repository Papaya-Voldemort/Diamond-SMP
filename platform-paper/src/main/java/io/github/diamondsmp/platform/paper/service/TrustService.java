package io.github.diamondsmp.platform.paper.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TrustService {
    private final Map<TrustPair, Instant> pendingRequests = new HashMap<>();
    private final Set<TrustPair> activeTrusts = new HashSet<>();

    public boolean isTrusted(UUID first, UUID second) {
        return activeTrusts.contains(TrustPair.of(first, second));
    }

    public boolean hasPendingRequest(UUID requester, UUID target) {
        return pendingRequests.containsKey(TrustPair.direct(requester, target));
    }

    public boolean hasIncomingRequest(UUID target, UUID requester) {
        return hasPendingRequest(requester, target);
    }

    public void request(UUID requester, UUID target) {
        pendingRequests.put(TrustPair.direct(requester, target), Instant.now());
    }

    public boolean accept(UUID accepter, UUID requester) {
        TrustPair pending = TrustPair.direct(requester, accepter);
        if (!pendingRequests.containsKey(pending)) {
            return false;
        }
        pendingRequests.remove(pending);
        activeTrusts.add(TrustPair.of(requester, accepter));
        return true;
    }

    public boolean remove(UUID first, UUID second) {
        boolean changed = activeTrusts.remove(TrustPair.of(first, second));
        changed |= pendingRequests.remove(TrustPair.direct(first, second)) != null;
        changed |= pendingRequests.remove(TrustPair.direct(second, first)) != null;
        return changed;
    }

    public Optional<UUID> pendingRequesterFor(UUID target) {
        return pendingRequests.keySet().stream()
            .filter(pair -> pair.targetId().equals(target))
            .map(TrustPair::requesterId)
            .findFirst();
    }

    private record TrustPair(UUID requesterId, UUID targetId) {
        private static TrustPair direct(UUID requesterId, UUID targetId) {
            return new TrustPair(requesterId, targetId);
        }

        private static TrustPair of(UUID first, UUID second) {
            return first.compareTo(second) <= 0
                ? new TrustPair(first, second)
                : new TrustPair(second, first);
        }
    }
}
