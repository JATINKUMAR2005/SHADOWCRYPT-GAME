package com.college.shadowcrypt.model;

/**
 * Abstract base class for dungeon enemies demonstrating inheritance and polymorphic AI behavior.
 */
public abstract class Enemy extends Entity {
    private static final long serialVersionUID = 1L;

    public enum AiState {
        IDLE,
        CHASE,
        WINDUP,
        COOLDOWN
    }

    protected final EnemyType enemyType;
    protected final double aggroRange;
    protected double attackCooldown;
    protected double stateTimer;
    protected double animPhase;
    protected AiState aiState = AiState.IDLE;

    protected Enemy(
            EnemyType enemyType,
            double x,
            double y,
            int scaledHp,
            int scaledDamage,
            double aggroRange) {
        super(x, y, enemyType.getRadius(), scaledHp, scaledDamage, enemyType.getBaseSpeed());
        this.enemyType = enemyType;
        this.aggroRange = aggroRange;
    }

    public final void tickEnemy(DungeonCombatContext ctx, Player player, double dt) {
        if (!alive) {
            return;
        }
        tickBaseTimers(dt);
        animPhase += dt * 5.0;
        if (attackCooldown > 0) {
            attackCooldown = Math.max(0.0, attackCooldown - dt);
        }
        if (stateTimer > 0) {
            stateTimer = Math.max(0.0, stateTimer - dt);
        }
        updateAI(ctx, player, dt);
    }

    protected abstract void updateAI(DungeonCombatContext ctx, Player player, double dt);

    public EnemyType getEnemyType() {
        return enemyType;
    }

    public AiState getAiState() {
        return aiState;
    }

    public double getAnimPhase() {
        return animPhase;
    }

    public boolean isBoss() {
        return enemyType == EnemyType.CRYPT_WARDEN_BOSS;
    }

    public int getXpReward() {
        return enemyType.getXpReward();
    }

    public int getScoreReward() {
        return enemyType.getScoreReward();
    }
}
