package com.college.shadowcrypt.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the tile grid, rooms, visibility state, and transition markers for one dungeon floor.
 */
public final class DungeonLevel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int width;
    private final int height;
    private final int floorNumber;
    private final long seed;
    private final boolean bossFloor;
    private final TileType[][] tiles;
    private final boolean[][] explored;
    private final boolean[][] visible;
    private final ArrayList<Room> rooms;

    private int spawnTileX;
    private int spawnTileY;
    private int stairsTileX;
    private int stairsTileY;
    private boolean stairsUnlocked;

    public DungeonLevel(int width, int height, int floorNumber, long seed, boolean bossFloor) {
        this.width = width;
        this.height = height;
        this.floorNumber = floorNumber;
        this.seed = seed;
        this.bossFloor = bossFloor;
        this.stairsUnlocked = !bossFloor;
        this.tiles = new TileType[height][width];
        this.explored = new boolean[height][width];
        this.visible = new boolean[height][width];
        this.rooms = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = TileType.VOID;
            }
        }
    }

    public boolean inBounds(int tx, int ty) {
        return tx >= 0 && tx < width && ty >= 0 && ty < height;
    }

    public TileType getTile(int tx, int ty) {
        if (!inBounds(tx, ty)) {
            return TileType.VOID;
        }
        return tiles[ty][tx];
    }

    public void setTile(int tx, int ty, TileType type) {
        if (inBounds(tx, ty)) {
            tiles[ty][tx] = type;
        }
    }

    public boolean isWalkable(int tx, int ty) {
        return getTile(tx, ty).isWalkable();
    }

    public boolean isTransparent(int tx, int ty) {
        return getTile(tx, ty).isTransparent();
    }

    public boolean isExplored(int tx, int ty) {
        return inBounds(tx, ty) && explored[ty][tx];
    }

    public boolean isVisible(int tx, int ty) {
        return inBounds(tx, ty) && visible[ty][tx];
    }

    public void setVisible(int tx, int ty, boolean state) {
        if (inBounds(tx, ty)) {
            visible[ty][tx] = state;
            if (state) {
                explored[ty][tx] = true;
            }
        }
    }

    public void clearVisible() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                visible[y][x] = false;
            }
        }
    }

    public void addRoom(Room room) {
        rooms.add(room);
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isBossFloor() {
        return bossFloor;
    }

    public int getSpawnTileX() {
        return spawnTileX;
    }

    public void setSpawnTileX(int spawnTileX) {
        this.spawnTileX = spawnTileX;
    }

    public int getSpawnTileY() {
        return spawnTileY;
    }

    public void setSpawnTileY(int spawnTileY) {
        this.spawnTileY = spawnTileY;
    }

    public int getStairsTileX() {
        return stairsTileX;
    }

    public void setStairsTileX(int stairsTileX) {
        this.stairsTileX = stairsTileX;
    }

    public int getStairsTileY() {
        return stairsTileY;
    }

    public void setStairsTileY(int stairsTileY) {
        this.stairsTileY = stairsTileY;
    }

    public boolean isStairsUnlocked() {
        return stairsUnlocked;
    }

    public void setStairsUnlocked(boolean stairsUnlocked) {
        this.stairsUnlocked = stairsUnlocked;
    }
}
