package io.github.diamondsmp.core.api;

import java.util.Collection;
import java.util.Optional;

public interface Registry<K, V> {
    void register(K key, V value);

    Optional<V> find(K key);

    Collection<V> values();
}

