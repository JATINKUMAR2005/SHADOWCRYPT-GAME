package com.college.shadowcrypt.model;

import java.io.Serializable;

/**
 * Game difficulty presets scaling enemy strength, incoming damage, and score multipliers.
 */
public enum Difficulty implements Serializable {
    EXPLORER("Explorer", 0.78, 0.75, 0.85, "Forgiving damage and gentler dungeon foes"),
    ADVENTURER("Adventurer", 1.00, 1.00, 1.00, "Balanced roguelike risk and reward"),
    NIGHTMARE("Nightmare", 1.35, 1.40, 1.50, "Relentless enemies with 1.5x score bonus");

    private final String label;
    private final double enemyHpMultiplier;
    private final double enemyDamageMultiplier;
    private final double scoreMultiplier;
    private final String description;

    Difficulty(
            String label,
            double enemyHpMultiplier,
            double enemyDamageMultiplier,
            double scoreMultiplier,
            String description) {
        this.label = label;
        this.enemyHpMultiplier = enemyHpMultiplier;
        this.enemyDamageMultiplier = enemyDamageMultiplier;
        this.scoreMultiplier = scoreMultiplier;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public double getEnemyHpMultiplier() {
        return enemyHpMultiplier;
    }

    public double getEnemyDamageMultiplier() {
        return enemyDamageMultiplier;
    }

    public double getScoreMultiplier() {
        return scoreMultiplier;
    }

    public String getDescription() {
        return description;
    }
}
