package io.github.diamondsmp.core.api;

import org.bukkit.plugin.java.JavaPlugin;

public interface PluginContext {
    JavaPlugin plugin();

    Registry<FeatureKey, FeatureDescriptor> featureRegistry();

    LifecycleTracker lifecycle();
}

