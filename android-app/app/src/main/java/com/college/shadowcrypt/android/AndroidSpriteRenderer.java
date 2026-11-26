package com.college.shadowcrypt.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

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

/**
 * Android Canvas renderer drawing procedural dungeon tiles, radial torch glow, Fog of War,
 * player sword cleave animations, enemies, particles, and floating damage numbers.
 */
public final class AndroidSpriteRenderer {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public void renderWorld(Canvas canvas, GameEngine engine, int viewWidth, int viewHeight, float density) {
        DungeonLevel level = engine.getLevel();
        Player player = engine.getPlayer();
        if (level == null || player == null) {
            return;
        }

        float tileSize = Math.max(36f, Math.min(viewHeight / 10.5f, 44f * density * 0.72f));
        double camX = engine.getCameraX() + engine.getShakeOffsetX();
        double camY = engine.getCameraY() + engine.getShakeOffsetY();
        float offsetX = (float) (viewWidth / 2.0 - camX * tileSize);
        float offsetY = (float) (viewHeight / 2.0 - camY * tileSize);

        canvas.save();
        canvas.translate(offsetX, offsetY);

        int minTx = Math.max(0, (int) Math.floor(-offsetX / tileSize) - 1);
        int maxTx = Math.min(level.getWidth() - 1, (int) Math.ceil((viewWidth - offsetX) / tileSize) + 1);
        int minTy = Math.max(0, (int) Math.floor(-offsetY / tileSize) - 1);
        int maxTy = Math.min(level.getHeight() - 1, (int) Math.ceil((viewHeight - offsetY) / tileSize) + 1);

        double time = engine.getElapsedTime();

