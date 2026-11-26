package com.college.shadowcrypt.engine;

import com.college.shadowcrypt.model.Difficulty;
import com.college.shadowcrypt.model.DungeonCombatContext;
import com.college.shadowcrypt.model.DungeonLevel;
import com.college.shadowcrypt.model.Enemy;
import com.college.shadowcrypt.model.EnemyType;
import com.college.shadowcrypt.model.Entity;
import com.college.shadowcrypt.model.FloatingText;
import com.college.shadowcrypt.model.ItemType;
import com.college.shadowcrypt.model.LeaderboardEntry;
import com.college.shadowcrypt.model.Particle;
import com.college.shadowcrypt.model.PickupItem;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.Projectile;
import com.college.shadowcrypt.model.SaveGameData;
import com.college.shadowcrypt.model.SkeletonEnemy;
import com.college.shadowcrypt.model.SlimeEnemy;
import com.college.shadowcrypt.model.TileType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Core simulation loop managing dungeon generation, combat, enemy AI, loot,
 * fog-of-war updates, camera tracking, and save/resume snapshots.
 */
public final class GameEngine implements DungeonCombatContext {

    public enum GamePhase {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private static final int VISION_RADIUS = 8;

    private final SoundListener soundListener;
    private final Random rng;

    private GamePhase phase = GamePhase.MENU;
    private Difficulty difficulty = Difficulty.ADVENTURER;
    private Player player;
    private DungeonLevel level;
    private List<Enemy> enemies = new ArrayList<>();
    private final List<Enemy> pendingSpawns = new ArrayList<>();
    private List<PickupItem> items = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    private double cameraX;
    private double cameraY;
    private double shakeTimer;
    private double shakeIntensity;
    private double elapsedTime;

    private String toastMessage = "";
    private double toastTimer;
    private boolean scoreRecordedForCurrentDeath;
    private boolean lastRunMadeTopTen;

    public GameEngine(SoundListener soundListener) {
        this(soundListener, new Random());
    }

    public GameEngine(SoundListener soundListener, Random rng) {
        this.soundListener = soundListener == null ? SoundListener.NO_OP : soundListener;
        this.rng = rng == null ? new Random() : rng;
    }

    public void startNewGame(String playerName, Difficulty chosenDifficulty) {
        startNewGame(playerName, chosenDifficulty, rng.nextLong());
    }

    public void startNewGame(String playerName, Difficulty chosenDifficulty, long floorOneSeed) {
        this.difficulty = chosenDifficulty == null ? Difficulty.ADVENTURER : chosenDifficulty;
        this.player = new Player(playerName, 0, 0);
        this.scoreRecordedForCurrentDeath = false;
        this.lastRunMadeTopTen = false;
        this.particles.clear();
        this.floatingTexts.clear();
        this.pendingSpawns.clear();

        loadFloor(1, floorOneSeed, false);
        this.phase = GamePhase.PLAYING;
        showToast("Floor 1 — Descend & survive the ShadowCrypt", 3.2);
    }

    private void loadFloor(int floorNumber, long seed, boolean playStairSound) {
        DungeonGenerator.GeneratedFloor generated = DungeonGenerator.generate(floorNumber, difficulty, seed);
        this.level = generated.level();
        this.enemies = new ArrayList<>(generated.enemies());
        this.items = new ArrayList<>(generated.items());
        this.projectiles = new ArrayList<>();
        this.pendingSpawns.clear();
        this.particles.clear();

        player.setFloor(floorNumber);
        player.setX(level.getSpawnTileX() + 0.5);
        player.setY(level.getSpawnTileY() + 0.5);
        cameraX = player.getX();
        cameraY = player.getY();

        CollisionSystem.updateFogOfWar(level, player.getX(), player.getY(), VISION_RADIUS);

        if (playStairSound) {
            soundListener.play(SoundEffect.STAIRS_DESCEND);
        }
        if (level.isBossFloor()) {
            showToast("FLOOR " + floorNumber + " — BOSS LAIR: Defeat the Crypt Warden!", 4.0);
            soundListener.play(SoundEffect.BOSS_ROAR);
        } else if (floorNumber > 1) {
            showToast("Descended to Dungeon Floor " + floorNumber, 2.8);
        }
    }

