package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Moving projectile fired by the player's arcane bolt or ranged dungeon enemies.
 */
public final class Projectile implements Serializable {
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final double radius;
    private final int damage;
    private final boolean fromPlayer;
    private final int colorRgb;
    private double lifeTimer;
    private boolean expired;

    public Projectile(
            double x,
            double y,
            double vx,
            double vy,
            double radius,
            int damage,
            boolean fromPlayer,
            int colorRgb,
            double lifeSeconds) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.damage = damage;
        this.fromPlayer = fromPlayer;
        this.colorRgb = colorRgb;
        this.lifeTimer = lifeSeconds;
        this.expired = false;
    }

    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
        lifeTimer -= dt;
        if (lifeTimer <= 0) {
            expired = true;
        }
    }

    public void expire() {
        this.expired = true;
    }

    public boolean isExpired() {
        return expired;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getVx() {
        return vx;
    }

    public double getVy() {
        return vy;
    }

    public double getRadius() {
        return radius;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isFromPlayer() {
        return fromPlayer;
    }

    public int getColorRgb() {
        return colorRgb;
    }
}
