package com.college.shadowcrypt.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Dark-fantasy color palette, typography, and vector UI drawing utilities for Android Canvas.
 */
public final class AndroidTheme {

    public static final int BG_ABYSS = 0xFF090C14;
    public static final int FLOOR_DARK = 0xFF191F2E;
    public static final int FLOOR_LIGHT = 0xFF1E2638;
    public static final int CORRIDOR_TILE = 0xFF151A27;
    public static final int WALL_TOP = 0xFF38465E;
    public static final int WALL_FACE = 0xFF222C3E;
    public static final int WALL_BORDER = 0xFF4E6180;
    public static final int FOG_EXPLORED_VEIL = 0xB0080B12;

    public static final int PANEL_BG = 0xE20D121E;
    public static final int PANEL_BORDER = 0xFF384866;
    public static final int PANEL_BORDER_BRIGHT = 0xFF6E8CBE;

    public static final int TEXT_PRIMARY = 0xFFF1F5F9;
    public static final int TEXT_MUTED = 0xFF94A3B8;
    public static final int GOLD = 0xFFFACC15;
    public static final int CRIMSON = 0xFFEF4444;
    public static final int ROSE = 0xFFF43F5E;
    public static final int CYAN = 0xFF38BDF8;
    public static final int EMERALD = 0xFF34D399;
    public static final int VIOLET = 0xFFC084FC;
    public static final int AMBER = 0xFFFB923C;

    public static final Typeface SANS_NORMAL = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    public static final Typeface SANS_BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private AndroidTheme() {}

    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public static void drawRoundedPanel(
            Canvas canvas,
            Paint paint,
            RectF rect,
            float radius,
            int bgColor,
            int borderColor,
            float strokeWidth) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        canvas.drawRoundRect(rect, radius, radius, paint);
        if (borderColor != 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(borderColor);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
    }

    public static void drawButton(
            Canvas canvas,
            Paint paint,
            RectF rect,
            String label,
            int accent,
            boolean enabled,
            float textSizePx) {
        int bg = !enabled
                ? 0xB4181E2C
                : 0xE6161E2E;
        int border = !enabled
                ? 0xFF323C50
                : withAlpha(accent, 210);

        drawRoundedPanel(canvas, paint, rect, rect.height() * 0.25f, bg, border, 2.5f);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(SANS_BOLD);
        paint.setTextSize(textSizePx);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(enabled ? TEXT_PRIMARY : TEXT_MUTED);
        float textY = rect.centerY() - (paint.ascent() + paint.descent()) * 0.5f;
        canvas.drawText(label, rect.centerX(), textY, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    public static void drawCenteredText(Canvas canvas, Paint paint, String text, float cx, float baselineY) {
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, cx, baselineY, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}
