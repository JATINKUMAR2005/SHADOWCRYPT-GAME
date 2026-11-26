package com.college.shadowcrypt.ui;

import com.college.shadowcrypt.engine.GameEngine;
import com.college.shadowcrypt.model.Difficulty;
import com.college.shadowcrypt.model.LeaderboardEntry;
import com.college.shadowcrypt.model.Player;
import com.college.shadowcrypt.model.SaveGameData;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders interactive Menu, Leaderboard, How-To-Play Guide, Pause, and Game Over overlays
 * while keeping exact button hitboxes synchronized with screen layout.
 */
public final class OverlayRenderer {

    public enum MenuSubView {
        HOME,
        LEADERBOARD,
        GUIDE
    }

    public static final class OverlayHitboxes {
        public final Rectangle nameField = new Rectangle();
        public final Map<Difficulty, Rectangle> difficultyButtons = new EnumMap<>(Difficulty.class);
        public final Rectangle startButton = new Rectangle();
        public final Rectangle continueButton = new Rectangle();
        public final Rectangle leaderboardButton = new Rectangle();
        public final Rectangle guideButton = new Rectangle();
        public final Rectangle muteButton = new Rectangle();
        public final Rectangle backButton = new Rectangle();

        public final Rectangle pauseResumeButton = new Rectangle();
        public final Rectangle pauseSaveButton = new Rectangle();
        public final Rectangle pauseMuteButton = new Rectangle();
        public final Rectangle pauseMenuButton = new Rectangle();

        public final Rectangle gameOverRetryButton = new Rectangle();
        public final Rectangle gameOverMenuButton = new Rectangle();

        public void clear() {
            nameField.setBounds(0, 0, 0, 0);
            difficultyButtons.clear();
            startButton.setBounds(0, 0, 0, 0);
            continueButton.setBounds(0, 0, 0, 0);
            leaderboardButton.setBounds(0, 0, 0, 0);
            guideButton.setBounds(0, 0, 0, 0);
            muteButton.setBounds(0, 0, 0, 0);
            backButton.setBounds(0, 0, 0, 0);
            pauseResumeButton.setBounds(0, 0, 0, 0);
            pauseSaveButton.setBounds(0, 0, 0, 0);
            pauseMuteButton.setBounds(0, 0, 0, 0);
            pauseMenuButton.setBounds(0, 0, 0, 0);
            gameOverRetryButton.setBounds(0, 0, 0, 0);
            gameOverMenuButton.setBounds(0, 0, 0, 0);
        }
    }

    private OverlayRenderer() {}

    public static void renderMenu(
            Graphics2D g,
            int w,
            int h,
            MenuSubView subView,
            String playerName,
            Difficulty selectedDifficulty,
            SaveGameData savedRun,
            List<LeaderboardEntry> leaderboard,
            boolean muted,
            double elapsedTime,
            Point hoverPoint,
            OverlayHitboxes outBoxes) {
        outBoxes.clear();
        drawMenuBackdrop(g, w, h, elapsedTime);

        switch (subView) {
            case HOME -> drawHomeMenu(
                    g, w, h, playerName, selectedDifficulty, savedRun, muted, elapsedTime, hoverPoint, outBoxes);
            case LEADERBOARD -> drawLeaderboardModal(g, w, h, leaderboard, hoverPoint, outBoxes);
            case GUIDE -> drawGuideModal(g, w, h, hoverPoint, outBoxes);
        }
    }

