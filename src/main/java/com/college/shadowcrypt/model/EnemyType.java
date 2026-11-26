package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Archetype metadata for dungeon enemies and floor bosses.
 */
public enum EnemyType implements Serializable {
    SLIME("Mire Slime", 48, 12, 2.35, 0.34, 22, 100, 0x4ADE80),
    SKELETON_ARCHER("Bone Marksman", 56, 14, 2.85, 0.33, 32, 140, 0xE2E8F0),
    SHADOW_STALKER("Crypt Stalker", 72, 18, 3.60, 0.34, 44, 185, 0xA855F7),
    CRYPT_WARDEN_BOSS("Crypt Warden", 280, 24, 2.75, 0.56, 160, 650, 0xF43F5E);

    private final String displayName;
    private final int baseHp;
    private final int baseDamage;
    private final double baseSpeed;
    private final double radius;
    private final int xpReward;
    private final int scoreReward;
    private final int colorRgb;

    EnemyType(
            String displayName,
            int baseHp,
            int baseDamage,
            double baseSpeed,
            double radius,
            int xpReward,
            int scoreReward,
            int colorRgb) {
        this.displayName = displayName;
        this.baseHp = baseHp;
        this.baseDamage = baseDamage;
        this.baseSpeed = baseSpeed;
        this.radius = radius;
        this.xpReward = xpReward;
        this.scoreReward = scoreReward;
        this.colorRgb = colorRgb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public double getRadius() {
        return radius;
    }

    public int getXpReward() {
        return xpReward;
    }

    public int getScoreReward() {
        return scoreReward;
    }

    public int getColorRgb() {
        return colorRgb;
    }
}
