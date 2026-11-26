package com.college.shadowcrypt.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Persistent high-score entry stored in the local leaderboard.
 */
public final class LeaderboardEntry implements Serializable, Comparable<LeaderboardEntry> {
    private static final long serialVersionUID = 1L;

    private final String playerName;
    private final int score;
    private final int floorReached;
    private final int kills;
    private final int playerLevel;
    private final String weaponName;
    private final Difficulty difficulty;
    private final String achievedDate;

    public LeaderboardEntry(
            String playerName,
            int score,
            int floorReached,
            int kills,
            int playerLevel,
            String weaponName,
            Difficulty difficulty,
            String achievedDate) {
        this.playerName = (playerName == null || playerName.isBlank()) ? "Adventurer" : playerName.trim();
        this.score = Math.max(0, score);
        this.floorReached = Math.max(1, floorReached);
        this.kills = Math.max(0, kills);
        this.playerLevel = Math.max(1, playerLevel);
        this.weaponName = weaponName == null ? WeaponType.RUSTED_BLADE.getDisplayName() : weaponName;
        this.difficulty = difficulty == null ? Difficulty.ADVENTURER : difficulty;
        this.achievedDate = achievedDate == null ? LocalDate.now().toString() : achievedDate;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public int getFloorReached() {
        return floorReached;
    }

    public int getKills() {
        return kills;
    }

    public int getPlayerLevel() {
        return playerLevel;
    }

    public String getWeaponName() {
        return weaponName;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getAchievedDate() {
        return achievedDate;
    }

    @Override
    public int compareTo(LeaderboardEntry other) {
        int cmp = Integer.compare(other.score, this.score);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(other.floorReached, this.floorReached);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(other.kills, this.kills);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeaderboardEntry that)) return false;
        return score == that.score
                && floorReached == that.floorReached
                && kills == that.kills
                && playerLevel == that.playerLevel
                && Objects.equals(playerName, that.playerName)
                && Objects.equals(weaponName, that.weaponName)
                && difficulty == that.difficulty
                && Objects.equals(achievedDate, that.achievedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, score, floorReached, kills, playerLevel, weaponName, difficulty, achievedDate);
    }
}
