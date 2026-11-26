package com.college.shadowcrypt.model;

/**
 * Ranged skeleton marksman that maintains firing distance and looses bone bolts.
 */
public final class SkeletonEnemy extends Enemy {
    private static final long serialVersionUID = 1L;

    public SkeletonEnemy(double x, double y, int scaledHp, int scaledDamage) {
        super(EnemyType.SKELETON_ARCHER, x, y, scaledHp, scaledDamage, 10.5);
    }

    @Override
    protected void updateAI(DungeonCombatContext ctx, Player player, double dt) {
        double dist = distanceTo(player);
        boolean los = dist <= aggroRange && ctx.hasLineOfSight(x, y, player.getX(), player.getY());

        if (!los && dist > aggroRange) {
            aiState = AiState.IDLE;
            ctx.moveEntityWithCollision(this, 0, 0, dt);
            return;
        }

        double angle = Math.atan2(player.getY() - y, player.getX() - x);
        facingAngle = angle;

        if (los && dist >= 2.8 && dist <= 8.5) {
            if (aiState == AiState.WINDUP) {
                ctx.moveEntityWithCollision(this, 0, 0, dt);
                if (stateTimer <= 0) {
                    double projSpeed = 7.4;
                    ctx.spawnEnemyProjectile(
                            x + Math.cos(angle) * 0.45,
                            y + Math.sin(angle) * 0.45,
                            Math.cos(angle) * projSpeed,
                            Math.sin(angle) * projSpeed,
                            attackDamage,
                            0xF8FAFC,
                            0.14,
                            2.1);
                    ctx.playEnemyAttackSound(false);
                    aiState = AiState.COOLDOWN;
                    attackCooldown = 1.65;
                }
                return;
            }
            if (attackCooldown <= 0) {
                aiState = AiState.WINDUP;
                stateTimer = 0.34;
                return;
            }
            // Slight strafe while waiting on cooldown
            double strafe = Math.sin(animPhase * 0.5) >= 0 ? 1.0 : -1.0;
            double sx = -Math.sin(angle) * strafe * moveSpeed * 0.48;
            double sy = Math.cos(angle) * strafe * moveSpeed * 0.48;
            ctx.moveEntityWithCollision(this, sx, sy, dt);
            return;
        }

        if (los && dist < 2.8) {
            // Back away to keep ranged spacing
            aiState = AiState.CHASE;
            ctx.moveEntityWithCollision(
                    this,
                    -Math.cos(angle) * moveSpeed * 0.85,
                    -Math.sin(angle) * moveSpeed * 0.85,
                    dt);
            return;
        }

        aiState = AiState.CHASE;
        double[] step = ctx.findStepDirection(x, y, player.getX(), player.getY());
        ctx.moveEntityWithCollision(this, step[0] * moveSpeed, step[1] * moveSpeed, dt);
    }
}
