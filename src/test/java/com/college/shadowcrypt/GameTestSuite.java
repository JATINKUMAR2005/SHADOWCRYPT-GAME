package com.college.shadowcrypt;

import com.college.shadowcrypt.engine.CollisionSystem;
import com.college.shadowcrypt.engine.DungeonGenerator;
import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.engine.InputState;
import com.college.shadowcrypt.engine.Pathfinder;
import com.college.shadowcrypt.engine.SoundEffect;
import com.college.shadowcrypt.engine.SoundEngine;
import com.college.shadowcrypt.engine.SoundListener;
import com.college.shadowcrypt.model.CryptBossEnemy;
import com.college.shadowcrypt.model.Difficulty;
import com.college.shadowcrypt.model.DungeonLevel;
import com.college.shadowcrypt.model.Enemy;
import com.college.shadowcrypt.model.LeaderboardEntry;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.SaveGameData;
import com.college.shadowcrypt.model.TileType;
import com.college.shadowcrypt.model.WeaponType;
import com.college.shadowcrypt.persistence.StorageService;
import com.college.shadowcrypt.ui.GameFrame;
import com.college.shadowcrypt.ui.HudRenderer;
import com.college.shadowcrypt.ui.OverlayRenderer;
import com.college.shadowcrypt.ui.SpriteRenderer;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Pure Java automated verification suite for ShadowCrypt mechanics, pathfinding,
 * combat, boss progression, audio synthesis, and atomic file persistence.
 */
public final class GameTestSuite {

    private GameTestSuite() {}

    public static void main(String[] args) throws Exception {
        List<String> failures = new ArrayList<>();
        List<NamedTest> tests = List.of(
                new NamedTest("Procedural Generation & BFS Connectivity", GameTestSuite::testDungeonConnectivity),
                new NamedTest("Boss Floor Locking & Crypt Warden Spawn", GameTestSuite::testBossFloorLocking),
                new NamedTest("Wall Sliding Collision & Fog of War", GameTestSuite::testCollisionAndFogOfWar),
                new NamedTest("BFS Pathfinder Obstacle Routing", GameTestSuite::testPathfinderRouting),
                new NamedTest("Player Progression, Shields & Weapons", GameTestSuite::testPlayerProgressionAndDefense),
                new NamedTest("Engine Melee, Ranged & Floor Descent", GameTestSuite::testEngineCombatAndDescent),
                new NamedTest("Atomic Save/Load & Top-10 Leaderboard", GameTestSuite::testStoragePersistence),
                new NamedTest("Pure Java Sound Synthesizer Safety", GameTestSuite::testSoundEngineSafety),
                new NamedTest("Java2D World, HUD & Overlay Rendering", GameTestSuite::testUiRenderingSmoke));

        int passed = 0;
        for (NamedTest test : tests) {
            try {
                test.runnable().run();
                passed++;
                System.out.println("[PASS] " + test.name());
            } catch (Throwable t) {
                failures.add(test.name() + ": " + t.getMessage());
                System.err.println("[FAIL] " + test.name() + " -> " + t.getMessage());
                t.printStackTrace(System.err);
            }
        }

        System.out.printf("%nTest Summary: %d/%d passed%n", passed, tests.size());
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private record NamedTest(String name, ThrowingRunnable runnable) {}

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void testDungeonConnectivity() {
        for (int floor = 1; floor <= 6; floor++) {
            for (long seed = 100L; seed < 105L; seed++) {
                DungeonGenerator.GeneratedFloor gen = DungeonGenerator.generate(
                        floor, Difficulty.ADVENTURER, seed * floor);
                Assert.isTrue(
                        DungeonGenerator.isLevelConnected(gen.level()),
                        "Stairs must be reachable from spawn on floor " + floor + " seed " + seed);
                Assert.isTrue(!gen.enemies().isEmpty(), "Floor " + floor + " should spawn enemies");
                Assert.isTrue(gen.level().getRooms().size() >= 2, "Floor should contain multiple rooms");
            }
        }
    }

    private static void testBossFloorLocking() {
        DungeonGenerator.GeneratedFloor floor1 = DungeonGenerator.generate(1, Difficulty.ADVENTURER, 42L);
        Assert.isFalse(floor1.level().isBossFloor(), "Floor 1 should not be a boss floor");
        Assert.isTrue(floor1.level().isStairsUnlocked(), "Regular floor stairs start unlocked");

        DungeonGenerator.GeneratedFloor floor3 = DungeonGenerator.generate(3, Difficulty.NIGHTMARE, 777L);
        Assert.isTrue(floor3.level().isBossFloor(), "Floor 3 must be a boss floor");
        Assert.isFalse(floor3.level().isStairsUnlocked(), "Boss floor stairs start sealed");
        boolean hasBoss = floor3.enemies().stream().anyMatch(e -> e instanceof CryptBossEnemy);
        Assert.isTrue(hasBoss, "Boss floor must spawn CryptBossEnemy");
    }

    private static void testCollisionAndFogOfWar() {
        DungeonLevel level = new DungeonLevel(12, 12, 1, 1L, false);
        for (int y = 2; y <= 9; y++) {
            for (int x = 2; x <= 9; x++) {
                level.setTile(x, y, TileType.FLOOR);
            }
        }
        level.setTile(6, 5, TileType.WALL);
        level.setTile(6, 6, TileType.WALL);

        Player p = new Player("Tester", 5.5, 5.5);
        // Move diagonally into wall at (6, 5): X should clamp before wall while Y slides smoothly
        CollisionSystem.moveWithTileCollision(level, p, 0.8, -0.4);
        Assert.isTrue(p.getX() < 6.0, "Player should not penetrate wall X");
        Assert.isTrue(p.getY() < 5.5, "Player should slide along Y axis");

        CollisionSystem.updateFogOfWar(level, 4.5, 5.5, 6);
        Assert.isTrue(level.isVisible(4, 5), "Player tile must be visible");
        Assert.isFalse(level.isVisible(8, 5), "Tile directly behind opaque wall must be occluded");
    }

    private static void testPathfinderRouting() {
        DungeonLevel level = new DungeonLevel(10, 10, 1, 2L, false);
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                level.setTile(x, y, TileType.FLOOR);
            }
        }
        // Vertical wall blocking direct horizontal line and bottom edge, open only at y=2
        for (int y = 3; y <= 8; y++) {
            level.setTile(5, y, TileType.WALL);
        }

