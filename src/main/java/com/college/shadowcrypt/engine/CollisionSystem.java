package com.college.shadowcrypt.engine;

import com.college.shadowcrypt.model.DungeonLevel;
import com.college.shadowcrypt.model.Entity;

/**
 * Handles smooth circle-vs-tile-grid sliding collision, raycast line-of-sight, and Fog of War.
 */
public final class CollisionSystem {

    private CollisionSystem() {}

    public static void moveWithTileCollision(
            DungeonLevel level,
            Entity entity,
            double dx,
            double dy) {
        double r = entity.getRadius();

        double nextX = entity.getX() + dx;
        if (isCircleWalkable(level, nextX, entity.getY(), r)) {
            entity.setX(nextX);
        }

        double nextY = entity.getY() + dy;
        if (isCircleWalkable(level, entity.getX(), nextY, r)) {
            entity.setY(nextY);
        }
    }

    public static boolean isCircleWalkable(DungeonLevel level, double cx, double cy, double r) {
        int minTx = (int) Math.floor(cx - r);
        int maxTx = (int) Math.floor(cx + r);
        int minTy = (int) Math.floor(cy - r);
        int maxTy = (int) Math.floor(cy + r);

        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (level.isWalkable(tx, ty)) {
                    continue;
                }
                double nearestX = Math.max(tx, Math.min(cx, tx + 1.0));
                double nearestY = Math.max(ty, Math.min(cy, ty + 1.0));
                double distSq = (cx - nearestX) * (cx - nearestX) + (cy - nearestY) * (cy - nearestY);
                if (distSq < r * r) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean hasLineOfSight(
            DungeonLevel level,
            double x1,
            double y1,
            double x2,
            double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dist = Math.hypot(dx, dy);
        if (dist < 0.05) {
            return true;
        }
        int steps = Math.max(1, (int) Math.ceil(dist * 6.0));
        double stepX = dx / steps;
        double stepY = dy / steps;

        double cx = x1;
        double cy = y1;
        for (int i = 0; i < steps; i++) {
            int tx = (int) Math.floor(cx);
            int ty = (int) Math.floor(cy);
            if (!level.isTransparent(tx, ty)) {
                return false;
            }
            cx += stepX;
            cy += stepY;
        }
        return true;
    }

    public static void updateFogOfWar(
            DungeonLevel level,
            double playerX,
            double playerY,
            int visionRadius) {
        level.clearVisible();
        int px = (int) Math.floor(playerX);
        int py = (int) Math.floor(playerY);

        int minX = Math.max(0, px - visionRadius);
        int maxX = Math.min(level.getWidth() - 1, px + visionRadius);
        int minY = Math.max(0, py - visionRadius);
        int maxY = Math.min(level.getHeight() - 1, py + visionRadius);

        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
                double dist = Math.hypot((tx + 0.5) - playerX, (ty + 0.5) - playerY);
                if (dist > visionRadius + 0.5) {
                    continue;
                }
                if (hasLosToTile(level, playerX, playerY, tx, ty)) {
                    level.setVisible(tx, ty, true);
                }
            }
        }

        // Ensure opaque walls directly framing visible floor tiles render cleanly without dark corner gaps
        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
                if (!level.isWalkable(tx, ty) || !level.isVisible(tx, ty)) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = tx + dx;
                        int ny = ty + dy;
                        if (level.inBounds(nx, ny) && !level.isWalkable(nx, ny)) {
                            level.setVisible(nx, ny, true);
                        }
                    }
                }
            }
        }
    }

    private static boolean hasLosToTile(
            DungeonLevel level,
            double startX,
            double startY,
            int targetTx,
            int targetTy) {
        double endX = targetTx + 0.5;
        double endY = targetTy + 0.5;
        double dx = endX - startX;
        double dy = endY - startY;
        double dist = Math.hypot(dx, dy);
        int steps = Math.max(1, (int) Math.ceil(dist * 4.5));
        double stepX = dx / steps;
        double stepY = dy / steps;

        double cx = startX;
        double cy = startY;
        for (int i = 0; i < steps; i++) {
            int tx = (int) Math.floor(cx);
            int ty = (int) Math.floor(cy);
            if (tx == targetTx && ty == targetTy) {
                return true;
            }
            if (!level.isTransparent(tx, ty)) {
                return false;
            }
            cx += stepX;
            cy += stepY;
        }
        return true;
    }
}
