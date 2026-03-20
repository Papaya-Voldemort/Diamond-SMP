package io.github.diamondsmp.infra.config;

public interface ConfigRepository<T> {
    T current();
}

