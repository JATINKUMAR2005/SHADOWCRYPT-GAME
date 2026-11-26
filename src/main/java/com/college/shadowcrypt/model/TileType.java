package com.college.shadowcrypt.model;

/**
 * Represents each cell type in a procedurally generated dungeon floor.
 */
public enum TileType {
    VOID(false, false),
    WALL(false, false),
    FLOOR(true, true),
    CORRIDOR(true, true),
    TORCH_FLOOR(true, true),
    SPIKE_TRAP(true, true),
    CHEST_PEDESTAL(true, true),
    STAIRS_DOWN(true, true);

    private final boolean walkable;
    private final boolean transparent;

    TileType(boolean walkable, boolean transparent) {
        this.walkable = walkable;
        this.transparent = transparent;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public boolean isTransparent() {
        return transparent;
    }
}
