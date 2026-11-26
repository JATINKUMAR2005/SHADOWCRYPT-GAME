package com.college.shadowcrypt.android;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Renders the ShadowCrypt crest emblem directly onto Android Canvas for the title menu.
 */
public final class AndroidLogoRenderer {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shield = new Path();
    private final Path innerShield = new Path();
    private final Path blade = new Path();
    private final Path bladeGlow = new Path();
    private final Path hood = new Path();
    private final Path crown = new Path();
    private final Path eye = new Path();
    private final Path diamond = new Path();
    private final RectF oval = new RectF();

    public void drawCrest(Canvas canvas, float cx, float cy, float r) {
        // Outer halo
        float haloR = r * 1.24f;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(
                cx, cy, haloR,
                new int[]{0x4838BDF8, 0x18C084FC, 0x00090C14},
                new float[]{0f, 0.7f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, haloR, paint);
        paint.setShader(null);

        // Shield body
        buildShield(shield, cx, cy, r);
        paint.setShader(new LinearGradient(
                cx - r * 0.6f, cy - r,
                cx + r * 0.6f, cy + r,
                0xFF263452, 0xFF0E1422, Shader.TileMode.CLAMP));
        canvas.drawPath(shield, paint);
        paint.setShader(null);

        buildShield(innerShield, cx, cy, r * 0.86f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, r * 0.032f));
        paint.setColor(0x6438BDF8);
        canvas.drawPath(innerShield, paint);

        // Blade
        float bladeTop = cy - r * 0.72f;
        float guardY = cy - r * 0.44f;
        float bladeTip = cy + r * 0.82f;
        float halfBlade = r * 0.082f;

        paint.setStyle(Paint.Style.FILL);
        buildBlade(bladeGlow, cx, guardY, bladeTip + r * 0.05f, halfBlade * 1.65f);
        paint.setColor(0x4B38BDF8);
        canvas.drawPath(bladeGlow, paint);

        buildBlade(blade, cx, guardY, bladeTip, halfBlade);
        paint.setShader(new LinearGradient(
                cx - halfBlade, guardY,
                cx + halfBlade, bladeTip,
                0xFFE0F2FE, AndroidTheme.CYAN, Shader.TileMode.CLAMP));
        canvas.drawPath(blade, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, r * 0.024f));
        paint.setColor(0xD2FFFFFF);
        canvas.drawLine(cx, guardY + r * 0.05f, cx, bladeTip - r * 0.14f, paint);

