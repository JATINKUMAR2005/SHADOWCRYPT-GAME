package com.college.shadowcrypt.engine;

import com.college.shadowcrypt.model.CryptBossEnemy;
import com.college.shadowcrypt.model.Difficulty;
import com.college.shadowcrypt.model.DungeonLevel;
import com.college.shadowcrypt.model.Enemy;
import com.college.shadowcrypt.model.ItemType;
import com.college.shadowcrypt.model.PickupItem;
import com.college.shadowcrypt.model.Room;
import com.college.shadowcrypt.model.ShadowStalkerEnemy;
import com.college.shadowcrypt.model.SkeletonEnemy;
import com.college.shadowcrypt.model.SlimeEnemy;
import com.college.shadowcrypt.model.TileType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * Procedural room-and-corridor dungeon generator with BFS connectivity checks,
 * boss arenas every 3rd floor, traps, torches, and scaled enemy/loot placement.
 */
public final class DungeonGenerator {

    public static final int DEFAULT_WIDTH = 46;
    public static final int DEFAULT_HEIGHT = 34;

    public record GeneratedFloor(DungeonLevel level, List<Enemy> enemies, List<PickupItem> items) {}

    private DungeonGenerator() {}

    public static GeneratedFloor generate(int floorNumber, Difficulty difficulty, long seed) {
        Random rng = new Random(seed);
        boolean bossFloor = floorNumber % 3 == 0;
        DungeonLevel level = new DungeonLevel(DEFAULT_WIDTH, DEFAULT_HEIGHT, floorNumber, seed, bossFloor);

        List<Room> rooms = placeRooms(DEFAULT_WIDTH, DEFAULT_HEIGHT, floorNumber, bossFloor, rng);
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            carveRoom(level, r);
            level.addRoom(r);
            if (i > 0) {
                Room prev = rooms.get(i - 1);
                carveCorridor(level, prev.centerX(), prev.centerY(), r.centerX(), r.centerY(), rng);
            }
        }

        surroundWithWalls(level);

        Room startRoom = rooms.get(0);
        Room stairsRoom = pickFarthestRoom(rooms, startRoom);
        if (bossFloor) {
            for (Room r : rooms) {
                if (r.bossRoom()) {
                    stairsRoom = r;
                    break;
                }
            }
        }

        level.setSpawnTileX(startRoom.centerX());
        level.setSpawnTileY(startRoom.centerY());
        level.setStairsTileX(stairsRoom.centerX());
        level.setStairsTileY(stairsRoom.centerY());
        level.setTile(stairsRoom.centerX(), stairsRoom.centerY(), TileType.STAIRS_DOWN);

        decorateTorchesAndTraps(level, rooms, startRoom, stairsRoom, floorNumber, rng);

        List<Enemy> enemies = populateEnemies(level, rooms, startRoom, stairsRoom, floorNumber, difficulty, rng);
        List<PickupItem> items = populateItems(level, rooms, startRoom, stairsRoom, floorNumber, bossFloor, rng);

