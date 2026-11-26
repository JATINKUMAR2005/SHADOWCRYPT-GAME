package com.college.shadowcrypt.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.model.Difficulty;
import com.college.shadowcrypt.model.LeaderboardEntry;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.SaveGameData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders touch-friendly Menu, Hall of Fame, Controls Guide, Pause, and Game Over screens
 * onto Android Canvas with synchronized touch hitboxes.
 */
public final class AndroidOverlayRenderer {

    public enum MenuSubView {
        HOME,
        LEADERBOARD,
        GUIDE
    }

    public static final class Hitboxes {
        public final RectF nameField = new RectF();
        public final Map<Difficulty, RectF> difficultyButtons = new EnumMap<>(Difficulty.class);
        public final RectF startButton = new RectF();
        public final RectF continueButton = new RectF();
        public final RectF leaderboardButton = new RectF();
        public final RectF guideButton = new RectF();
        public final RectF muteButton = new RectF();
        public final RectF backButton = new RectF();

        public final RectF pauseResumeButton = new RectF();
        public final RectF pauseSaveButton = new RectF();
        public final RectF pauseMuteButton = new RectF();
        public final RectF pauseMenuButton = new RectF();

        public final RectF gameOverRetryButton = new RectF();
        public final RectF gameOverMenuButton = new RectF();

        public void clear() {
            nameField.setEmpty();
            difficultyButtons.clear();
            startButton.setEmpty();
            continueButton.setEmpty();
            leaderboardButton.setEmpty();
            guideButton.setEmpty();
            muteButton.setEmpty();
            backButton.setEmpty();
            pauseResumeButton.setEmpty();
            pauseSaveButton.setEmpty();
            pauseMuteButton.setEmpty();
            pauseMenuButton.setEmpty();
            gameOverRetryButton.setEmpty();
            gameOverMenuButton.setEmpty();
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final AndroidLogoRenderer logoRenderer = new AndroidLogoRenderer();

    public void renderMenu(
            Canvas canvas,
            int w,
            int h,
            float dp,
            MenuSubView subView,
            String playerName,
            Difficulty selectedDifficulty,
            SaveGameData savedRun,
            List<LeaderboardEntry> leaderboard,
            boolean muted,
            double elapsedTime,
            Hitboxes boxes) {
        boxes.clear();
        drawBackdrop(canvas, w, h, elapsedTime);
        switch (subView) {
            case HOME -> drawHome(canvas, w, h, dp, playerName, selectedDifficulty, savedRun, muted, boxes);
            case LEADERBOARD -> drawLeaderboard(canvas, w, h, dp, leaderboard, boxes);
            case GUIDE -> drawGuide(canvas, w, h, dp, boxes);
        }
    }

    private void drawBackdrop(Canvas canvas, int w, int h, double time) {
        canvas.drawColor(AndroidTheme.BG_ABYSS);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f);
        paint.setColor(0x551A2234);
        int tile = Math.max(40, Math.min(w, h) / 10);
        for (int x = 0; x < w; x += tile) canvas.drawLine(x, 0, x, h, paint);
        for (int y = 0; y < h; y += tile) canvas.drawLine(0, y, w, y, paint);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 18; i++) {
            float px = (float) ((i * 173.0 + time * (12 + (i % 5) * 3)) % Math.max(1, w));
            float py = h - (float) ((i * 97.0 + time * (22 + (i % 4) * 5)) % Math.max(1, h));
            paint.setColor(i % 3 == 0 ? 0x5038BDF8 : 0x55FB923C);
            canvas.drawCircle(px, py, 3f + (i % 3), paint);
        }
    }

