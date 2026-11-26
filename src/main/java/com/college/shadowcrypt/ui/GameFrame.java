package com.college.shadowcrypt.ui;

import com.college.shadowcrypt.engine.SoundEngine;
import com.college.shadowcrypt.persistence.StorageService;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main desktop application frame hosting the ShadowCrypt Java2D game canvas.
 */
public final class GameFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final transient GameCanvasPanel canvasPanel;

    public GameFrame() {
        super("ShadowCrypt — 2D Dungeon Roguelike (Java 21)");
        SoundEngine soundEngine = new SoundEngine();
        StorageService storageService = new StorageService();
        this.canvasPanel = new GameCanvasPanel(soundEngine, storageService);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImages(LogoRenderer.createWindowIcons());
        setMinimumSize(new Dimension(920, 600));
        setContentPane(canvasPanel);
        pack();

        Rectangle workArea = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if (workArea.width > 0 && workArea.height > 0) {
            int clampedW = Math.min(getWidth(), workArea.width);
            int clampedH = Math.min(getHeight(), workArea.height);
            setSize(Math.max(640, clampedW), Math.max(480, clampedH));
        }
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                canvasPanel.shutdown();
            }
        });
    }

    public void launch() {
        setVisible(true);
        canvasPanel.requestFocusInWindow();
    }
}
