package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Abstract base class for all living actors in the dungeon (Player and Enemies).
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected double x;
    protected double y;
    protected double radius;
    protected int hp;
    protected int maxHp;
    protected int attackDamage;
    protected double moveSpeed;
    protected double facingAngle;
    protected double hitFlashTimer;
    protected double knockbackVx;
    protected double knockbackVy;
    protected boolean alive = true;

    protected Entity(double x, double y, double radius, int maxHp, int attackDamage, double moveSpeed) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
        this.attackDamage = Math.max(1, attackDamage);
        this.moveSpeed = Math.max(0.5, moveSpeed);
    }

    public int takeDamage(int amount) {
        if (!alive || amount <= 0) {
            return 0;
        }
        int actual = Math.min(hp, amount);
        hp -= actual;
        hitFlashTimer = 0.14;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
        return actual;
    }

    public int heal(int amount) {
        if (!alive || amount <= 0 || hp >= maxHp) {
            return 0;
        }
        int actual = Math.min(maxHp - hp, amount);
        hp += actual;
        return actual;
    }

    public void applyKnockback(double kVx, double kVy) {
        this.knockbackVx += kVx;
        this.knockbackVy += kVy;
    }

    public void tickBaseTimers(double dt) {
        if (hitFlashTimer > 0) {
            hitFlashTimer = Math.max(0.0, hitFlashTimer - dt);
        }
        double decay = Math.max(0.0, 1.0 - 9.0 * dt);
        knockbackVx *= decay;
        knockbackVy *= decay;
        if (Math.hypot(knockbackVx, knockbackVy) < 0.08) {
            knockbackVx = 0;
            knockbackVy = 0;
        }
    }

    public double distanceTo(Entity other) {
        return Math.hypot(other.x - x, other.y - y);
    }

    public double distanceTo(double ox, double oy) {
        return Math.hypot(ox - x, oy - y);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getRadius() {
        return radius;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttackDamage() {
        return attackDamage;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public double getFacingAngle() {
        return facingAngle;
    }

    public void setFacingAngle(double facingAngle) {
        this.facingAngle = facingAngle;
    }

    public double getHitFlashTimer() {
        return hitFlashTimer;
    }

    public double getKnockbackVx() {
        return knockbackVx;
    }

    public double getKnockbackVy() {
        return knockbackVy;
    }

    public boolean isAlive() {
        return alive;
    }
}