    private void drawHome(
            Canvas canvas,
            int w,
            int h,
            float dp,
            String playerName,
            Difficulty selectedDifficulty,
            SaveGameData savedRun,
            boolean muted,
            Hitboxes boxes) {
        float cardW = Math.min(w * 0.90f, 640f * dp);
        float cardH = Math.min(h * 0.92f, 360f * dp);
        float cx = (w - cardW) * 0.5f;
        float cy = (h - cardH) * 0.5f;

        rect.set(cx, cy, cx + cardW, cy + cardH);
        AndroidTheme.drawRoundedPanel(canvas, paint, rect, 16f * dp, AndroidTheme.PANEL_BG, AndroidTheme.PANEL_BORDER_BRIGHT, 1.5f * dp);

        logoRenderer.drawCrest(canvas, w * 0.5f - cardW * 0.34f, cy + cardH * 0.11f, cardH * 0.072f);
        logoRenderer.drawCrest(canvas, w * 0.5f + cardW * 0.34f, cy + cardH * 0.11f, cardH * 0.072f);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(cardH * 0.11f);
        paint.setColor(AndroidTheme.GOLD);
        AndroidTheme.drawCenteredText(canvas, paint, "SHADOWCRYPT", w * 0.5f, cy + cardH * 0.14f);

        paint.setTypeface(AndroidTheme.SANS_NORMAL);
        paint.setTextSize(cardH * 0.042f);
        paint.setColor(AndroidTheme.CYAN);
        AndroidTheme.drawCenteredText(canvas, paint, "Pure Java Android Edition • Touch Roguelike", w * 0.5f, cy + cardH * 0.20f);

        float innerX = cx + cardW * 0.06f;
        float innerW = cardW * 0.88f;

        // Name field (tap to edit via dialog)
        boxes.nameField.set(innerX, cy + cardH * 0.24f, innerX + innerW, cy + cardH * 0.34f);
        AndroidTheme.drawRoundedPanel(canvas, paint, boxes.nameField, 8f * dp, 0xFF111827, AndroidTheme.PANEL_BORDER, 1.5f * dp);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(cardH * 0.045f);
        paint.setColor(AndroidTheme.TEXT_PRIMARY);
        canvas.drawText("Hero: " + playerName + "  (Tap to edit)", boxes.nameField.left + 12f * dp, boxes.nameField.centerY() + cardH * 0.015f, paint);

        // Difficulty row
        Difficulty[] diffs = Difficulty.values();
        float gap = cardW * 0.02f;
        float dw = (innerW - gap * 2f) / 3f;
        float dy = cy + cardH * 0.37f;
        float dh = cardH * 0.16f;
        for (int i = 0; i < diffs.length; i++) {
            Difficulty d = diffs[i];
            float dx = innerX + i * (dw + gap);
            RectF dr = new RectF(dx, dy, dx + dw, dy + dh);
            boxes.difficultyButtons.put(d, dr);
            boolean sel = d == selectedDifficulty;
            int acc = switch (d) {
                case EXPLORER -> AndroidTheme.EMERALD;
                case ADVENTURER -> AndroidTheme.CYAN;
                case NIGHTMARE -> AndroidTheme.ROSE;
            };
            AndroidTheme.drawRoundedPanel(
                    canvas, paint, dr, 10f * dp,
                    sel ? AndroidTheme.withAlpha(acc, 48) : 0xDC141B2A,
                    sel ? acc : AndroidTheme.PANEL_BORDER,
                    sel ? 2.2f * dp : 1.2f * dp);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(AndroidTheme.SANS_BOLD);
            paint.setTextSize(cardH * 0.044f);
            paint.setColor(sel ? acc : AndroidTheme.TEXT_PRIMARY);
            AndroidTheme.drawCenteredText(canvas, paint, d.getLabel(), dr.centerX(), dr.centerY() - dh * 0.06f);
            paint.setTypeface(AndroidTheme.SANS_NORMAL);
            paint.setTextSize(cardH * 0.032f);
            paint.setColor(AndroidTheme.TEXT_MUTED);
            AndroidTheme.drawCenteredText(
                    canvas, paint,
                    String.format("HP %.0f%% • %.2fx Score", d.getEnemyHpMultiplier() * 100, d.getScoreMultiplier()),
                    dr.centerX(), dr.centerY() + dh * 0.25f);
        }

        // Start & Continue buttons
        boxes.startButton.set(innerX, cy + cardH * 0.56f, innerX + innerW, cardH * 0.68f + cy);
        AndroidTheme.drawButton(canvas, paint, boxes.startButton, "ENTER THE CRYPT (New Run)", AndroidTheme.GOLD, true, cardH * 0.048f);

        boolean hasSave = savedRun != null && savedRun.getPlayer() != null;
        String contText = hasSave
                ? String.format("CONTINUE RUN (F%d • %s • Score %d)", savedRun.getPlayer().getFloor(), savedRun.getPlayer().getName(), savedRun.getPlayer().getScore())
                : "NO SAVED RUN AVAILABLE";
        boxes.continueButton.set(innerX, cy + cardH * 0.70f, innerX + innerW, cy + cardH * 0.81f);
        AndroidTheme.drawButton(canvas, paint, boxes.continueButton, contText, AndroidTheme.EMERALD, hasSave, cardH * 0.042f);

        // Bottom utility buttons
        float uy = cy + cardH * 0.84f;
        float uh = cardH * 0.11f;
        boxes.leaderboardButton.set(innerX, uy, innerX + dw, uy + uh);
        boxes.guideButton.set(innerX + dw + gap, uy, innerX + dw * 2f + gap, uy + uh);
        boxes.muteButton.set(innerX + (dw + gap) * 2f, uy, innerX + innerW, uy + uh);
        AndroidTheme.drawButton(canvas, paint, boxes.leaderboardButton, "Hall of Fame", AndroidTheme.VIOLET, true, cardH * 0.040f);
        AndroidTheme.drawButton(canvas, paint, boxes.guideButton, "Touch Guide", AndroidTheme.CYAN, true, cardH * 0.040f);
        AndroidTheme.drawButton(canvas, paint, boxes.muteButton, muted ? "Sound: OFF" : "Sound: ON", muted ? AndroidTheme.CRIMSON : AndroidTheme.EMERALD, true, cardH * 0.040f);
    }

