package com.college.shadowcrypt.engine;

import com.college.shadowcrypt.model.DungeonLevel;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Bounded Breadth-First Search pathfinder allowing enemies to navigate around dungeon walls.
 */
public final class Pathfinder {

    private static final int MAX_VISITED = 280;
    private static final int[][] CARDINAL_AND_DIAGONAL = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private Pathfinder() {}

    public static double[] findStepDirection(
            DungeonLevel level,
            double fromX,
            double fromY,
            double targetX,
            double targetY) {
        if (CollisionSystem.hasLineOfSight(level, fromX, fromY, targetX, targetY)) {
            return normalize(targetX - fromX, targetY - fromY);
        }

        int sx = (int) Math.floor(fromX);
        int sy = (int) Math.floor(fromY);
        int tx = (int) Math.floor(targetX);
        int ty = (int) Math.floor(targetY);

        if (sx == tx && sy == ty) {
            return normalize(targetX - fromX, targetY - fromY);
        }

        // Reverse BFS from target toward start so the first move from start is immediately known
        boolean[][] visited = new boolean[level.getHeight()][level.getWidth()];
        Queue<int[]> queue = new ArrayDeque<>();
        if (level.inBounds(tx, ty) && level.isWalkable(tx, ty)) {
            queue.add(new int[]{tx, ty});
            visited[ty][tx] = true;
        }

        int visitedCount = 0;
        while (!queue.isEmpty() && visitedCount < MAX_VISITED) {
            int[] cur = queue.poll();
            visitedCount++;
            for (int[] dir : CARDINAL_AND_DIAGONAL) {
                int nx = cur[0] + dir[0];
                int ny = cur[1] + dir[1];
                if (!level.inBounds(nx, ny) || visited[ny][nx] || !level.isWalkable(nx, ny)) {
                    continue;
                }
                // Prevent diagonal corner cutting through walls
                if (dir[0] != 0 && dir[1] != 0) {
                    if (!level.isWalkable(cur[0] + dir[0], cur[1])
                            || !level.isWalkable(cur[0], cur[1] + dir[1])) {
                        continue;
                    }
                }
                if (nx == sx && ny == sy) {
                    return normalize((cur[0] + 0.5) - fromX, (cur[1] + 0.5) - fromY);
                }
                visited[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }

        return normalize(targetX - fromX, targetY - fromY);
    }

    private static double[] normalize(double dx, double dy) {
        double len = Math.hypot(dx, dy);
        if (len < 1e-5) {
            return new double[]{0.0, 0.0};
        }
        return new double[]{dx / len, dy / len};
    }
}
