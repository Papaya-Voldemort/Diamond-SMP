package io.github.diamondsmp.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.diamondsmp.core.api.LifecyclePhase;
import io.github.diamondsmp.core.service.InMemoryLifecycleTracker;
import org.junit.jupiter.api.Test;

class ScaffoldMetadataTest {
    @Test
    void lifecycleStartsInBootstrapPhase() {
        InMemoryLifecycleTracker tracker = new InMemoryLifecycleTracker();

        assertEquals(LifecyclePhase.BOOTSTRAP, tracker.phases().getFirst());
    }
}
