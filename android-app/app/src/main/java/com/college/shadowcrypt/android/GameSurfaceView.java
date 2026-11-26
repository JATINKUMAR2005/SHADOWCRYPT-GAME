package com.college.shadowcrypt.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.engine.InputState;
import com.college.shadowcrypt.model.Difficulty;
import com.college.shadowcrypt.model.Enemy;
import com.college.shadowcrypt.model.LeaderboardEntry;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.SaveGameData;
import com.college.shadowcrypt.persistence.StorageService;

import java.util.List;
import java.util.Map;

/**
 * 60 FPS Android SurfaceView hosting the ShadowCrypt game loop, Canvas rendering,
 * smart mobile target assist, and touch event dispatch.
 */
public final class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface NameEditCallback {
        void onRequestEditName(String currentName);
    }

    private final AndroidSoundEngine soundEngine;
    private final StorageService storageService;
    private final GameEngine engine;
    private final InputState input = new InputState();
    private final AndroidSpriteRenderer spriteRenderer = new AndroidSpriteRenderer();
    private final AndroidHudRenderer hudRenderer = new AndroidHudRenderer();
    private final AndroidOverlayRenderer overlayRenderer = new AndroidOverlayRenderer();
    private final AndroidOverlayRenderer.Hitboxes hitboxes = new AndroidOverlayRenderer.Hitboxes();
    private final TouchControls touchControls = new TouchControls();
    private final float density;
    private final NameEditCallback nameEditCallback;

    private Thread gameThread;
    private volatile boolean running;

    private AndroidOverlayRenderer.MenuSubView menuSubView = AndroidOverlayRenderer.MenuSubView.HOME;
    private String playerName = "Hero";
    private Difficulty selectedDifficulty = Difficulty.ADVENTURER;
    private SaveGameData cachedSave;
    private List<LeaderboardEntry> cachedLeaderboard;

    public GameSurfaceView(Context context, NameEditCallback nameEditCallback) {
        super(context);
        this.nameEditCallback = nameEditCallback;
        this.density = context.getResources().getDisplayMetrics().density;
        this.soundEngine = new AndroidSoundEngine();
        this.storageService = new StorageService(context.getFilesDir().toPath());
        this.engine = new GameEngine(soundEngine);
        refreshPersistenceCache();
        getHolder().addCallback(this);
        setFocusable(true);
    }

    private void refreshPersistenceCache() {
        this.cachedLeaderboard = storageService.loadLeaderboard();
        this.cachedSave = storageService.loadSavedGame().orElse(null);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        touchControls.layout(getWidth(), getHeight(), density);
        startLoopIfReady();
    }

    public void onActivityResumed() {
        if (getHolder().getSurface().isValid()) {
            startLoopIfReady();
        }
    }

    private void startLoopIfReady() {
        if (running) {
            return;
        }
        running = true;
        gameThread = new Thread(this, "shadowcrypt-android-loop");
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        touchControls.layout(width, height, density);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    public void stopLoop() {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join(600);
            } catch (InterruptedException ignored) {
            }
            gameThread = null;
        }
    }

    public void onActivityPaused() {
        synchronized (engine) {
            SaveGameData snap = engine.createSaveSnapshot();
            if (snap != null) {
                storageService.saveGame(snap);
                refreshPersistenceCache();
            }
            touchControls.reset(input);
        }
        stopLoop();
    }

    public void onActivityDestroyed() {
        stopLoop();
        soundEngine.shutdown();
    }

    public boolean handleBackPress() {
        synchronized (engine) {
            if (engine.getPhase() == GameEngine.GamePhase.PLAYING) {
                touchControls.reset(input);
                engine.setPhase(GameEngine.GamePhase.PAUSED);
                return true;
            }
            if (engine.getPhase() == GameEngine.GamePhase.PAUSED) {
                engine.setPhase(GameEngine.GamePhase.PLAYING);
                return true;
            }
            if (engine.getPhase() == GameEngine.GamePhase.MENU && menuSubView != AndroidOverlayRenderer.MenuSubView.HOME) {
                menuSubView = AndroidOverlayRenderer.MenuSubView.HOME;
                return true;
            }
        }
        return false;
    }

    public void setPlayerName(String newName) {
        synchronized (engine) {
            if (newName != null && !newName.isBlank()) {
                this.playerName = newName.trim().substring(0, Math.min(16, newName.trim().length()));
            }
        }
    }

    @Override
    public void run() {
        long lastNanos = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            double dt = (frameStart - lastNanos) / 1_000_000_000.0;
            lastNanos = frameStart;

            synchronized (engine) {
                applySmartMobileAim();
                engine.update(dt, input);
                if (engine.getPhase() == GameEngine.GamePhase.GAME_OVER && !engine.isScoreRecordedForCurrentDeath()) {
                    LeaderboardEntry entry = engine.toLeaderboardEntry();
                    boolean topTen = storageService.recordScore(entry);
                    storageService.deleteSavedGame();
                    engine.markScoreRecorded(topTen);
                    refreshPersistenceCache();
                }
            }

            SurfaceHolder holder = getHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (engine) {
                        renderFrame(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            long elapsedMs = (System.nanoTime() - frameStart) / 1_000_000L;
            long sleepMs = Math.max(2L, 16L - elapsedMs);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void applySmartMobileAim() {
        if (engine.getPhase() != GameEngine.GamePhase.PLAYING) {
            input.hasMouseAim = false;
            return;
        }
        if (!input.attackHeld && !input.rangedPressed) {
            input.hasMouseAim = false;
            return;
        }
        Player p = engine.getPlayer();
        if (p == null) return;

        Enemy closest = null;
        double bestDist = 8.5;
        for (Enemy e : engine.getEnemies()) {
            if (!e.isAlive()) continue;
            double d = p.distanceTo(e);
            if (d < bestDist && engine.hasLineOfSight(p.getX(), p.getY(), e.getX(), e.getY())) {
                bestDist = d;
                closest = e;
            }
        }
        if (closest != null) {
            input.mouseWorldX = closest.getX();
            input.mouseWorldY = closest.getY();
            input.hasMouseAim = true;
        } else {
            input.hasMouseAim = false;
        }
    }

    private void renderFrame(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        GameEngine.GamePhase phase = engine.getPhase();

        if (phase == GameEngine.GamePhase.MENU) {
            overlayRenderer.renderMenu(
                    canvas, w, h, density, menuSubView,
                    playerName, selectedDifficulty, cachedSave, cachedLeaderboard,
                    soundEngine.isMuted(), engine.getElapsedTime(), hitboxes);
            return;
        }

        canvas.drawColor(AndroidTheme.BG_ABYSS);
        spriteRenderer.renderWorld(canvas, engine, w, h, density);
        hudRenderer.renderHud(canvas, engine, w, h, density, soundEngine.isMuted());

        if (phase == GameEngine.GamePhase.PLAYING) {
            touchControls.render(canvas, engine, density);
        } else if (phase == GameEngine.GamePhase.PAUSED) {
            overlayRenderer.renderPause(canvas, w, h, density, soundEngine.isMuted(), hitboxes);
        } else if (phase == GameEngine.GamePhase.GAME_OVER) {
            overlayRenderer.renderGameOver(canvas, w, h, density, engine, cachedLeaderboard, hitboxes);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (engine) {
            if (engine.getPhase() == GameEngine.GamePhase.PLAYING) {
                return touchControls.handleTouch(
                        event,
                        input,
                        engine,
                        hudRenderer,
                        soundEngine::toggleMute,
                        () -> {
                            touchControls.reset(input);
                            engine.setPhase(GameEngine.GamePhase.PAUSED);
                        });
            }
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                handleOverlayTap(event.getX(), event.getY());
            }
        }
        return true;
    }

    private void handleOverlayTap(float x, float y) {
        GameEngine.GamePhase phase = engine.getPhase();
        if (phase == GameEngine.GamePhase.MENU) {
            if (menuSubView == AndroidOverlayRenderer.MenuSubView.HOME) {
                if (hitboxes.nameField.contains(x, y)) {
                    nameEditCallback.onRequestEditName(playerName);
                    return;
                }
                for (Map.Entry<Difficulty, RectF> entry : hitboxes.difficultyButtons.entrySet()) {
                    if (entry.getValue().contains(x, y)) {
                        selectedDifficulty = entry.getKey();
                        return;
                    }
                }
                if (hitboxes.startButton.contains(x, y)) {
                    touchControls.reset(input);
                    engine.startNewGame(playerName, selectedDifficulty);
                } else if (cachedSave != null && hitboxes.continueButton.contains(x, y)) {
                    touchControls.reset(input);
                    engine.loadFromSave(cachedSave);
                } else if (hitboxes.leaderboardButton.contains(x, y)) {
                    refreshPersistenceCache();
                    menuSubView = AndroidOverlayRenderer.MenuSubView.LEADERBOARD;
                } else if (hitboxes.guideButton.contains(x, y)) {
                    menuSubView = AndroidOverlayRenderer.MenuSubView.GUIDE;
                } else if (hitboxes.muteButton.contains(x, y)) {
                    soundEngine.toggleMute();
                }
            } else if (hitboxes.backButton.contains(x, y)) {
                menuSubView = AndroidOverlayRenderer.MenuSubView.HOME;
            }
        } else if (phase == GameEngine.GamePhase.PAUSED) {
            if (hitboxes.pauseResumeButton.contains(x, y)) {
                engine.setPhase(GameEngine.GamePhase.PLAYING);
            } else if (hitboxes.pauseSaveButton.contains(x, y)) {
                saveNow();
            } else if (hitboxes.pauseMuteButton.contains(x, y)) {
                soundEngine.toggleMute();
            } else if (hitboxes.pauseMenuButton.contains(x, y)) {
                saveNow();
                touchControls.reset(input);
                engine.setPhase(GameEngine.GamePhase.MENU);
                menuSubView = AndroidOverlayRenderer.MenuSubView.HOME;
            }
        } else if (phase == GameEngine.GamePhase.GAME_OVER) {
            if (hitboxes.gameOverRetryButton.contains(x, y)) {
                touchControls.reset(input);
                engine.startNewGame(playerName, selectedDifficulty);
            } else if (hitboxes.gameOverMenuButton.contains(x, y)) {
                touchControls.reset(input);
                engine.setPhase(GameEngine.GamePhase.MENU);
                menuSubView = AndroidOverlayRenderer.MenuSubView.HOME;
            }
        }
    }

    private void saveNow() {
        SaveGameData snapshot = engine.createSaveSnapshot();
        if (snapshot != null && storageService.saveGame(snapshot)) {
            refreshPersistenceCache();
            engine.showToast("Run Saved!", 2.0);
        }
    }
}
