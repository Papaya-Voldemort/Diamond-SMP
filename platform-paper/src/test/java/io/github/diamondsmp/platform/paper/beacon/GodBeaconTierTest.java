package io.github.diamondsmp.platform.paper.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GodBeaconTierTest {
    @Test
    void nextTierFollowsLockedProgressionPath() {
        assertEquals(GodBeaconTier.INFUSED, GodBeaconTier.VANILLA.next());
        assertEquals(GodBeaconTier.RESONANT, GodBeaconTier.INFUSED.next());
        assertEquals(GodBeaconTier.ASCENDED, GodBeaconTier.RESONANT.next());
        assertEquals(GodBeaconTier.GOD, GodBeaconTier.ASCENDED.next());
        assertNull(GodBeaconTier.GOD.next());
    }

    @Test
    void godTierRequiresFullPyramidButLowerTiersDoNot() {
        assertTrue(!GodBeaconTier.INFUSED.requiresFullPyramid());
        assertTrue(!GodBeaconTier.RESONANT.requiresFullPyramid());
        assertTrue(!GodBeaconTier.ASCENDED.requiresFullPyramid());
        assertTrue(GodBeaconTier.GOD.requiresFullPyramid());
    }
}