    public void loadFromSave(SaveGameData save) {
        if (save == null || save.getPlayer() == null || save.getLevel() == null) {
            return;
        }
        this.player = save.getPlayer();
        this.difficulty = save.getDifficulty() == null ? Difficulty.ADVENTURER : save.getDifficulty();
        this.level = save.getLevel();
        this.enemies = new ArrayList<>(save.getEnemies());
        this.items = new ArrayList<>(save.getItems());
        this.projectiles = new ArrayList<>(save.getProjectiles());
        this.particles.clear();
        this.floatingTexts.clear();
        this.pendingSpawns.clear();
        this.scoreRecordedForCurrentDeath = false;
        this.lastRunMadeTopTen = false;
        this.cameraX = player.getX();
        this.cameraY = player.getY();
        CollisionSystem.updateFogOfWar(level, player.getX(), player.getY(), VISION_RADIUS);
        this.phase = GamePhase.PLAYING;
        showToast("Resumed Run — Floor " + player.getFloor(), 2.8);
    }

    public SaveGameData createSaveSnapshot() {
        if (player == null || level == null || phase == GamePhase.MENU || phase == GamePhase.GAME_OVER) {
            return null;
        }
        return new SaveGameData(
                player,
                difficulty,
                level,
                enemies,
                items,
                projectiles,
                System.currentTimeMillis());
    }

    public void update(double dt, InputState input) {
        elapsedTime += dt;
        if (toastTimer > 0) {
            toastTimer = Math.max(0.0, toastTimer - dt);
        }
        if (shakeTimer > 0) {
            shakeTimer = Math.max(0.0, shakeTimer - dt);
        }

        if (phase != GamePhase.PLAYING || player == null || level == null) {
            return;
        }

        double clampedDt = Math.min(0.05, Math.max(0.0, dt));
        player.tickPlayerTimers(clampedDt);

        handlePlayerMovementAndActions(clampedDt, input);
        updateEnemies(clampedDt);
        updateProjectiles(clampedDt);
        updatePickups(clampedDt);
        checkHazardsAndStairs(input);
        updateVisualEffects(clampedDt);

        CollisionSystem.updateFogOfWar(level, player.getX(), player.getY(), VISION_RADIUS);

        // Smooth camera follow
        double lerp = Math.min(1.0, clampedDt * 9.5);
        cameraX += (player.getX() - cameraX) * lerp;
        cameraY += (player.getY() - cameraY) * lerp;

        input.clearOneShotTriggers();
    }

