package io.github.diamondsmp.platform.paper.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PvpTestPlayerService {
    private static final int MAX_TEST_PLAYERS_PER_ADMIN = 24;
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 16;

    private final Map<UUID, LinkedHashMap<String, UUID>> testIdsByOwnerAndName = new LinkedHashMap<>();
    private final Map<UUID, TestPlayer> testPlayerById = new LinkedHashMap<>();

    public CreateResult create(UUID ownerId, String requestedName) {
        String normalized = normalizeName(requestedName);
        if (normalized == null) {
            return new CreateResult(CreateStatus.INVALID_NAME, null);
        }
        LinkedHashMap<String, UUID> namesById = testIdsByOwnerAndName.computeIfAbsent(ownerId, ignored -> new LinkedHashMap<>());
        String key = normalized.toLowerCase(java.util.Locale.ROOT);
        UUID existing = namesById.get(key);
        if (existing != null) {
            return new CreateResult(CreateStatus.ALREADY_EXISTS, testPlayerById.get(existing));
        }
        if (namesById.size() >= MAX_TEST_PLAYERS_PER_ADMIN) {
            return new CreateResult(CreateStatus.LIMIT_REACHED, null);
        }
        UUID id = UUID.nameUUIDFromBytes(("diamondsmp:test:" + ownerId + ":" + key).getBytes(StandardCharsets.UTF_8));
        TestPlayer testPlayer = new TestPlayer(id, ownerId, normalized);
        namesById.put(key, id);
        testPlayerById.put(id, testPlayer);
        return new CreateResult(CreateStatus.CREATED, testPlayer);
    }

    public Optional<TestPlayer> resolveOwned(UUID ownerId, String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return Optional.empty();
        }
        Map<String, UUID> namesById = testIdsByOwnerAndName.get(ownerId);
        if (namesById == null) {
            return Optional.empty();
        }
        UUID id = namesById.get(normalized.toLowerCase(java.util.Locale.ROOT));
        return id == null ? Optional.empty() : Optional.ofNullable(testPlayerById.get(id));
    }

    public Optional<TestPlayer> byId(UUID playerId) {
        return Optional.ofNullable(testPlayerById.get(playerId));
    }

    public List<TestPlayer> listOwned(UUID ownerId) {
        Map<String, UUID> namesById = testIdsByOwnerAndName.get(ownerId);
        if (namesById == null || namesById.isEmpty()) {
            return List.of();
        }
        return namesById.values().stream()
            .map(testPlayerById::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    public boolean removeOwned(UUID ownerId, String name) {
        Optional<TestPlayer> testPlayer = resolveOwned(ownerId, name);
        if (testPlayer.isEmpty()) {
            return false;
        }
        removeById(testPlayer.get().id());
        return true;
    }

    public int clearOwned(UUID ownerId) {
        List<TestPlayer> owned = listOwned(ownerId);
        owned.forEach(testPlayer -> removeById(testPlayer.id()));
        return owned.size();
    }

    public boolean isTestPlayer(UUID playerId) {
        return testPlayerById.containsKey(playerId);
    }

    public Optional<String> displayName(UUID playerId) {
        return Optional.ofNullable(testPlayerById.get(playerId)).map(TestPlayer::name);
    }

    public Collection<TestPlayer> all() {
        return new ArrayList<>(testPlayerById.values());
    }

    public String nextAutoName(UUID ownerId) {
        Map<String, UUID> namesById = testIdsByOwnerAndName.computeIfAbsent(ownerId, ignored -> new LinkedHashMap<>());
        for (int index = 1; index <= MAX_TEST_PLAYERS_PER_ADMIN; index++) {
            String candidate = "test" + index;
            if (!namesById.containsKey(candidate)) {
                return candidate;
            }
        }
        return "test" + (MAX_TEST_PLAYERS_PER_ADMIN + 1);
    }

    private void removeById(UUID id) {
        TestPlayer testPlayer = testPlayerById.remove(id);
        if (testPlayer == null) {
            return;
        }
        LinkedHashMap<String, UUID> namesById = testIdsByOwnerAndName.get(testPlayer.ownerId());
        if (namesById != null) {
            namesById.remove(testPlayer.name().toLowerCase(java.util.Locale.ROOT));
            if (namesById.isEmpty()) {
                testIdsByOwnerAndName.remove(testPlayer.ownerId());
            }
        }
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            return null;
        }
        for (int index = 0; index < trimmed.length(); index++) {
            char c = trimmed.charAt(index);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                continue;
            }
            return null;
        }
        return trimmed;
    }

    public enum CreateStatus {
        CREATED,
        ALREADY_EXISTS,
        INVALID_NAME,
        LIMIT_REACHED
    }

    public record CreateResult(CreateStatus status, TestPlayer player) {}

    public record TestPlayer(UUID id, UUID ownerId, String name) {}
}