    private void drawLeaderboard(Canvas canvas, int w, int h, float dp, List<LeaderboardEntry> entries, Hitboxes boxes) {
        float cardW = Math.min(w * 0.92f, 680f * dp);
        float cardH = Math.min(h * 0.90f, 350f * dp);
        float cx = (w - cardW) * 0.5f;
        float cy = (h - cardH) * 0.5f;

        rect.set(cx, cy, cx + cardW, cy + cardH);
        AndroidTheme.drawRoundedPanel(canvas, paint, rect, 16f * dp, AndroidTheme.PANEL_BG, AndroidTheme.VIOLET, 1.8f * dp);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(cardH * 0.068f);
        paint.setColor(AndroidTheme.GOLD);
        AndroidTheme.drawCenteredText(canvas, paint, "HALL OF FAME — TOP SCORES", w * 0.5f, cy + cardH * 0.11f);

        if (entries.isEmpty()) {
            paint.setTypeface(AndroidTheme.SANS_NORMAL);
            paint.setTextSize(cardH * 0.05f);
            paint.setColor(AndroidTheme.TEXT_MUTED);
            AndroidTheme.drawCenteredText(canvas, paint, "No runs recorded yet. Play to claim #1!", w * 0.5f, cy + cardH * 0.50f);
        } else {
            float ry = cy + cardH * 0.21f;
            float rowStep = cardH * 0.072f;
            int limit = Math.min(8, entries.size());
            for (int i = 0; i < limit; i++) {
                LeaderboardEntry e = entries.get(i);
                paint.setTypeface(i == 0 ? AndroidTheme.SANS_BOLD : AndroidTheme.SANS_NORMAL);
                paint.setTextSize(cardH * 0.042f);
                paint.setColor(i == 0 ? AndroidTheme.GOLD : AndroidTheme.TEXT_PRIMARY);
                String line = String.format("#%d  %-12s  %5d pts   F%d   %d kills   (%s)",
                        i + 1, e.getPlayerName(), e.getScore(), e.getFloorReached(), e.getKills(), e.getDifficulty().getLabel());
                canvas.drawText(line, cx + cardW * 0.07f, ry, paint);
                ry += rowStep;
            }
        }

        boxes.backButton.set(w * 0.5f - cardW * 0.20f, cy + cardH * 0.83f, w * 0.5f + cardW * 0.20f, cy + cardH * 0.94f);
        AndroidTheme.drawButton(canvas, paint, boxes.backButton, "Back to Menu", AndroidTheme.CYAN, true, cardH * 0.045f);
    }