    private static void drawMenuBackdrop(Graphics2D g, int w, int h, double time) {
        g.setColor(Theme.BG_ABYSS);
        g.fillRect(0, 0, w, h);

        // Subtle animated dungeon grid silhouette behind menu
        int tile = 48;
        g.setColor(new Color(26, 34, 52, 95));
        for (int gx = 0; gx < w; gx += tile) {
            g.drawLine(gx, 0, gx, h);
        }
        for (int gy = 0; gy < h; gy += tile) {
            g.drawLine(0, gy, w, gy);
        }

        // Ambient floating embers
        for (int i = 0; i < 24; i++) {
            double px = ((i * 173.0 + time * (12 + (i % 5) * 3)) % Math.max(1, w));
            double py = h - ((i * 97.0 + time * (24 + (i % 4) * 5)) % Math.max(1, h));
            g.setColor(i % 3 == 0 ? new Color(56, 189, 248, 80) : new Color(251, 146, 60, 85));
            int sz = 4 + (i % 4);
            g.fillOval((int) px, (int) py, sz, sz);
        }
    }

    private static void drawHomeMenu(
            Graphics2D g,
            int w,
            int h,
            String playerName,
            Difficulty selectedDifficulty,
            SaveGameData savedRun,
            boolean muted,
            double time,
            Point hover,
            OverlayHitboxes boxes) {
        int cardW = 680;
        int cardH = 540;
        int cx = w / 2 - cardW / 2;
        int cy = h / 2 - cardH / 2;

        Theme.drawRoundedPanel(g, cx, cy, cardW, cardH, 20, Theme.PANEL_BG, Theme.PANEL_BORDER_BRIGHT);

        // Title, Flanking Crest Logos & Subtitle
        LogoRenderer.drawCrest(g, w / 2.0 - 208, cy + 54, 26);
        LogoRenderer.drawCrest(g, w / 2.0 + 208, cy + 54, 26);
        g.setFont(Theme.FONT_TITLE);
        g.setColor(Theme.GOLD);
        Theme.drawCenteredString(g, "SHADOWCRYPT", w / 2, cy + 64);

        g.setFont(Theme.FONT_BODY);
        g.setColor(Theme.CYAN);
        Theme.drawCenteredString(g, "Procedural 2D Action Roguelike  •  100% Pure Java 21", w / 2, cy + 92);

        // Hero Name Input
        g.setFont(Theme.FONT_SMALL_BOLD);
        g.setColor(Theme.TEXT_MUTED);
        g.drawString("ADVENTURER NAME (TYPE TO EDIT)", cx + 44, cy + 128);

        boxes.nameField.setBounds(cx + 44, cy + 136, cardW - 88, 38);
        boolean nameHovered = boxes.nameField.contains(hover);
        Theme.drawRoundedPanel(
                g,
                boxes.nameField.x,
                boxes.nameField.y,
                boxes.nameField.width,
                boxes.nameField.height,
                10,
                new Color(17, 24, 39),
                nameHovered ? Theme.CYAN : Theme.PANEL_BORDER);
        g.setFont(Theme.FONT_SUBHEADING);
        g.setColor(Theme.TEXT_PRIMARY);
        String cursor = ((int) (time * 2) % 2 == 0) ? "|" : "";
        String displayedName = (playerName.isEmpty() ? "" : playerName) + cursor;
        g.drawString(displayedName, boxes.nameField.x + 14, boxes.nameField.y + 25);

        // Difficulty selection cards
        g.setFont(Theme.FONT_SMALL_BOLD);
        g.setColor(Theme.TEXT_MUTED);
        g.drawString("SELECT DIFFICULTY", cx + 44, cy + 200);

        Difficulty[] diffs = Difficulty.values();
        int gap = 14;
        int diffW = (cardW - 88 - gap * 2) / 3;
        int diffY = cy + 210;
        int diffH = 88;

        for (int i = 0; i < diffs.length; i++) {
            Difficulty d = diffs[i];
            int dx = cx + 44 + i * (diffW + gap);
            Rectangle rect = new Rectangle(dx, diffY, diffW, diffH);
            boxes.difficultyButtons.put(d, rect);

            boolean selected = d == selectedDifficulty;
            boolean hovered = rect.contains(hover);
            Color accent = switch (d) {
                case EXPLORER -> Theme.EMERALD;
                case ADVENTURER -> Theme.CYAN;
                case NIGHTMARE -> Theme.ROSE;
            };
            Color bg = selected
                    ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45)
                    : new Color(20, 27, 42, 220);
            Color border = selected ? accent : (hovered ? Theme.PANEL_BORDER_BRIGHT : Theme.PANEL_BORDER);
            Theme.drawRoundedPanel(g, rect.x, rect.y, rect.width, rect.height, 12, bg, border);

            g.setFont(Theme.FONT_SUBHEADING);
            g.setColor(selected ? accent : Theme.TEXT_PRIMARY);
            Theme.drawCenteredString(g, d.getLabel(), rect.x + rect.width / 2, rect.y + 28);

            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(Theme.TEXT_MUTED);
            String mult = String.format("HP %.0f%% • Score %.2fx", d.getEnemyHpMultiplier() * 100, d.getScoreMultiplier());
            Theme.drawCenteredString(g, mult, rect.x + rect.width / 2, rect.y + 50);
            Theme.drawCenteredString(g, d == Difficulty.NIGHTMARE ? "High Stakes" : (d == Difficulty.EXPLORER ? "Casual" : "Standard"),
                    rect.x + rect.width / 2, rect.y + 70);
        }

