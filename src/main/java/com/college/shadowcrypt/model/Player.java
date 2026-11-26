package com.college.shadowcrypt.model;

/**
 * Playable dungeon adventurer tracking progression, combat abilities, equipment, and buffs.
 */
public final class Player extends Entity {
    private static final long serialVersionUID = 1L;

    public enum DamageOutcome {
        INVULNERABLE,
        SHIELDED,
        DAMAGED,
        KILLED
    }

    private final String name;
    private int score;
    private int gold;
    private int floor = 1;
    private int xp;
    private int level = 1;
    private int xpToNext = 80;
    private int kills;
    private int potions = 2;
    private int shieldCharges;
    private WeaponType weapon = WeaponType.RUSTED_BLADE;
    private int bonusDamage;

    private double attackCooldown;
    private double attackSwingTimer;
    private double swingAngle;
    private double rangedCooldown;
    private double dashCooldown;
    private double dashTimer;
    private double dashDirX;
    private double dashDirY;
    private double invulnTimer;
    private double speedBoostTimer;
    private double powerBoostTimer;
    private double trapCooldown;

    public Player(String name, double startX, double startY) {
        super(startX, startY, 0.32, 120, 4, 5.20);
        this.name = (name == null || name.isBlank()) ? "Adventurer" : name.trim();
    }

    public int getEffectiveAttackDamage() {
        int base = weapon.getBaseDamage() + attackDamage + bonusDamage;
        if (powerBoostTimer > 0) {
            return (int) Math.round(base * 1.40);
        }
        return base;
    }

    public int getEffectiveRangedDamage() {
        return Math.max(12, (int) Math.round(getEffectiveAttackDamage() * 0.78));
    }

    public double getEffectiveMoveSpeed() {
        return speedBoostTimer > 0 ? moveSpeed * 1.35 : moveSpeed;
    }

    public void tickPlayerTimers(double dt) {
        tickBaseTimers(dt);
        if (attackCooldown > 0) attackCooldown = Math.max(0.0, attackCooldown - dt);
        if (attackSwingTimer > 0) attackSwingTimer = Math.max(0.0, attackSwingTimer - dt);
        if (rangedCooldown > 0) rangedCooldown = Math.max(0.0, rangedCooldown - dt);
        if (dashCooldown > 0) dashCooldown = Math.max(0.0, dashCooldown - dt);
        if (dashTimer > 0) dashTimer = Math.max(0.0, dashTimer - dt);
        if (invulnTimer > 0) invulnTimer = Math.max(0.0, invulnTimer - dt);
        if (speedBoostTimer > 0) speedBoostTimer = Math.max(0.0, speedBoostTimer - dt);
        if (powerBoostTimer > 0) powerBoostTimer = Math.max(0.0, powerBoostTimer - dt);
        if (trapCooldown > 0) trapCooldown = Math.max(0.0, trapCooldown - dt);
    }

    public DamageOutcome applyIncomingDamage(int rawDamage) {
        if (!alive || rawDamage <= 0) {
            return DamageOutcome.INVULNERABLE;
        }
        if (invulnTimer > 0 || dashTimer > 0) {
            return DamageOutcome.INVULNERABLE;
        }
        if (shieldCharges > 0) {
            shieldCharges--;
            invulnTimer = 0.45;
            hitFlashTimer = 0.14;
            return DamageOutcome.SHIELDED;
        }
        takeDamage(rawDamage);
        invulnTimer = 0.55;
        return alive ? DamageOutcome.DAMAGED : DamageOutcome.KILLED;
    }

    public boolean addXp(int amount) {
        if (amount <= 0) {
            return false;
        }
        xp += amount;
        boolean leveledUp = false;
        while (xp >= xpToNext) {
            xp -= xpToNext;
            level++;
            xpToNext = (int) Math.round(xpToNext * 1.35 + 25);
            maxHp += 15;
            hp = Math.min(maxHp, hp + 28);
            attackDamage += 3;
            leveledUp = true;
        }
        return leveledUp;
    }

    public int usePotion() {
        if (!alive || potions <= 0 || hp >= maxHp) {
            return 0;
        }
        potions--;
        return heal(45);
    }

    public boolean upgradeWeapon() {
        if (weapon.isMaxTier()) {
            bonusDamage += 6;
            score += 150;
            return false;
        }
        weapon = weapon.nextTier();
        return true;
    }

    public boolean startDash(double dirX, double dirY) {
        if (dashCooldown > 0 || dashTimer > 0) {
            return false;
        }
        double len = Math.hypot(dirX, dirY);
        if (len < 1e-4) {
            dashDirX = Math.cos(facingAngle);
            dashDirY = Math.sin(facingAngle);
        } else {
            dashDirX = dirX / len;
            dashDirY = dirY / len;
        }
        dashTimer = 0.18;
        dashCooldown = 1.30;
        return true;
    }

    public void triggerMeleeSwing(double angle) {
        this.facingAngle = angle;
        this.swingAngle = angle;
        this.attackSwingTimer = 0.22;
        this.attackCooldown = weapon.getCooldownSeconds();
    }

    public void triggerRangedCast(double angle) {
        this.facingAngle = angle;
        this.rangedCooldown = 0.85;
    }

    public void addScore(int delta) {
        if (delta > 0) {
            score += delta;
        }
    }

    public void addGold(int delta) {
        if (delta > 0) {
            gold += delta;
        }
    }

    public void incrementKills() {
        kills++;
    }

    public void addPotions(int count) {
        potions = Math.min(9, potions + Math.max(0, count));
    }

    public void addShieldCharges(int charges) {
        shieldCharges = Math.min(5, shieldCharges + Math.max(0, charges));
    }

    public void activateSpeedBoost(double seconds) {
        speedBoostTimer = Math.max(speedBoostTimer, seconds);
    }

    public void activatePowerBoost(double seconds) {
        powerBoostTimer = Math.max(powerBoostTimer, seconds);
    }

    public boolean isTrapReady() {
        return trapCooldown <= 0;
    }

    public void resetTrapCooldown() {
        trapCooldown = 0.85;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public int getGold() {
        return gold;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = Math.max(1, floor);
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public int getXpToNext() {
        return xpToNext;
    }

    public int getKills() {
        return kills;
    }

    public int getPotions() {
        return potions;
    }

    public int getShieldCharges() {
        return shieldCharges;
    }

    public WeaponType getWeapon() {
        return weapon;
    }

    public double getAttackCooldown() {
        return attackCooldown;
    }

    public double getAttackSwingTimer() {
        return attackSwingTimer;
    }

    public double getSwingAngle() {
        return swingAngle;
    }

    public double getRangedCooldown() {
        return rangedCooldown;
    }

    public double getDashCooldown() {
        return dashCooldown;
    }

    public double getDashTimer() {
        return dashTimer;
    }

    public double getDashDirX() {
        return dashDirX;
    }

    public double getDashDirY() {
        return dashDirY;
    }

    public double getInvulnTimer() {
        return invulnTimer;
    }

    public double getSpeedBoostTimer() {
        return speedBoostTimer;
    }

    public double getPowerBoostTimer() {
        return powerBoostTimer;
    }
}
