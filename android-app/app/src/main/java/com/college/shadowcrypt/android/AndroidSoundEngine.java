package com.college.shadowcrypt.android;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.college.shadowcrypt.engine.SoundEffect;
import com.college.shadowcrypt.engine.SoundListener;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pure Java procedural PCM sound synthesizer powered by Android's AudioTrack API.
 */
public final class AndroidSoundEngine implements SoundListener {

    private static final int SAMPLE_RATE = 22050;

    private final Map<SoundEffect, short[]> buffers = new EnumMap<>(SoundEffect.class);
    private final ExecutorService executor;
    private volatile boolean muted;

    public AndroidSoundEngine() {
        synthesizeAll();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "shadowcrypt-android-audio");
            t.setDaemon(true);
            return t;
        });
    }

    private void synthesizeAll() {
        buffers.put(SoundEffect.SLASH, synthSweepAndNoise(95, 580, 210, 0.35, 0.28));
        buffers.put(SoundEffect.ARCANE_BOLT, synthSweep(110, 760, 440, 0.24, Waveform.TRIANGLE));
        buffers.put(SoundEffect.HIT, synthSweepAndNoise(85, 210, 80, 0.40, 0.34));
        buffers.put(SoundEffect.CRIT_HIT, synthArpeggio(new double[]{587.3, 880.0, 1174.7}, 55, 0.32));
        buffers.put(SoundEffect.PLAYER_HURT, synthSweep(150, 260, 95, 0.40, Waveform.SAWTOOTH));
        buffers.put(SoundEffect.SHIELD_BLOCK, synthArpeggio(new double[]{660.0, 990.0}, 55, 0.30));
        buffers.put(SoundEffect.PICKUP, synthArpeggio(new double[]{523.3, 659.3, 784.0}, 45, 0.26));
        buffers.put(SoundEffect.POTION, synthArpeggio(new double[]{440.0, 554.4, 659.3}, 60, 0.28));
        buffers.put(SoundEffect.DASH, synthSweepAndNoise(130, 320, 620, 0.32, 0.18));
        buffers.put(SoundEffect.LEVEL_UP, synthArpeggio(new double[]{523.3, 659.3, 784.0, 1046.5}, 85, 0.32));
        buffers.put(SoundEffect.BOSS_ROAR, synthSweep(280, 140, 58, 0.38, Waveform.SAWTOOTH));
        buffers.put(SoundEffect.STAIRS_DESCEND, synthArpeggio(new double[]{392.0, 329.6, 261.6}, 95, 0.28));
        buffers.put(SoundEffect.GAME_OVER, synthArpeggio(new double[]{329.6, 293.7, 261.6, 196.0}, 150, 0.32));
    }

    @Override
    public void play(SoundEffect effect) {
        if (muted || effect == null) {
            return;
        }
        short[] pcm = buffers.get(effect);
        if (pcm == null) {
            return;
        }
        executor.execute(() -> playPcm(pcm));
    }

    private void playPcm(short[] pcm) {
        int minBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufBytes = Math.max(minBytes, pcm.length * 2);
        AudioTrack track = null;
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            track.play();
            track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            track.stop();
        } catch (Throwable ignored) {
        } finally {
            if (track != null) {
                try {
                    track.release();
                } catch (Throwable ignored) {
                }
            }
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
        executor.shutdownNow();
    }

    private enum Waveform {
        SINE,
        TRIANGLE,
        SAWTOOTH
    }

    private static short[] synthSweep(
            int durationMs, double startFreq, double endFreq, double volume, Waveform waveform) {
        int samples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        short[] out = new short[samples];
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
            out[i] = clampToShort(val * env * volume);
        }
        return out;
    }

    private static short[] synthSweepAndNoise(
            int durationMs, double startFreq, double endFreq, double noiseMix, double volume) {
        int samples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        short[] out = new short[samples];
        Random rng = new Random(42);
        double phase = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / samples;
            double freq = startFreq + (endFreq - startFreq) * t;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            double env = Math.pow(1.0 - t, 1.3);
            double tone = Math.sin(phase);
            double noise = rng.nextDouble() * 2.0 - 1.0;
            out[i] = clampToShort((tone * (1.0 - noiseMix) + noise * noiseMix) * env * volume);
        }
        return out;
    }

    private static short[] synthArpeggio(double[] freqs, int noteMs, double volume) {
        int perNote = (int) ((noteMs / 1000.0) * SAMPLE_RATE);
        short[] out = new short[perNote * freqs.length];
        for (int n = 0; n < freqs.length; n++) {
            double freq = freqs[n];
            for (int i = 0; i < perNote; i++) {
                double t = (double) i / perNote;
                double env = (1.0 - t * 0.75) * Math.min(1.0, i / (SAMPLE_RATE * 0.005));
                double phase = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
                double sample = (0.75 * Math.sin(phase) + 0.25 * Math.sin(phase * 2.0)) * env * volume;
                out[n * perNote + i] = clampToShort(sample);
            }
        }
        return out;
    }

    private static short clampToShort(double normalized) {
        double clamped = Math.max(-1.0, Math.min(1.0, normalized));
        return (short) (clamped * Short.MAX_VALUE);
    }
}
