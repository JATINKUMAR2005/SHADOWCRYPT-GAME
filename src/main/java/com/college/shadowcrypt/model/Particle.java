package com.college.shadowcrypt.model;

/**
 * Lightweight visual particle emitted on impacts, spells, dashes, and loot pickups.
 */
public final class Particle {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private final double size;
    private final int colorRgb;
    private double lifeTimer;
    private final double maxLife;

    public Particle(
            double x,
            double y,
            double vx,
            double vy,
            double size,
            int colorRgb,
            double maxLife) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.colorRgb = colorRgb;
        this.lifeTimer = maxLife;
        this.maxLife = maxLife;
    }

    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        double damping = Math.max(0.0, 1.0 - 3.5 * dt);
        vx *= damping;
        vy *= damping;
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

    public double getSize() {
        return size;
    }

    public int getColorRgb() {
        return colorRgb;
    }
}
