package com.college.shadowcrypt.engine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 100% Pure Java procedural PCM sound synthesizer using javax.sound.sampled.
 * Pre-generates waveform buffers at startup and plays them asynchronously without blocking the UI.
 */
public final class SoundEngine implements SoundListener {

    private static final float SAMPLE_RATE = 22050f;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final Map<SoundEffect, byte[]> buffers = new EnumMap<>(SoundEffect.class);
    private final ExecutorService audioExecutor;
    private volatile boolean muted;
    private final boolean audioSupported;

    public SoundEngine() {
        this.audioSupported = probeAudioSupport();
        synthesizeAll();
        this.audioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "shadowcrypt-audio");
            t.setDaemon(true);
            return t;
        });
    }

    private boolean probeAudioSupport() {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
            return AudioSystem.isLineSupported(info);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void synthesizeAll() {
        buffers.put(SoundEffect.SLASH, synthSweepAndNoise(95, 580, 210, 0.35, 0.25));
        buffers.put(SoundEffect.ARCANE_BOLT, synthSweep(110, 760, 440, 0.22, Waveform.TRIANGLE));
        buffers.put(SoundEffect.HIT, synthSweepAndNoise(85, 210, 80, 0.40, 0.32));
        buffers.put(SoundEffect.CRIT_HIT, synthArpeggio(new double[]{587.3, 880.0, 1174.7}, 55, 0.30));
        buffers.put(SoundEffect.PLAYER_HURT, synthSweep(150, 260, 95, 0.38, Waveform.SAWTOOTH));
        buffers.put(SoundEffect.SHIELD_BLOCK, synthArpeggio(new double[]{660.0, 990.0}, 55, 0.28));
        buffers.put(SoundEffect.PICKUP, synthArpeggio(new double[]{523.3, 659.3, 784.0}, 45, 0.24));
        buffers.put(SoundEffect.POTION, synthArpeggio(new double[]{440.0, 554.4, 659.3}, 60, 0.26));
        buffers.put(SoundEffect.DASH, synthSweepAndNoise(130, 320, 620, 0.32, 0.16));
        buffers.put(SoundEffect.LEVEL_UP, synthArpeggio(new double[]{523.3, 659.3, 784.0, 1046.5}, 85, 0.30));
        buffers.put(SoundEffect.BOSS_ROAR, synthSweep(280, 140, 58, 0.36, Waveform.SAWTOOTH));
        buffers.put(SoundEffect.STAIRS_DESCEND, synthArpeggio(new double[]{392.0, 329.6, 261.6}, 95, 0.26));
        buffers.put(SoundEffect.GAME_OVER, synthArpeggio(new double[]{329.6, 293.7, 261.6, 196.0}, 150, 0.30));
    }

    @Override
    public void play(SoundEffect effect) {
        if (muted || !audioSupported || effect == null) {
            return;
        }
        byte[] pcm = buffers.get(effect);
        if (pcm == null) {
            return;
        }
        audioExecutor.execute(() -> playPcm(pcm));
    }

    private void playPcm(byte[] pcm) {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(AUDIO_FORMAT)) {
            line.open(AUDIO_FORMAT, Math.max(4096, pcm.length));
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (Exception ignored) {
            // Gracefully ignore audio device contention or headless environment errors
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void toggleMute() {
        this.muted = !this.muted;
    }

    public void shutdown() {
        audioExecutor.shutdownNow();
    }

    private enum Waveform {
        SINE,
        TRIANGLE,
        SAWTOOTH
    }

    private static byte[] synthSweep(
            int durationMs,
            double startFreq,
            double endFreq,
            double volume,
            Waveform waveform) {
        int samples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        byte[] out = new byte[samples * 2];
        double phase = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / samples;
            double freq = startFreq + (endFreq - startFreq) * t;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            double env = (1.0 - t) * Math.min(1.0, i / (SAMPLE_RATE * 0.008));
            double val = switch (waveform) {
                case SINE -> Math.sin(phase);
                case TRIANGLE -> (2.0 / Math.PI) * Math.asin(Math.sin(phase));
                case SAWTOOTH -> ((phase / Math.PI) % 2.0) - 1.0;
            };
            writeSample(out, i, val * env * volume);
        }
        return out;
    }

    private static byte[] synthSweepAndNoise(
            int durationMs,
            double startFreq,
            double endFreq,
            double noiseMix,
            double volume) {
        int samples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        byte[] out = new byte[samples * 2];
        Random rng = new Random(42);
        double phase = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / samples;
            double freq = startFreq + (endFreq - startFreq) * t;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            double env = Math.pow(1.0 - t, 1.3);
            double tone = Math.sin(phase);
            double noise = rng.nextDouble() * 2.0 - 1.0;
            double mixed = tone * (1.0 - noiseMix) + noise * noiseMix;
            writeSample(out, i, mixed * env * volume);
        }
        return out;
    }

    private static byte[] synthArpeggio(double[] freqs, int noteMs, double volume) {
        int perNote = (int) ((noteMs / 1000.0) * SAMPLE_RATE);
        int total = perNote * freqs.length;
        byte[] out = new byte[total * 2];
        for (int n = 0; n < freqs.length; n++) {
            double freq = freqs[n];
            for (int i = 0; i < perNote; i++) {
                double t = (double) i / perNote;
                double env = (1.0 - t * 0.75) * Math.min(1.0, i / (SAMPLE_RATE * 0.005));
                double phase = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
                double sample = (0.75 * Math.sin(phase) + 0.25 * Math.sin(phase * 2.0)) * env * volume;
                writeSample(out, n * perNote + i, sample);
            }
        }
        return out;
    }

    private static void writeSample(byte[] out, int index, double normalized) {
        double clamped = Math.max(-1.0, Math.min(1.0, normalized));
        short pcm = (short) (clamped * Short.MAX_VALUE);
        out[index * 2] = (byte) (pcm & 0xFF);
        out[index * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
    }
}
