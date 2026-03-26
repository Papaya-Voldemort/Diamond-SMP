package io.github.diamondsmp.platform.paper.beacon;

import java.time.Instant;
import java.util.UUID;

public record GodBeaconRecord(
    BeaconPosition position,
    GodBeaconTier tier,
    UUID ownerId,
    Instant createdAt,
    GodBeaconEffect primaryEffect,
    GodBeaconEffect secondaryEffect
) {
    public GodBeaconRecord withPosition(BeaconPosition nextPosition) {
        return new GodBeaconRecord(nextPosition, tier, ownerId, createdAt, primaryEffect, secondaryEffect);
    }

    public GodBeaconRecord withTier(GodBeaconTier nextTier) {
        return new GodBeaconRecord(position, nextTier, ownerId, createdAt, primaryEffect, secondaryEffect);
    }

    public GodBeaconRecord withEffects(GodBeaconEffect primary, GodBeaconEffect secondary) {
        return new GodBeaconRecord(position, tier, ownerId, createdAt, primary, secondary);
    }
}
