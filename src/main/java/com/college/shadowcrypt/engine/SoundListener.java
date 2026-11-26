package com.college.shadowcrypt.engine;

/**
 * Audio observer interface allowing the engine to run with sound or silently in headless tests.
 */
@FunctionalInterface
public interface SoundListener {
    SoundListener NO_OP = effect -> {};

    void play(SoundEffect effect);
}
