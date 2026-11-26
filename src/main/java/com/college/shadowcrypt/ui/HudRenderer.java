package com.college.shadowcrypt.ui;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.model.CryptBossEnemy;
import com.college.shadowcrypt.model.DungeonLevel;
import com.college.shadowcrypt.model.Enemy;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.TileType;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Renders the gameplay HUD: player stats card, ability cooldowns, boss health bar,
 * live dungeon minimap, stair interaction prompts, and toast notifications.
 */
public final class HudRenderer {

    private HudRenderer() {}

    public static void renderHud(
            Graphics2D g,
            GameEngine engine,
            int viewWidth,
            int viewHeight,
            boolean muted) {
        Player player = engine.getPlayer();
        DungeonLevel level = engine.getLevel();
        if (player == null || level == null) {
            return;
        }

        drawPlayerStatusPanel(g, engine, player, muted);
        drawBossBarIfActive(g, engine, viewWidth);
        drawMinimap(g, engine, level, player, viewWidth);
        drawPromptsAndToast(g, engine, player, level, viewWidth, viewHeight);
    }

    private static void drawPlayerStatusPanel(
            Graphics2D g,
            GameEngine engine,
            Player player,
            boolean muted) {
        int x = 16;
        int y = 16;
        int w = 296;
        int h = 152;
        Theme.drawRoundedPanel(g, x, y, w, h, 14, Theme.PANEL_BG, Theme.PANEL_BORDER);

        // Header line: Name, Level, Floor, Difficulty
        g.setFont(Theme.FONT_BODY_BOLD);
        g.setColor(Theme.TEXT_PRIMARY);
        g.drawString(player.getName() + "  (Lv." + player.getLevel() + ")", x + 14, y + 24);

        g.setFont(Theme.FONT_SMALL_BOLD);
        g.setColor(Theme.GOLD);
        String floorLabel = "Floor " + player.getFloor() + " • " + engine.getDifficulty().getLabel();
        g.drawString(floorLabel, x + w - g.getFontMetrics().stringWidth(floorLabel) - 14, y + 24);

        // HP Bar
        int barX = x + 14;
        int barW = w - 28;
        drawProgressBar(
                g,
                barX,
                y + 34,
                barW,
                18,
                (double) player.getHp() / player.getMaxHp(),
                Theme.CRIMSON,
                "HP " + player.getHp() + " / " + player.getMaxHp());

        // XP Bar
        drawProgressBar(
                g,
                barX,
                y + 58,
                barW,
                12,
                (double) player.getXp() / player.getXpToNext(),
                Theme.VIOLET,
                "XP " + player.getXp() + "/" + player.getXpToNext());

        // Stats row
        g.setFont(Theme.FONT_SMALL_BOLD);
        g.setColor(new Color(player.getWeapon().getTrailColorRgb()));
        g.drawString(player.getWeapon().getDisplayName() + " (" + player.getEffectiveAttackDamage() + " DMG)", barX, y + 90);

        g.setColor(Theme.TEXT_PRIMARY);
        g.drawString("Score: " + player.getScore() + "   Kills: " + player.getKills() + "   Gold: " + player.getGold(), barX, y + 110);

        // Flasks, Shields, Cooldowns, Sound badge
        StringBuilder statusLine = new StringBuilder();
        statusLine.append("[Q] Flasks: ").append(player.getPotions());
        if (player.getShieldCharges() > 0) {
            statusLine.append("  |  Aegis: ").append(player.getShieldCharges());
        }
        statusLine.append("  |  ").append(muted ? "Sound: OFF" : "Sound: ON");
        g.setColor(Theme.EMERALD);
        g.drawString(statusLine.toString(), barX, y + 129);

        // Active buffs & cooldown pills below card
        int pillY = y + h + 8;
        int pillX = x;
        if (player.getDashCooldown() > 0) {
            pillX += drawPill(g, pillX, pillY, String.format("Dash %.1fs", player.getDashCooldown()), Theme.TEXT_MUTED);
        } else {
            pillX += drawPill(g, pillX, pillY, "Dash READY", Theme.CYAN);
        }
        if (player.getRangedCooldown() > 0) {
            pillX += drawPill(g, pillX, pillY, String.format("Bolt %.1fs", player.getRangedCooldown()), Theme.TEXT_MUTED);
        } else {
            pillX += drawPill(g, pillX, pillY, "Bolt READY", Theme.CYAN);
        }
        if (player.getPowerBoostTimer() > 0) {
            pillX += drawPill(g, pillX, pillY, String.format("Fury %.0fs", player.getPowerBoostTimer()), Theme.AMBER);
        }
        if (player.getSpeedBoostTimer() > 0) {
            drawPill(g, pillX, pillY, String.format("Haste %.0fs", player.getSpeedBoostTimer()), Theme.EMERALD);
        }
    }