        // Primary Start & Continue buttons
        boxes.startButton.setBounds(cx + 44, cy + 322, cardW - 88, 48);
        Theme.drawButton(
                g,
                boxes.startButton,
                "ENTER THE CRYPT  (Start New Run)",
                boxes.startButton.contains(hover),
                Theme.GOLD,
                true);

        boolean hasSave = savedRun != null && savedRun.getPlayer() != null;
        String contLabel = hasSave
                ? String.format(
                        "CONTINUE SAVED RUN  (Floor %d • %s • Score %d)",
                        savedRun.getPlayer().getFloor(),
                        savedRun.getPlayer().getName(),
                        savedRun.getPlayer().getScore())
                : "NO SAVED RUN AVAILABLE";
        boxes.continueButton.setBounds(cx + 44, cy + 382, cardW - 88, 44);
        Theme.drawButton(
                g,
                boxes.continueButton,
                contLabel,
                hasSave && boxes.continueButton.contains(hover),
                Theme.EMERALD,
                hasSave);

        // Secondary buttons row
        int utilY = cy + 446;
        int utilW = (cardW - 88 - gap * 2) / 3;
        boxes.leaderboardButton.setBounds(cx + 44, utilY, utilW, 42);
        boxes.guideButton.setBounds(cx + 44 + utilW + gap, utilY, utilW, 42);
        boxes.muteButton.setBounds(cx + 44 + (utilW + gap) * 2, utilY, utilW, 42);

        Theme.drawButton(g, boxes.leaderboardButton, "Hall of Fame", boxes.leaderboardButton.contains(hover), Theme.VIOLET, true);
        Theme.drawButton(g, boxes.guideButton, "Controls & Guide", boxes.guideButton.contains(hover), Theme.CYAN, true);
        Theme.drawButton(
                g,
                boxes.muteButton,
                muted ? "Sound: OFF" : "Sound: ON",
                boxes.muteButton.contains(hover),
                muted ? Theme.CRIMSON : Theme.EMERALD,
                true);

