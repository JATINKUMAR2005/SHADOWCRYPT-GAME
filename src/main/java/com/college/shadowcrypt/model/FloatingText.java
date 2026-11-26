package com.college.shadowcrypt.model;

/**
 * Floating world-space combat or pickup text that rises and fades out.
 */
public final class FloatingText {
    private double x;
    private double y;
    private final String text;
    private final int colorRgb;
    private double lifeTimer;
    private final double maxLife;
    private final boolean large;

    public FloatingText(double x, double y, String text, int colorRgb, double maxLife, boolean large) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.colorRgb = colorRgb;
        this.lifeTimer = maxLife;
        this.maxLife = maxLife;
        this.large = large;
    }

    public void update(double dt) {
        y -= 0.75 * dt;
        lifeTimer -= dt;
    }

    public boolean isExpired() {
        return lifeTimer <= 0;
    }

    public float alpha() {
        return (float) Math.max(0.0, Math.min(1.0, lifeTimer / maxLife));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getText() {
        return text;
    }

    public int getColorRgb() {
        return colorRgb;
    }

    public boolean isLarge() {
        return large;
    }
}
