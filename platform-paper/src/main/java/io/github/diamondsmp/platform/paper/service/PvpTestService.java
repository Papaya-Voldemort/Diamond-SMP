package io.github.diamondsmp.platform.paper.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PvpTestService {
    private final Map<UUID, TestPlayerProfile> profilesById = new HashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> profileIdsByOwner = new HashMap<>();

    public List<TestPlayerProfile> create(UUID ownerId, int count) {
        int safeCount = Math.max(1, Math.min(8, count));
        List<TestPlayerProfile> created = new ArrayList<>(safeCount);
        for (int index = 0; index < safeCount; index++) {
            TestPlayerProfile profile = new TestPlayerProfile(UUID.randomUUID(), ownerId, nextName(ownerId));
            profilesById.put(profile.id(), profile);
            profileIdsByOwner.computeIfAbsent(ownerId, ignored -> new LinkedHashSet<>()).add(profile.id());
            created.add(profile);
        }
        return List.copyOf(created);
    }

    public List<TestPlayerProfile> profilesOwnedBy(UUID ownerId) {
        Set<UUID> ids = profileIdsByOwner.get(ownerId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
            .map(profilesById::get)
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(TestPlayerProfile::name))
            .toList();
    }

    public List<UUID> clearOwnedBy(UUID ownerId) {
        Set<UUID> ids = profileIdsByOwner.remove(ownerId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        ids.forEach(profilesById::remove);
        return List.copyOf(ids);
    }

    public boolean isTestPlayer(UUID playerId) {
        return profilesById.containsKey(playerId);
    }

    public Optional<TestPlayerProfile> find(UUID playerId) {
        return Optional.ofNullable(profilesById.get(playerId));
    }

    public Optional<String> displayName(UUID playerId) {
        return find(playerId).map(TestPlayerProfile::name);
    }

    private String nextName(UUID ownerId) {
        int next = profilesOwnedBy(ownerId).size() + 1;
        return "Test-" + next;
    }

    public record TestPlayerProfile(UUID id, UUID ownerId, String name) {}
}
