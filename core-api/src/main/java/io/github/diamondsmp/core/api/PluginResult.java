package io.github.diamondsmp.core.api;

public record PluginResult<T>(T value, DomainError error) {
    public static <T> PluginResult<T> success(T value) {
        return new PluginResult<>(value, null);
    }

    public static <T> PluginResult<T> failure(DomainError error) {
        return new PluginResult<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }
}

