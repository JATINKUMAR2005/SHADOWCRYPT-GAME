package com.college.shadowcrypt.model;

/**
 * Decoupled combat world contract passed to polymorphic enemy AI implementations.
 */
public interface DungeonCombatContext {

    boolean hasLineOfSight(double x1, double y1, double x2, double y2);

    /**
     * Computes a normalized direction vector [dx, dy] toward target coordinates,
     * navigating around dungeon walls when needed.
     */
    double[] findStepDirection(double fromX, double fromY, double targetX, double targetY);

    void moveEntityWithCollision(Entity entity, double vx, double vy, double dt);

    void spawnEnemyProjectile(
            double x,
            double y,
            double vx,
            double vy,
            int damage,
            int colorRgb,
            double radius,
            double lifeSeconds);

    void damagePlayer(int rawDamage, double sourceX, double sourceY, double knockbackStrength);

    void spawnMinionNear(EnemyType type, double originX, double originY);

    void spawnParticles(double x, double y, int colorRgb, int count, double speed);

    void spawnFloatingText(double x, double y, String text, int colorRgb, boolean large);

    void playEnemyAttackSound(boolean boss);

    double randomDouble();
}
