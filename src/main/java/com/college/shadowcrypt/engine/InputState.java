package com.college.shadowcrypt.engine;

/**
 * Mutable snapshot of keyboard and mouse inputs consumed on each simulation tick.
 */
public final class InputState {
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;
    public double analogX;
    public double analogY;
    public boolean attackHeld;
    public boolean rangedPressed;
    public boolean dashPressed;
    public boolean potionPressed;
    public boolean interactPressed;

    public double mouseWorldX;
    public double mouseWorldY;
    public boolean hasMouseAim;

    public void clearOneShotTriggers() {
        rangedPressed = false;
        dashPressed = false;
        potionPressed = false;
        interactPressed = false;
    }

    public void resetAll() {
        up = false;
        down = false;
        left = false;
        right = false;
        analogX = 0.0;
        analogY = 0.0;
        attackHeld = false;
        clearOneShotTriggers();
    }
}