        return new GeneratedFloor(level, enemies, items);
    }

    private static List<Room> placeRooms(
            int mapW,
            int mapH,
            int floorNumber,
            boolean bossFloor,
            Random rng) {
        List<Room> rooms = new ArrayList<>();
        int targetRooms = Math.min(11, 7 + floorNumber / 2);

        // Start room
        int sw = 7;
        int sh = 6;
        rooms.add(new Room(3, mapH / 2 - sh / 2, sw, sh, true, false));

        // On boss floors, reserve a spacious arena on the eastern side
        if (bossFloor) {
            int bw = 11;
            int bh = 9;
            int bx = mapW - bw - 3;
            int by = mapH / 2 - bh / 2 + rng.nextInt(5) - 2;
            by = Math.max(3, Math.min(mapH - bh - 3, by));
            rooms.add(new Room(bx, by, bw, bh, false, true));
        }

        for (int attempt = 0; attempt < 110 && rooms.size() < targetRooms; attempt++) {
            int rw = 5 + rng.nextInt(5);
            int rh = 5 + rng.nextInt(4);
            int rx = 2 + rng.nextInt(mapW - rw - 4);
            int ry = 2 + rng.nextInt(mapH - rh - 4);
            Room candidate = new Room(rx, ry, rw, rh, false, false);

            boolean overlaps = false;
            for (Room existing : rooms) {
                if (candidate.intersects(existing, 2)) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                rooms.add(candidate);
            }
        }

        if (rooms.size() < 2) {
            rooms.add(new Room(mapW - 11, mapH / 2 - 3, 7, 6, false, bossFloor));
        }

        // Order rooms left-to-right (keeping startRoom first, bossRoom last if boss floor)
        Room start = rooms.get(0);
        Room boss = null;
        List<Room> middle = new ArrayList<>();
        for (int i = 1; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            if (r.bossRoom()) {
                boss = r;
            } else {
                middle.add(r);
            }
        }
        middle.sort(Comparator.comparingInt(Room::centerX).thenComparingInt(Room::centerY));

        List<Room> ordered = new ArrayList<>();
        ordered.add(start);
        ordered.addAll(middle);
        if (boss != null) {
            ordered.add(boss);
        }
        return ordered;
    }

    private static void carveRoom(DungeonLevel level, Room room) {
        for (int y = room.y(); y < room.y() + room.height(); y++) {
            for (int x = room.x(); x < room.x() + room.width(); x++) {
                level.setTile(x, y, TileType.FLOOR);
            }
        }
    }

    private static void carveCorridor(
            DungeonLevel level,
            int x1,
            int y1,
            int x2,
            int y2,
            Random rng) {
        boolean horizontalFirst = rng.nextBoolean();
        if (horizontalFirst) {
            carveHorizontalSegment(level, x1, x2, y1);
            carveVerticalSegment(level, y1, y2, x2);
        } else {
            carveVerticalSegment(level, y1, y2, x1);
            carveHorizontalSegment(level, x1, x2, y2);
        }
    }

    private static void carveHorizontalSegment(DungeonLevel level, int x1, int x2, int y) {
        int start = Math.min(x1, x2);
        int end = Math.max(x1, x2);
        for (int x = start; x <= end; x++) {
            for (int dy = 0; dy <= 1; dy++) {
                if (level.getTile(x, y + dy) == TileType.VOID) {
                    level.setTile(x, y + dy, TileType.CORRIDOR);
                }
            }
        }
    }

    private static void carveVerticalSegment(DungeonLevel level, int y1, int y2, int x) {
        int start = Math.min(y1, y2);
        int end = Math.max(y1, y2);
        for (int y = start; y <= end; y++) {
            for (int dx = 0; dx <= 1; dx++) {
                if (level.getTile(x + dx, y) == TileType.VOID) {
                    level.setTile(x + dx, y, TileType.CORRIDOR);
                }
            }
        }
    }

    private static void surroundWithWalls(DungeonLevel level) {
        int w = level.getWidth();
        int h = level.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (level.getTile(x, y) != TileType.VOID) {
                    continue;
                }
                if (hasWalkableNeighbor(level, x, y)) {
                    level.setTile(x, y, TileType.WALL);
                }
            }
        }
    }

    private static boolean hasWalkableNeighbor(DungeonLevel level, int cx, int cy) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (level.isWalkable(cx + dx, cy + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Room pickFarthestRoom(List<Room> rooms, Room startRoom) {
        Room best = rooms.get(rooms.size() - 1);
        double bestDist = -1;
        for (Room room : rooms) {
            if (room == startRoom) continue;
            double dist = Math.hypot(room.centerX() - startRoom.centerX(), room.centerY() - startRoom.centerY());
            if (dist > bestDist) {
                bestDist = dist;
                best = room;
            }
        }
        return best;
    }

    private static void decorateTorchesAndTraps(
            DungeonLevel level,
            List<Room> rooms,
            Room startRoom,
            Room stairsRoom,
            int floorNumber,
            Random rng) {
        for (Room room : rooms) {
            // Place warm torch tiles near corners of each room
            if (room.width() >= 5 && room.height() >= 5) {
                int tx = room.x() + 1;
                int ty = room.y() + 1;
                if (level.getTile(tx, ty) == TileType.FLOOR) {
                    level.setTile(tx, ty, TileType.TORCH_FLOOR);
                }
                int tx2 = room.x() + room.width() - 2;
                int ty2 = room.y() + room.height() - 2;
                if (level.getTile(tx2, ty2) == TileType.FLOOR) {
                    level.setTile(tx2, ty2, TileType.TORCH_FLOOR);
                }
            }

            if (room == startRoom || room.bossRoom()) {
                continue;
            }

            int trapCount = Math.min(3, 1 + floorNumber / 2);
            for (int i = 0; i < trapCount; i++) {
                if (rng.nextDouble() > 0.65) {
                    continue;
                }
                int tx = room.x() + 1 + rng.nextInt(Math.max(1, room.width() - 2));
                int ty = room.y() + 1 + rng.nextInt(Math.max(1, room.height() - 2));
                if (tx == stairsRoom.centerX() && ty == stairsRoom.centerY()) {
                    continue;
                }
                if (level.getTile(tx, ty) == TileType.FLOOR) {
                    level.setTile(tx, ty, TileType.SPIKE_TRAP);
                }
            }
        }
    }

    private static List<Enemy> populateEnemies(
            DungeonLevel level,
            List<Room> rooms,
            Room startRoom,
            Room stairsRoom,
            int floorNumber,
            Difficulty difficulty,
            Random rng) {
        List<Enemy> enemies = new ArrayList<>();
        double floorScale = 1.0 + (floorNumber - 1) * 0.16;

        for (Room room : rooms) {
            if (room == startRoom) {
                continue;
            }
            if (room.bossRoom()) {
                double bx = room.centerX() + 0.5;
                double by = room.centerY() + 0.5;
                if (room == stairsRoom) {
                    bx = room.x() + room.width() * 0.68;
                }
                int bossHp = scaleStat(280, floorScale * difficulty.getEnemyHpMultiplier());
                int bossDmg = scaleStat(24, (1.0 + (floorNumber - 1) * 0.12) * difficulty.getEnemyDamageMultiplier());
                enemies.add(new CryptBossEnemy(bx, by, bossHp, bossDmg));
                continue;
            }

            int count = 2 + rng.nextInt(Math.min(4, 2 + floorNumber / 2));
            for (int i = 0; i < count; i++) {
                double ex = room.x() + 1.2 + rng.nextDouble() * Math.max(1.0, room.width() - 2.4);
                double ey = room.y() + 1.2 + rng.nextDouble() * Math.max(1.0, room.height() - 2.4);
                if (!level.isWalkable((int) ex, (int) ey)) {
                    continue;
                }
                enemies.add(createScaledEnemy(floorNumber, floorScale, difficulty, ex, ey, rng));
            }
        }
        return enemies;
    }

    private static Enemy createScaledEnemy(
            int floorNumber,
            double floorScale,
            Difficulty difficulty,
            double x,
            double y,
            Random rng) {
        double roll = rng.nextDouble();
        double hpMult = floorScale * difficulty.getEnemyHpMultiplier();
        double dmgMult = (1.0 + (floorNumber - 1) * 0.12) * difficulty.getEnemyDamageMultiplier();

        if (floorNumber >= 2 && roll > 0.72) {
            return new ShadowStalkerEnemy(x, y, scaleStat(72, hpMult), scaleStat(18, dmgMult));
        }
        if (roll > 0.40) {
            return new SkeletonEnemy(x, y, scaleStat(56, hpMult), scaleStat(14, dmgMult));
        }
        return new SlimeEnemy(x, y, scaleStat(48, hpMult), scaleStat(12, dmgMult));
    }

    private static List<PickupItem> populateItems(
            DungeonLevel level,
            List<Room> rooms,
            Room startRoom,
            Room stairsRoom,
            int floorNumber,
            boolean bossFloor,
            Random rng) {
        List<PickupItem> items = new ArrayList<>();
        boolean placedChest = false;

        for (Room room : rooms) {
            if (room == startRoom) {
                // Always provide a coin or torch-lit starter pickup on floor 1
                if (floorNumber == 1) {
                    items.add(new PickupItem(ItemType.GOLD_COIN, room.centerX() + 1.5, room.centerY() + 0.5, 0.4));
                }
                continue;
            }
            if (room.bossRoom()) {
                // Place a pre-boss health potion near entrance of boss room
                items.add(new PickupItem(
                        ItemType.HEALTH_POTION,
                        room.x() + 1.5,
                        room.centerY() + 0.5,
                        rng.nextDouble() * Math.PI));
                continue;
            }

            if (!placedChest && (bossFloor || rng.nextDouble() < 0.45)) {
                int cx = room.centerX();
                int cy = room.centerY();
                if (cx != stairsRoom.centerX() || cy != stairsRoom.centerY()) {
                    level.setTile(cx, cy, TileType.CHEST_PEDESTAL);
                    items.add(new PickupItem(ItemType.WEAPON_CHEST, cx + 0.5, cy + 0.5, 0.0));
                    placedChest = true;
                    continue;
                }
            }

            double roll = rng.nextDouble();
            double ix = room.x() + 1.5 + rng.nextDouble() * Math.max(0.5, room.width() - 3.0);
            double iy = room.y() + 1.5 + rng.nextDouble() * Math.max(0.5, room.height() - 3.0);
            if (!level.isWalkable((int) ix, (int) iy)) {
                continue;
            }

            ItemType type;
            if (roll < 0.34) {
                type = ItemType.GOLD_COIN;
            } else if (roll < 0.58) {
                type = ItemType.HEALTH_POTION;
            } else if (roll < 0.72) {
                type = ItemType.POWER_ORB;
            } else if (roll < 0.85) {
                type = ItemType.SPEED_BOOTS;
            } else {
                type = ItemType.SHIELD_CHARM;
            }
            items.add(new PickupItem(type, ix, iy, rng.nextDouble() * Math.PI * 2));
        }

        if (!placedChest && rooms.size() >= 2) {
            Room fallback = rooms.get(1);
            items.add(new PickupItem(ItemType.WEAPON_CHEST, fallback.centerX() + 0.5, fallback.centerY() + 0.5, 0.0));
        }

        return items;
    }

    private static int scaleStat(int base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    /**
     * Verifies via Breadth-First Search that the floor stairs are reachable from spawn.
     */
    public static boolean isLevelConnected(DungeonLevel level) {
        int sx = level.getSpawnTileX();
        int sy = level.getSpawnTileY();
        int tx = level.getStairsTileX();
        int ty = level.getStairsTileY();
        if (!level.isWalkable(sx, sy) || !level.isWalkable(tx, ty)) {
            return false;
        }

        boolean[][] visited = new boolean[level.getHeight()][level.getWidth()];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sy});
        visited[sy][sx] = true;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == tx && cur[1] == ty) {
                return true;
            }
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                if (level.inBounds(nx, ny) && !visited[ny][nx] && level.isWalkable(nx, ny)) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }
}
