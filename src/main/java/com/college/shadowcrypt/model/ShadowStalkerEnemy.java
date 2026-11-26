package com.college.shadowcrypt.model;

/**
 * Fast flanking assassin that telegraphs briefly before dashing rapidly at the player.
 */
public final class ShadowStalkerEnemy extends Enemy {
    private static final long serialVersionUID = 1L;

    private double dashVx;
    private double dashVy;
    private double dashBurstTimer;

    public ShadowStalkerEnemy(double x, double y, int scaledHp, int scaledDamage) {
        super(EnemyType.SHADOW_STALKER, x, y, scaledHp, scaledDamage, 9.5);
    }

    @Override
    protected void updateAI(DungeonCombatContext ctx, Player player, double dt) {
        double dist = distanceTo(player);
        boolean los = dist <= aggroRange && ctx.hasLineOfSight(x, y, player.getX(), player.getY());

        if (dashBurstTimer > 0) {
            dashBurstTimer -= dt;
            ctx.moveEntityWithCollision(this, dashVx, dashVy, dt);
            if (distanceTo(player) <= radius + player.getRadius() + 0.18) {
                ctx.damagePlayer(attackDamage, x, y, 7.2);
                dashBurstTimer = 0;
                attackCooldown = 1.45;
                aiState = AiState.COOLDOWN;
            }
            return;
        }

        if (aiState == AiState.WINDUP) {
            ctx.moveEntityWithCollision(this, 0, 0, dt);
            if (stateTimer <= 0) {
                double angle = Math.atan2(player.getY() - y, player.getX() - x);
                facingAngle = angle;
                dashVx = Math.cos(angle) * 9.8;
                dashVy = Math.sin(angle) * 9.8;
                dashBurstTimer = 0.30;
                ctx.spawnParticles(x, y, 0xA855F7, 8, 2.5);
                aiState = AiState.CHASE;
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

        if (los && dist <= 4.0 && attackCooldown <= 0) {
            aiState = AiState.WINDUP;
            stateTimer = 0.38;
            return;
        }

        if (dist <= radius + player.getRadius() + 0.15 && attackCooldown <= 0) {
            ctx.damagePlayer(attackDamage, x, y, 6.0);
            attackCooldown = 1.10;
            return;
        }

        aiState = AiState.CHASE;
        double[] step = ctx.findStepDirection(x, y, player.getX(), player.getY());
        ctx.moveEntityWithCollision(this, step[0] * moveSpeed, step[1] * moveSpeed, dt);
    }
}
