package io.github.diamondsmp.core.api;

public interface ModuleInstaller {
    String moduleId();

    void install(PluginContext context);
}

