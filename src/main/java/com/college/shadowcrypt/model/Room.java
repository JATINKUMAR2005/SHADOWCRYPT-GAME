package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Immutable rectangular room carved into a dungeon floor.
 */
public record Room(
        int x,
        int y,
        int width,
        int height,
        boolean startRoom,
        boolean bossRoom) implements Serializable {

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public boolean intersects(Room other, int padding) {
        return x - padding < other.x + other.width
                && x + width + padding > other.x
                && y - padding < other.y + other.height
                && y + height + padding > other.y;
    }

    public boolean contains(int tx, int ty) {
        return tx >= x && tx < x + width && ty >= y && ty < y + height;
    }
}
