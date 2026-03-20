package io.github.diamondsmp.core.service;

import io.github.diamondsmp.core.api.FeatureDescriptor;
import io.github.diamondsmp.core.api.FeatureKey;
import io.github.diamondsmp.core.api.LifecycleTracker;
import io.github.diamondsmp.core.api.PluginContext;
import io.github.diamondsmp.core.api.Registry;
import org.bukkit.plugin.java.JavaPlugin;

public final class DefaultPluginContext implements PluginContext {
    private final JavaPlugin plugin;
    private final Registry<FeatureKey, FeatureDescriptor> featureRegistry = new InMemoryRegistry<>();
    private final LifecycleTracker lifecycle = new InMemoryLifecycleTracker();

    public DefaultPluginContext(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public JavaPlugin plugin() {
        return plugin;
    }

    @Override
    public Registry<FeatureKey, FeatureDescriptor> featureRegistry() {
        return featureRegistry;
    }

    @Override
    public LifecycleTracker lifecycle() {
        return lifecycle;
    }
}

