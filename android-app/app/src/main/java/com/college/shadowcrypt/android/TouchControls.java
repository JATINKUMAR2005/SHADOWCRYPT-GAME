package com.college.shadowcrypt.android;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.engine.InputState;
import com.college.shadowcrypt.model.Player;

/**
 * Multi-touch virtual analog thumbstick and combat action buttons for mobile gameplay.
 */
public final class TouchControls {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private int joystickPointerId = -1;
    private int attackPointerId = -1;

    private float joyCenterX;
    private float joyCenterY;
    private float joyBaseRadius;
    private float joyKnobX;
    private float joyKnobY;

    private float slashX;
    private float slashY;
    private float slashR;
    private float boltX;
    private float boltY;
    private float boltR;
    private float dashX;
    private float dashY;
    private float dashR;
    private float flaskX;
    private float flaskY;
    private float flaskR;
    private float stairsX;
    private float stairsY;
    private float stairsR;

    public void layout(int w, int h, float dp) {
        joyBaseRadius = 50f * dp;
        joyCenterX = 76f * dp;
        joyCenterY = h - 76f * dp;
        if (joystickPointerId == -1) {
            joyKnobX = joyCenterX;
            joyKnobY = joyCenterY;
        }

        slashR = 36f * dp;
        slashX = w - 64f * dp;
        slashY = h - 66f * dp;

        boltR = 26f * dp;
        boltX = slashX - 72f * dp;
        boltY = slashY + 6f * dp;

        dashR = 26f * dp;
        dashX = slashX - 54f * dp;
        dashY = slashY - 64f * dp;

        flaskR = 23f * dp;
        flaskX = slashX + 8f * dp;
        flaskY = slashY - 76f * dp;

        stairsR = 27f * dp;
        stairsX = boltX - 66f * dp;
        stairsY = boltY - 8f * dp;
    }

    public boolean handleTouch(
            MotionEvent event,
            InputState input,
            GameEngine engine,
            AndroidHudRenderer hud,
            Runnable onMuteTap,
            Runnable onPauseTap) {
        int action = event.getActionMasked();
        int idx = event.getActionIndex();
        int pid = event.getPointerId(idx);
        float px = event.getX(idx);
        float py = event.getY(idx);

        switch (action) {
            case MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (hud.muteButton.contains(px, py)) {
                    onMuteTap.run();
                    return true;
                }
                if (hud.pauseButton.contains(px, py)) {
                    onPauseTap.run();
                    return true;
                }
                if (dist(px, py, slashX, slashY) <= slashR * 1.25f) {
                    attackPointerId = pid;
                    input.attackHeld = true;
                    return true;
                }
                if (dist(px, py, boltX, boltY) <= boltR * 1.25f) {
                    input.rangedPressed = true;
                    return true;
                }
                if (dist(px, py, dashX, dashY) <= dashR * 1.25f) {
                    input.dashPressed = true;
                    return true;
                }
                if (dist(px, py, flaskX, flaskY) <= flaskR * 1.30f) {
                    input.potionPressed = true;
                    return true;
                }
                if (engine.isPlayerOnStairs() && dist(px, py, stairsX, stairsY) <= stairsR * 1.35f) {
                    input.interactPressed = true;
                    return true;
                }
                if (joystickPointerId == -1 && px < hud.muteButton.left && dist(px, py, joyCenterX, joyCenterY) <= joyBaseRadius * 2.1f) {
                    joystickPointerId = pid;
                    updateJoystick(px, py, input);
                    return true;
                }
            }
            case MotionEvent.ACTION_MOVE -> {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (event.getPointerId(i) == joystickPointerId) {
                        updateJoystick(event.getX(i), event.getY(i), input);
                    }
                }
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (pid == joystickPointerId || action == MotionEvent.ACTION_CANCEL) {
                    joystickPointerId = -1;
                    joyKnobX = joyCenterX;
                    joyKnobY = joyCenterY;
                    input.analogX = 0.0;
                    input.analogY = 0.0;
                }
                if (pid == attackPointerId || action == MotionEvent.ACTION_CANCEL) {
                    attackPointerId = -1;
                    input.attackHeld = false;
                }
            }
        }
        return true;
    }

    private void updateJoystick(float px, float py, InputState input) {
        float dx = px - joyCenterX;
        float dy = py - joyCenterY;
        float d = (float) Math.hypot(dx, dy);
        if (d > joyBaseRadius) {
            dx = (dx / d) * joyBaseRadius;
            dy = (dy / d) * joyBaseRadius;
        }
        joyKnobX = joyCenterX + dx;
        joyKnobY = joyCenterY + dy;
        input.analogX = dx / joyBaseRadius;
        input.analogY = dy / joyBaseRadius;
    }

    public void reset(InputState input) {
        joystickPointerId = -1;
        attackPointerId = -1;
        joyKnobX = joyCenterX;
        joyKnobY = joyCenterY;
        input.resetAll();
    }

    public void render(Canvas canvas, GameEngine engine, float dp) {
        Player p = engine.getPlayer();
        if (p == null) {
            return;
        }

        // Virtual joystick base & thumb knob
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x50162033);
        canvas.drawCircle(joyCenterX, joyCenterY, joyBaseRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * dp * 0.5f);
        paint.setColor(0x9038BDF8);
        canvas.drawCircle(joyCenterX, joyCenterY, joyBaseRadius, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(joystickPointerId != -1 ? 0xCC38BDF8 : 0x99CBD5E1);
        canvas.drawCircle(joyKnobX, joyKnobY, joyBaseRadius * 0.42f, paint);

        // Action buttons
        drawActionButton(canvas, slashX, slashY, slashR, "SLASH", AndroidTheme.GOLD, 0.0, dp, attackPointerId != -1);
        drawActionButton(canvas, boltX, boltY, boltR, "BOLT", AndroidTheme.CYAN, p.getRangedCooldown() / 0.85, dp, false);
        drawActionButton(canvas, dashX, dashY, dashR, "DASH", AndroidTheme.EMERALD, p.getDashCooldown() / 1.30, dp, false);
        drawActionButton(canvas, flaskX, flaskY, flaskR, "HP x" + p.getPotions(), AndroidTheme.CRIMSON, 0.0, dp, false);

        if (engine.isPlayerOnStairs()) {
            int col = engine.getLevel().isStairsUnlocked() ? AndroidTheme.EMERALD : AndroidTheme.ROSE;
            drawActionButton(canvas, stairsX, stairsY, stairsR, "STAIRS", col, 0.0, dp, false);
        }
    }

    private void drawActionButton(
            Canvas canvas,
            float cx,
            float cy,
            float r,
            String label,
            int accent,
            double cooldownFrac,
            float dp,
            boolean active) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? AndroidTheme.withAlpha(accent, 110) : 0x99162033);
        canvas.drawCircle(cx, cy, r, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.2f * dp * 0.6f);
        paint.setColor(cooldownFrac > 0.02 ? AndroidTheme.TEXT_MUTED : accent);
        canvas.drawCircle(cx, cy, r, paint);

        if (cooldownFrac > 0.02) {
            paint.setColor(AndroidTheme.withAlpha(accent, 140));
            oval.set(cx - r + 2f, cy - r + 2f, cx + r - 2f, cy + r - 2f);
            canvas.drawArc(oval, -90f, (float) (360.0 * (1.0 - Math.min(1.0, cooldownFrac))), false, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(r * 0.36f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(cooldownFrac > 0.02 ? AndroidTheme.TEXT_MUTED : Color.WHITE);
        float textY = cy - (paint.ascent() + paint.descent()) * 0.5f;
        canvas.drawText(label, cx, textY, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }
}
