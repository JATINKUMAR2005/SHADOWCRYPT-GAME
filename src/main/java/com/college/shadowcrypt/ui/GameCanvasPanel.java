package com.college.shadowcrypt.ui;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.engine.InputState;
import com.college.shadowcrypt.engine.SoundEngine;
import com.college.shadowcrypt.model.Difficulty;
import com.college.shadowcrypt.model.LeaderboardEntry;
import com.college.shadowcrypt.model.SaveGameData;
import com.college.shadowcrypt.persistence.StorageService;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Map;

/**
 * Primary interactive Swing surface hosting the 60 FPS game loop, keyboard/mouse input dispatch,
 * world rendering, HUD, and interactive overlay screens.
 */
public final class GameCanvasPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final transient SoundEngine soundEngine;
    private final transient StorageService storageService;
    private final transient GameEngine engine;
    private final transient InputState input = new InputState();
    private final transient OverlayRenderer.OverlayHitboxes hitboxes = new OverlayRenderer.OverlayHitboxes();
    private final transient Timer loopTimer;

    private OverlayRenderer.MenuSubView menuSubView = OverlayRenderer.MenuSubView.HOME;
    private String playerNameInput = "Hero";
    private Difficulty selectedDifficulty = Difficulty.ADVENTURER;
    private transient SaveGameData cachedSavedRun;
    private transient List<LeaderboardEntry> cachedLeaderboard;

    private final Point mouseScreenPoint = new Point(0, 0);
    private long lastTickNanos;

    public GameCanvasPanel(SoundEngine soundEngine, StorageService storageService) {
        this.soundEngine = soundEngine;
        this.storageService = storageService;
        this.engine = new GameEngine(soundEngine);
        refreshPersistenceCache();

        setPreferredSize(new Dimension(1100, 650));
        setBackground(Theme.BG_ABYSS);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        attachInputListeners();

        this.lastTickNanos = System.nanoTime();
        this.loopTimer = new Timer(16, e -> onTick());
        this.loopTimer.start();
    }

    private void refreshPersistenceCache() {
        this.cachedLeaderboard = storageService.loadLeaderboard();
        this.cachedSavedRun = storageService.loadSavedGame().orElse(null);
    }

    private void onTick() {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;

        updateMouseWorldCoordinates();
        engine.update(dt, input);

        if (engine.getPhase() == GameEngine.GamePhase.GAME_OVER && !engine.isScoreRecordedForCurrentDeath()) {
            LeaderboardEntry entry = engine.toLeaderboardEntry();
            boolean madeTopTen = storageService.recordScore(entry);
            storageService.deleteSavedGame();
            engine.markScoreRecorded(madeTopTen);
            refreshPersistenceCache();
        }

        repaint();
    }

    private void updateMouseWorldCoordinates() {
        if (engine.getPhase() != GameEngine.GamePhase.PLAYING) {
            return;
        }
        double camX = engine.getCameraX();
        double camY = engine.getCameraY();
        double offsetX = getWidth() / 2.0 - camX * SpriteRenderer.TILE_SIZE;
        double offsetY = getHeight() / 2.0 - camY * SpriteRenderer.TILE_SIZE;
        input.mouseWorldX = (mouseScreenPoint.x - offsetX) / SpriteRenderer.TILE_SIZE;
        input.mouseWorldY = (mouseScreenPoint.y - offsetY) / SpriteRenderer.TILE_SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        GameEngine.GamePhase phase = engine.getPhase();

        if (phase == GameEngine.GamePhase.MENU) {
            OverlayRenderer.renderMenu(
                    g2,
                    w,
                    h,
                    menuSubView,
                    playerNameInput,
                    selectedDifficulty,
                    cachedSavedRun,
                    cachedLeaderboard,
                    soundEngine.isMuted(),
                    engine.getElapsedTime(),
                    mouseScreenPoint,
                    hitboxes);
        } else {
            g2.setColor(Theme.BG_ABYSS);
            g2.fillRect(0, 0, w, h);
            SpriteRenderer.renderWorld(g2, engine, w, h);
            HudRenderer.renderHud(g2, engine, w, h, soundEngine.isMuted());

            if (phase == GameEngine.GamePhase.PAUSED) {
                OverlayRenderer.renderPauseOverlay(g2, w, h, soundEngine.isMuted(), mouseScreenPoint, hitboxes);
            } else if (phase == GameEngine.GamePhase.GAME_OVER) {
                OverlayRenderer.renderGameOverOverlay(
                        g2, w, h, engine, cachedLeaderboard, mouseScreenPoint, hitboxes);
            }
        }

        g2.dispose();
    }

    private void attachInputListeners() {
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                input.resetAll();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyReleased(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                handleKeyTyped(e);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseScreenPoint.setLocation(e.getPoint());
                input.hasMouseAim = true;
                updateCursor();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseScreenPoint.setLocation(e.getPoint());
                input.hasMouseAim = true;
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                mouseScreenPoint.setLocation(e.getPoint());
                if (engine.getPhase() == GameEngine.GamePhase.PLAYING) {
                    input.hasMouseAim = true;
                    updateMouseWorldCoordinates();
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        input.attackHeld = true;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        input.rangedPressed = true;
                    }
                    return;
                }
                handleOverlayClick(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    input.attackHeld = false;
                }
            }
        });
    }

    private void handleKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (engine.getPhase() == GameEngine.GamePhase.MENU) {
            if (menuSubView == OverlayRenderer.MenuSubView.HOME) {
                if (code == KeyEvent.VK_BACK_SPACE && !playerNameInput.isEmpty()) {
                    playerNameInput = playerNameInput.substring(0, playerNameInput.length() - 1);
                } else if (code == KeyEvent.VK_ENTER) {
                    startNewRun();
                }
            } else if (code == KeyEvent.VK_ESCAPE) {
                menuSubView = OverlayRenderer.MenuSubView.HOME;
            }
            return;
        }

        if (code == KeyEvent.VK_M) {
            soundEngine.toggleMute();
            return;
        }

        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_P) {
            if (engine.getPhase() == GameEngine.GamePhase.PLAYING) {
                input.resetAll();
                engine.setPhase(GameEngine.GamePhase.PAUSED);
            } else if (engine.getPhase() == GameEngine.GamePhase.PAUSED) {
                engine.setPhase(GameEngine.GamePhase.PLAYING);
            }
            return;
        }

        if (code == KeyEvent.VK_F5 && (engine.getPhase() == GameEngine.GamePhase.PLAYING || engine.getPhase() == GameEngine.GamePhase.PAUSED)) {
            quickSaveCurrentRun();
            return;
        }

        if (engine.getPhase() != GameEngine.GamePhase.PLAYING) {
            return;
        }

        switch (code) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> input.up = true;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> input.down = true;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> input.left = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> input.right = true;
            case KeyEvent.VK_SPACE, KeyEvent.VK_J -> {
                input.hasMouseAim = false;
                input.attackHeld = true;
            }
            case KeyEvent.VK_F, KeyEvent.VK_K -> input.rangedPressed = true;
            case KeyEvent.VK_SHIFT -> input.dashPressed = true;
            case KeyEvent.VK_Q, KeyEvent.VK_H, KeyEvent.VK_1 -> input.potionPressed = true;
            case KeyEvent.VK_E -> input.interactPressed = true;
            default -> {}
        }
    }

    private void handleKeyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> input.up = false;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> input.down = false;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> input.left = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> input.right = false;
            case KeyEvent.VK_SPACE, KeyEvent.VK_J -> input.attackHeld = false;
            default -> {}
        }
    }

    private void handleKeyTyped(KeyEvent e) {
        if (engine.getPhase() != GameEngine.GamePhase.MENU || menuSubView != OverlayRenderer.MenuSubView.HOME) {
            return;
        }
        char ch = e.getKeyChar();
        if ((Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == '-') && playerNameInput.length() < 16) {
            if (playerNameInput.isEmpty() && ch == ' ') {
                return;
            }
            playerNameInput += ch;
        }
    }

    private void handleOverlayClick(Point pt) {
        GameEngine.GamePhase phase = engine.getPhase();
        if (phase == GameEngine.GamePhase.MENU) {
            if (menuSubView == OverlayRenderer.MenuSubView.HOME) {
                for (Map.Entry<Difficulty, Rectangle> entry : hitboxes.difficultyButtons.entrySet()) {
                    if (entry.getValue().contains(pt)) {
                        selectedDifficulty = entry.getKey();
                        return;
                    }
                }
                if (hitboxes.startButton.contains(pt)) {
                    startNewRun();
                } else if (cachedSavedRun != null && hitboxes.continueButton.contains(pt)) {
                    input.resetAll();
                    engine.loadFromSave(cachedSavedRun);
                } else if (hitboxes.leaderboardButton.contains(pt)) {
                    refreshPersistenceCache();
                    menuSubView = OverlayRenderer.MenuSubView.LEADERBOARD;
                } else if (hitboxes.guideButton.contains(pt)) {
                    menuSubView = OverlayRenderer.MenuSubView.GUIDE;
                } else if (hitboxes.muteButton.contains(pt)) {
                    soundEngine.toggleMute();
                }
            } else if (hitboxes.backButton.contains(pt)) {
                menuSubView = OverlayRenderer.MenuSubView.HOME;
            }
        } else if (phase == GameEngine.GamePhase.PAUSED) {
            if (hitboxes.pauseResumeButton.contains(pt)) {
                engine.setPhase(GameEngine.GamePhase.PLAYING);
            } else if (hitboxes.pauseSaveButton.contains(pt)) {
                quickSaveCurrentRun();
            } else if (hitboxes.pauseMuteButton.contains(pt)) {
                soundEngine.toggleMute();
            } else if (hitboxes.pauseMenuButton.contains(pt)) {
                quickSaveCurrentRun();
                input.resetAll();
                engine.setPhase(GameEngine.GamePhase.MENU);
                menuSubView = OverlayRenderer.MenuSubView.HOME;
            }
        } else if (phase == GameEngine.GamePhase.GAME_OVER) {
            if (hitboxes.gameOverRetryButton.contains(pt)) {
                startNewRun();
            } else if (hitboxes.gameOverMenuButton.contains(pt)) {
                input.resetAll();
                engine.setPhase(GameEngine.GamePhase.MENU);
                menuSubView = OverlayRenderer.MenuSubView.HOME;
            }
        }
    }

    private void startNewRun() {
        input.resetAll();
        String chosenName = playerNameInput.isBlank() ? "Adventurer" : playerNameInput.trim();
        engine.startNewGame(chosenName, selectedDifficulty);
    }

    private void quickSaveCurrentRun() {
        SaveGameData snapshot = engine.createSaveSnapshot();
        if (snapshot != null && storageService.saveGame(snapshot)) {
            refreshPersistenceCache();
            engine.showToast("Run Saved Successfully!", 2.2);
        } else {
            engine.showToast("Unable to save right now.", 2.0);
        }
    }

    private void updateCursor() {
        if (engine.getPhase() == GameEngine.GamePhase.PLAYING) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void shutdown() {
        SaveGameData snapshot = engine.createSaveSnapshot();
        if (snapshot != null) {
            storageService.saveGame(snapshot);
        }
        loopTimer.stop();
        soundEngine.shutdown();
    }
}
