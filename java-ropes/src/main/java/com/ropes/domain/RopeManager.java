package com.ropes.domain;

import com.ropes.util.RopePosKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for all rope chains: owns the chain registry plus
 * the position and support-block indices. A faithful port of
 * scripts/RopeManager.js, minus the Minecraft persistence I/O — the Bedrock
 * version reads/writes a world dynamic property, whereas here {@link #load}
 * and {@link #exportState} exchange plain maps so the Fabric persistence
 * layer (a SavedData wrapper) owns the actual serialization. That keeps this
 * class free of Minecraft imports and unit-testable.
 */
public class RopeManager {
    private final Map<String, RopeChain> chains = new LinkedHashMap<>();
    private final Map<String, String> positionIndex = new HashMap<>();
    private final Map<String, Set<String>> supportIndex = new HashMap<>();
    private boolean loaded = false;
    private int nextId = 1;

    private static final Map<String, RopePos> FACE_OFFSETS = new HashMap<>();
    static {
        FACE_OFFSETS.put("up", new RopePos(0, 1, 0));
        FACE_OFFSETS.put("down", new RopePos(0, -1, 0));
        FACE_OFFSETS.put("north", new RopePos(0, 0, -1));
        FACE_OFFSETS.put("south", new RopePos(0, 0, 1));
        FACE_OFFSETS.put("east", new RopePos(1, 0, 0));
        FACE_OFFSETS.put("west", new RopePos(-1, 0, 0));
    }

    private static String key(String dimensionId, RopePos pos) {
        return RopePosKey.of(dimensionId, pos.x(), pos.y(), pos.z());
    }

    /** Hydrate from persisted chain maps. Idempotent: a second call is a no-op. */
    public void load(List<Map<String, Object>> data) {
        if (loaded) return;
        if (data != null) {
            for (Map<String, Object> entry : data) {
                RopeChain chain = RopeChain.fromMap(entry);
                chains.put(chain.getId(), chain);
                for (RopePos pos : chain.getAllPositions()) {
                    positionIndex.put(key(chain.getDimensionId(), pos), chain.getId());
                }
                indexSupport(chain);

                String numPart = chain.getId().replace("rope_", "");
                try {
                    int n = Integer.parseInt(numPart);
                    if (n >= nextId) nextId = n + 1;
                } catch (NumberFormatException ignored) {
                    // non-numeric id; leave nextId untouched
                }
            }
        }
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Snapshot every chain as a plain map for the persistence layer to serialize. */
    public List<Map<String, Object>> exportState() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (RopeChain chain : chains.values()) {
            data.add(chain.toMap());
        }
        return data;
    }

    public RopeChain createChain(String type, String dimensionId, RopePos anchorPos,
                                 String anchorFace, int initialSegments) {
        String id = "rope_" + (nextId++);
        RopeChain chain = new RopeChain(id, type, dimensionId, anchorPos, anchorFace, initialSegments);
        chains.put(id, chain);
        positionIndex.put(key(dimensionId, anchorPos), id);
        indexSupport(chain);
        return chain;
    }

    public void removeChain(String chainId) {
        RopeChain chain = chains.get(chainId);
        if (chain == null) return;
        for (RopePos pos : chain.getAllPositions()) {
            positionIndex.remove(key(chain.getDimensionId(), pos));
        }
        removeSupport(chain);
        chains.remove(chainId);
    }

    public RopeChain getChainAtPosition(String dimensionId, RopePos pos) {
        String id = positionIndex.get(key(dimensionId, pos));
        if (id == null) return null;
        return chains.get(id);
    }

    public RopeChain getChain(String chainId) {
        return chains.get(chainId);
    }

    public Collection<RopeChain> getAllChains() {
        return chains.values();
    }

    public void addSegmentPosition(String chainId, String dimensionId, RopePos pos) {
        positionIndex.put(key(dimensionId, pos), chainId);
    }

    public void removeSegmentPosition(String dimensionId, RopePos pos) {
        positionIndex.remove(key(dimensionId, pos));
    }

    public void addSupportBlock(String dimensionId, RopePos supportPos, String chainId) {
        supportIndex.computeIfAbsent(key(dimensionId, supportPos), k -> new HashSet<>()).add(chainId);
    }

    public void removeSupportBlock(String dimensionId, RopePos supportPos, String chainId) {
        String supportKey = key(dimensionId, supportPos);
        Set<String> set = supportIndex.get(supportKey);
        if (set == null) return;
        set.remove(chainId);
        if (set.isEmpty()) {
            supportIndex.remove(supportKey);
        }
    }

    public Set<String> getChainsForSupportBlock(String dimensionId, RopePos pos) {
        return supportIndex.get(key(dimensionId, pos));
    }

    private void indexSupport(RopeChain chain) {
        RopePos off = FACE_OFFSETS.get(chain.getAnchorFace());
        if (off == null) return;
        RopePos anchor = chain.getAnchorPos();
        RopePos supportPos = new RopePos(anchor.x() + off.x(), anchor.y() + off.y(), anchor.z() + off.z());
        addSupportBlock(chain.getDimensionId(), supportPos, chain.getId());
    }

    private void removeSupport(RopeChain chain) {
        RopePos off = FACE_OFFSETS.get(chain.getAnchorFace());
        if (off == null) return;
        RopePos anchor = chain.getAnchorPos();
        RopePos supportPos = new RopePos(anchor.x() + off.x(), anchor.y() + off.y(), anchor.z() + off.z());
        removeSupportBlock(chain.getDimensionId(), supportPos, chain.getId());
    }
}