        g.setFont(Theme.FONT_SMALL);
        g.setColor(Theme.TEXT_MUTED);
        Theme.drawCenteredString(
                g,
                "Press [Enter] to start  •  Defeat the Crypt Warden boss every 3rd floor",
                w / 2,
                cy + cardH - 18);
    }

    private static void drawLeaderboardModal(
            Graphics2D g,
            int w,
            int h,
            List<LeaderboardEntry> entries,
            Point hover,
            OverlayHitboxes boxes) {
        int cardW = 780;
        int cardH = 540;
        int cx = w / 2 - cardW / 2;
        int cy = h / 2 - cardH / 2;

        Theme.drawRoundedPanel(g, cx, cy, cardW, cardH, 20, Theme.PANEL_BG, Theme.VIOLET);
        g.setFont(Theme.FONT_HEADING);
        g.setColor(Theme.GOLD);
        Theme.drawCenteredString(g, "HALL OF FAME — TOP 10 RUNS", w / 2, cy + 46);

        int tableX = cx + 32;
        int tableY = cy + 72;
        int rowH = 36;

        g.setFont(Theme.FONT_SMALL_BOLD);
        g.setColor(Theme.CYAN);
        drawLeaderboardRow(g, tableX, tableY, "#", "ADVENTURER", "SCORE", "FLOOR", "KILLS", "WEAPON", "MODE", "DATE");
        g.setColor(Theme.PANEL_BORDER);
        g.drawLine(tableX, tableY + 10, cx + cardW - 32, tableY + 10);

        if (entries.isEmpty()) {
            g.setFont(Theme.FONT_BODY);
            g.setColor(Theme.TEXT_MUTED);
            Theme.drawCenteredString(g, "No runs recorded yet. Descend into the ShadowCrypt to claim #1!", w / 2, cy + 250);
        } else {
            int limit = Math.min(10, entries.size());
            for (int i = 0; i < limit; i++) {
                LeaderboardEntry e = entries.get(i);
                int ry = tableY + 34 + i * rowH;
                if (i % 2 == 0) {
                    g.setColor(new Color(255, 255, 255, 10));
                    g.fillRoundRect(tableX - 6, ry - 18, cardW - 52, 28, 8, 8);
                }
                g.setFont(Theme.FONT_BODY);
                g.setColor(i == 0 ? Theme.GOLD : Theme.TEXT_PRIMARY);
                drawLeaderboardRow(
                        g,
                        tableX,
                        ry,
                        String.valueOf(i + 1),
                        truncate(e.getPlayerName(), 14),
                        String.valueOf(e.getScore()),
                        "F" + e.getFloorReached(),
                        String.valueOf(e.getKills()),
                        truncate(e.getWeaponName(), 16),
                        e.getDifficulty().getLabel(),
                        e.getAchievedDate());
            }
        }

        boxes.backButton.setBounds(w / 2 - 110, cy + cardH - 60, 220, 42);
        Theme.drawButton(g, boxes.backButton, "Back to Menu", boxes.backButton.contains(hover), Theme.CYAN, true);
    }

    private static void drawLeaderboardRow(
            Graphics2D g,
            int x,
            int y,
            String rank,
            String name,
            String score,
            String floor,
            String kills,
            String weapon,
            String mode,
            String date) {
        g.drawString(rank, x, y);
        g.drawString(name, x + 36, y);
        g.drawString(score, x + 170, y);
        g.drawString(floor, x + 250, y);
        g.drawString(kills, x + 312, y);
        g.drawString(weapon, x + 372, y);
        g.drawString(mode, x + 528, y);
        g.drawString(date, x + 630, y);
    }

    private static void drawGuideModal(
            Graphics2D g,
            int w,
            int h,
            Point hover,
            OverlayHitboxes boxes) {
        int cardW = 760;
        int cardH = 540;
        int cx = w / 2 - cardW / 2;
        int cy = h / 2 - cardH / 2;

        Theme.drawRoundedPanel(g, cx, cy, cardW, cardH, 20, Theme.PANEL_BG, Theme.CYAN);
        g.setFont(Theme.FONT_HEADING);
        g.setColor(Theme.CYAN);
        Theme.drawCenteredString(g, "CONTROLS & SURVIVAL GUIDE", w / 2, cy + 46);

        int col1 = cx + 40;
        int col2 = cx + 395;
        int topY = cy + 88;

        g.setFont(Theme.FONT_SUBHEADING);
        g.setColor(Theme.GOLD);
        g.drawString("Controls", col1, topY);
        g.drawString("Dungeon Mechanics", col2, topY);

        String[][] controls = {
            {"W A S D / Arrows", "Move in 8 directions"},
            {"Mouse Aim + Left Click", "Sword Cleave (or Space / J)"},
            {"Right Click / F / K", "Cast Arcane Bolt (Ranged)"},
            {"Left Shift", "Dodge Dash (Grants i-frames)"},
            {"Q / H / 1", "Drink Crimson Health Flask"},
            {"E (on Stairs)", "Descend to next floor"},
            {"F5", "Quick Save current run"},
            {"Esc / P", "Pause Menu"},
            {"M", "Toggle Sound Effects"}
        };

        int ry = topY + 30;
        for (String[] row : controls) {
            g.setFont(Theme.FONT_SMALL_BOLD);
            g.setColor(Theme.CYAN);
            g.drawString(row[0], col1, ry);
            g.setFont(Theme.FONT_SMALL);
            g.setColor(Theme.TEXT_PRIMARY);
            g.drawString(row[1], col1 + 150, ry);
            ry += 34;
        }

        String[] tips = {
            "• Every floor is procedurally carved with rooms,",
            "  traps, chests, and dynamic Fog of War.",
            "• Every 3rd floor (3, 6, 9...) is a Boss Lair.",
            "  The stairs stay sealed until the Crypt Warden falls!",
            "• Defeating enemies grants XP & Level Ups",
            "  (+Max HP, healing, and permanent attack power).",
            "• Look for purple Runic Chests to upgrade your",
            "  blade through 4 tiers up to Shadowbane Edge.",
            "• Use Shift Dash to phase safely through attacks",
            "  and cross floor Spike Traps unharmed.",
            "• Press F5 anytime to save mid-dungeon!"
        };

        int ty = topY + 28;
        g.setFont(Theme.FONT_SMALL);
        g.setColor(Theme.TEXT_PRIMARY);
        for (String line : tips) {
            g.drawString(line, col2, ty);
            ty += 26;
        }

        boxes.backButton.setBounds(w / 2 - 110, cy + cardH - 60, 220, 42);
        Theme.drawButton(g, boxes.backButton, "Back to Menu", boxes.backButton.contains(hover), Theme.CYAN, true);
    }

    public static void renderPauseOverlay(
            Graphics2D g,
            int w,
            int h,
            boolean muted,
            Point hover,
            OverlayHitboxes boxes) {
        boxes.clear();
        g.setColor(new Color(6, 9, 16, 170));
        g.fillRect(0, 0, w, h);

        int cardW = 380;
        int cardH = 340;
        int cx = w / 2 - cardW / 2;
        int cy = h / 2 - cardH / 2;

        Theme.drawRoundedPanel(g, cx, cy, cardW, cardH, 18, Theme.PANEL_BG, Theme.CYAN);
        g.setFont(Theme.FONT_HEADING);
        g.setColor(Theme.TEXT_PRIMARY);
        Theme.drawCenteredString(g, "GAME PAUSED", w / 2, cy + 48);

        int bx = cx + 40;
        int bw = cardW - 80;
        boxes.pauseResumeButton.setBounds(bx, cy + 78, bw, 44);
        boxes.pauseSaveButton.setBounds(bx, cy + 134, bw, 44);
        boxes.pauseMuteButton.setBounds(bx, cy + 190, bw, 44);
        boxes.pauseMenuButton.setBounds(bx, cy + 246, bw, 44);

        Theme.drawButton(g, boxes.pauseResumeButton, "Resume Run  [Esc]", boxes.pauseResumeButton.contains(hover), Theme.EMERALD, true);
        Theme.drawButton(g, boxes.pauseSaveButton, "Quick Save Run  [F5]", boxes.pauseSaveButton.contains(hover), Theme.CYAN, true);
        Theme.drawButton(g, boxes.pauseMuteButton, muted ? "Sound: OFF  [M]" : "Sound: ON  [M]", boxes.pauseMuteButton.contains(hover), Theme.GOLD, true);
        Theme.drawButton(g, boxes.pauseMenuButton, "Save & Return to Menu", boxes.pauseMenuButton.contains(hover), Theme.ROSE, true);
    }

    public static void renderGameOverOverlay(
            Graphics2D g,
            int w,
            int h,
            GameEngine engine,
            List<LeaderboardEntry> leaderboard,
            Point hover,
            OverlayHitboxes boxes) {
        boxes.clear();
        g.setColor(new Color(28, 6, 12, 185));
        g.fillRect(0, 0, w, h);

        Player p = engine.getPlayer();
        int cardW = 580;
        int cardH = 450;
        int cx = w / 2 - cardW / 2;
        int cy = h / 2 - cardH / 2;

        Theme.drawRoundedPanel(g, cx, cy, cardW, cardH, 20, Theme.PANEL_BG, Theme.ROSE);

        g.setFont(Theme.FONT_GAME_OVER);
        g.setColor(Theme.ROSE);
        Theme.drawCenteredString(g, "YOU HAVE FALLEN", w / 2, cy + 52);

        if (engine.isLastRunMadeTopTen()) {
            g.setFont(Theme.FONT_BODY_BOLD);
            g.setColor(Theme.GOLD);
            Theme.drawCenteredString(g, "NEW TOP 10 HALL OF FAME RECORD!", w / 2, cy + 80);
        }

        if (p != null) {
            Theme.drawRoundedPanel(g, cx + 34, cy + 98, cardW - 68, 122, 12, new Color(20, 27, 42, 220), Theme.PANEL_BORDER);
            g.setFont(Theme.FONT_SUBHEADING);
            g.setColor(Theme.GOLD);
            Theme.drawCenteredString(g, "Final Score: " + p.getScore(), w / 2, cy + 126);

            g.setFont(Theme.FONT_BODY);
            g.setColor(Theme.TEXT_PRIMARY);
            Theme.drawCenteredString(
                    g,
                    "Floor Reached: " + p.getFloor() + "   •   Enemies Slain: " + p.getKills() + "   •   Hero Level: " + p.getLevel(),
                    w / 2,
                    cy + 158);
            g.setColor(Theme.CYAN);
            Theme.drawCenteredString(
                    g,
                    "Weapon: " + p.getWeapon().getDisplayName() + "   •   Difficulty: " + engine.getDifficulty().getLabel(),
                    w / 2,
                    cy + 188);
        }

        // Top 3 preview
        g.setFont(Theme.FONT_SMALL_BOLD);
        g.setColor(Theme.TEXT_MUTED);
        Theme.drawCenteredString(g, "TOP HALL OF FAME SCORES", w / 2, cy + 248);

        int ly = cy + 272;
        for (int i = 0; i < Math.min(3, leaderboard.size()); i++) {
            LeaderboardEntry e = leaderboard.get(i);
            g.setFont(Theme.FONT_BODY);
            g.setColor(i == 0 ? Theme.GOLD : Theme.TEXT_PRIMARY);
            String line = String.format("#%d  %s  —  %d pts (Floor %d)", i + 1, e.getPlayerName(), e.getScore(), e.getFloorReached());
            Theme.drawCenteredString(g, line, w / 2, ly);
            ly += 24;
        }

        int btnW = 220;
        boxes.gameOverRetryButton.setBounds(w / 2 - btnW - 14, cy + cardH - 68, btnW, 46);
        boxes.gameOverMenuButton.setBounds(w / 2 + 14, cy + cardH - 68, btnW, 46);
        Theme.drawButton(g, boxes.gameOverRetryButton, "Play Again", boxes.gameOverRetryButton.contains(hover), Theme.EMERALD, true);
        Theme.drawButton(g, boxes.gameOverMenuButton, "Main Menu", boxes.gameOverMenuButton.contains(hover), Theme.CYAN, true);
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }
}
