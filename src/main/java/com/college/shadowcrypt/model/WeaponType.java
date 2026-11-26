package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Melee weapon progression tiers unlocked from dungeon chests and boss rewards.
 */
public enum WeaponType implements Serializable {
    RUSTED_BLADE("Iron Shortsword", 22, 1.65, 0.32, 0.08, 0xCBD5E1),
    RUNE_SCIMITAR("Runed Scimitar", 30, 1.85, 0.27, 0.14, 0x38BDF8),
    SUNFIRE_GREATSWORD("Sunfire Blade", 40, 2.15, 0.30, 0.20, 0xFBBF24),
    SHADOWBANE_EDGE("Shadowbane Edge", 54, 2.35, 0.24, 0.28, 0xC084FC);

    private final String displayName;
    private final int baseDamage;
    private final double reach;
    private final double cooldownSeconds;
    private final double critChance;
    private final int trailColorRgb;

    WeaponType(
            String displayName,
            int baseDamage,
            double reach,
            double cooldownSeconds,
            double critChance,
            int trailColorRgb) {
        this.displayName = displayName;
        this.baseDamage = baseDamage;
        this.reach = reach;
        this.cooldownSeconds = cooldownSeconds;
        this.critChance = critChance;
        this.trailColorRgb = trailColorRgb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public double getReach() {
        return reach;
    }

    public double getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getCritChance() {
        return critChance;
    }

    public int getTrailColorRgb() {
        return trailColorRgb;
    }

    public WeaponType nextTier() {
        WeaponType[] tiers = values();
        int next = ordinal() + 1;
        return next < tiers.length ? tiers[next] : this;
    }

    public boolean isMaxTier() {
        return ordinal() == values().length - 1;
    }
}
