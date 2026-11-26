package com.college.shadowcrypt.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Centralized dark-fantasy color palette, typography, and drawing helpers for Swing/Java2D UI.
 */
public final class Theme {

    public static final Color BG_ABYSS = new Color(9, 12, 20);
    public static final Color FLOOR_DARK = new Color(25, 31, 46);
    public static final Color FLOOR_LIGHT = new Color(30, 38, 56);
    public static final Color CORRIDOR_TILE = new Color(21, 26, 39);
    public static final Color WALL_TOP = new Color(56, 70, 94);
    public static final Color WALL_FACE = new Color(34, 44, 62);
    public static final Color WALL_BORDER = new Color(78, 97, 128);
    public static final Color FOG_EXPLORED_VEIL = new Color(8, 11, 18, 175);

    public static final Color PANEL_BG = new Color(13, 18, 30, 226);
    public static final Color PANEL_BORDER = new Color(56, 72, 102);
    public static final Color PANEL_BORDER_BRIGHT = new Color(110, 140, 190);

    public static final Color TEXT_PRIMARY = new Color(241, 245, 249);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);
    public static final Color GOLD = new Color(250, 204, 21);
    public static final Color CRIMSON = new Color(239, 68, 68);
    public static final Color ROSE = new Color(244, 63, 94);
    public static final Color CYAN = new Color(56, 189, 248);
    public static final Color EMERALD = new Color(52, 211, 153);
    public static final Color VIOLET = new Color(192, 132, 252);
    public static final Color AMBER = new Color(251, 146, 60);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 44);
    public static final Font FONT_GAME_OVER = new Font("SansSerif", Font.BOLD, 30);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_SUBHEADING = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_TINY = new Font("SansSerif", Font.BOLD, 10);
    public static final Font FONT_MONO = new Font("Monospaced", Font.BOLD, 13);

    private Theme() {}

    public static void drawRoundedPanel(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            int arc,
            Color bg,
            Color border) {
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, arc, arc);
        if (border != null) {
            g.setColor(border);
            g.drawRoundRect(x, y, w, h, arc, arc);
        }
    }

    public static void drawButton(
            Graphics2D g,
            Rectangle bounds,
            String label,
            boolean hovered,
            Color accent,
            boolean enabled) {
        Color bg = !enabled
                ? new Color(24, 30, 44, 180)
                : hovered
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 75)
                        : new Color(22, 30, 46, 230);
        Color border = !enabled
                ? new Color(50, 60, 80)
                : hovered ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 165);

        drawRoundedPanel(g, bounds.x, bounds.y, bounds.width, bounds.height, 12, bg, border);
        g.setFont(FONT_SUBHEADING);
        g.setColor(enabled ? TEXT_PRIMARY : TEXT_MUTED);
        drawCenteredString(g, label, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 + 6);
    }

    public static void drawCenteredString(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(text);
        g.drawString(text, centerX - width / 2, baselineY);
    }
}
