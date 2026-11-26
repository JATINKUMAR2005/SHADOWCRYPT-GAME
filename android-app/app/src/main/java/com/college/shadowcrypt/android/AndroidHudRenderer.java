package com.college.shadowcrypt.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.model.CryptBossEnemy;
import com.college.shadowcrypt.model.DungeonLevel;
import com.college.shadowcrypt.model.Enemy;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.TileType;

/**
 * Responsive Android HUD renderer drawing player stats, boss health bar, live minimap,
 * top Pause/Mute buttons, and contextual dungeon prompts.
 */
public final class AndroidHudRenderer {

    public final RectF pauseButton = new RectF();
    public final RectF muteButton = new RectF();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public void renderHud(
            Canvas canvas,
            GameEngine engine,
            int w,
            int h,
            float dp,
            boolean muted) {
        Player player = engine.getPlayer();
        DungeonLevel level = engine.getLevel();
        if (player == null || level == null) {
            return;
        }

        float pad = 10f * dp;
        float cardW = Math.min(248f * dp, w * 0.30f);
        float cardH = 108f * dp;
        rect.set(pad, pad, pad + cardW, pad + cardH);
        AndroidTheme.drawRoundedPanel(canvas, paint, rect, 12f * dp, AndroidTheme.PANEL_BG, AndroidTheme.PANEL_BORDER, 1.5f * dp);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(12f * dp);
        paint.setColor(AndroidTheme.TEXT_PRIMARY);
        canvas.drawText(player.getName() + " (Lv." + player.getLevel() + ")", rect.left + 10f * dp, rect.top + 18f * dp, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(AndroidTheme.GOLD);
        canvas.drawText("F" + player.getFloor() + " • " + engine.getDifficulty().getLabel(), rect.right - 10f * dp, rect.top + 18f * dp, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        float barX = rect.left + 10f * dp;
        float barW = cardW - 20f * dp;
        drawProgressBar(
                canvas, barX, rect.top + 24f * dp, barW, 15f * dp, dp,
                (double) player.getHp() / player.getMaxHp(),
                AndroidTheme.CRIMSON,
                "HP " + player.getHp() + "/" + player.getMaxHp());
        drawProgressBar(
                canvas, barX, rect.top + 43f * dp, barW, 10f * dp, dp,
                (double) player.getXp() / player.getXpToNext(),
                AndroidTheme.VIOLET,
                "XP " + player.getXp() + "/" + player.getXpToNext());

        paint.setTextSize(10.5f * dp);
        paint.setColor(AndroidTheme.opaque(player.getWeapon().getTrailColorRgb()));
        canvas.drawText(player.getWeapon().getDisplayName() + " (" + player.getEffectiveAttackDamage() + " DMG)", barX, rect.top + 69f * dp, paint);

        paint.setColor(AndroidTheme.TEXT_PRIMARY);
        canvas.drawText("Score: " + player.getScore() + "  Kills: " + player.getKills() + "  Gold: " + player.getGold(), barX, rect.top + 84f * dp, paint);

        paint.setColor(AndroidTheme.EMERALD);
        String buffLine = "Flasks: " + player.getPotions() + (player.getShieldCharges() > 0 ? "  |  Aegis: " + player.getShieldCharges() : "");
        canvas.drawText(buffLine, barX, rect.top + 99f * dp, paint);

        // Minimap top-right
        float mapScale = Math.max(2f, Math.min(3.5f * dp, (w * 0.21f) / level.getWidth()));
        float mapW = level.getWidth() * mapScale;
        float mapH = level.getHeight() * mapScale;
        float mapX = w - mapW - pad - 4f * dp;
        float mapY = pad;

        rect.set(mapX - 4f * dp, mapY - 4f * dp, mapX + mapW + 4f * dp, mapY + mapH + 4f * dp);
        AndroidTheme.drawRoundedPanel(canvas, paint, rect, 10f * dp, AndroidTheme.PANEL_BG, AndroidTheme.PANEL_BORDER, 1.5f * dp);

        paint.setStyle(Paint.Style.FILL);
        for (int ty = 0; ty < level.getHeight(); ty++) {
            for (int tx = 0; tx < level.getWidth(); tx++) {
                if (!level.isExplored(tx, ty)) continue;
                TileType tile = level.getTile(tx, ty);
                if (tile == TileType.WALL) {
                    paint.setColor(0xFF556888);
                } else if (tile == TileType.STAIRS_DOWN) {
                    paint.setColor(level.isStairsUnlocked() ? AndroidTheme.EMERALD : AndroidTheme.ROSE);
                } else if (tile.isWalkable()) {
                    paint.setColor(level.isVisible(tx, ty) ? 0xFF384866 : 0xFF212B3E);
                } else {
                    continue;
                }
                canvas.drawRect(
                        mapX + tx * mapScale,
                        mapY + ty * mapScale,
                        mapX + (tx + 1) * mapScale,
                        mapY + (ty + 1) * mapScale,
                        paint);
            }
        }
        for (Enemy e : engine.getEnemies()) {
            if (!e.isAlive()) continue;
            int ex = (int) Math.floor(e.getX());
            int ey = (int) Math.floor(e.getY());
            if (level.isVisible(ex, ey) || e.isBoss()) {
                paint.setColor(e.isBoss() ? AndroidTheme.ROSE : AndroidTheme.AMBER);
                canvas.drawCircle(mapX + (ex + 0.5f) * mapScale, mapY + (ey + 0.5f) * mapScale, e.isBoss() ? 3f * dp : 1.8f * dp, paint);
            }
        }
        paint.setColor(AndroidTheme.CYAN);
        canvas.drawCircle(mapX + (float) (player.getX() * mapScale), mapY + (float) (player.getY() * mapScale), 2.5f * dp, paint);

        // Touch Mute & Pause buttons directly below minimap
        float btnW = (mapW + 8f * dp - 6f * dp) * 0.5f;
        float btnH = 26f * dp;
        float btnY = rect.bottom + 6f * dp;
        muteButton.set(rect.left, btnY, rect.left + btnW, btnY + btnH);
        pauseButton.set(muteButton.right + 6f * dp, btnY, rect.right, btnY + btnH);
        AndroidTheme.drawButton(canvas, paint, muteButton, muted ? "SFX OFF" : "SFX ON", muted ? AndroidTheme.CRIMSON : AndroidTheme.EMERALD, true, 10.5f * dp);
        AndroidTheme.drawButton(canvas, paint, pauseButton, "PAUSE", AndroidTheme.CYAN, true, 10.5f * dp);

        // Boss bar safely centered between status card and minimap
        Enemy boss = engine.getActiveBoss();
        if (boss != null) {
            float leftBound = pad + cardW + 12f * dp;
            float rightBound = rect.left - 12f * dp;
            float avail = rightBound - leftBound;
            if (avail > 140f * dp) {
                float bw = Math.min(340f * dp, avail - 12f * dp);
                float cx = (leftBound + rightBound) * 0.5f;
                rect.set(cx - bw * 0.5f - 8f * dp, pad, cx + bw * 0.5f + 8f * dp, pad + 38f * dp);
                AndroidTheme.drawRoundedPanel(canvas, paint, rect, 10f * dp, AndroidTheme.PANEL_BG, AndroidTheme.ROSE, 1.5f * dp);
                boolean enraged = (boss instanceof CryptBossEnemy cb) && cb.isPhaseTwo();
                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(AndroidTheme.SANS_BOLD);
                paint.setTextSize(10.5f * dp);
                paint.setColor(enraged ? AndroidTheme.GOLD : AndroidTheme.ROSE);
                AndroidTheme.drawCenteredText(canvas, paint, enraged ? "CRYPT WARDEN — ENRAGED" : "CRYPT WARDEN — BOSS", cx, pad + 14f * dp);
                drawProgressBar(canvas, cx - bw * 0.5f, pad + 18f * dp, bw, 14f * dp, dp, (double) boss.getHp() / boss.getMaxHp(), AndroidTheme.ROSE, boss.getHp() + "/" + boss.getMaxHp());
            }
        }

        // Toast notification
        if (engine.getToastTimer() > 0 && !engine.getToastMessage().isBlank()) {
            String msg = engine.getToastMessage();
            paint.setTypeface(AndroidTheme.SANS_BOLD);
            paint.setTextSize(12f * dp);
            float tw = Math.min(w * 0.6f, Math.max(200f * dp, paint.measureText(msg) + 28f * dp));
            float ty = boss != null ? pad + 44f * dp : pad;
            rect.set(w * 0.5f - tw * 0.5f, ty, w * 0.5f + tw * 0.5f, ty + 28f * dp);
            AndroidTheme.drawRoundedPanel(canvas, paint, rect, 10f * dp, AndroidTheme.PANEL_BG, AndroidTheme.GOLD, 1.5f * dp);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(AndroidTheme.GOLD);
            AndroidTheme.drawCenteredText(canvas, paint, msg, w * 0.5f, ty + 19f * dp);
        }

        // Stairs prompt
        if (engine.isPlayerOnStairs()) {
            String msg = level.isStairsUnlocked()
                    ? "Tap [STAIRS] to Descend to Floor " + (player.getFloor() + 1)
                    : "Stairs Sealed — Defeat Crypt Warden!";
            int border = level.isStairsUnlocked() ? AndroidTheme.EMERALD : AndroidTheme.ROSE;
            float pw = 290f * dp;
            rect.set(w * 0.5f - pw * 0.5f, h - 62f * dp, w * 0.5f + pw * 0.5f, h - 32f * dp);
            AndroidTheme.drawRoundedPanel(canvas, paint, rect, 10f * dp, AndroidTheme.PANEL_BG, border, 1.5f * dp);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(AndroidTheme.SANS_BOLD);
            paint.setTextSize(11.5f * dp);
            paint.setColor(border);
            AndroidTheme.drawCenteredText(canvas, paint, msg, w * 0.5f, h - 42f * dp);
        }
    }

    private void drawProgressBar(
            Canvas canvas,
            float x,
            float y,
            float w,
            float h,
            float dp,
            double fraction,
            int fillColor,
            String label) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        rect.set(x, y, x + w, y + h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF161C2C);
        canvas.drawRoundRect(rect, 5f * dp, 5f * dp, paint);
        if (clamped > 0) {
            rect.set(x, y, x + Math.max(4f * dp, (float) (w * clamped)), y + h);
            paint.setColor(fillColor);
            canvas.drawRoundRect(rect, 5f * dp, 5f * dp, paint);
        }
        rect.set(x, y, x + w, y + h);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(0x30FFFFFF);
        canvas.drawRoundRect(rect, 5f * dp, 5f * dp, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(Math.max(8f * dp, h * 0.66f));
        paint.setColor(Color.WHITE);
        float ty = rect.centerY() - (paint.ascent() + paint.descent()) * 0.5f;
        AndroidTheme.drawCenteredText(canvas, paint, label, rect.centerX(), ty);
    }
}
