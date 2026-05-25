package com.ropes.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable block position. The Bedrock side uses plain {x, y, z} objects;
 * the record gives us value equality for free, which the chain logic relies
 * on when matching coil/segment positions.
 */
public record RopePos(int x, int y, int z) {

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        return m;
    }

    public static RopePos fromMap(Map<String, Object> map) {
        return new RopePos(
            ((Number) map.get("x")).intValue(),
            ((Number) map.get("y")).intValue(),
            ((Number) map.get("z")).intValue()
        );
    }
}
