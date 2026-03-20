package io.github.diamondsmp.core.domain;

import io.github.diamondsmp.core.api.FeatureDescriptor;
import io.github.diamondsmp.core.api.FeatureKey;
import java.util.List;

public final class PlannedFeatures {
    private PlannedFeatures() {}

    public static List<FeatureDescriptor> all() {
        return List.of(
            feature("items", "Custom items, god sets, and item systems"),
            feature("villagers", "Custom villager behavior, trades, and progression"),
            feature("world-rules", "World restrictions including netherite and progression rules"),
            feature("events", "Custom internal and gameplay event surfaces"),
            feature("commands", "Player, admin, and debugging command surfaces"),
            feature("permissions", "Permission nodes and access-control surfaces"),
            feature("chat-tags", "Chat formatting and name tag systems"),
            feature("crafting-loot", "Custom recipes, loot, and drop rules"),
            feature("progression", "Unlocks, milestones, and gating systems"),
            feature("kits", "Starter kits, reward kits, and delivery flows"),
            feature("economy", "Currency and trading-related extension points"),
            feature("combat", "Combat modifiers, effects, and rules"),
            feature("protection", "Claiming, region, and anti-grief surfaces"),
            feature("admin", "Admin tools, moderation, and diagnostics"),
            feature("integrations", "Third-party plugin integration seams"),
            feature("seasonal", "Time-limited and seasonal event systems")
        );
    }

    private static FeatureDescriptor feature(String key, String description) {
        return new FeatureDescriptor(FeatureKey.of(key), description, false);
    }
}

