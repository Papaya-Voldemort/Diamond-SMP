package io.github.diamondsmp.core.api;

public record FeatureKey(String value) {
    public static FeatureKey of(String value) {
        return new FeatureKey(value);
    }
}