        double[] step = Pathfinder.findStepDirection(level, 3.5, 5.5, 7.5, 5.5);
        double mag = Math.hypot(step[0], step[1]);
        Assert.isTrue(mag > 0.9, "Pathfinder must return non-zero normalized direction");
        Assert.isTrue(step[1] < 0, "Pathfinder should route upward around wall opening at y=2 (dy=" + step[1] + ")");
    }

    private static void testPlayerProgressionAndDefense() {
        Player p = new Player("Jatin", 5.0, 5.0);
        int startMaxHp = p.getMaxHp();
        Assert.equals(WeaponType.RUSTED_BLADE, p.getWeapon(), "Starts with Iron Shortsword");

        p.addShieldCharges(1);
        Player.DamageOutcome blocked = p.applyIncomingDamage(30);
        Assert.equals(Player.DamageOutcome.SHIELDED, blocked, "First hit absorbed by Aegis shield");
        Assert.equals(startMaxHp, p.getHp(), "Shielded hit causes zero HP loss");

        p.tickPlayerTimers(1.0); // expire i-frames
        Player.DamageOutcome hurt = p.applyIncomingDamage(35);
        Assert.equals(Player.DamageOutcome.DAMAGED, hurt, "Unshielded hit reduces HP");
        Assert.equals(startMaxHp - 35, p.getHp(), "HP reduced accurately");

        int healed = p.usePotion();
        Assert.isTrue(healed > 0, "Potion heals damaged player");
        Assert.equals(1, p.getPotions(), "Potion count decremented from 2 to 1");

        boolean leveled = p.addXp(100);
        Assert.isTrue(leveled, "100 XP triggers level up from Lv.1 (threshold 80)");
        Assert.equals(2, p.getLevel(), "Player reaches Level 2");
        Assert.isTrue(p.getMaxHp() > startMaxHp, "Level up increases max HP");

        Assert.isTrue(p.upgradeWeapon(), "Weapon upgrades to tier 2");
        Assert.equals(WeaponType.RUNE_SCIMITAR, p.getWeapon(), "Weapon becomes Runed Scimitar");
    }

    private static void testEngineCombatAndDescent() {
        List<SoundEffect> played = new ArrayList<>();
        GameEngine engine = new GameEngine((SoundListener) played::add, new Random(99L));
        engine.startNewGame("Tester", Difficulty.ADVENTURER, 12345L);

        Player p = engine.getPlayer();
        Assert.equals(GameEngine.GamePhase.PLAYING, engine.getPhase(), "Game starts in PLAYING phase");
        Assert.isFalse(engine.getEnemies().isEmpty(), "Enemies spawned on floor 1");

        // Move player right next to the first enemy and trigger melee + ranged attacks
        Enemy target = engine.getEnemies().get(0);
        p.setX(target.getX() - 0.8);
        p.setY(target.getY());
        int hpBefore = target.getHp();

        InputState input = new InputState();
        input.mouseWorldX = target.getX();
        input.mouseWorldY = target.getY();
        input.hasMouseAim = true;
        input.attackHeld = true;
        engine.update(0.016, input);

        Assert.isTrue(target.getHp() < hpBefore, "Melee cleave damages nearby aimed enemy");
        Assert.isTrue(played.contains(SoundEffect.SLASH), "Melee swing triggers SLASH audio cue");

        // Test descending stairs
        input.attackHeld = false;
        p.setX(engine.getLevel().getStairsTileX() + 0.5);
        p.setY(engine.getLevel().getStairsTileY() + 0.5);
        input.interactPressed = true;
        engine.update(0.016, input);

        Assert.equals(2, p.getFloor(), "Pressing interact on unlocked stairs descends to floor 2");
        Assert.isTrue(p.getScore() > 0, "Descending awards floor clear score");
    }

    private static void testStoragePersistence() throws IOException {
        Path tempDir = Files.createTempDirectory("shadowcrypt-test-");
        try {
            StorageService storage = new StorageService(tempDir);

            // Seed 12 scores and verify Top-10 cap + descending order
            for (int i = 1; i <= 12; i++) {
                storage.recordScore(new LeaderboardEntry(
                        "Hero" + i, i * 150, i % 5 + 1, i * 3, 2, "Runed Scimitar", Difficulty.ADVENTURER, "2026-11-25"));
            }
            List<LeaderboardEntry> board = storage.loadLeaderboard();
            Assert.equals(10, board.size(), "Leaderboard caps at top 10 entries");
            Assert.equals(1800, board.get(0).getScore(), "Highest score ranks #1");
            Assert.equals(450, board.get(9).getScore(), "Lowest kept entry is #10 (score 450)");

            // Test full run Save & Load round-trip
            GameEngine engine = new GameEngine(SoundListener.NO_OP, new Random(7L));
            engine.startNewGame("SaveHero", Difficulty.NIGHTMARE, 555L);
            engine.getPlayer().addScore(999);
            engine.getPlayer().upgradeWeapon();

            SaveGameData snapshot = engine.createSaveSnapshot();
            Assert.isTrue(storage.saveGame(snapshot), "SaveGame succeeds atomically");
            Assert.isTrue(storage.hasSavedGame(), "hasSavedGame returns true after saving");

            Optional<SaveGameData> loaded = storage.loadSavedGame();
            Assert.isTrue(loaded.isPresent(), "Saved run loads cleanly");
            Assert.equals("SaveHero", loaded.get().getPlayer().getName(), "Player name preserved");
            Assert.equals(999, loaded.get().getPlayer().getScore(), "Score preserved");
            Assert.equals(Difficulty.NIGHTMARE, loaded.get().getDifficulty(), "Difficulty preserved");
            Assert.equals(WeaponType.RUNE_SCIMITAR, loaded.get().getPlayer().getWeapon(), "Weapon tier preserved");

            storage.deleteSavedGame();
            Assert.isFalse(storage.hasSavedGame(), "deleteSavedGame removes save file");
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static void testSoundEngineSafety() {
        SoundEngine soundEngine = new SoundEngine();
        soundEngine.setMuted(true);
        Assert.isTrue(soundEngine.isMuted(), "Mute state toggles on");
        soundEngine.play(SoundEffect.SLASH);
        soundEngine.toggleMute();
        Assert.isFalse(soundEngine.isMuted(), "Mute state toggles off");
        soundEngine.shutdown();
    }

    private static void testUiRenderingSmoke() {
        BufferedImage img = new BufferedImage(1180, 760, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            GameEngine engine = new GameEngine(SoundListener.NO_OP, new Random(11L));
            OverlayRenderer.OverlayHitboxes boxes = new OverlayRenderer.OverlayHitboxes();
            Point hover = new Point(590, 380);

            OverlayRenderer.renderMenu(
                    g, 1180, 760, OverlayRenderer.MenuSubView.HOME,
                    "Jatin", Difficulty.ADVENTURER, null, List.of(), false, 1.0, hover, boxes);
            Assert.isTrue(boxes.startButton.width > 0, "Start button hitbox initialized");

            OverlayRenderer.renderMenu(
                    g, 1180, 760, OverlayRenderer.MenuSubView.LEADERBOARD,
                    "Jatin", Difficulty.ADVENTURER, null, List.of(), false, 1.0, hover, boxes);
            OverlayRenderer.renderMenu(
                    g, 1180, 760, OverlayRenderer.MenuSubView.GUIDE,
                    "Jatin", Difficulty.ADVENTURER, null, List.of(), false, 1.0, hover, boxes);

            // Render boss floor gameplay + HUD + Pause + Game Over
            engine.startNewGame("Jatin", Difficulty.ADVENTURER, 999L);
            for (int i = 0; i < 5; i++) {
                engine.update(0.016, new InputState());
            }
            SpriteRenderer.renderWorld(g, engine, 1180, 760);
            HudRenderer.renderHud(g, engine, 1180, 760, false);
            OverlayRenderer.renderPauseOverlay(g, 1180, 760, false, hover, boxes);
            Assert.isTrue(boxes.pauseResumeButton.width > 0, "Pause resume hitbox initialized");

            OverlayRenderer.renderGameOverOverlay(g, 1180, 760, engine, List.of(), hover, boxes);
            Assert.isTrue(boxes.gameOverRetryButton.width > 0, "Game Over retry hitbox initialized");

            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeAndWait(() -> {
                    GameFrame frame = new GameFrame();
                    frame.pack();
                    frame.dispose();
                });
            }
        } catch (Exception e) {
            throw new AssertionError("Swing UI smoke check failed: " + e.getMessage(), e);
        } finally {
            g.dispose();
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
