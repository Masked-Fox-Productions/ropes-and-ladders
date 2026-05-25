package com.ropes.domain;

import com.ropes.util.RopeConstants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure game logic for a single rope/rope-ladder/whip chain. No Minecraft
 * imports — this is a faithful port of scripts/domain/RopeChain.js and is
 * exercised by RopeChainTest with plain JUnit.
 */
public class RopeChain {
    private final String id;
    private final String type;
    private final String dimensionId;
    private final RopePos anchorPos;
    private final String anchorFace;
    private boolean whipDeployed = false;
    private String deployerName = null;
    private List<Drop> drops = new ArrayList<>();

    public RopeChain(String id, String type, String dimensionId, RopePos anchorPos,
                     String anchorFace, int initialSegments) {
        this.id = id;
        this.type = type;
        this.dimensionId = dimensionId;
        this.anchorPos = anchorPos;
        this.anchorFace = anchorFace;
        this.drops.add(new Drop(anchorPos, initialSegments));
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getDimensionId() { return dimensionId; }
    public RopePos getAnchorPos() { return anchorPos; }
    public String getAnchorFace() { return anchorFace; }
    public boolean isWhipDeployed() { return whipDeployed; }
    public void setWhipDeployed(boolean whipDeployed) { this.whipDeployed = whipDeployed; }
    public String getDeployerName() { return deployerName; }
    public void setDeployerName(String deployerName) { this.deployerName = deployerName; }
    public List<Drop> getDrops() { return drops; }

    public int getTotalSegments() {
        int total = 0;
        for (Drop drop : drops) {
            total += drop.getRemaining() + drop.getSegments().size();
        }
        return total;
    }

    public Drop getDrop(int index) {
        if (index < 0 || index >= drops.size()) return null;
        return drops.get(index);
    }

    public DropLocation getDropForPosition(RopePos pos) {
        for (int i = 0; i < drops.size(); i++) {
            Drop drop = drops.get(i);
            if (drop.getCoilPos().equals(pos)) {
                return new DropLocation(drop, i, true, -1);
            }
            List<RopePos> segments = drop.getSegments();
            for (int s = 0; s < segments.size(); s++) {
                if (segments.get(s).equals(pos)) {
                    return new DropLocation(drop, i, false, s);
                }
            }
        }
        return null;
    }

    public boolean isAnchor(RopePos pos) {
        return anchorPos.equals(pos);
    }

    public boolean isCoilPosition(RopePos pos) {
        for (Drop drop : drops) {
            if (drop.getCoilPos().equals(pos)) return true;
        }
        return false;
    }

    public int addSegments(int count) {
        return addSegments(count, 0);
    }

    public int addSegments(int count, int dropIndex) {
        Drop drop = getDrop(dropIndex);
        if (drop == null) return 0;
        int space = RopeConstants.MAX_CHAIN_LENGTH - getTotalSegments();
        int added = Math.min(count, space);
        drop.addRemaining(added);
        return added;
    }

    public ExtendResult extendDrop(int dropIndex, List<RopePos> positions) {
        Drop drop = getDrop(dropIndex);
        if (drop == null) return new ExtendResult(0, 0);
        int toExtend = Math.min(positions.size(), drop.getRemaining());
        for (int i = 0; i < toExtend; i++) {
            drop.getSegments().add(positions.get(i));
            drop.setRemaining(drop.getRemaining() - 1);
        }
        return new ExtendResult(toExtend, drop.getRemaining());
    }

    public Drop createLedgeCoil(int dropIndex, RopePos coilPos) {
        Drop parentDrop = getDrop(dropIndex);
        if (parentDrop == null || parentDrop.getRemaining() <= 0) return null;
        Drop newDrop = new Drop(coilPos, parentDrop.getRemaining());
        parentDrop.setRemaining(0);
        drops.add(dropIndex + 1, newDrop);
        return newDrop;
    }

    public RetractResult retractOneFromBottom() {
        for (int i = drops.size() - 1; i >= 0; i--) {
            Drop drop = drops.get(i);
            List<RopePos> segments = drop.getSegments();
            if (!segments.isEmpty()) {
                RopePos removed = segments.remove(segments.size() - 1);
                drop.addRemaining(1);
                if (segments.isEmpty() && i > 0) {
                    Drop parent = drops.get(i - 1);
                    parent.addRemaining(drop.getRemaining());
                    RopePos ledgeCoil = drop.getCoilPos();
                    drops.remove(i);
                    return new RetractResult(removed, ledgeCoil);
                }
                return new RetractResult(removed, null);
            }
        }
        return null;
    }

    public List<RopePos> fullRecoil() {
        List<RopePos> removedPositions = new ArrayList<>();
        for (int i = drops.size() - 1; i >= 0; i--) {
            Drop drop = drops.get(i);
            removedPositions.addAll(drop.getSegments());
            if (i > 0) {
                removedPositions.add(drop.getCoilPos());
            }
        }
        int total = getTotalSegments();
        drops = new ArrayList<>();
        drops.add(new Drop(anchorPos, total));
        return removedPositions;
    }

    public List<RopePos> breakAtSegment(int dropIndex, int segmentIndex) {
        List<RopePos> brokenPositions = new ArrayList<>();
        Drop drop = drops.get(dropIndex);
        List<RopePos> segments = drop.getSegments();

        for (int s = segmentIndex; s < segments.size(); s++) {
            brokenPositions.add(segments.get(s));
        }
        while (segments.size() > segmentIndex) {
            segments.remove(segments.size() - 1);
        }

        for (int i = drops.size() - 1; i > dropIndex; i--) {
            Drop d = drops.get(i);
            brokenPositions.addAll(d.getSegments());
            brokenPositions.add(d.getCoilPos());
        }
        while (drops.size() > dropIndex + 1) {
            drops.remove(drops.size() - 1);
        }

        return brokenPositions;
    }

    public List<RopePos> breakAll() {
        List<RopePos> positions = new ArrayList<>();
        for (Drop drop : drops) {
            positions.addAll(drop.getSegments());
            if (!drop.getCoilPos().equals(anchorPos)) {
                positions.add(drop.getCoilPos());
            }
        }
        positions.add(anchorPos);
        return positions;
    }

    public List<RopePos> getAllPositions() {
        List<RopePos> positions = new ArrayList<>();
        positions.add(anchorPos);
        for (Drop drop : drops) {
            if (!drop.getCoilPos().equals(anchorPos)) {
                positions.add(drop.getCoilPos());
            }
            positions.addAll(drop.getSegments());
        }
        return positions;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("type", type);
        map.put("dimensionId", dimensionId);
        map.put("anchorPos", anchorPos.toMap());
        map.put("anchorFace", anchorFace);
        map.put("isWhipDeployed", whipDeployed);
        map.put("deployerName", deployerName);
        List<Object> dropList = new ArrayList<>();
        for (Drop drop : drops) {
            Map<String, Object> dropMap = new LinkedHashMap<>();
            dropMap.put("coilPos", drop.getCoilPos().toMap());
            dropMap.put("remaining", drop.getRemaining());
            List<Object> segs = new ArrayList<>();
            for (RopePos seg : drop.getSegments()) {
                segs.add(seg.toMap());
            }
            dropMap.put("segments", segs);
            dropList.add(dropMap);
        }
        map.put("drops", dropList);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static RopeChain fromMap(Map<String, Object> json) {
        RopeChain chain = new RopeChain(
            (String) json.get("id"),
            (String) json.get("type"),
            (String) json.get("dimensionId"),
            RopePos.fromMap((Map<String, Object>) json.get("anchorPos")),
            (String) json.get("anchorFace"),
            0
        );
        Object whip = json.get("isWhipDeployed");
        chain.whipDeployed = whip instanceof Boolean && (Boolean) whip;
        chain.deployerName = (String) json.get("deployerName");

        List<Drop> newDrops = new ArrayList<>();
        List<Map<String, Object>> dropMaps = (List<Map<String, Object>>) json.get("drops");
        for (Map<String, Object> dropMap : dropMaps) {
            RopePos coil = RopePos.fromMap((Map<String, Object>) dropMap.get("coilPos"));
            int remaining = ((Number) dropMap.get("remaining")).intValue();
            Drop drop = new Drop(coil, remaining);
            List<Map<String, Object>> segMaps = (List<Map<String, Object>>) dropMap.get("segments");
            for (Map<String, Object> segMap : segMaps) {
                drop.getSegments().add(RopePos.fromMap(segMap));
            }
            newDrops.add(drop);
        }
        chain.drops = newDrops;
        return chain;
    }

    /** Result of {@link #getDropForPosition}; segmentIndex is -1 for a coil hit. */
    public record DropLocation(Drop drop, int dropIndex, boolean isCoil, int segmentIndex) {}

    /** Result of {@link #extendDrop}. */
    public record ExtendResult(int extended, int remaining) {}

    /** Result of {@link #retractOneFromBottom}; ledgeCoilRemoved is null when no drop merged. */
    public record RetractResult(RopePos removed, RopePos ledgeCoilRemoved) {}
}