    private void handlePlayerMovementAndActions(double dt, InputState input) {
        double moveX = (input.right ? 1.0 : 0.0) - (input.left ? 1.0 : 0.0);
        double moveY = (input.down ? 1.0 : 0.0) - (input.up ? 1.0 : 0.0);
        if (Math.hypot(input.analogX, input.analogY) > 0.05) {
            moveX = input.analogX;
            moveY = input.analogY;
        }

        if (input.hasMouseAim) {
            player.setFacingAngle(Math.atan2(input.mouseWorldY - player.getY(), input.mouseWorldX - player.getX()));
        } else if (moveX != 0 || moveY != 0) {
            player.setFacingAngle(Math.atan2(moveY, moveX));
        }

        if (input.dashPressed && player.startDash(moveX, moveY)) {
            soundListener.play(SoundEffect.DASH);
            spawnParticles(player.getX(), player.getY(), 0x38BDF8, 10, 3.0);
        }

        double vx;
        double vy;
        if (player.getDashTimer() > 0) {
            vx = player.getDashDirX() * 13.0;
            vy = player.getDashDirY() * 13.0;
            if (rng.nextDouble() < 0.6) {
                particles.add(new Particle(player.getX(), player.getY(), 0, 0, 0.14, 0x38BDF8, 0.22));
            }
        } else {
            double len = Math.hypot(moveX, moveY);
            if (len > 1e-4) {
                double speed = player.getEffectiveMoveSpeed();
                vx = (moveX / len) * speed;
                vy = (moveY / len) * speed;
            } else {
                vx = 0;
                vy = 0;
            }
        }

        CollisionSystem.moveWithTileCollision(
                level,
                player,
                (vx + player.getKnockbackVx()) * dt,
                (vy + player.getKnockbackVy()) * dt);

        if (input.attackHeld && player.getAttackCooldown() <= 0) {
            performMeleeAttack();
        }
        if (input.rangedPressed && player.getRangedCooldown() <= 0) {
            performRangedAttack();
        }
        if (input.potionPressed) {
            int healed = player.usePotion();
            if (healed > 0) {
                soundListener.play(SoundEffect.POTION);
                spawnFloatingText(player.getX(), player.getY() - 0.5, "+" + healed + " HP", 0x4ADE80, false);
                spawnParticles(player.getX(), player.getY(), 0x4ADE80, 12, 2.8);
            } else if (player.getPotions() == 0) {
                showToast("No Crimson Flasks remaining!", 1.5);
            } else {
                showToast("Health is already full!", 1.5);
            }
        }
    }

    private void performMeleeAttack() {
        double angle = player.getFacingAngle();
        player.triggerMeleeSwing(angle);
        soundListener.play(SoundEffect.SLASH);

        double reach = player.getWeapon().getReach();
        int baseDamage = player.getEffectiveAttackDamage();
        boolean landedAny = false;
        boolean critAny = false;

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            double dist = player.distanceTo(enemy);
            if (dist > reach + enemy.getRadius()) {
                continue;
            }
            double angleToEnemy = Math.atan2(enemy.getY() - player.getY(), enemy.getX() - player.getX());
            double deltaAngle = Math.abs(normalizeAngle(angleToEnemy - angle));
            if (deltaAngle > Math.toRadians(70) && dist > 1.0) {
                continue;
            }

            boolean crit = rng.nextDouble() < player.getWeapon().getCritChance();
            int dmg = crit ? (int) Math.round(baseDamage * 1.65) : baseDamage;
            int dealt = enemy.takeDamage(dmg);
            if (dealt > 0) {
                landedAny = true;
                critAny |= crit;
                double kAngle = Math.atan2(enemy.getY() - player.getY(), enemy.getX() - player.getX());
                double kStrength = enemy.isBoss() ? 2.8 : 6.2;
                enemy.applyKnockback(Math.cos(kAngle) * kStrength, Math.sin(kAngle) * kStrength);
                spawnParticles(enemy.getX(), enemy.getY(), crit ? 0xFACC15 : 0xF87171, crit ? 14 : 8, 3.6);
                spawnFloatingText(
                        enemy.getX(),
                        enemy.getY() - 0.45,
                        crit ? "CRIT -" + dealt : "-" + dealt,
                        crit ? 0xFACC15 : 0xFFFFFF,
                        crit);
                if (!enemy.isAlive()) {
                    handleEnemyDefeated(enemy);
                }
            }
        }

        if (landedAny) {
            soundListener.play(critAny ? SoundEffect.CRIT_HIT : SoundEffect.HIT);
            triggerShake(critAny ? 0.16 : 0.09, critAny ? 0.18 : 0.09);
        }
    }

    private void performRangedAttack() {
        double angle = player.getFacingAngle();
        player.triggerRangedCast(angle);
        soundListener.play(SoundEffect.ARCANE_BOLT);

        double speed = 11.5;
        projectiles.add(new Projectile(
                player.getX() + Math.cos(angle) * 0.45,
                player.getY() + Math.sin(angle) * 0.45,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                0.16,
                player.getEffectiveRangedDamage(),
                true,
                0x38BDF8,
                1.65));
        spawnParticles(
                player.getX() + Math.cos(angle) * 0.4,
                player.getY() + Math.sin(angle) * 0.4,
                0x38BDF8,
                6,
                2.2);
    }

