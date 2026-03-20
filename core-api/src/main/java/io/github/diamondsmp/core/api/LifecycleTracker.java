package io.github.diamondsmp.core.api;

import java.util.List;

public interface LifecycleTracker {
    void mark(LifecyclePhase phase);

    void markInstalled(String moduleId);

    List<LifecyclePhase> phases();

    List<String> installedModules();
}

