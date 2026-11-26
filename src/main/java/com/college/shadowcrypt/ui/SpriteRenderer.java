package com.college.shadowcrypt.ui;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.model.CryptBossEnemy;
import com.college.shadowcrypt.model.DungeonLevel;
import com.college.shadowcrypt.model.Enemy;
import com.college.shadowcrypt.model.FloatingText;
import com.college.shadowcrypt.model.ItemType;
import com.college.shadowcrypt.model.Particle;
import com.college.shadowcrypt.model.PickupItem;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.Projectile;
import com.college.shadowcrypt.model.TileType;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * Custom Java2D world renderer drawing procedural dungeon stone tiles, dynamic torch lighting,
 * fog-of-war, animated player combat arcs, polymorphic enemy sprites, particles, and combat text.
 */
public final class SpriteRenderer {

    public static final int TILE_SIZE = 44;

    private SpriteRenderer() {}

    public static void renderWorld(Graphics2D g, GameEngine engine, int viewWidth, int viewHeight) {
        DungeonLevel level = engine.getLevel();
        Player player = engine.getPlayer();
        if (level == null || player == null) {
            return;
        }

        double camX = engine.getCameraX() + engine.getShakeOffsetX();
        double camY = engine.getCameraY() + engine.getShakeOffsetY();
        double offsetX = viewWidth / 2.0 - camX * TILE_SIZE;
        double offsetY = viewHeight / 2.0 - camY * TILE_SIZE;

        Graphics2D worldG = (Graphics2D) g.create();
        worldG.translate(offsetX, offsetY);

        int minTx = Math.max(0, (int) Math.floor(-offsetX / TILE_SIZE) - 1);
        int maxTx = Math.min(level.getWidth() - 1, (int) Math.ceil((viewWidth - offsetX) / TILE_SIZE) + 1);
        int minTy = Math.max(0, (int) Math.floor(-offsetY / TILE_SIZE) - 1);
        int maxTy = Math.min(level.getHeight() - 1, (int) Math.ceil((viewHeight - offsetY) / TILE_SIZE) + 1);

        double time = engine.getElapsedTime();

        // 1. Tiles & Fog of War
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (!level.isExplored(tx, ty)) {
                    continue;
                }
                TileType tile = level.getTile(tx, ty);
                drawTile(worldG, level, tx, ty, tile, time);
                if (!level.isVisible(tx, ty)) {
                    worldG.setColor(Theme.FOG_EXPLORED_VEIL);
                    worldG.fillRect(tx * TILE_SIZE, ty * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        // 2. Warm flickering torch radial glows
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (level.isVisible(tx, ty) && level.getTile(tx, ty) == TileType.TORCH_FLOOR) {
                    drawTorchGlow(worldG, tx, ty, time);
                }
            }
        }

        // 3. Pickup items (visible tiles only)
        for (PickupItem item : engine.getItems()) {
            int ix = (int) Math.floor(item.getX());
            int iy = (int) Math.floor(item.getY());
            if (level.isVisible(ix, iy)) {
                drawPickupItem(worldG, item, time);
            }
        }

        // 4. Enemies (visible tiles only)
        for (Enemy enemy : engine.getEnemies()) {
            if (!enemy.isAlive()) continue;
            int ex = (int) Math.floor(enemy.getX());
            int ey = (int) Math.floor(enemy.getY());
            if (level.isVisible(ex, ey)) {
                drawEnemy(worldG, enemy, time);
            }
        }

        // 5. Player
        drawPlayer(worldG, player, time);

        // 6. Projectiles
        for (Projectile p : engine.getProjectiles()) {
            int px = (int) Math.floor(p.getX());
            int py = (int) Math.floor(p.getY());
            if (p.isFromPlayer() || level.isVisible(px, py)) {
                drawProjectile(worldG, p);
            }
        }

        // 7. Particles
        for (Particle particle : engine.getParticles()) {
            Composite prev = worldG.getComposite();
            worldG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, particle.alpha()));
            worldG.setColor(new Color(particle.getColorRgb()));
            int size = Math.max(3, (int) Math.round(particle.getSize() * TILE_SIZE));
            int sx = (int) Math.round(particle.getX() * TILE_SIZE - size / 2.0);
            int sy = (int) Math.round(particle.getY() * TILE_SIZE - size / 2.0);
            worldG.fillOval(sx, sy, size, size);
            worldG.setComposite(prev);
        }

        // 8. Floating damage & loot text
        for (FloatingText ft : engine.getFloatingTexts()) {
            Composite prev = worldG.getComposite();
            worldG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ft.alpha()));
            worldG.setFont(ft.isLarge() ? Theme.FONT_SUBHEADING : Theme.FONT_SMALL_BOLD);
            int sx = (int) Math.round(ft.getX() * TILE_SIZE);
            int sy = (int) Math.round(ft.getY() * TILE_SIZE);
            worldG.setColor(Color.BLACK);
            Theme.drawCenteredString(worldG, ft.getText(), sx + 1, sy + 1);
            worldG.setColor(new Color(ft.getColorRgb()));
            Theme.drawCenteredString(worldG, ft.getText(), sx, sy);
            worldG.setComposite(prev);
        }

        worldG.dispose();
    }

    private static void drawTile(
            Graphics2D g,
            DungeonLevel level,
            int tx,
            int ty,
            TileType tile,
            double time) {
        int px = tx * TILE_SIZE;
        int py = ty * TILE_SIZE;

        if (tile == TileType.VOID) {
            return;
        }

        if (tile == TileType.WALL) {
            g.setColor(Theme.WALL_FACE);
            g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
            g.setColor(Theme.WALL_TOP);
            g.fillRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 10);
            g.setColor(Theme.WALL_BORDER);
            g.drawRect(px + 1, py + 1, TILE_SIZE - 2, TILE_SIZE - 2);
            // Stone brick seam
            g.setColor(new Color(24, 32, 46));
            g.drawLine(px + 4, py + TILE_SIZE / 2, px + TILE_SIZE - 4, py + TILE_SIZE / 2);
            return;
        }

        Color baseFloor = (tile == TileType.CORRIDOR)
                ? Theme.CORRIDOR_TILE
                : (((tx + ty) & 1) == 0 ? Theme.FLOOR_DARK : Theme.FLOOR_LIGHT);
        g.setColor(baseFloor);
        g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(255, 255, 255, 14));
        g.drawRect(px, py, TILE_SIZE - 1, TILE_SIZE - 1);

        switch (tile) {
            case TORCH_FLOOR -> {
                g.setColor(new Color(71, 85, 105));
                g.fillOval(px + TILE_SIZE / 2 - 6, py + TILE_SIZE / 2 - 6, 12, 12);
                double flicker = 0.8 + 0.2 * Math.sin(time * 12.0 + tx * 3.1 + ty);
                int r = (int) Math.round(7 * flicker);
                g.setColor(Theme.AMBER);
                g.fillOval(px + TILE_SIZE / 2 - r / 2 - 1, py + TILE_SIZE / 2 - r / 2 - 2, r + 2, r + 2);
                g.setColor(Theme.GOLD);
                g.fillOval(px + TILE_SIZE / 2 - 3, py + TILE_SIZE / 2 - 3, 6, 6);
            }
            case SPIKE_TRAP -> {
                g.setColor(new Color(45, 52, 68));
                g.fillRoundRect(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12, 6, 6);
                g.setColor(new Color(180, 195, 215));
                int[] offsets = {12, TILE_SIZE / 2, TILE_SIZE - 12};
                for (int ox : offsets) {
                    for (int oy : offsets) {
                        int[] xs = {px + ox, px + ox - 4, px + ox + 4};
                        int[] ys = {py + oy - 5, py + oy + 4, py + oy + 4};
                        g.fillPolygon(xs, ys, 3);
                    }
                }
                g.setColor(new Color(239, 68, 68, 90));
                g.drawRoundRect(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12, 6, 6);
            }
            case CHEST_PEDESTAL -> {
                g.setColor(new Color(168, 85, 247, 55));
                g.fillOval(px + 5, py + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                g.setColor(new Color(192, 132, 252, 160));
                g.drawOval(px + 8, py + 8, TILE_SIZE - 16, TILE_SIZE - 16);
            }
            case STAIRS_DOWN -> {
                boolean unlocked = level.isStairsUnlocked();
                Color portalColor = unlocked ? Theme.EMERALD : Theme.ROSE;
                g.setColor(new Color(portalColor.getRed(), portalColor.getGreen(), portalColor.getBlue(), 45));
                g.fillRoundRect(px + 3, py + 3, TILE_SIZE - 6, TILE_SIZE - 6, 10, 10);
                // Descending steps
                for (int s = 0; s < 4; s++) {
                    int inset = 7 + s * 4;
                    g.setColor(new Color(15 + s * 10, 22 + s * 12, 34 + s * 14));
                    g.fillRect(px + inset, py + 8 + s * 7, TILE_SIZE - inset * 2, 6);
                }
                g.setColor(portalColor);
                g.drawRoundRect(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8, 8, 8);
            }
            default -> {}
        }
    }

    private static void drawTorchGlow(Graphics2D g, int tx, int ty, double time) {
        float cx = (float) ((tx + 0.5) * TILE_SIZE);
        float cy = (float) ((ty + 0.5) * TILE_SIZE);
        float flicker = (float) (84.0 + 10.0 * Math.sin(time * 9.5 + tx * 2.3 + ty * 1.7));
        RadialGradientPaint paint = new RadialGradientPaint(
                cx,
                cy,
                flicker,
                new float[]{0f, 1f},
                new Color[]{new Color(251, 146, 60, 72), new Color(251, 146, 60, 0)});
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(cx - flicker, cy - flicker, flicker * 2, flicker * 2));
    }

    private static void drawPickupItem(Graphics2D g, PickupItem item, double time) {
        int cx = (int) Math.round(item.getX() * TILE_SIZE);
        int cy = (int) Math.round((item.getY() + item.getBobOffset()) * TILE_SIZE);
        ItemType type = item.getType();
        Color color = new Color(type.getColorRgb());

        // Soft aura
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 48));
        g.fillOval(cx - 14, cy - 14, 28, 28);

        switch (type) {
            case HEALTH_POTION -> {
                g.setColor(color);
                g.fillOval(cx - 7, cy - 4, 14, 14);
                g.setColor(new Color(226, 232, 240));
                g.fillRect(cx - 3, cy - 9, 6, 6);
                g.setColor(new Color(255, 255, 255, 160));
                g.fillOval(cx - 4, cy - 1, 4, 4);
            }
            case GOLD_COIN -> {
                g.setColor(color);
                g.fillOval(cx - 7, cy - 7, 14, 14);
                g.setColor(new Color(180, 120, 10));
                g.drawOval(cx - 7, cy - 7, 14, 14);
                g.setColor(Color.WHITE);
                g.fillRect(cx - 2, cy - 4, 2, 7);
            }
            case POWER_ORB -> {
                int[] xs = {cx, cx + 8, cx, cx - 8};
                int[] ys = {cy - 9, cy, cy + 9, cy};
                g.setColor(color);
                g.fillPolygon(xs, ys, 4);
                g.setColor(Color.WHITE);
                g.drawPolygon(xs, ys, 4);
            }
            case SPEED_BOOTS -> {
                Stroke prevStroke = g.getStroke();
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(color);
                g.drawLine(cx - 6, cy + 4, cx, cy - 3);
                g.drawLine(cx, cy - 3, cx + 6, cy + 4);
                g.drawLine(cx - 5, cy - 2, cx, cy - 8);
                g.drawLine(cx, cy - 8, cx + 5, cy - 2);
                g.setStroke(prevStroke);
            }
            case SHIELD_CHARM -> {
                int[] xs = {cx - 7, cx + 7, cx + 7, cx, cx - 7};
                int[] ys = {cy - 8, cy - 8, cy + 1, cy + 9, cy + 1};
                g.setColor(color);
                g.fillPolygon(xs, ys, 5);
                g.setColor(Color.WHITE);
                g.drawPolygon(xs, ys, 5);
            }
            case WEAPON_CHEST -> {
                double pulse = 1.0 + 0.1 * Math.sin(time * 5.0);
                int w = (int) Math.round(22 * pulse);
                int h = (int) Math.round(16 * pulse);
                g.setColor(new Color(109, 40, 217));
                g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 5, 5);
                g.setColor(Theme.GOLD);
                g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 5, 5);
                g.fillRect(cx - 2, cy - 3, 4, 6);
            }
        }
    }

    private static void drawPlayer(Graphics2D g, Player player, double time) {
        int cx = (int) Math.round(player.getX() * TILE_SIZE);
        int cy = (int) Math.round(player.getY() * TILE_SIZE);
        int r = (int) Math.round(player.getRadius() * TILE_SIZE);

        // Shadow
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(cx - r, cy + r / 2, r * 2, r);

        // Buff auras
        if (player.getPowerBoostTimer() > 0) {
            int pulse = (int) Math.round(5 + 2 * Math.sin(time * 7.0));
            g.setColor(new Color(251, 146, 60, 105));
            g.drawOval(cx - r - pulse, cy - r - pulse, (r + pulse) * 2, (r + pulse) * 2);
        }
        if (player.getShieldCharges() > 0) {
            int pulse = (int) Math.round(4 + 1.5 * Math.cos(time * 5.0));
            g.setColor(new Color(52, 211, 153, 150));
            Stroke prevStroke = g.getStroke();
            g.setStroke(new BasicStroke(2.2f));
            g.drawOval(cx - r - pulse, cy - r - pulse, (r + pulse) * 2, (r + pulse) * 2);
            g.setStroke(prevStroke);
        }

        // Melee cleave arc animation
        if (player.getAttackSwingTimer() > 0) {
            double progress = 1.0 - (player.getAttackSwingTimer() / 0.22);
            double reachPx = player.getWeapon().getReach() * TILE_SIZE;
            double deg = -Math.toDegrees(player.getSwingAngle());
            Color trail = new Color(player.getWeapon().getTrailColorRgb());
            Composite prev = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.75 * (1.0 - progress * 0.6))));
            Stroke prevStroke = g.getStroke();
            g.setColor(trail);
            g.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(
                    cx - reachPx,
                    cy - reachPx,
                    reachPx * 2,
                    reachPx * 2,
                    deg - 60,
                    120,
                    Arc2D.OPEN));
            g.setStroke(prevStroke);
            g.setComposite(prev);
        }

        // Invulnerability blink / hit flash
        boolean flash = player.getHitFlashTimer() > 0;
        Color bodyColor = flash
                ? Color.WHITE
                : (player.getDashTimer() > 0 ? Theme.CYAN : new Color(59, 130, 246));

        // Hero cloak & armor
        g.setColor(bodyColor);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(226, 232, 240));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);

        // Directional helm visor
        double cos = Math.cos(player.getFacingAngle());
        double sin = Math.sin(player.getFacingAngle());
        int vx = cx + (int) Math.round(cos * r * 0.45);
        int vy = cy + (int) Math.round(sin * r * 0.45);
        g.setColor(Theme.GOLD);
        g.fillOval(vx - 4, vy - 4, 8, 8);

        // Held sword blade
        int hiltX = cx + (int) Math.round(cos * (r + 2));
        int hiltY = cy + (int) Math.round(sin * (r + 2));
        int tipX = cx + (int) Math.round(cos * (r + 16));
        int tipY = cy + (int) Math.round(sin * (r + 16));
        Stroke prevStroke = g.getStroke();
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(player.getWeapon().getTrailColorRgb()));
        g.drawLine(hiltX, hiltY, tipX, tipY);
        g.setStroke(prevStroke);
    }

    private static void drawEnemy(Graphics2D g, Enemy enemy, double time) {
        int cx = (int) Math.round(enemy.getX() * TILE_SIZE);
        int cy = (int) Math.round(enemy.getY() * TILE_SIZE);
        int r = (int) Math.round(enemy.getRadius() * TILE_SIZE);
        boolean flash = enemy.getHitFlashTimer() > 0;
        boolean winding = enemy.getAiState() == Enemy.AiState.WINDUP;

        // Shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(cx - r, cy + r / 2, r * 2, r);

        if (winding) {
            g.setColor(new Color(244, 63, 94, 155));
            g.drawOval(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2);
        }

        Color base = flash ? Color.WHITE : new Color(enemy.getEnemyType().getColorRgb());

        switch (enemy.getEnemyType()) {
            case SLIME -> {
                double squash = 1.0 + 0.12 * Math.sin(enemy.getAnimPhase());
                int w = (int) Math.round(r * 2 * squash);
                int h = (int) Math.round(r * 1.8 / squash);
                g.setColor(base);
                g.fillOval(cx - w / 2, cy - h / 2, w, h);
                g.setColor(new Color(20, 83, 45));
                g.drawOval(cx - w / 2, cy - h / 2, w, h);
                g.setColor(Color.BLACK);
                g.fillOval(cx - 5, cy - 3, 3, 4);
                g.fillOval(cx + 2, cy - 3, 3, 4);
            }
            case SKELETON_ARCHER -> {
                g.setColor(base);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
                g.setColor(new Color(30, 41, 59));
                g.fillOval(cx - 5, cy - 3, 4, 4);
                g.fillOval(cx + 2, cy - 3, 4, 4);
                // Directional bow facing target
                int deg = (int) Math.round(-Math.toDegrees(enemy.getFacingAngle()));
                Stroke prevStroke = g.getStroke();
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(205, 138, 68));
                g.drawArc(cx - r - 3, cy - r - 3, (r + 3) * 2, (r + 3) * 2, deg - 55, 110);
                g.setStroke(prevStroke);
            }
            case SHADOW_STALKER -> {
                int[] xs = {cx, cx + r, cx, cx - r};
                int[] ys = {cy - r - 3, cy, cy + r + 2, cy};
                g.setColor(base);
                g.fillPolygon(xs, ys, 4);
                g.setColor(Theme.ROSE);
                g.fillOval(cx - 5, cy - 3, 4, 3);
                g.fillOval(cx + 2, cy - 3, 4, 3);
            }
            case CRYPT_WARDEN_BOSS -> {
                CryptBossEnemy boss = (CryptBossEnemy) enemy;
                if (boss.isPhaseTwo()) {
                    double pulse = 1.0 + 0.14 * Math.sin(time * 8.0);
                    int auraR = (int) Math.round((r + 9) * pulse);
                    g.setColor(new Color(244, 63, 94, 70));
                    g.fillOval(cx - auraR, cy - auraR, auraR * 2, auraR * 2);
                }
                g.setColor(base);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
                g.setColor(Theme.GOLD);
                g.drawOval(cx - r, cy - r, r * 2, r * 2);
                // Crown horns
                int[] lx = {cx - r + 4, cx - r - 4, cx - 6};
                int[] ly = {cy - 6, cy - r - 10, cy - r + 2};
                int[] rx = {cx + r - 4, cx + r + 4, cx + 6};
                int[] ry = {cy - 6, cy - r - 10, cy - r + 2};
                g.fillPolygon(lx, ly, 3);
                g.fillPolygon(rx, ry, 3);
                // Glowing eyes
                g.setColor(Theme.GOLD);
                g.fillOval(cx - 8, cy - 4, 5, 5);
                g.fillOval(cx + 3, cy - 4, 5, 5);
            }
        }

        // Compact HP bar above damaged non-boss enemies
        if (!enemy.isBoss() && enemy.getHp() < enemy.getMaxHp()) {
            int barW = Math.max(26, r * 2);
            int barX = cx - barW / 2;
            int barY = cy - r - 10;
            g.setColor(new Color(15, 23, 42, 210));
            g.fillRect(barX, barY, barW, 5);
            double frac = (double) enemy.getHp() / enemy.getMaxHp();
            g.setColor(Theme.CRIMSON);
            g.fillRect(barX, barY, (int) Math.round(barW * frac), 5);
        }
    }

    private static void drawProjectile(Graphics2D g, Projectile p) {
        int cx = (int) Math.round(p.getX() * TILE_SIZE);
        int cy = (int) Math.round(p.getY() * TILE_SIZE);
        int r = Math.max(4, (int) Math.round(p.getRadius() * TILE_SIZE));
        Color c = new Color(p.getColorRgb());
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
        g.fillOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);
        g.setColor(c);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(Color.WHITE);
        g.fillOval(cx - r / 2, cy - r / 2, Math.max(2, r), Math.max(2, r));
    }
}
