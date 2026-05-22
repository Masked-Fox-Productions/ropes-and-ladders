package com.ropes.util;

/**
 * Builds the dimension-prefixed position key used to index rope blocks,
 * mirroring the Bedrock scripts/util/ropePosKey.js. Kept dependency-free
 * (plain ints) so the util layer never reaches into the domain layer.
 */
public final class RopePosKey {
    private RopePosKey() {}

    public static String of(String dimensionId, int x, int y, int z) {
        return dimensionId + ":" + x + "," + y + "," + z;
    }
}
