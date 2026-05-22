package com.ropes.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Port of tests/RopeManager.test.mjs. Persistence is exercised via the
 *  exportState/load map exchange that stands in for the world property I/O. */
class RopeManagerTest {

    private RopeManager mgr;

    private static RopePos p(int x, int y, int z) {
        return new RopePos(x, y, z);
    }

    @BeforeEach
    void setUp() {
        mgr = new RopeManager();
    }

    @Test
    void createChainStoresChainRetrievableByPosition() {
        RopeChain chain = mgr.createChain("rope", "minecraft:overworld", p(0, 64, 0), "north", 10);
        assertEquals("rope_1", chain.getId());
        assertEquals("rope", chain.getType());
        assertEquals(10, chain.getTotalSegments());

        RopeChain found = mgr.getChainAtPosition("minecraft:overworld", p(0, 64, 0));
        assertSame(chain, found);
    }

    @Test
    void saveLoadRoundTrips() {
        mgr.createChain("rope", "minecraft:overworld", p(0, 64, 0), "north", 10);
        mgr.createChain("rope_ladder", "minecraft:overworld", p(5, 64, 5), "east", 5);
        List<Map<String, Object>> exported = mgr.exportState();

        RopeManager mgr2 = new RopeManager();
        mgr2.load(exported);

        RopeChain chain1 = mgr2.getChain("rope_1");
        assertNotNull(chain1);
        assertEquals("rope", chain1.getType());
        assertEquals(10, chain1.getTotalSegments());

        RopeChain chain2 = mgr2.getChain("rope_2");
        assertNotNull(chain2);
        assertEquals("rope_ladder", chain2.getType());
    }

    @Test
    void loadIsIdempotent() {
        mgr.createChain("rope", "minecraft:overworld", p(0, 64, 0), "north", 5);
        List<Map<String, Object>> exported = mgr.exportState();

        RopeManager mgr2 = new RopeManager();
        mgr2.load(exported);
        mgr2.load(exported);

        assertEquals(1, mgr2.getAllChains().size());
    }

    @Test
    void indexesSupportBlockPosition() {
        mgr.createChain("rope", "minecraft:overworld", p(5, 64, 5), "north", 5);
        Set<String> supportChains = mgr.getChainsForSupportBlock("minecraft:overworld", p(5, 64, 4));
        assertNotNull(supportChains);
        assertTrue(supportChains.contains("rope_1"));
    }

    @Test
    void twoChainsOnSameSupportBlockDifferentFaces() {
        mgr.createChain("rope", "minecraft:overworld", p(5, 64, 4), "south", 5); // support z+1 = (5,64,5)
        mgr.createChain("rope", "minecraft:overworld", p(4, 64, 5), "east", 5);  // support x+1 = (5,64,5)
        Set<String> supportChains = mgr.getChainsForSupportBlock("minecraft:overworld", p(5, 64, 5));
        assertNotNull(supportChains);
        assertTrue(supportChains.contains("rope_1"));
        assertTrue(supportChains.contains("rope_2"));
    }

    @Test
    void removeChainCleansUpIndices() {
        RopeChain chain = mgr.createChain("rope", "minecraft:overworld", p(0, 64, 0), "north", 5);
        mgr.removeChain(chain.getId());

        assertNull(mgr.getChainAtPosition("minecraft:overworld", p(0, 64, 0)));
        assertNull(mgr.getChainsForSupportBlock("minecraft:overworld", p(0, 64, -1)));
    }

    @Test
    void positionIndexUsesDimensionPrefixedKeys() {
        mgr.createChain("rope", "minecraft:overworld", p(0, 64, 0), "up", 5);
        mgr.createChain("rope", "minecraft:nether", p(0, 64, 0), "up", 3);

        RopeChain ow = mgr.getChainAtPosition("minecraft:overworld", p(0, 64, 0));
        RopeChain nether = mgr.getChainAtPosition("minecraft:nether", p(0, 64, 0));

        assertEquals("rope_1", ow.getId());
        assertEquals("rope_2", nether.getId());
    }

    @Test
    void addAndRemoveSegmentPositionUpdatesIndex() {
        RopeChain chain = mgr.createChain("rope", "minecraft:overworld", p(0, 64, 0), "up", 5);
        mgr.addSegmentPosition(chain.getId(), "minecraft:overworld", p(0, 63, 0));
        assertSame(chain, mgr.getChainAtPosition("minecraft:overworld", p(0, 63, 0)));

        mgr.removeSegmentPosition("minecraft:overworld", p(0, 63, 0));
        assertNull(mgr.getChainAtPosition("minecraft:overworld", p(0, 63, 0)));
    }

    @Test
    void getChainReturnsNullForUnknownId() {
        assertNull(mgr.getChain("nonexistent"));
    }

    @Test
    void getChainAtPositionReturnsNullForUnindexedPosition() {
        assertNull(mgr.getChainAtPosition("minecraft:overworld", p(99, 99, 99)));
    }
}