    private void drawGuide(Canvas canvas, int w, int h, float dp, Hitboxes boxes) {
        float cardW = Math.min(w * 0.92f, 680f * dp);
        float cardH = Math.min(h * 0.90f, 350f * dp);
        float cx = (w - cardW) * 0.5f;
        float cy = (h - cardH) * 0.5f;

        rect.set(cx, cy, cx + cardW, cy + cardH);
        AndroidTheme.drawRoundedPanel(canvas, paint, rect, 16f * dp, AndroidTheme.PANEL_BG, AndroidTheme.CYAN, 1.8f * dp);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(cardH * 0.068f);
        paint.setColor(AndroidTheme.CYAN);
        AndroidTheme.drawCenteredText(canvas, paint, "MOBILE CONTROLS & DUNGEON GUIDE", w * 0.5f, cy + cardH * 0.11f);

        String[] lines = {
            "• Left Thumbstick: Move smoothly in 360°",
            "• SLASH (Right): Hold or tap to cleave nearby enemies (auto-aims closest foe!)",
            "• BOLT: Fire ranged Arcane Bolt at the nearest visible enemy",
            "• DASH: Burst dodge with brief invulnerability frames across traps & attacks",
            "• HP Flask: Drink Crimson Flask (+45 HP) anytime in combat",
            "• STAIRS: Appears when standing on stairs; defeats Crypt Warden Boss every 3rd floor to unlock!"
        };
        paint.setTypeface(AndroidTheme.SANS_NORMAL);
        paint.setTextSize(cardH * 0.042f);
        paint.setColor(AndroidTheme.TEXT_PRIMARY);
        float ly = cy + cardH * 0.22f;
        for (String l : lines) {
            canvas.drawText(l, cx + cardW * 0.06f, ly, paint);
            ly += cardH * 0.095f;
        }

        boxes.backButton.set(w * 0.5f - cardW * 0.20f, cy + cardH * 0.83f, w * 0.5f + cardW * 0.20f, cy + cardH * 0.94f);
        AndroidTheme.drawButton(canvas, paint, boxes.backButton, "Back to Menu", AndroidTheme.CYAN, true, cardH * 0.045f);
    }

    public void renderPause(Canvas canvas, int w, int h, float dp, boolean muted, Hitboxes boxes) {
        boxes.clear();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xAA060910);
        canvas.drawRect(0, 0, w, h, paint);

        float cardW = Math.min(w * 0.70f, 340f * dp);
        float cardH = Math.min(h * 0.84f, 270f * dp);
        float cx = (w - cardW) * 0.5f;
        float cy = (h - cardH) * 0.5f;
        rect.set(cx, cy, cx + cardW, cy + cardH);
        AndroidTheme.drawRoundedPanel(canvas, paint, rect, 16f * dp, AndroidTheme.PANEL_BG, AndroidTheme.CYAN, 1.8f * dp);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(cardH * 0.095f);
        paint.setColor(AndroidTheme.TEXT_PRIMARY);
        AndroidTheme.drawCenteredText(canvas, paint, "GAME PAUSED", w * 0.5f, cy + cardH * 0.16f);

        float bx = cx + cardW * 0.10f;
        float br = cx + cardW * 0.90f;
        float bh = cardH * 0.15f;
        boxes.pauseResumeButton.set(bx, cy + cardH * 0.24f, br, cy + cardH * 0.24f + bh);
        boxes.pauseSaveButton.set(bx, cy + cardH * 0.42f, br, cy + cardH * 0.42f + bh);
        boxes.pauseMuteButton.set(bx, cy + cardH * 0.60f, br, cy + cardH * 0.60f + bh);
        boxes.pauseMenuButton.set(bx, cy + cardH * 0.78f, br, cy + cardH * 0.78f + bh);

