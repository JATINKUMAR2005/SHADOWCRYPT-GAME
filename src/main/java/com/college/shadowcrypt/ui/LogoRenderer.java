package com.college.shadowcrypt.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MultipleGradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Pure Java vector renderer for the ShadowCrypt crest emblem, generating crisp
 * multi-resolution window/taskbar icons and in-game title artwork.
 */
public final class LogoRenderer {

    private LogoRenderer() {}

    public static List<Image> createWindowIcons() {
        return List.of(
                renderIconImage(16, true),
                renderIconImage(32, true),
                renderIconImage(64, true),
                renderIconImage(128, true),
                renderIconImage(256, true));
    }

    public static BufferedImage renderIconImage(int size, boolean roundedBadge) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (roundedBadge) {
            int arc = Math.max(4, (int) Math.round(size * 0.22));
            g.setPaint(new RadialGradientPaint(
                    size * 0.5f,
                    size * 0.45f,
                    size * 0.65f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(22, 33, 56), new Color(9, 12, 20)}));
            g.fillRoundRect(0, 0, size, size, arc, arc);
            g.setColor(new Color(56, 189, 248, 105));
            g.setStroke(new BasicStroke(Math.max(1f, size * 0.025f)));
            g.drawRoundRect(1, 1, size - 2, size - 2, arc, arc);
            drawCrest(g, size * 0.5, size * 0.5, size * 0.39);
        } else {
            drawCrest(g, size * 0.5, size * 0.5, size * 0.34);
        }

        g.dispose();
        return img;
    }

    /**
     * Draws the ShadowCrypt crest emblem centered at (cx, cy) with outer radius r.
     */
    public static void drawCrest(Graphics2D g, double cx, double cy, double r) {
        Stroke prevStroke = g.getStroke();

        // 1. Subtle arcane cyan outer halo
        float haloR = (float) (r * 1.24);
        g.setPaint(new RadialGradientPaint(
                (float) cx,
                (float) cy,
                haloR,
                new float[]{0f, 0.7f, 1f},
                new Color[]{
                    new Color(56, 189, 248, 72),
                    new Color(192, 132, 252, 24),
                    new Color(9, 12, 20, 0)
                },
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillOval((int) Math.round(cx - haloR), (int) Math.round(cy - haloR),
                Math.round(haloR * 2), Math.round(haloR * 2));

        // 2. Shield silhouette
        Path2D shield = createShieldPath(cx, cy, r);
        g.setPaint(new GradientPaint(
                (float) (cx - r * 0.6), (float) (cy - r),
                new Color(38, 52, 82),
                (float) (cx + r * 0.6), (float) (cy + r),
                new Color(14, 20, 34)));
        g.fill(shield);

        // Inner shield trim
        Path2D innerShield = createShieldPath(cx, cy, r * 0.86);
        g.setColor(new Color(56, 189, 248, 100));
        g.setStroke(new BasicStroke((float) Math.max(1.0, r * 0.032), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(innerShield);

        // 3. Vertical Runic Sword spanning inside shield
        double bladeTop = cy - r * 0.72;
        double guardY = cy - r * 0.44;
        double bladeTip = cy + r * 0.82;
        double halfBlade = r * 0.082;

        // Tapered cyan aura matching blade silhouette
        Path2D bladeGlow = createBladePath(cx, guardY, bladeTip + r * 0.05, halfBlade * 1.65);
        g.setColor(new Color(56, 189, 248, 75));
        g.fill(bladeGlow);

        Path2D blade = createBladePath(cx, guardY, bladeTip, halfBlade);
        g.setPaint(new GradientPaint(
                (float) (cx - halfBlade), (float) guardY,
                new Color(224, 242, 254),
                (float) (cx + halfBlade), (float) bladeTip,
                new Color(56, 189, 248)));
        g.fill(blade);

        // Center fuller highlight
        g.setColor(new Color(255, 255, 255, 210));
        g.setStroke(new BasicStroke((float) Math.max(1.0, r * 0.024)));
        g.drawLine((int) Math.round(cx), (int) Math.round(guardY + r * 0.05),
                (int) Math.round(cx), (int) Math.round(bladeTip - r * 0.14));

        // 4. Hooded Shadow Knight Silhouette + Crown (cleanly below crossguard)
        Path2D hood = new Path2D.Double();
        hood.moveTo(cx, cy - r * 0.24);
        hood.curveTo(cx + r * 0.42, cy - r * 0.14, cx + r * 0.46, cy + r * 0.20, cx + r * 0.34, cy + r * 0.50);
        hood.lineTo(cx, cy + r * 0.36);
        hood.lineTo(cx - r * 0.34, cy + r * 0.50);
        hood.curveTo(cx - r * 0.46, cy + r * 0.20, cx - r * 0.42, cy - r * 0.14, cx, cy - r * 0.24);
        hood.closePath();
        g.setColor(new Color(10, 14, 24, 242));
        g.fill(hood);
        g.setColor(new Color(148, 163, 184, 165));
        g.setStroke(new BasicStroke((float) Math.max(1.0, r * 0.03)));
        g.draw(hood);

        // Spiked crimson-gold crown sitting neatly atop the hood
        double crownBaseY = cy - r * 0.12;
        Path2D crown = new Path2D.Double();
        crown.moveTo(cx - r * 0.22, crownBaseY);
        crown.lineTo(cx - r * 0.28, cy - r * 0.34);
        crown.lineTo(cx - r * 0.10, cy - r * 0.22);
        crown.lineTo(cx, cy - r * 0.38);
        crown.lineTo(cx + r * 0.10, cy - r * 0.22);
        crown.lineTo(cx + r * 0.28, cy - r * 0.34);
        crown.lineTo(cx + r * 0.22, crownBaseY);
        crown.closePath();
        g.setPaint(new GradientPaint(
                (float) cx, (float) (cy - r * 0.38),
                Theme.GOLD,
                (float) cx, (float) crownBaseY,
                new Color(244, 63, 94)));
        g.fill(crown);

        // Fierce angled glowing golden visor eyes
        g.setColor(Theme.GOLD);
        Polygon leftEye = new Polygon(
                new int[]{
                    (int) Math.round(cx - r * 0.24),
                    (int) Math.round(cx - r * 0.09),
                    (int) Math.round(cx - r * 0.11),
                    (int) Math.round(cx - r * 0.23)
                },
                new int[]{
                    (int) Math.round(cy + r * 0.04),
                    (int) Math.round(cy + r * 0.09),
                    (int) Math.round(cy + r * 0.15),
                    (int) Math.round(cy + r * 0.11)
                },
                4);
        Polygon rightEye = new Polygon(
                new int[]{
                    (int) Math.round(cx + r * 0.24),
                    (int) Math.round(cx + r * 0.09),
                    (int) Math.round(cx + r * 0.11),
                    (int) Math.round(cx + r * 0.23)
                },
                new int[]{
                    (int) Math.round(cy + r * 0.04),
                    (int) Math.round(cy + r * 0.09),
                    (int) Math.round(cy + r * 0.15),
                    (int) Math.round(cy + r * 0.11)
                },
                4);
        g.fill(leftEye);
        g.fill(rightEye);

        // 5. Golden Sword Crossguard & Pommel cleanly above crown
        g.setPaint(new GradientPaint(
                (float) (cx - r * 0.30), (float) guardY,
                Theme.GOLD,
                (float) (cx + r * 0.30), (float) guardY,
                new Color(217, 119, 6)));
        g.setStroke(new BasicStroke((float) Math.max(1.5, r * 0.068), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) Math.round(cx - r * 0.26), (int) Math.round(guardY),
                (int) Math.round(cx + r * 0.26), (int) Math.round(guardY));

        g.setStroke(new BasicStroke((float) Math.max(1.5, r * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) Math.round(cx), (int) Math.round(bladeTop + r * 0.06),
                (int) Math.round(cx), (int) Math.round(guardY));
        double pommelR = Math.max(2.0, r * 0.075);
        g.fillOval((int) Math.round(cx - pommelR), (int) Math.round(bladeTop - pommelR * 0.4),
                (int) Math.round(pommelR * 2), (int) Math.round(pommelR * 2));

        // 6. Crisp beveled gold outer border on shield
        g.setPaint(new GradientPaint(
                (float) (cx - r), (float) (cy - r),
                new Color(253, 224, 71),
                (float) (cx + r), (float) (cy + r),
                new Color(217, 119, 6)));
        g.setStroke(new BasicStroke((float) Math.max(1.6, r * 0.075), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shield);

        // 7. Side arcane diamond accents
        drawDiamond(g, cx - r * 0.58, cy - r * 0.08, r * 0.075, Theme.CYAN);
        drawDiamond(g, cx + r * 0.58, cy - r * 0.08, r * 0.075, Theme.CYAN);

        g.setStroke(prevStroke);
    }

    private static Path2D createShieldPath(double cx, double cy, double r) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - r * 0.88);
        p.lineTo(cx + r * 0.80, cy - r * 0.56);
        p.curveTo(cx + r * 0.82, cy + r * 0.14, cx + r * 0.54, cy + r * 0.62, cx, cy + r * 0.96);
        p.curveTo(cx - r * 0.54, cy + r * 0.62, cx - r * 0.82, cy + r * 0.14, cx - r * 0.80, cy - r * 0.56);
        p.closePath();
        return p;
    }

    private static Path2D createBladePath(double cx, double topY, double tipY, double halfWidth) {
        Path2D blade = new Path2D.Double();
        double shoulderY = topY + (tipY - topY) * 0.78;
        blade.moveTo(cx - halfWidth, topY);
        blade.lineTo(cx + halfWidth, topY);
        blade.lineTo(cx + halfWidth * 0.72, shoulderY);
        blade.lineTo(cx, tipY);
        blade.lineTo(cx - halfWidth * 0.72, shoulderY);
        blade.closePath();
        return blade;
    }

    private static void drawDiamond(Graphics2D g, double x, double y, double s, Color color) {
        Polygon d = new Polygon(
                new int[]{(int) Math.round(x), (int) Math.round(x + s), (int) Math.round(x), (int) Math.round(x - s)},
                new int[]{(int) Math.round(y - s), (int) Math.round(y), (int) Math.round(y + s), (int) Math.round(y)},
                4);
        g.setColor(color);
        g.fill(d);
    }
}
