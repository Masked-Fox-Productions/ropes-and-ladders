package com.ropes.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A single hanging run of rope: a coil at the top and the segments dropped
 * from it, plus however many segments are still spooled ("remaining").
 * Mutable, mirroring the plain drop objects on the Bedrock side.
 */
public class Drop {
    private RopePos coilPos;
    private int remaining;
    private final List<RopePos> segments = new ArrayList<>();

    public Drop(RopePos coilPos, int remaining) {
        this.coilPos = coilPos;
        this.remaining = remaining;
    }

    public RopePos getCoilPos() { return coilPos; }
    public void setCoilPos(RopePos coilPos) { this.coilPos = coilPos; }

    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }
    public void addRemaining(int delta) { this.remaining += delta; }

    public List<RopePos> getSegments() { return segments; }
}