        // 1. Tiles & Fog of War
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (!level.isExplored(tx, ty)) {
                    continue;
                }
                drawTile(canvas, level, tx, ty, level.getTile(tx, ty), tileSize, time);
                if (!level.isVisible(tx, ty)) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(AndroidTheme.FOG_EXPLORED_VEIL);
                    canvas.drawRect(tx * tileSize, ty * tileSize, (tx + 1) * tileSize, (ty + 1) * tileSize, paint);
                }
            }
        }

        // 2. Torch glow
        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                if (level.isVisible(tx, ty) && level.getTile(tx, ty) == TileType.TORCH_FLOOR) {
                    drawTorchGlow(canvas, tx, ty, tileSize, time);
                }
            }
        }

        // 3. Pickup items
        for (PickupItem item : engine.getItems()) {
            int ix = (int) Math.floor(item.getX());
            int iy = (int) Math.floor(item.getY());
            if (level.isVisible(ix, iy)) {
                drawPickupItem(canvas, item, tileSize, time);
            }
        }

        // 4. Enemies
        for (Enemy enemy : engine.getEnemies()) {
            if (!enemy.isAlive()) continue;
            int ex = (int) Math.floor(enemy.getX());
            int ey = (int) Math.floor(enemy.getY());
            if (level.isVisible(ex, ey)) {
                drawEnemy(canvas, enemy, tileSize, time);
            }
        }

        // 5. Player
        drawPlayer(canvas, player, tileSize, time);

        // 6. Projectiles
        for (Projectile p : engine.getProjectiles()) {
            int px = (int) Math.floor(p.getX());
            int py = (int) Math.floor(p.getY());
            if (p.isFromPlayer() || level.isVisible(px, py)) {
                drawProjectile(canvas, p, tileSize);
            }
        }

        // 7. Particles
        paint.setStyle(Paint.Style.FILL);
        for (Particle particle : engine.getParticles()) {
            int alpha = Math.max(0, Math.min(255, (int) (particle.alpha() * 255)));
            paint.setColor(AndroidTheme.withAlpha(AndroidTheme.opaque(particle.getColorRgb()), alpha));
            float r = Math.max(2.5f, (float) (particle.getSize() * tileSize * 0.5));
            canvas.drawCircle((float) (particle.getX() * tileSize), (float) (particle.getY() * tileSize), r, paint);
        }

        // 8. Floating Combat Text
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        for (FloatingText ft : engine.getFloatingTexts()) {
            int alpha = Math.max(0, Math.min(255, (int) (ft.alpha() * 255)));
            paint.setTextSize(ft.isLarge() ? tileSize * 0.42f : tileSize * 0.34f);
            float sx = (float) (ft.getX() * tileSize);
            float sy = (float) (ft.getY() * tileSize);
            paint.setColor(AndroidTheme.withAlpha(Color.BLACK, alpha));
            canvas.drawText(ft.getText(), sx + 2f, sy + 2f, paint);
            paint.setColor(AndroidTheme.withAlpha(AndroidTheme.opaque(ft.getColorRgb()), alpha));
            canvas.drawText(ft.getText(), sx, sy, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);

        canvas.restore();
    }

    private void drawTile(
            Canvas canvas,
            DungeonLevel level,
            int tx,
            int ty,
            TileType tile,
            float ts,
            double time) {
        if (tile == TileType.VOID) {
            return;
        }
        float px = tx * ts;
        float py = ty * ts;

        if (tile == TileType.WALL) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(AndroidTheme.WALL_FACE);
            canvas.drawRect(px, py, px + ts, py + ts, paint);
            paint.setColor(AndroidTheme.WALL_TOP);
            canvas.drawRect(px + 2f, py + 2f, px + ts - 2f, py + ts - ts * 0.22f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(AndroidTheme.WALL_BORDER);
            canvas.drawRect(px + 1f, py + 1f, px + ts - 1f, py + ts - 1f, paint);
            paint.setColor(0xFF18202E);
            canvas.drawLine(px + 3f, py + ts * 0.5f, px + ts - 3f, py + ts * 0.5f, paint);
            return;
        }

        int baseFloor = (tile == TileType.CORRIDOR)
                ? AndroidTheme.CORRIDOR_TILE
                : (((tx + ty) & 1) == 0 ? AndroidTheme.FLOOR_DARK : AndroidTheme.FLOOR_LIGHT);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(baseFloor);
        canvas.drawRect(px, py, px + ts, py + ts, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(0x10FFFFFF);
        canvas.drawRect(px, py, px + ts, py + ts, paint);

        paint.setStyle(Paint.Style.FILL);
        switch (tile) {
            case TORCH_FLOOR -> {
                float cx = px + ts * 0.5f;
                float cy = py + ts * 0.5f;
                paint.setColor(0xFF475569);
                canvas.drawCircle(cx, cy, ts * 0.14f, paint);
                float flicker = (float) (0.8 + 0.2 * Math.sin(time * 12.0 + tx * 3.1 + ty));
                paint.setColor(AndroidTheme.AMBER);
                canvas.drawCircle(cx, cy - ts * 0.03f, ts * 0.11f * flicker, paint);
                paint.setColor(AndroidTheme.GOLD);
                canvas.drawCircle(cx, cy, ts * 0.06f, paint);
            }
            case SPIKE_TRAP -> {
                rect.set(px + ts * 0.14f, py + ts * 0.14f, px + ts * 0.86f, py + ts * 0.86f);
                paint.setColor(0xFF2D3444);
                canvas.drawRoundRect(rect, 6f, 6f, paint);
                paint.setColor(0xFFB4C3D7);
                float[] ofs = {0.28f, 0.50f, 0.72f};
                for (float ox : ofs) {
                    for (float oy : ofs) {
                        float sx = px + ts * ox;
                        float sy = py + ts * oy;
                        path.reset();
                        path.moveTo(sx, sy - ts * 0.10f);
                        path.lineTo(sx - ts * 0.08f, sy + ts * 0.08f);
                        path.lineTo(sx + ts * 0.08f, sy + ts * 0.08f);
                        path.close();
                        canvas.drawPath(path, paint);
                    }
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.5f);
                paint.setColor(0x60EF4444);
                canvas.drawRoundRect(rect, 6f, 6f, paint);
            }
            case CHEST_PEDESTAL -> {
                float cx = px + ts * 0.5f;
                float cy = py + ts * 0.5f;
                paint.setColor(0x38A855F7);
                canvas.drawCircle(cx, cy, ts * 0.38f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
                paint.setColor(0xA0C084FC);
                canvas.drawCircle(cx, cy, ts * 0.30f, paint);
            }
            case STAIRS_DOWN -> {
                boolean unlocked = level.isStairsUnlocked();
                int portal = unlocked ? AndroidTheme.EMERALD : AndroidTheme.ROSE;
                rect.set(px + ts * 0.08f, py + ts * 0.08f, px + ts * 0.92f, py + ts * 0.92f);
                paint.setColor(AndroidTheme.withAlpha(portal, 48));
                canvas.drawRoundRect(rect, 8f, 8f, paint);
                for (int s = 0; s < 4; s++) {
                    float inset = ts * (0.16f + s * 0.09f);
                    paint.setColor(Color.rgb(15 + s * 10, 22 + s * 12, 34 + s * 14));
                    canvas.drawRect(
                            px + inset,
                            py + ts * (0.18f + s * 0.16f),
                            px + ts - inset,
                            py + ts * (0.31f + s * 0.16f),
                            paint);
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.2f);
                paint.setColor(portal);
                canvas.drawRoundRect(rect, 8f, 8f, paint);
            }
            default -> {}
        }
    }

    private void drawTorchGlow(Canvas canvas, int tx, int ty, float ts, double time) {
        float cx = (tx + 0.5f) * ts;
        float cy = (ty + 0.5f) * ts;
        float r = (float) (ts * (1.85 + 0.22 * Math.sin(time * 9.5 + tx * 2.3 + ty * 1.7)));
        RadialGradient grad = new RadialGradient(
                cx, cy, r,
                new int[]{0x4AFB923C, 0x00FB923C},
                null,
                Shader.TileMode.CLAMP);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(grad);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setShader(null);
    }

    private void drawPickupItem(Canvas canvas, PickupItem item, float ts, double time) {
        float cx = (float) (item.getX() * ts);
        float cy = (float) ((item.getY() + item.getBobOffset()) * ts);
        ItemType type = item.getType();
        int col = AndroidTheme.opaque(type.getColorRgb());

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AndroidTheme.withAlpha(col, 55));
        canvas.drawCircle(cx, cy, ts * 0.30f, paint);

        switch (type) {
            case HEALTH_POTION -> {
                paint.setColor(col);
                canvas.drawCircle(cx, cy + ts * 0.05f, ts * 0.16f, paint);
                paint.setColor(0xFFE2E8F0);
                canvas.drawRect(cx - ts * 0.06f, cy - ts * 0.20f, cx + ts * 0.06f, cy - ts * 0.06f, paint);
            }
            case GOLD_COIN -> {
                paint.setColor(col);
                canvas.drawCircle(cx, cy, ts * 0.16f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
                paint.setColor(0xFFB4780A);
                canvas.drawCircle(cx, cy, ts * 0.16f, paint);
            }
            case POWER_ORB -> {
                path.reset();
                path.moveTo(cx, cy - ts * 0.20f);
                path.lineTo(cx + ts * 0.18f, cy);
                path.lineTo(cx, cy + ts * 0.20f);
                path.lineTo(cx - ts * 0.18f, cy);
                path.close();
                paint.setColor(col);
                canvas.drawPath(path, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.8f);
                paint.setColor(Color.WHITE);
                canvas.drawPath(path, paint);
            }
            case SPEED_BOOTS -> {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(col);
                canvas.drawLine(cx - ts * 0.14f, cy + ts * 0.08f, cx, cy - ts * 0.06f, paint);
                canvas.drawLine(cx, cy - ts * 0.06f, cx + ts * 0.14f, cy + ts * 0.08f, paint);
                canvas.drawLine(cx - ts * 0.12f, cy - ts * 0.04f, cx, cy - ts * 0.18f, paint);
                canvas.drawLine(cx, cy - ts * 0.18f, cx + ts * 0.12f, cy - ts * 0.04f, paint);
            }
            case SHIELD_CHARM -> {
                path.reset();
                path.moveTo(cx - ts * 0.16f, cy - ts * 0.18f);
                path.lineTo(cx + ts * 0.16f, cy - ts * 0.18f);
                path.lineTo(cx + ts * 0.16f, cy + ts * 0.02f);
                path.lineTo(cx, cy + ts * 0.20f);
                path.lineTo(cx - ts * 0.16f, cy + ts * 0.02f);
                path.close();
                paint.setColor(col);
                canvas.drawPath(path, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.8f);
                paint.setColor(Color.WHITE);
                canvas.drawPath(path, paint);
            }
            case WEAPON_CHEST -> {
                float pulse = (float) (1.0 + 0.1 * Math.sin(time * 5.0));
                float hw = ts * 0.25f * pulse;
                float hh = ts * 0.18f * pulse;
                rect.set(cx - hw, cy - hh, cx + hw, cy + hh);
                paint.setColor(0xFF6D28D9);
                canvas.drawRoundRect(rect, 5f, 5f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f);
                paint.setColor(AndroidTheme.GOLD);
                canvas.drawRoundRect(rect, 5f, 5f, paint);
            }
        }
    }

    private void drawPlayer(Canvas canvas, Player player, float ts, double time) {
        float cx = (float) (player.getX() * ts);
        float cy = (float) (player.getY() * ts);
        float r = (float) (player.getRadius() * ts);

        // Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x70000000);
        rect.set(cx - r, cy + r * 0.45f, cx + r, cy + r * 1.15f);
        canvas.drawOval(rect, paint);

        if (player.getPowerBoostTimer() > 0) {
            float pulse = (float) (4f + 2f * Math.sin(time * 7.0));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.2f);
            paint.setColor(0x80FB923C);
            canvas.drawCircle(cx, cy, r + pulse, paint);
        }
        if (player.getShieldCharges() > 0) {
            float pulse = (float) (4f + 1.5f * Math.cos(time * 5.0));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.6f);
            paint.setColor(0x9634D399);
            canvas.drawCircle(cx, cy, r + pulse, paint);
        }

        // Melee cleave arc
        if (player.getAttackSwingTimer() > 0) {
            double progress = 1.0 - (player.getAttackSwingTimer() / 0.22);
            float reachPx = (float) (player.getWeapon().getReach() * ts);
            float deg = (float) Math.toDegrees(player.getSwingAngle());
            int alpha = Math.max(0, Math.min(255, (int) (210 * (1.0 - progress * 0.6))));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6.5f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(AndroidTheme.withAlpha(AndroidTheme.opaque(player.getWeapon().getTrailColorRgb()), alpha));
            rect.set(cx - reachPx, cy - reachPx, cx + reachPx, cy + reachPx);
            canvas.drawArc(rect, deg - 60f, 120f, false, paint);
        }

        boolean flash = player.getHitFlashTimer() > 0;
        int bodyColor = flash
                ? Color.WHITE
                : (player.getDashTimer() > 0 ? AndroidTheme.CYAN : 0xFF3B82F6);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bodyColor);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(0xFFE2E8F0);
        canvas.drawCircle(cx, cy, r, paint);

        double cos = Math.cos(player.getFacingAngle());
        double sin = Math.sin(player.getFacingAngle());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AndroidTheme.GOLD);
        canvas.drawCircle(cx + (float) (cos * r * 0.45), cy + (float) (sin * r * 0.45), r * 0.28f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(AndroidTheme.opaque(player.getWeapon().getTrailColorRgb()));
        canvas.drawLine(
                cx + (float) (cos * (r + 2f)),
                cy + (float) (sin * (r + 2f)),
                cx + (float) (cos * (r + ts * 0.36f)),
                cy + (float) (sin * (r + ts * 0.36f)),
                paint);
    }

    private void drawEnemy(Canvas canvas, Enemy enemy, float ts, double time) {
        float cx = (float) (enemy.getX() * ts);
        float cy = (float) (enemy.getY() * ts);
        float r = (float) (enemy.getRadius() * ts);
        boolean flash = enemy.getHitFlashTimer() > 0;
        boolean winding = enemy.getAiState() == Enemy.AiState.WINDUP;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x64000000);
        rect.set(cx - r, cy + r * 0.45f, cx + r, cy + r * 1.1f);
        canvas.drawOval(rect, paint);

        if (winding) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.4f);
            paint.setColor(0xA0F43F5E);
            canvas.drawCircle(cx, cy, r + 5f, paint);
        }

        int base = flash ? Color.WHITE : AndroidTheme.opaque(enemy.getEnemyType().getColorRgb());
        paint.setStyle(Paint.Style.FILL);

        switch (enemy.getEnemyType()) {
            case SLIME -> {
                float squash = (float) (1.0 + 0.12 * Math.sin(enemy.getAnimPhase()));
                float hw = r * squash;
                float hh = r * 0.9f / squash;
                rect.set(cx - hw, cy - hh, cx + hw, cy + hh);
                paint.setColor(base);
                canvas.drawOval(rect, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.8f);
                paint.setColor(0xFF14532D);
                canvas.drawOval(rect, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.BLACK);
                canvas.drawCircle(cx - r * 0.25f, cy - r * 0.10f, r * 0.13f, paint);
                canvas.drawCircle(cx + r * 0.25f, cy - r * 0.10f, r * 0.13f, paint);
            }
            case SKELETON_ARCHER -> {
                paint.setColor(base);
                canvas.drawCircle(cx, cy, r, paint);
                paint.setColor(0xFF1E293B);
                canvas.drawCircle(cx - r * 0.25f, cy - r * 0.10f, r * 0.14f, paint);
                canvas.drawCircle(cx + r * 0.25f, cy - r * 0.10f, r * 0.14f, paint);
                float deg = (float) Math.toDegrees(enemy.getFacingAngle());
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.6f);
                paint.setColor(0xFFCD8A44);
                rect.set(cx - r - 3f, cy - r - 3f, cx + r + 3f, cy + r + 3f);
                canvas.drawArc(rect, deg - 55f, 110f, false, paint);
            }
            case SHADOW_STALKER -> {
                path.reset();
                path.moveTo(cx, cy - r - 3f);
                path.lineTo(cx + r, cy);
                path.lineTo(cx, cy + r + 2f);
                path.lineTo(cx - r, cy);
                path.close();
                paint.setColor(base);
                canvas.drawPath(path, paint);
                paint.setColor(AndroidTheme.ROSE);
                canvas.drawCircle(cx - r * 0.22f, cy - r * 0.10f, r * 0.13f, paint);
                canvas.drawCircle(cx + r * 0.22f, cy - r * 0.10f, r * 0.13f, paint);
            }
            case CRYPT_WARDEN_BOSS -> {
                CryptBossEnemy boss = (CryptBossEnemy) enemy;
                if (boss.isPhaseTwo()) {
                    float pulse = (float) (1.0 + 0.14 * Math.sin(time * 8.0));
                    paint.setColor(0x46F43F5E);
                    canvas.drawCircle(cx, cy, (r + 9f) * pulse, paint);
                }
                paint.setColor(base);
                canvas.drawCircle(cx, cy, r, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.5f);
                paint.setColor(AndroidTheme.GOLD);
                canvas.drawCircle(cx, cy, r, paint);
                paint.setStyle(Paint.Style.FILL);
                path.reset();
                path.moveTo(cx - r + 4f, cy - 6f);
                path.lineTo(cx - r - 4f, cy - r - 10f);
                path.lineTo(cx - 6f, cy - r + 2f);
                path.close();
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(cx + r - 4f, cy - 6f);
                path.lineTo(cx + r + 4f, cy - r - 10f);
                path.lineTo(cx + 6f, cy - r + 2f);
                path.close();
                canvas.drawPath(path, paint);
                canvas.drawCircle(cx - r * 0.28f, cy - r * 0.12f, r * 0.14f, paint);
                canvas.drawCircle(cx + r * 0.28f, cy - r * 0.12f, r * 0.14f, paint);
            }
        }

        if (!enemy.isBoss() && enemy.getHp() < enemy.getMaxHp()) {
            float barW = Math.max(26f, r * 2f);
            float bx = cx - barW * 0.5f;
            float by = cy - r - 10f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xD20F172A);
            canvas.drawRect(bx, by, bx + barW, by + 5f, paint);
            float frac = Math.max(0f, Math.min(1f, (float) enemy.getHp() / enemy.getMaxHp()));
            paint.setColor(AndroidTheme.CRIMSON);
            canvas.drawRect(bx, by, bx + barW * frac, by + 5f, paint);
        }
    }

    private void drawProjectile(Canvas canvas, Projectile p, float ts) {
        float cx = (float) (p.getX() * ts);
        float cy = (float) (p.getY() * ts);
        float r = Math.max(4f, (float) (p.getRadius() * ts));
        int col = AndroidTheme.opaque(p.getColorRgb());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AndroidTheme.withAlpha(col, 85));
        canvas.drawCircle(cx, cy, r + 4f, paint);
        paint.setColor(col);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, Math.max(2f, r * 0.5f), paint);
    }
}
