package com.ropes.util;

/**
 * Central home for gameplay constants, mirroring the Bedrock
 * scripts/util/RopeConstants.js. No magic numbers belong in the domain
 * or (eventually) the Fabric runtime layer.
 */
public final class RopeConstants {
    private RopeConstants() {}

    public static final String ROPE_BLOCK_ID = "ropes:rope";
    public static final String ROPE_LADDER_BLOCK_ID = "ropes:rope_ladder";
    public static final String WHIP_ITEM_ID = "ropes:whip";
    public static final String ROPES_PERSISTENCE_KEY = "ropes:state";

    public static final int MAX_CHAIN_LENGTH = 64;
    public static final int CLIMB_INTERVAL_TICKS = 2;
    public static final double ROPE_CLIMB_SPEED = 0.15;
    public static final double LADDER_CLIMB_SPEED = 0.22;
    public static final double WHIP_CLIMB_SPEED = 0.35;
    public static final boolean ALLOW_ADD_SEGMENTS_BY_CLICKING_COIL = false;
    public static final int WHIP_RANGE = 8;
    public static final int WHIP_DEPLOY_SEGMENTS = 4;
    public static final int WHIP_DAMAGE = 3;
    public static final int BREAK_BATCH_THRESHOLD = 16;
}
