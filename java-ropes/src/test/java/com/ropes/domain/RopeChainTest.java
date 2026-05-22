package com.ropes.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Port of tests/RopeChain.test.mjs — pure domain logic, no game server. */
class RopeChainTest {

    private static RopePos p(int x, int y, int z) {
        return new RopePos(x, y, z);
    }

    @Test
    void createsWithInitialStateOneDropAllInRemaining() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "north", 10);
        assertEquals("rope_1", chain.getId());
        assertEquals("rope", chain.getType());
        assertEquals("minecraft:overworld", chain.getDimensionId());
        assertEquals(p(0, 64, 0), chain.getAnchorPos());
        assertEquals("north", chain.getAnchorFace());
        assertEquals(10, chain.getTotalSegments());
        assertEquals(1, chain.getDrops().size());
        assertEquals(10, chain.getDrops().get(0).getRemaining());
        assertEquals(0, chain.getDrops().get(0).getSegments().size());
    }

    @Test
    void toMapRoundTripsThroughFromMap() {
        RopeChain chain = new RopeChain("rope_5", "rope_ladder", "minecraft:nether", p(10, 20, 30), "east", 8);
        chain.setWhipDeployed(true);
        chain.setDeployerName("TestPlayer");
        chain.extendDrop(0, List.of(p(10, 19, 30), p(10, 18, 30)));

        Map<String, Object> map = chain.toMap();
        RopeChain restored = RopeChain.fromMap(map);

        assertEquals("rope_5", restored.getId());
        assertEquals("rope_ladder", restored.getType());
        assertEquals("minecraft:nether", restored.getDimensionId());
        assertEquals(p(10, 20, 30), restored.getAnchorPos());
        assertEquals("east", restored.getAnchorFace());
        assertTrue(restored.isWhipDeployed());
        assertEquals("TestPlayer", restored.getDeployerName());
        assertEquals(8, restored.getTotalSegments());
        assertEquals(6, restored.getDrops().get(0).getRemaining());
        assertEquals(2, restored.getDrops().get(0).getSegments().size());
    }

    @Test
    void totalSegmentsSumsRemainingAndSegmentsAcrossDrops() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 20);
        chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0)));
        assertEquals(20, chain.getTotalSegments());
        assertEquals(18, chain.getDrops().get(0).getRemaining());
        assertEquals(2, chain.getDrops().get(0).getSegments().size());
    }

    @Test
    void handlesZeroSegments() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "north", 0);
        assertEquals(0, chain.getTotalSegments());
        assertEquals(0, chain.getDrops().get(0).getRemaining());
    }

    @Test
    void respectsMaxChainLengthWhenAddingSegments() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "north", 60);
        int added = chain.addSegments(10);
        assertEquals(4, added);
        assertEquals(64, chain.getTotalSegments());
    }

    @Test
    void extendDropPlacesSegmentsAndDecrementsRemaining() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 5);
        RopeChain.ExtendResult result = chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0), p(0, 61, 0)));
        assertEquals(3, result.extended());
        assertEquals(2, result.remaining());
        assertEquals(3, chain.getDrops().get(0).getSegments().size());
    }

    @Test
    void extendDropDoesNotExtendBeyondRemaining() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 2);
        RopeChain.ExtendResult result = chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0), p(0, 61, 0), p(0, 60, 0)));
        assertEquals(2, result.extended());
        assertEquals(2, chain.getDrops().get(0).getSegments().size());
    }

    @Test
    void createLedgeCoilTransfersRemainingFromParent() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 20);
        chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0)));
        Drop ledge = chain.createLedgeCoil(0, p(0, 61, 0));
        assertNotNull(ledge);
        assertEquals(18, ledge.getRemaining());
        assertEquals(0, chain.getDrops().get(0).getRemaining());
        assertEquals(2, chain.getDrops().size());
        assertEquals(20, chain.getTotalSegments());
    }

    @Test
    void retractOneFromBottomRemovesLastSegment() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 5);
        chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0)));
        RopeChain.RetractResult result = chain.retractOneFromBottom();
        assertEquals(p(0, 62, 0), result.removed());
        assertEquals(1, chain.getDrops().get(0).getSegments().size());
        assertEquals(4, chain.getDrops().get(0).getRemaining());
    }

    @Test
    void retractOneFromBottomMergesEmptyDropIntoParent() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 10);
        chain.extendDrop(0, List.of(p(0, 63, 0)));
        chain.createLedgeCoil(0, p(0, 62, 0));
        chain.extendDrop(1, List.of(p(0, 61, 0)));

        chain.retractOneFromBottom();
        assertEquals(1, chain.getDrops().size());
        assertEquals(9, chain.getDrops().get(0).getRemaining());
    }

    @Test
    void fullRecoilClearsAllDropsAndRestoresTotalToAnchor() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 20);
        chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0)));
        chain.createLedgeCoil(0, p(0, 61, 0));
        chain.extendDrop(1, List.of(p(0, 60, 0)));

        List<RopePos> removed = chain.fullRecoil();
        assertEquals(1, chain.getDrops().size());
        assertEquals(20, chain.getTotalSegments());
        assertEquals(20, chain.getDrops().get(0).getRemaining());
        assertFalse(removed.isEmpty());
    }

    @Test
    void breakAtSegmentRemovesSegmentAndDownstreamDrops() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 10);
        chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0), p(0, 61, 0)));
        List<RopePos> broken = chain.breakAtSegment(0, 1);
        assertEquals(2, broken.size());
        assertEquals(p(0, 62, 0), broken.get(0));
        assertEquals(p(0, 61, 0), broken.get(1));
        assertEquals(1, chain.getDrops().get(0).getSegments().size());
    }

    @Test
    void breakAllReturnsAllPositionsIncludingAnchor() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 3);
        chain.extendDrop(0, List.of(p(0, 63, 0)));
        List<RopePos> all = chain.breakAll();
        assertTrue(all.contains(p(0, 64, 0)));
        assertTrue(all.contains(p(0, 63, 0)));
    }

    @Test
    void getAllPositionsIncludesAnchorLedgeCoilsAndSegments() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 10);
        chain.extendDrop(0, List.of(p(0, 63, 0)));
        chain.createLedgeCoil(0, p(0, 62, 0));

        List<RopePos> positions = chain.getAllPositions();
        assertTrue(positions.contains(p(0, 64, 0)));
        assertTrue(positions.contains(p(0, 63, 0)));
        assertTrue(positions.contains(p(0, 62, 0)));
    }

    @Test
    void getDropForPositionFindsCoilAndSegmentPositions() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 5);
        chain.extendDrop(0, List.of(p(0, 63, 0)));

        RopeChain.DropLocation coilResult = chain.getDropForPosition(p(0, 64, 0));
        assertNotNull(coilResult);
        assertTrue(coilResult.isCoil());

        RopeChain.DropLocation segResult = chain.getDropForPosition(p(0, 63, 0));
        assertNotNull(segResult);
        assertFalse(segResult.isCoil());
        assertEquals(0, segResult.segmentIndex());

        assertNull(chain.getDropForPosition(p(99, 99, 99)));
    }

    @Test
    void isAnchorIdentifiesAnchorPosition() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(5, 10, 15), "north", 3);
        assertTrue(chain.isAnchor(p(5, 10, 15)));
        assertFalse(chain.isAnchor(p(5, 9, 15)));
    }

    @Test
    void multiCascadeThreeDropsWithCorrectRemaining() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 20);
        chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0)));
        chain.createLedgeCoil(0, p(0, 61, 0));
        chain.extendDrop(1, List.of(p(0, 60, 0), p(0, 59, 0)));
        chain.createLedgeCoil(1, p(0, 58, 0));

        assertEquals(3, chain.getDrops().size());
        assertEquals(0, chain.getDrops().get(0).getRemaining());
        assertEquals(2, chain.getDrops().get(0).getSegments().size());
        assertEquals(0, chain.getDrops().get(1).getRemaining());
        assertEquals(2, chain.getDrops().get(1).getSegments().size());
        assertEquals(16, chain.getDrops().get(2).getRemaining());
        assertEquals(20, chain.getTotalSegments());
    }

    @Test
    void retractOneFromBottomWithCascadeRemovesFromDeepestFirst() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 10);
        chain.extendDrop(0, List.of(p(0, 63, 0)));
        chain.createLedgeCoil(0, p(0, 62, 0));
        chain.extendDrop(1, List.of(p(0, 61, 0), p(0, 60, 0)));

        RopeChain.RetractResult result = chain.retractOneFromBottom();
        assertEquals(p(0, 60, 0), result.removed());
        assertEquals(1, chain.getDrops().get(1).getSegments().size());
        assertEquals(8, chain.getDrops().get(1).getRemaining());
        assertEquals(2, chain.getDrops().size());
    }

    @Test
    void retractLastSegmentInCascadeDropMergesToParent() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 10);
        chain.extendDrop(0, List.of(p(0, 63, 0)));
        chain.createLedgeCoil(0, p(0, 62, 0));
        chain.extendDrop(1, List.of(p(0, 61, 0)));

        RopeChain.RetractResult result = chain.retractOneFromBottom();
        assertEquals(p(0, 61, 0), result.removed());
        assertEquals(p(0, 62, 0), result.ledgeCoilRemoved());
        assertEquals(1, chain.getDrops().size());
        assertEquals(9, chain.getDrops().get(0).getRemaining());
    }

    @Test
    void fullRecoilWithThreeLedgeCoilsClearsAllAndRestoresTotal() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 20);
        chain.extendDrop(0, List.of(p(0, 63, 0)));
        chain.createLedgeCoil(0, p(0, 62, 0));
        chain.extendDrop(1, List.of(p(0, 61, 0)));
        chain.createLedgeCoil(1, p(0, 60, 0));
        chain.extendDrop(2, List.of(p(0, 59, 0)));
        chain.createLedgeCoil(2, p(0, 58, 0));

        List<RopePos> removed = chain.fullRecoil();
        assertEquals(1, chain.getDrops().size());
        assertEquals(20, chain.getTotalSegments());
        assertEquals(20, chain.getDrops().get(0).getRemaining());
        assertTrue(removed.stream().anyMatch(pos -> pos.y() == 62));
        assertTrue(removed.stream().anyMatch(pos -> pos.y() == 60));
        assertTrue(removed.stream().anyMatch(pos -> pos.y() == 58));
    }

    @Test
    void breakAtSegmentAboveLedgeCoilRemovesAllDownstream() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 15);
        chain.extendDrop(0, List.of(p(0, 63, 0), p(0, 62, 0)));
        chain.createLedgeCoil(0, p(0, 61, 0));
        chain.extendDrop(1, List.of(p(0, 60, 0), p(0, 59, 0)));

        List<RopePos> broken = chain.breakAtSegment(0, 1);

        assertEquals(4, broken.size());
        assertTrue(broken.stream().anyMatch(pos -> pos.y() == 62));
        assertTrue(broken.stream().anyMatch(pos -> pos.y() == 61));
        assertTrue(broken.stream().anyMatch(pos -> pos.y() == 60));
        assertTrue(broken.stream().anyMatch(pos -> pos.y() == 59));
        assertEquals(1, chain.getDrops().size());
        assertEquals(1, chain.getDrops().get(0).getSegments().size());
    }

    @Test
    void partialCascadeFullRecoilClearsEverything() {
        RopeChain chain = new RopeChain("rope_1", "rope", "minecraft:overworld", p(0, 64, 0), "up", 20);
        chain.extendDrop(0, List.of(p(0, 63, 0)));
        chain.createLedgeCoil(0, p(0, 62, 0));

        assertEquals(19, chain.getDrops().get(1).getRemaining());
        assertEquals(0, chain.getDrops().get(1).getSegments().size());

        List<RopePos> removed = chain.fullRecoil();
        assertEquals(1, chain.getDrops().size());
        assertEquals(20, chain.getTotalSegments());
        assertTrue(removed.stream().anyMatch(pos -> pos.y() == 62));
    }
}
