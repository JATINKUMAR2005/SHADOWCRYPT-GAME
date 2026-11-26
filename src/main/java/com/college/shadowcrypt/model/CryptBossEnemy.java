package com.college.shadowcrypt.model;

/**
 * Multi-phase dungeon boss guarding every 3rd floor with radial novas, charges, and minion summons.
 */
public final class CryptBossEnemy extends Enemy {
    private static final long serialVersionUID = 1L;

    private enum BossAttack {
        SHADOW_NOVA,
        AIMED_VOLLEY,
        WARDEN_CHARGE
    }

    private BossAttack nextAttack = BossAttack.SHADOW_NOVA;
    private boolean phaseTwoTriggered;
    private double chargeVx;
    private double chargeVy;
    private double chargeTimer;

    public CryptBossEnemy(double x, double y, int scaledHp, int scaledDamage) {
        super(EnemyType.CRYPT_WARDEN_BOSS, x, y, scaledHp, scaledDamage, 13.0);
        this.attackCooldown = 1.4;
    }

    public boolean isPhaseTwo() {
        return hp <= maxHp * 0.55;
    }

    @Override
    protected void updateAI(DungeonCombatContext ctx, Player player, double dt) {
        double dist = distanceTo(player);
        boolean los = dist <= aggroRange && ctx.hasLineOfSight(x, y, player.getX(), player.getY());

        if (!phaseTwoTriggered && isPhaseTwo()) {
            phaseTwoTriggered = true;
            ctx.spawnFloatingText(x, y - 0.8, "WARDEN ENRAGED!", 0xF43F5E, true);
            ctx.spawnParticles(x, y, 0xF43F5E, 22, 4.2);
            ctx.playEnemyAttackSound(true);
            ctx.spawnMinionNear(EnemyType.SLIME, x - 1.2, y);
            ctx.spawnMinionNear(EnemyType.SKELETON_ARCHER, x + 1.2, y);
        }

        if (chargeTimer > 0) {
            chargeTimer -= dt;
            ctx.moveEntityWithCollision(this, chargeVx, chargeVy, dt);
            if (distanceTo(player) <= radius + player.getRadius() + 0.22) {
                ctx.damagePlayer((int) Math.round(attackDamage * 1.15), x, y, 9.0);
                chargeTimer = 0;
            }
            return;
        }

        if (aiState == AiState.WINDUP) {
            ctx.moveEntityWithCollision(this, 0, 0, dt);
            if (stateTimer <= 0) {
                executeBossAttack(ctx, player);
            }
            return;
        }

        if (!los && dist > aggroRange) {
            aiState = AiState.IDLE;
            ctx.moveEntityWithCollision(this, 0, 0, dt);
            return;
        }

        double angle = Math.atan2(player.getY() - y, player.getX() - x);
        facingAngle = angle;

        if (los && attackCooldown <= 0) {
            aiState = AiState.WINDUP;
            stateTimer = isPhaseTwo() ? 0.36 : 0.48;
            return;
        }

        if (dist <= radius + player.getRadius() + 0.16 && attackCooldown <= 0.4) {
            ctx.damagePlayer(attackDamage, x, y, 7.5);
            attackCooldown = Math.max(attackCooldown, 0.9);
        }

        aiState = AiState.CHASE;
        double speed = isPhaseTwo() ? moveSpeed * 1.22 : moveSpeed;
        double[] step = ctx.findStepDirection(x, y, player.getX(), player.getY());
        ctx.moveEntityWithCollision(this, step[0] * speed, step[1] * speed, dt);
    }

    private void executeBossAttack(DungeonCombatContext ctx, Player player) {
        double angle = Math.atan2(player.getY() - y, player.getX() - x);
        facingAngle = angle;
        ctx.playEnemyAttackSound(true);

        switch (nextAttack) {
            case SHADOW_NOVA -> {
                int count = isPhaseTwo() ? 12 : 8;
                double speed = isPhaseTwo() ? 6.2 : 5.4;
                for (int i = 0; i < count; i++) {
                    double a = (2 * Math.PI * i) / count + animPhase * 0.1;
                    ctx.spawnEnemyProjectile(
                            x + Math.cos(a) * 0.65,
                            y + Math.sin(a) * 0.65,
                            Math.cos(a) * speed,
                            Math.sin(a) * speed,
                            (int) Math.round(attackDamage * 0.85),
                            0xFB7185,
                            0.18,
                            2.4);
                }
                nextAttack = BossAttack.AIMED_VOLLEY;
                attackCooldown = isPhaseTwo() ? 1.35 : 1.75;
            }
            case AIMED_VOLLEY -> {
                int fan = isPhaseTwo() ? 5 : 3;
                double spread = 0.20;
                int half = fan / 2;
                for (int i = -half; i <= half; i++) {
                    double a = angle + i * spread;
                    ctx.spawnEnemyProjectile(
                            x + Math.cos(a) * 0.65,
                            y + Math.sin(a) * 0.65,
                            Math.cos(a) * 7.2,
                            Math.sin(a) * 7.2,
                            attackDamage,
                            0xF43F5E,
                            0.16,
                            2.2);
                }
                nextAttack = BossAttack.WARDEN_CHARGE;
                attackCooldown = isPhaseTwo() ? 1.25 : 1.60;
            }
            case WARDEN_CHARGE -> {
                double chargeSpeed = isPhaseTwo() ? 9.8 : 8.5;
                chargeVx = Math.cos(angle) * chargeSpeed;
                chargeVy = Math.sin(angle) * chargeSpeed;
                chargeTimer = 0.44;
                ctx.spawnParticles(x, y, 0xF43F5E, 14, 3.0);
                nextAttack = BossAttack.SHADOW_NOVA;
                attackCooldown = isPhaseTwo() ? 1.45 : 1.85;
            }
        }
        aiState = AiState.CHASE;
    }
}
