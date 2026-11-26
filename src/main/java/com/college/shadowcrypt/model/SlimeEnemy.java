package com.college.shadowcrypt.model;

/**
 * Bouncy melee enemy that stalks the player and performs short-range lunges.
 */
public final class SlimeEnemy extends Enemy {
    private static final long serialVersionUID = 1L;

    private double lungeDirX;
    private double lungeDirY;

    public SlimeEnemy(double x, double y, int scaledHp, int scaledDamage) {
        super(EnemyType.SLIME, x, y, scaledHp, scaledDamage, 8.5);
    }

    @Override
    protected void updateAI(DungeonCombatContext ctx, Player player, double dt) {
        double dist = distanceTo(player);
        boolean los = dist <= aggroRange && ctx.hasLineOfSight(x, y, player.getX(), player.getY());

        if (aiState == AiState.WINDUP) {
            ctx.moveEntityWithCollision(this, 0, 0, dt);
            if (stateTimer <= 0) {
                aiState = AiState.COOLDOWN;
                stateTimer = 0.25;
                attackCooldown = 0.85;
                applyKnockback(lungeDirX * 7.5, lungeDirY * 7.5);
                if (dist <= radius + player.getRadius() + 0.55) {
                    ctx.damagePlayer(attackDamage, x, y, 5.5);
                }
            }
            return;
        }

        if (dist > aggroRange + 2.5 || (!los && aiState == AiState.IDLE)) {
            aiState = AiState.IDLE;
            ctx.moveEntityWithCollision(this, 0, 0, dt);
            return;
        }

        aiState = AiState.CHASE;
        double angle = Math.atan2(player.getY() - y, player.getX() - x);
        facingAngle = angle;

        if (dist <= radius + player.getRadius() + 0.45 && attackCooldown <= 0) {
            aiState = AiState.WINDUP;
            stateTimer = 0.26;
            lungeDirX = Math.cos(angle);
            lungeDirY = Math.sin(angle);
            return;
        }

        double[] step = ctx.findStepDirection(x, y, player.getX(), player.getY());
        double hopFactor = 0.72 + 0.38 * Math.abs(Math.sin(animPhase));
        ctx.moveEntityWithCollision(this, step[0] * moveSpeed * hopFactor, step[1] * moveSpeed * hopFactor, dt);
    }
}
