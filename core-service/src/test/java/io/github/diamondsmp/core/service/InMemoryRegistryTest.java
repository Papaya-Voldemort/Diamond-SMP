package io.github.diamondsmp.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.diamondsmp.core.api.FeatureDescriptor;
import io.github.diamondsmp.core.api.FeatureKey;
import org.junit.jupiter.api.Test;

class InMemoryRegistryTest {
    @Test
    void storesRegisteredValues() {
        InMemoryRegistry<FeatureKey, FeatureDescriptor> registry = new InMemoryRegistry<>();
        FeatureDescriptor descriptor = new FeatureDescriptor(FeatureKey.of("items"), "Items placeholder", false);

        registry.register(descriptor.key(), descriptor);

        assertTrue(registry.find(descriptor.key()).isPresent());
        assertEquals(1, registry.values().size());
    }
}

