package io.github.diamondsmp.bootstrap;

import io.github.diamondsmp.core.api.FeatureDescriptor;
import io.github.diamondsmp.core.api.LifecyclePhase;
import io.github.diamondsmp.core.api.ModuleInstaller;
import io.github.diamondsmp.core.api.PluginContext;
import io.github.diamondsmp.core.service.DefaultPluginContext;
import io.github.diamondsmp.platform.paper.PaperModuleInstaller;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Thin Paper entrypoint. Future gameplay systems should be installed through module installers rather
 * than added directly to this class.
 */
public final class DiamondSmpPlugin extends JavaPlugin {
    private PluginContext pluginContext;
    private List<ModuleInstaller> installers;

    @Override
    public void onEnable() {
        this.pluginContext = new DefaultPluginContext(this);
        this.installers = List.of(new PaperModuleInstaller());

        for (ModuleInstaller installer : installers) {
            installer.install(pluginContext);
        }

        getLogger().info("Diamond SMP enabled for api-version " + getPluginMeta().getAPIVersion());
        for (FeatureDescriptor feature : pluginContext.featureRegistry().values()) {
            getLogger().fine("Registered feature placeholder: " + feature.key().value());
        }
        pluginContext.lifecycle().mark(LifecyclePhase.POST_ENABLE);
    }

    @Override
    public void onDisable() {
        if (pluginContext != null) {
            pluginContext.lifecycle().mark(LifecyclePhase.SHUTDOWN);
        }
    }
}
