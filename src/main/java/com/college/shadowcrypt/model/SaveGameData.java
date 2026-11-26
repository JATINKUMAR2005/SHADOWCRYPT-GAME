package com.college.shadowcrypt.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of an active run enabling Save & Resume across sessions.
 */
public final class SaveGameData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Player player;
    private final Difficulty difficulty;
    private final DungeonLevel level;
    private final ArrayList<Enemy> enemies;
    private final ArrayList<PickupItem> items;
    private final ArrayList<Projectile> projectiles;
    private final long savedAtEpochMillis;

    public SaveGameData(
            Player player,
            Difficulty difficulty,
            DungeonLevel level,
            List<Enemy> enemies,
            List<PickupItem> items,
            List<Projectile> projectiles,
            long savedAtEpochMillis) {
        this.player = player;
        this.difficulty = difficulty;
        this.level = level;
        this.enemies = new ArrayList<>(enemies);
        this.items = new ArrayList<>(items);
        this.projectiles = new ArrayList<>(projectiles);
        this.savedAtEpochMillis = savedAtEpochMillis;
    }

    public Player getPlayer() {
        return player;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public DungeonLevel getLevel() {
        return level;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<PickupItem> getItems() {
        return items;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public long getSavedAtEpochMillis() {
        return savedAtEpochMillis;
    }
}
