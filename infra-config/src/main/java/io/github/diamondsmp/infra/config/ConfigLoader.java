package io.github.diamondsmp.infra.config;

public interface ConfigLoader<T> {
    String configName();

    T load();
}