    private static int drawPill(Graphics2D g, int x, int y, String text, Color color) {
        g.setFont(Theme.FONT_SMALL_BOLD);
        int textW = g.getFontMetrics().stringWidth(text);
        int pw = textW + 16;
        Theme.drawRoundedPanel(g, x, y, pw, 22, 10, Theme.PANEL_BG, color);
        g.setColor(color);
        g.drawString(text, x + 8, y + 15);
        return pw + 6;
    }

    private static void drawProgressBar(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            double fraction,
            Color fillColor,
            String label) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        g.setColor(new Color(22, 28, 44));
        g.fillRoundRect(x, y, w, h, 8, 8);
        if (clamped > 0) {
            g.setColor(fillColor);
            g.fillRoundRect(x, y, Math.max(4, (int) Math.round(w * clamped)), h, 8, 8);
        }
        g.setColor(new Color(255, 255, 255, 45));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(h >= 16 ? Theme.FONT_SMALL_BOLD : Theme.FONT_TINY);
        g.setColor(Color.WHITE);
        Theme.drawCenteredString(g, label, x + w / 2, y + h - (h >= 16 ? 4 : 2));
    }

    private static void drawBossBarIfActive(Graphics2D g, GameEngine engine, int viewWidth) {
        Enemy boss = engine.getActiveBoss();
        DungeonLevel level = engine.getLevel();
        if (boss == null || level == null) {
            return;
        }
        int leftBound = 326;
        int rightBound = viewWidth - (level.getWidth() * 4 + 42);
        int available = rightBound - leftBound;
        int barW = Math.max(200, Math.min(440, available - 20));
        int centerX = available >= 220 ? (leftBound + rightBound) / 2 : viewWidth / 2;
        int x = centerX - barW / 2;
        int y = 18;
        Theme.drawRoundedPanel(g, x - 10, y - 4, barW + 20, 48, 12, Theme.PANEL_BG, Theme.ROSE);

        boolean enraged = (boss instanceof CryptBossEnemy cryptBoss) && cryptBoss.isPhaseTwo();
        g.setFont(Theme.FONT_SMALL_BOLD);
        g.setColor(enraged ? Theme.GOLD : Theme.ROSE);
        String title = enraged ? "CRYPT WARDEN  —  PHASE II ENRAGED" : "CRYPT WARDEN  —  FLOOR BOSS";
        Theme.drawCenteredString(g, title, centerX, y + 12);

        drawProgressBar(
                g,
                x,
                y + 18,
                barW,
                16,
                (double) boss.getHp() / boss.getMaxHp(),
                Theme.ROSE,
                boss.getHp() + " / " + boss.getMaxHp());
    }

    private static void drawMinimap(
            Graphics2D g,
            GameEngine engine,
            DungeonLevel level,
            Player player,
            int viewWidth) {
        int scale = 4;
        int mapW = level.getWidth() * scale;
        int mapH = level.getHeight() * scale;
        int x = viewWidth - mapW - 22;
        int y = 16;

        Theme.drawRoundedPanel(g, x - 6, y - 6, mapW + 12, mapH + 24, 12, Theme.PANEL_BG, Theme.PANEL_BORDER);

        for (int ty = 0; ty < level.getHeight(); ty++) {
            for (int tx = 0; tx < level.getWidth(); tx++) {
                if (!level.isExplored(tx, ty)) {
                    continue;
                }
                TileType tile = level.getTile(tx, ty);
                if (tile == TileType.WALL) {
                    g.setColor(new Color(85, 104, 136));
                } else if (tile == TileType.STAIRS_DOWN) {
                    g.setColor(level.isStairsUnlocked() ? Theme.EMERALD : Theme.ROSE);
                } else if (tile.isWalkable()) {
                    g.setColor(level.isVisible(tx, ty) ? new Color(56, 72, 102) : new Color(33, 43, 62));
                } else {
                    continue;
                }
                g.fillRect(x + tx * scale, y + ty * scale, scale, scale);
            }
        }

        // Visible enemies on minimap
        for (Enemy e : engine.getEnemies()) {
            if (!e.isAlive()) continue;
            int ex = (int) Math.floor(e.getX());
            int ey = (int) Math.floor(e.getY());
            if (level.isVisible(ex, ey) || e.isBoss()) {
                g.setColor(e.isBoss() ? Theme.ROSE : Theme.AMBER);
                int dot = e.isBoss() ? 6 : 4;
                g.fillOval(x + ex * scale - dot / 2 + 2, y + ey * scale - dot / 2 + 2, dot, dot);
            }
        }

        // Player dot
        int px = (int) Math.round(player.getX() * scale);
        int py = (int) Math.round(player.getY() * scale);
        g.setColor(Theme.CYAN);
        g.fillOval(x + px - 3, y + py - 3, 6, 6);

        g.setFont(Theme.FONT_TINY);
        g.setColor(Theme.TEXT_MUTED);
        Theme.drawCenteredString(g, "MINIMAP  •  [Esc] Pause", x + mapW / 2, y + mapH + 13);
    }

    private static void drawPromptsAndToast(
            Graphics2D g,
            GameEngine engine,
            Player player,
            DungeonLevel level,
            int viewWidth,
            int viewHeight) {
        if (engine.isPlayerOnStairs()) {
            String prompt = level.isStairsUnlocked()
                    ? "Press [E] to Descend to Floor " + (player.getFloor() + 1)
                    : "Stairs Sealed — Defeat the Crypt Warden!";
            Color border = level.isStairsUnlocked() ? Theme.EMERALD : Theme.ROSE;
            int pw = 360;
            int px = viewWidth / 2 - pw / 2;
            int py = viewHeight - 96;
            Theme.drawRoundedPanel(g, px, py, pw, 36, 12, Theme.PANEL_BG, border);
            g.setFont(Theme.FONT_BODY_BOLD);
            g.setColor(border);
            Theme.drawCenteredString(g, prompt, viewWidth / 2, py + 23);
        }

        if (engine.getToastTimer() > 0 && !engine.getToastMessage().isBlank()) {
            String msg = engine.getToastMessage();
            g.setFont(Theme.FONT_BODY_BOLD);
            int tw = Math.max(280, g.getFontMetrics().stringWidth(msg) + 40);
            int tx = viewWidth / 2 - tw / 2;
            int ty = 78;
            Theme.drawRoundedPanel(g, tx, ty, tw, 34, 12, Theme.PANEL_BG, Theme.GOLD);
            g.setColor(Theme.GOLD);
            Theme.drawCenteredString(g, msg, viewWidth / 2, ty + 22);
        }

        // Bottom control strip
        String controls = viewWidth >= 980
                ? "WASD/Arrows: Move  |  LMB / Space: Slash  |  RMB / F: Arcane Bolt  |  Shift: Dash  |  Q: Potion  |  F5: Save  |  M: Mute"
                : "WASD: Move | LMB/Space: Slash | RMB/F: Bolt | Shift: Dash | Q: Heal | F5: Save | M: Mute";
        g.setFont(Theme.FONT_SMALL);
        int cw = g.getFontMetrics().stringWidth(controls) + 28;
        int cx = viewWidth / 2 - cw / 2;
        int cy = viewHeight - 36;
        Theme.drawRoundedPanel(g, cx, cy, cw, 24, 8, new Color(11, 15, 25, 200), Theme.PANEL_BORDER);
        g.setColor(Theme.TEXT_MUTED);
        Theme.drawCenteredString(g, controls, viewWidth / 2, cy + 16);
    }
}