        // Hooded shadow knight
        hood.reset();
        hood.moveTo(cx, cy - r * 0.24f);
        hood.cubicTo(cx + r * 0.42f, cy - r * 0.14f, cx + r * 0.46f, cy + r * 0.20f, cx + r * 0.34f, cy + r * 0.50f);
        hood.lineTo(cx, cy + r * 0.36f);
        hood.lineTo(cx - r * 0.34f, cy + r * 0.50f);
        hood.cubicTo(cx - r * 0.46f, cy + r * 0.20f, cx - r * 0.42f, cy - r * 0.14f, cx, cy - r * 0.24f);
        hood.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xF20A0E18);
        canvas.drawPath(hood, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, r * 0.03f));
        paint.setColor(0xA594A3B8);
        canvas.drawPath(hood, paint);

        // Spiked crown
        float crownBaseY = cy - r * 0.12f;
        crown.reset();
        crown.moveTo(cx - r * 0.22f, crownBaseY);
        crown.lineTo(cx - r * 0.28f, cy - r * 0.34f);
        crown.lineTo(cx - r * 0.10f, cy - r * 0.22f);
        crown.lineTo(cx, cy - r * 0.38f);
        crown.lineTo(cx + r * 0.10f, cy - r * 0.22f);
        crown.lineTo(cx + r * 0.28f, cy - r * 0.34f);
        crown.lineTo(cx + r * 0.22f, crownBaseY);
        crown.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(cx, cy - r * 0.38f, cx, crownBaseY, AndroidTheme.GOLD, AndroidTheme.ROSE, Shader.TileMode.CLAMP));
        canvas.drawPath(crown, paint);
        paint.setShader(null);

        // Angled golden eyes
        paint.setColor(AndroidTheme.GOLD);
        drawEye(canvas, cx - r * 0.24f, cy + r * 0.04f, cx - r * 0.09f, cy + r * 0.09f, cx - r * 0.11f, cy + r * 0.15f, cx - r * 0.23f, cy + r * 0.11f);
        drawEye(canvas, cx + r * 0.24f, cy + r * 0.04f, cx + r * 0.09f, cy + r * 0.09f, cx + r * 0.11f, cy + r * 0.15f, cx + r * 0.23f, cy + r * 0.11f);

        // Golden crossguard & hilt
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setShader(new LinearGradient(cx - r * 0.30f, guardY, cx + r * 0.30f, guardY, AndroidTheme.GOLD, 0xFFD97706, Shader.TileMode.CLAMP));
        paint.setStrokeWidth(Math.max(1.5f, r * 0.068f));
        canvas.drawLine(cx - r * 0.26f, guardY, cx + r * 0.26f, guardY, paint);
        paint.setStrokeWidth(Math.max(1.5f, r * 0.06f));
        canvas.drawLine(cx, bladeTop + r * 0.06f, cx, guardY, paint);
        paint.setStyle(Paint.Style.FILL);
        float pommelR = Math.max(2f, r * 0.075f);
        oval.set(cx - pommelR, bladeTop - pommelR * 0.4f, cx + pommelR, bladeTop + pommelR * 1.6f);
        canvas.drawOval(oval, paint);
        paint.setShader(null);

        // Gold outer border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.6f, r * 0.075f));
        paint.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xFFFDE047, 0xFFD97706, Shader.TileMode.CLAMP));
        canvas.drawPath(shield, paint);
        paint.setShader(null);

        // Side arcane diamonds
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AndroidTheme.CYAN);
        drawDiamond(canvas, cx - r * 0.58f, cy - r * 0.08f, r * 0.075f);
        drawDiamond(canvas, cx + r * 0.58f, cy - r * 0.08f, r * 0.075f);
    }

    private void buildShield(Path p, float cx, float cy, float r) {
        p.reset();
        p.moveTo(cx, cy - r * 0.88f);
        p.lineTo(cx + r * 0.80f, cy - r * 0.56f);
        p.cubicTo(cx + r * 0.82f, cy + r * 0.14f, cx + r * 0.54f, cy + r * 0.62f, cx, cy + r * 0.96f);
        p.cubicTo(cx - r * 0.54f, cy + r * 0.62f, cx - r * 0.82f, cy + r * 0.14f, cx - r * 0.80f, cy - r * 0.56f);
        p.close();
    }

    private void buildBlade(Path p, float cx, float topY, float tipY, float halfWidth) {
        p.reset();
        float shoulderY = topY + (tipY - topY) * 0.78f;
        p.moveTo(cx - halfWidth, topY);
        p.lineTo(cx + halfWidth, topY);
        p.lineTo(cx + halfWidth * 0.72f, shoulderY);
        p.lineTo(cx, tipY);
        p.lineTo(cx - halfWidth * 0.72f, shoulderY);
        p.close();
    }

    private void drawEye(Canvas canvas, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        eye.reset();
        eye.moveTo(x1, y1);
        eye.lineTo(x2, y2);
        eye.lineTo(x3, y3);
        eye.lineTo(x4, y4);
        eye.close();
        canvas.drawPath(eye, paint);
    }

    private void drawDiamond(Canvas canvas, float x, float y, float s) {
        diamond.reset();
        diamond.moveTo(x, y - s);
        diamond.lineTo(x + s, y);
        diamond.lineTo(x, y + s);
        diamond.lineTo(x - s, y);
        diamond.close();
        canvas.drawPath(diamond, paint);
    }
}