        AndroidTheme.drawButton(canvas, paint, boxes.pauseResumeButton, "Resume Run", AndroidTheme.EMERALD, true, cardH * 0.056f);
        AndroidTheme.drawButton(canvas, paint, boxes.pauseSaveButton, "Quick Save Run", AndroidTheme.CYAN, true, cardH * 0.056f);
        AndroidTheme.drawButton(canvas, paint, boxes.pauseMuteButton, muted ? "Sound: OFF" : "Sound: ON", AndroidTheme.GOLD, true, cardH * 0.056f);
        AndroidTheme.drawButton(canvas, paint, boxes.pauseMenuButton, "Save & Main Menu", AndroidTheme.ROSE, true, cardH * 0.056f);
    }

    public void renderGameOver(
            Canvas canvas,
            int w,
            int h,
            float dp,
            GameEngine engine,
            List<LeaderboardEntry> leaderboard,
            Hitboxes boxes) {
        boxes.clear();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xB81C060C);
        canvas.drawRect(0, 0, w, h, paint);

        float cardW = Math.min(w * 0.85f, 480f * dp);
        float cardH = Math.min(h * 0.88f, 320f * dp);
        float cx = (w - cardW) * 0.5f;
        float cy = (h - cardH) * 0.5f;
        rect.set(cx, cy, cx + cardW, cy + cardH);
        AndroidTheme.drawRoundedPanel(canvas, paint, rect, 16f * dp, AndroidTheme.PANEL_BG, AndroidTheme.ROSE, 2f * dp);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(AndroidTheme.SANS_BOLD);
        paint.setTextSize(cardH * 0.095f);
        paint.setColor(AndroidTheme.ROSE);
        AndroidTheme.drawCenteredText(canvas, paint, "YOU HAVE FALLEN", w * 0.5f, cy + cardH * 0.15f);

        if (engine.isLastRunMadeTopTen()) {
            paint.setTextSize(cardH * 0.048f);
            paint.setColor(AndroidTheme.GOLD);
            AndroidTheme.drawCenteredText(canvas, paint, "NEW TOP 10 RECORD!", w * 0.5f, cy + cardH * 0.23f);
        }

        Player p = engine.getPlayer();
        if (p != null) {
            paint.setTextSize(cardH * 0.060f);
            paint.setColor(AndroidTheme.GOLD);
            AndroidTheme.drawCenteredText(canvas, paint, "Final Score: " + p.getScore(), w * 0.5f, cy + cardH * 0.36f);
            paint.setTypeface(AndroidTheme.SANS_NORMAL);
            paint.setTextSize(cardH * 0.045f);
            paint.setColor(AndroidTheme.TEXT_PRIMARY);
            AndroidTheme.drawCenteredText(
                    canvas, paint,
                    "Floor " + p.getFloor() + "  •  " + p.getKills() + " Kills  •  Lv." + p.getLevel() + "  •  " + p.getWeapon().getDisplayName(),
                    w * 0.5f, cy + cardH * 0.45f);
        }

        if (!leaderboard.isEmpty()) {
            paint.setTypeface(AndroidTheme.SANS_BOLD);
            paint.setTextSize(cardH * 0.042f);
            paint.setColor(AndroidTheme.CYAN);
            LeaderboardEntry top = leaderboard.get(0);
            AndroidTheme.drawCenteredText(
                    canvas, paint,
                    "Top Record: " + top.getPlayerName() + " — " + top.getScore() + " pts (F" + top.getFloorReached() + ")",
                    w * 0.5f, cy + cardH * 0.62f);
        }

        float btnW = cardW * 0.40f;
        float by = cy + cardH * 0.76f;
        float bh = cardH * 0.15f;
        boxes.gameOverRetryButton.set(w * 0.5f - btnW - 10f * dp, by, w * 0.5f - 10f * dp, by + bh);
        boxes.gameOverMenuButton.set(w * 0.5f + 10f * dp, by, w * 0.5f + btnW + 10f * dp, by + bh);
        AndroidTheme.drawButton(canvas, paint, boxes.gameOverRetryButton, "Play Again", AndroidTheme.EMERALD, true, cardH * 0.052f);
        AndroidTheme.drawButton(canvas, paint, boxes.gameOverMenuButton, "Main Menu", AndroidTheme.CYAN, true, cardH * 0.052f);
    }
}
