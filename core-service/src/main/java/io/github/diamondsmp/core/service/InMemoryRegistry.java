package io.github.diamondsmp.core.service;

import io.github.diamondsmp.core.api.Registry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRegistry<K, V> implements Registry<K, V> {
    private final Map<K, V> values = new LinkedHashMap<>();

    @Override
    public void register(K key, V value) {
        values.put(key, value);
    }

    @Override
    public Optional<V> find(K key) {
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public Collection<V> values() {
        return values.values();
    }
}

