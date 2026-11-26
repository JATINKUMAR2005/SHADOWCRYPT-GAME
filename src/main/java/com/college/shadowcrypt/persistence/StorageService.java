package com.college.shadowcrypt.persistence;

import com.college.shadowcrypt.model.LeaderboardEntry;
import com.college.shadowcrypt.model.SaveGameData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Atomic file-based persistence service for high-score leaderboards and saved dungeon runs.
 */
public final class StorageService {

    private static final int MAX_LEADERBOARD_ENTRIES = 10;

    private final Path dataDir;
    private final Path leaderboardFile;
    private final Path saveGameFile;

    public StorageService() {
        this(Path.of("data"));
    }

    public StorageService(Path dataDir) {
        this.dataDir = dataDir;
        this.leaderboardFile = dataDir.resolve("leaderboard.dat");
        this.saveGameFile = dataDir.resolve("savegame.dat");
    }

    public List<LeaderboardEntry> loadLeaderboard() {
        if (!Files.exists(leaderboardFile)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(leaderboardFile))) {
            Object raw = ois.readObject();
            if (raw instanceof List<?> rawList) {
                List<LeaderboardEntry> entries = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof LeaderboardEntry entry) {
                        entries.add(entry);
                    }
                }
                Collections.sort(entries);
                return entries;
            }
        } catch (Exception ignored) {
            // Recover gracefully if file was corrupted or incompatible
        }
        return new ArrayList<>();
    }

    public boolean recordScore(LeaderboardEntry entry) {
        if (entry == null || entry.getScore() <= 0) {
            return false;
        }
        List<LeaderboardEntry> entries = loadLeaderboard();
        entries.add(entry);
        Collections.sort(entries);
        if (entries.size() > MAX_LEADERBOARD_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, MAX_LEADERBOARD_ENTRIES));
        }
        boolean madeTopTen = entries.contains(entry);
        writeObjectAtomically(leaderboardFile, entries);
        return madeTopTen;
    }

    public boolean saveGame(SaveGameData saveData) {
        if (saveData == null) {
            return false;
        }
        return writeObjectAtomically(saveGameFile, saveData);
    }

    public Optional<SaveGameData> loadSavedGame() {
        if (!Files.exists(saveGameFile)) {
            return Optional.empty();
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(saveGameFile))) {
            Object raw = ois.readObject();
            if (raw instanceof SaveGameData saveData) {
                return Optional.of(saveData);
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public boolean hasSavedGame() {
        return Files.exists(saveGameFile);
    }

    public void deleteSavedGame() {
        try {
            Files.deleteIfExists(saveGameFile);
        } catch (IOException ignored) {
        }
    }

    private boolean writeObjectAtomically(Path target, Object data) {
        try {
            Files.createDirectories(dataDir);
            Path tmp = dataDir.resolve(target.getFileName().toString() + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                oos.writeObject(data);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
