package io.github.diamondsmp.testfixtures;

import io.github.diamondsmp.core.api.FeatureDescriptor;
import io.github.diamondsmp.core.api.FeatureKey;

public final class SampleFeatureData {
    private SampleFeatureData() {}

    public static FeatureDescriptor itemsFeature() {
        return new FeatureDescriptor(FeatureKey.of("items"), "Placeholder custom items feature", false);
    }
}
