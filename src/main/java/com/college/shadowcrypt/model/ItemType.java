package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Collectible dungeon loot items and power-ups.
 */
public enum ItemType implements Serializable {
    HEALTH_POTION("Crimson Flask", "+1 Potion (Restores 45 HP)", 0xEF4444),
    GOLD_COIN("Ancient Gold", "+25 Gold & +60 Score", 0xFACC15),
    POWER_ORB("Fury Relic", "+40% Damage for 15s", 0xFB923C),
    SPEED_BOOTS("Windstride Charm", "+35% Speed for 15s", 0x38BDF8),
    SHIELD_CHARM("Aegis Ward", "+2 Shield Charges", 0x34D399),
    WEAPON_CHEST("Runic Chest", "Upgrades Weapon Tier", 0xC084FC);

    private final String displayName;
    private final String effectDescription;
    private final int colorRgb;

    ItemType(String displayName, String effectDescription, int colorRgb) {
        this.displayName = displayName;
        this.effectDescription = effectDescription;
        this.colorRgb = colorRgb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEffectDescription() {
        return effectDescription;
    }

    public int getColorRgb() {
        return colorRgb;
    }
}
