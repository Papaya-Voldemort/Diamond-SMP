package io.github.diamondsmp.core.api;

public enum LifecyclePhase {
    BOOTSTRAP,
    LOAD_CONFIG,
    REGISTER_SERVICES,
    REGISTER_LISTENERS,
    REGISTER_COMMANDS,
    POST_ENABLE,
    SHUTDOWN
}

