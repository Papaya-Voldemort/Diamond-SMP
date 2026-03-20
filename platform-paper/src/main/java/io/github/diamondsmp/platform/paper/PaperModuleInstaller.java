package io.github.diamondsmp.platform.paper;

import io.github.diamondsmp.core.api.FeatureDescriptor;
import io.github.diamondsmp.core.api.FeatureKey;
import io.github.diamondsmp.core.api.ModuleInstaller;
import io.github.diamondsmp.core.api.PluginContext;
import io.github.diamondsmp.core.domain.PlannedFeatures;

public final class PaperModuleInstaller implements ModuleInstaller {
    @Override
    public String moduleId() {
        return "platform-paper";
    }

    @Override
    public void install(PluginContext context) {
        context.lifecycle().markInstalled(moduleId());
        context.featureRegistry().register(
            FeatureKey.of("platform.paper"),
            new FeatureDescriptor(FeatureKey.of("platform.paper"), "Paper integration seams", false)
        );
        for (FeatureDescriptor descriptor : PlannedFeatures.all()) {
            context.featureRegistry().register(descriptor.key(), descriptor);
        }
        new DiamondPaperModule(context).install();
    }
}
