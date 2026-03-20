package io.github.diamondsmp.core.service;

import io.github.diamondsmp.core.api.LifecyclePhase;
import io.github.diamondsmp.core.api.LifecycleTracker;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryLifecycleTracker implements LifecycleTracker {
    private final List<LifecyclePhase> phases = new ArrayList<>();
    private final List<String> installedModules = new ArrayList<>();

    public InMemoryLifecycleTracker() {
        phases.add(LifecyclePhase.BOOTSTRAP);
    }

    @Override
    public void mark(LifecyclePhase phase) {
        phases.add(phase);
    }

    @Override
    public void markInstalled(String moduleId) {
        installedModules.add(moduleId);
    }

    @Override
    public List<LifecyclePhase> phases() {
        return List.copyOf(phases);
    }

    @Override
    public List<String> installedModules() {
        return List.copyOf(installedModules);
    }
}

