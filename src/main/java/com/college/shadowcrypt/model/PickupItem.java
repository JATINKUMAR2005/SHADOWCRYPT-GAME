package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Collectible dungeon item resting on the floor with gentle bobbing animation.
 */
public final class PickupItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ItemType type;
    private final double x;
    private final double y;
    private double bobPhase;
    private boolean collected;

    public PickupItem(ItemType type, double x, double y, double bobPhase) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.bobPhase = bobPhase;
    }

    public void update(double dt) {
        bobPhase += dt * 3.4;
    }

    public ItemType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getBobOffset() {
        return Math.sin(bobPhase) * 0.08;
    }

    public boolean isCollected() {
        return collected;
    }

    public void markCollected() {
        this.collected = true;
    }
}