    private void updateEnemies(double dt) {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            if (!enemy.isAlive()) {
                it.remove();
                continue;
            }
            enemy.tickEnemy(this, player, dt);
        }
        if (!pendingSpawns.isEmpty()) {
            enemies.addAll(pendingSpawns);
            pendingSpawns.clear();
        }
    }

    private void handleEnemyDefeated(Enemy enemy) {
        player.incrementKills();
        int gainedScore = (int) Math.round(enemy.getScoreReward() * difficulty.getScoreMultiplier());
        player.addScore(gainedScore);
        boolean leveledUp = player.addXp(enemy.getXpReward());
        spawnParticles(enemy.getX(), enemy.getY(), enemy.getEnemyType().getColorRgb(), 18, 4.0);

        if (leveledUp) {
            soundListener.play(SoundEffect.LEVEL_UP);
            spawnFloatingText(player.getX(), player.getY() - 0.75, "LEVEL " + player.getLevel() + "!", 0xFACC15, true);
            spawnParticles(player.getX(), player.getY(), 0xFACC15, 22, 4.5);
            showToast("Level Up! Max HP & Damage Increased", 2.8);
        }

        if (enemy.isBoss()) {
            level.setStairsUnlocked(true);
            soundListener.play(SoundEffect.LEVEL_UP);
            items.add(new PickupItem(ItemType.WEAPON_CHEST, enemy.getX() - 0.45, enemy.getY(), 0.0));
            items.add(new PickupItem(ItemType.HEALTH_POTION, enemy.getX() + 0.45, enemy.getY(), 1.2));
            showToast("CRYPT WARDEN SLAIN! Stairs to Floor " + (player.getFloor() + 1) + " Unlocked!", 4.2);
            return;
        }

        double dropRoll = rng.nextDouble();
        if (dropRoll < 0.28) {
            items.add(new PickupItem(ItemType.GOLD_COIN, enemy.getX(), enemy.getY(), rng.nextDouble() * Math.PI));
        } else if (dropRoll < 0.40) {
            items.add(new PickupItem(ItemType.HEALTH_POTION, enemy.getX(), enemy.getY(), rng.nextDouble() * Math.PI));
        } else if (dropRoll < 0.46) {
            items.add(new PickupItem(ItemType.SHIELD_CHARM, enemy.getX(), enemy.getY(), rng.nextDouble() * Math.PI));
        }
    }

    private void updateProjectiles(double dt) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.update(dt);
            if (p.isExpired()) {
                it.remove();
                continue;
            }
            if (!CollisionSystem.isCircleWalkable(level, p.getX(), p.getY(), p.getRadius() * 0.5)) {
                spawnParticles(p.getX(), p.getY(), p.getColorRgb(), 5, 2.0);
                it.remove();
                continue;
            }

            if (p.isFromPlayer()) {
                for (Enemy enemy : enemies) {
                    if (!enemy.isAlive()) continue;
                    double dist = Math.hypot(enemy.getX() - p.getX(), enemy.getY() - p.getY());
                    if (dist <= enemy.getRadius() + p.getRadius()) {
                        int dealt = enemy.takeDamage(p.getDamage());
                        soundListener.play(SoundEffect.HIT);
                        spawnParticles(p.getX(), p.getY(), 0x38BDF8, 10, 3.0);
                        spawnFloatingText(enemy.getX(), enemy.getY() - 0.4, "-" + dealt, 0x7DD3FC, false);
                        if (!enemy.isAlive()) {
                            handleEnemyDefeated(enemy);
                        }
                        p.expire();
                        break;
                    }
                }
            } else {
                double dist = Math.hypot(player.getX() - p.getX(), player.getY() - p.getY());
                if (dist <= player.getRadius() + p.getRadius()) {
                    damagePlayer(p.getDamage(), p.getX(), p.getY(), 4.2);
                    spawnParticles(p.getX(), p.getY(), p.getColorRgb(), 8, 2.6);
                    p.expire();
                }
            }

            if (p.isExpired()) {
                it.remove();
            }
        }
    }

    private void updatePickups(double dt) {
        Iterator<PickupItem> it = items.iterator();
        while (it.hasNext()) {
            PickupItem item = it.next();
            item.update(dt);
            if (item.isCollected()) {
                it.remove();
                continue;
            }
            double dist = Math.hypot(player.getX() - item.getX(), player.getY() - item.getY());
            if (dist <= player.getRadius() + 0.42) {
                applyPickup(item);
                item.markCollected();
                it.remove();
            }
        }
    }

    private void applyPickup(PickupItem item) {
        ItemType type = item.getType();
        soundListener.play(SoundEffect.PICKUP);
        spawnParticles(item.getX(), item.getY(), type.getColorRgb(), 12, 3.0);

        switch (type) {
            case HEALTH_POTION -> {
                if (player.getHp() < player.getMaxHp() && player.getPotions() >= 4) {
                    int healed = player.heal(40);
                    spawnFloatingText(player.getX(), player.getY() - 0.5, "+" + healed + " HP", 0x4ADE80, false);
                } else {
                    player.addPotions(1);
                    spawnFloatingText(player.getX(), player.getY() - 0.5, "+1 Flask", 0xEF4444, false);
                }
            }
            case GOLD_COIN -> {
                player.addGold(25);
                int pts = (int) Math.round(60 * difficulty.getScoreMultiplier());
                player.addScore(pts);
                spawnFloatingText(player.getX(), player.getY() - 0.5, "+25g / +" + pts, 0xFACC15, false);
            }
            case POWER_ORB -> {
                player.activatePowerBoost(15.0);
                spawnFloatingText(player.getX(), player.getY() - 0.5, "FURY +40% DMG", 0xFB923C, true);
                showToast("Fury Relic active (+40% Damage for 15s)", 2.2);
            }
            case SPEED_BOOTS -> {
                player.activateSpeedBoost(15.0);
                spawnFloatingText(player.getX(), player.getY() - 0.5, "HASTE +35% SPD", 0x38BDF8, true);
                showToast("Windstride active (+35% Speed for 15s)", 2.2);
            }
            case SHIELD_CHARM -> {
                player.addShieldCharges(2);
                spawnFloatingText(player.getX(), player.getY() - 0.5, "+2 AEGIS", 0x34D399, true);
                showToast("Aegis Ward gained (+2 Hit Shields)", 2.2);
            }
            case WEAPON_CHEST -> {
                boolean upgraded = player.upgradeWeapon();
                soundListener.play(SoundEffect.LEVEL_UP);
                if (upgraded) {
                    spawnFloatingText(
                            player.getX(),
                            player.getY() - 0.65,
                            player.getWeapon().getDisplayName() + "!",
                            0xC084FC,
                            true);
                    showToast("Weapon Upgraded to " + player.getWeapon().getDisplayName() + "!", 3.2);
                } else {
                    spawnFloatingText(player.getX(), player.getY() - 0.65, "MASTERWORK +6 DMG", 0xC084FC, true);
                    showToast("Masterwork Polish! +6 Permanent Damage", 2.8);
                }
            }
        }
    }

    private void checkHazardsAndStairs(InputState input) {
        int tx = (int) Math.floor(player.getX());
        int ty = (int) Math.floor(player.getY());
        TileType currentTile = level.getTile(tx, ty);

        if (currentTile == TileType.SPIKE_TRAP && player.isTrapReady() && player.getDashTimer() <= 0) {
            player.resetTrapCooldown();
            int trapDmg = (int) Math.round(12 * difficulty.getEnemyDamageMultiplier());
            damagePlayer(trapDmg, tx + 0.5, ty + 0.5, 2.2);
        }

        if (isPlayerOnStairs() && input.interactPressed) {
            if (!level.isStairsUnlocked()) {
                showToast("Stairs Sealed! Defeat the Crypt Warden first!", 2.2);
                return;
            }
            int clearBonus = (int) Math.round(120 * player.getFloor() * difficulty.getScoreMultiplier());
            player.addScore(clearBonus);
            player.heal(12);
            loadFloor(player.getFloor() + 1, rng.nextLong(), true);
        }
    }

    public boolean isPlayerOnStairs() {
        if (player == null || level == null) {
            return false;
        }
        double dist = Math.hypot(
                player.getX() - (level.getStairsTileX() + 0.5),
                player.getY() - (level.getStairsTileY() + 0.5));
        return dist <= 1.05;
    }

    private void updateVisualEffects(double dt) {
        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            p.update(dt);
            if (p.isExpired()) {
                pIt.remove();
            }
        }
        Iterator<FloatingText> fIt = floatingTexts.iterator();
        while (fIt.hasNext()) {
            FloatingText ft = fIt.next();
            ft.update(dt);
            if (ft.isExpired()) {
                fIt.remove();
            }
        }
    }

    public void showToast(String message, double durationSeconds) {
        this.toastMessage = message;
        this.toastTimer = durationSeconds;
    }

    private void triggerShake(double duration, double intensity) {
        this.shakeTimer = Math.max(this.shakeTimer, duration);
        this.shakeIntensity = Math.max(this.shakeIntensity, intensity);
    }

    private static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    @Override
    public boolean hasLineOfSight(double x1, double y1, double x2, double y2) {
        return CollisionSystem.hasLineOfSight(level, x1, y1, x2, y2);
    }

    @Override
    public double[] findStepDirection(double fromX, double fromY, double targetX, double targetY) {
        return Pathfinder.findStepDirection(level, fromX, fromY, targetX, targetY);
    }

    @Override
    public void moveEntityWithCollision(Entity entity, double vx, double vy, double dt) {
        CollisionSystem.moveWithTileCollision(
                level,
                entity,
                (vx + entity.getKnockbackVx()) * dt,
                (vy + entity.getKnockbackVy()) * dt);
    }

    @Override
    public void spawnEnemyProjectile(
            double x,
            double y,
            double vx,
            double vy,
            int damage,
            int colorRgb,
            double radius,
            double lifeSeconds) {
        projectiles.add(new Projectile(x, y, vx, vy, radius, damage, false, colorRgb, lifeSeconds));
    }

    @Override
    public void damagePlayer(int rawDamage, double sourceX, double sourceY, double knockbackStrength) {
        if (player == null || !player.isAlive()) {
            return;
        }
        Player.DamageOutcome outcome = player.applyIncomingDamage(rawDamage);
        if (outcome == Player.DamageOutcome.INVULNERABLE) {
            return;
        }

        double angle = Math.atan2(player.getY() - sourceY, player.getX() - sourceX);
        player.applyKnockback(Math.cos(angle) * knockbackStrength, Math.sin(angle) * knockbackStrength);

        if (outcome == Player.DamageOutcome.SHIELDED) {
            soundListener.play(SoundEffect.SHIELD_BLOCK);
            spawnFloatingText(player.getX(), player.getY() - 0.5, "BLOCKED!", 0x34D399, true);
            spawnParticles(player.getX(), player.getY(), 0x34D399, 12, 3.2);
            return;
        }

        soundListener.play(SoundEffect.PLAYER_HURT);
        triggerShake(0.18, 0.22);
        spawnFloatingText(player.getX(), player.getY() - 0.5, "-" + rawDamage, 0xF43F5E, false);
        spawnParticles(player.getX(), player.getY(), 0xF43F5E, 10, 3.0);

        if (outcome == Player.DamageOutcome.KILLED) {
            phase = GamePhase.GAME_OVER;
            soundListener.play(SoundEffect.GAME_OVER);
        }
    }

    @Override
    public void spawnMinionNear(EnemyType type, double originX, double originY) {
        if (!level.isWalkable((int) originX, (int) originY)) {
            originX = player.getX();
            originY = player.getY();
        }
        double hpMult = (1.0 + (player.getFloor() - 1) * 0.14) * difficulty.getEnemyHpMultiplier();
        double dmgMult = (1.0 + (player.getFloor() - 1) * 0.10) * difficulty.getEnemyDamageMultiplier();
        Enemy minion = switch (type) {
            case SKELETON_ARCHER -> new SkeletonEnemy(
                    originX,
                    originY,
                    Math.max(1, (int) Math.round(45 * hpMult)),
                    Math.max(1, (int) Math.round(12 * dmgMult)));
            default -> new SlimeEnemy(
                    originX,
                    originY,
                    Math.max(1, (int) Math.round(38 * hpMult)),
                    Math.max(1, (int) Math.round(10 * dmgMult)));
        };
        pendingSpawns.add(minion);
        spawnParticles(originX, originY, type.getColorRgb(), 10, 2.5);
    }

    @Override
    public void spawnParticles(double x, double y, int colorRgb, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double s = (0.3 + rng.nextDouble() * 0.7) * speed;
            particles.add(new Particle(
                    x,
                    y,
                    Math.cos(a) * s,
                    Math.sin(a) * s,
                    0.08 + rng.nextDouble() * 0.08,
                    colorRgb,
                    0.25 + rng.nextDouble() * 0.25));
        }
    }

    @Override
    public void spawnFloatingText(double x, double y, String text, int colorRgb, boolean large) {
        floatingTexts.add(new FloatingText(x, y, text, colorRgb, large ? 1.15 : 0.80, large));
    }

    @Override
    public void playEnemyAttackSound(boolean boss) {
        soundListener.play(boss ? SoundEffect.BOSS_ROAR : SoundEffect.ARCANE_BOLT);
    }

    @Override
    public double randomDouble() {
        return rng.nextDouble();
    }

    public LeaderboardEntry toLeaderboardEntry() {
        if (player == null) {
            return null;
        }
        return new LeaderboardEntry(
                player.getName(),
                player.getScore(),
                player.getFloor(),
                player.getKills(),
                player.getLevel(),
                player.getWeapon().getDisplayName(),
                difficulty,
                LocalDate.now().toString());
    }

    public Enemy getActiveBoss() {
        for (Enemy e : enemies) {
            if (e.isAlive() && e.isBoss()) {
                return e;
            }
        }
        return null;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Player getPlayer() {
        return player;
    }

    public DungeonLevel getLevel() {
        return level;
    }

    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public List<PickupItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<Projectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }

    public List<Particle> getParticles() {
        return Collections.unmodifiableList(particles);
    }

    public List<FloatingText> getFloatingTexts() {
        return Collections.unmodifiableList(floatingTexts);
    }

    public double getCameraX() {
        return cameraX;
    }

    public double getCameraY() {
        return cameraY;
    }

    public double getShakeOffsetX() {
        return shakeTimer > 0 ? (rng.nextDouble() * 2.0 - 1.0) * shakeIntensity : 0.0;
    }

    public double getShakeOffsetY() {
        return shakeTimer > 0 ? (rng.nextDouble() * 2.0 - 1.0) * shakeIntensity : 0.0;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public String getToastMessage() {
        return toastMessage;
    }

    public double getToastTimer() {
        return toastTimer;
    }

    public boolean isScoreRecordedForCurrentDeath() {
        return scoreRecordedForCurrentDeath;
    }

    public void markScoreRecorded(boolean madeTopTen) {
        this.scoreRecordedForCurrentDeath = true;
        this.lastRunMadeTopTen = madeTopTen;
    }

    public boolean isLastRunMadeTopTen() {
        return lastRunMadeTopTen;
    }
}
