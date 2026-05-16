package beastclash.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * SoundManager – semua suara di-generate secara procedural (PCM synthesis).
 * Tidak perlu file audio eksternal (.wav / .mp3).
 *
 * FIX:
 *  1. setVol() diperbaiki: gunakan rumus dB yang benar (20*log10) bukan linear.
 *  2. Volume tidak lagi di-baked ke PCM saat generate — semua dikontrol lewat Clip gain.
 *  3. openClip() lebih robust: cetak error ke stderr agar mudah di-debug.
 *  4. BGM thread diberi nama dan di-daemon agar tidak mencegah JVM exit.
 *  5. Fallback: jika AudioSystem tidak tersedia, semua operasi no-op tanpa crash.
 *
 * BGM tracks : MENU, BATTLE, VICTORY, STORY
 * SFX names  : ATTACK, SKILL, ULTIMATE, HURT, VICTORY_SFX,
 *              DEFEAT, GACHA, FREEZE, CLICK, RUN, EGG, UNLOCK
 */
public class SoundManager {

    private static SoundManager instance;
    private static final int SR = 44100; // sample rate

    private boolean sfxOn  = true;
    private boolean bgmOn  = true;
    private float   bgmVol = 0.55f;  // 0.0 – 1.0
    private float   sfxVol = 0.75f;  // 0.0 – 1.0

    // Flag: apakah audio device tersedia di sistem ini
    private final boolean audioAvailable;

    private Clip   bgmClip    = null;
    private String currentBgm = "";
    private final Map<String, byte[]> sfxCache = new ConcurrentHashMap<>();

    private SoundManager() {
        // Deteksi apakah Clip line benar-benar tersedia di sistem ini
        boolean avail = false;
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers == null || mixers.length == 0) {
                System.err.println("[Audio] Tidak ada audio device ditemukan.");
            } else {
                // Coba buat Clip kecil (silence) untuk verifikasi
                AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
                byte[] silence = new byte[2048];
                Clip testClip = AudioSystem.getClip();
                testClip.open(new AudioInputStream(
                    new java.io.ByteArrayInputStream(silence), fmt, silence.length / 2L));
                testClip.close();
                avail = true;
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println("[Audio] Audio tidak tersedia: " + msg);
        }
        audioAvailable = avail;
        if (avail) {
            System.out.println("[Audio] Audio siap.");
            // Pre-build semua SFX di background agar tidak ada race condition
            // saat pertama kali dipanggil dari thread berbeda
            Thread preload = new Thread(() -> {
                String[] names = {"ATTACK","SKILL","ULTIMATE","HURT","VICTORY_SFX",
                                  "DEFEAT","GACHA","FREEZE","CLICK","RUN","EGG","UNLOCK"};
                for (String n : names) {
                    try { sfxCache.put(n, buildSFX(n)); }
                    catch (Exception e) { System.err.println("[Audio] Preload gagal: " + n); }
                }
            }, "SFX-Preload");
            preload.setDaemon(true);
            preload.start();
        }
    }

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    // ── BGM ──────────────────────────────────────────────────────────────────
    public synchronized void playBGM(String track) {
        if (!bgmOn || !audioAvailable) return;
        if (track.equals(currentBgm) && bgmClip != null && bgmClip.isRunning()) return;
        stopBGM();
        currentBgm = track;
        Thread t = new Thread(() -> {
            try {
                byte[] pcm = buildBGM(track);
                // Cek apakah track masih sama (belum di-stop oleh playBGM lain)
                if (!track.equals(currentBgm)) return;
                Clip clip = openClip(pcm);
                if (clip == null) return;
                setVol(clip, bgmVol);
                synchronized (SoundManager.this) {
                    // Cek lagi setelah dapat lock
                    if (!track.equals(currentBgm)) { clip.close(); return; }
                    bgmClip = clip;
                }
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
            } catch (Exception e) {
                System.err.println("[Audio] BGM error (" + track + "): " + e.getMessage());
            }
        }, "BGM-" + track);
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stopBGM() {
        currentBgm = "";
        if (bgmClip != null) {
            try { bgmClip.stop(); bgmClip.close(); } catch (Exception ignored) {}
            bgmClip = null;
        }
    }

    // ── SFX ──────────────────────────────────────────────────────────────────
    public void playSFX(String name) {
        if (!sfxOn || !audioAvailable) return;
        Thread t = new Thread(() -> {
            try {
                byte[] pcm = sfxCache.computeIfAbsent(name, this::buildSFX);
                Clip c = openClip(pcm);
                if (c == null) return;
                setVol(c, sfxVol);
                c.start();
                c.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) c.close();
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                System.err.println("[Audio] SFX error (" + name + "): " + msg);
            }
        }, "SFX-" + name);
        t.setDaemon(true);
        t.start();
    }

    // ── BGM generators ───────────────────────────────────────────────────────
    // FIX: volume TIDAK lagi di-multiply ke PCM saat generate.
    //      Semua volume control dilakukan lewat Clip gain di setVol().
    private byte[] buildBGM(String t) {
        int n = SR * 8;
        byte[] b = new byte[n * 2];
        switch (t) {
            case "MENU":    menuBGM(b, n);    break;
            case "BATTLE":  battleBGM(b, n);  break;
            case "VICTORY": victoryBGM(b, n); break;
            default:        storyBGM(b, n);   break;
        }
        return b;
    }

    private void menuBGM(byte[] b, int n) {
        int[] hz = {262, 294, 330, 392, 440, 392, 330, 294};
        int nd = SR / 2;
        for (int i = 0; i < n; i++) {
            float f = hz[(i / nd) % hz.length];
            float t = (float) i / SR;
            float v = (float)(Math.sin(2 * Math.PI * f * t) * 0.55
                    + Math.sin(2 * Math.PI * f * 2 * t) * 0.12);
            float e = envelope(i % nd, nd);
            write(b, i, v * e * 13000);
        }
    }

    private void battleBGM(byte[] b, int n) {
        int[] bass = {110, 110, 147, 131, 110, 110, 147, 165};
        int nd = SR / 4;
        for (int i = 0; i < n; i++) {
            float t   = (float) i / SR;
            float bf  = bass[(i / nd) % bass.length];
            float saw   = (float)((t * bf % 1.0) * 2 - 1) * 0.35f;
            float pulse = (float) Math.sin(2 * Math.PI * bf * 2 * t) * 0.25f;
            int ki = i % (SR / 2);
            float kick = ki < 600 ? (float) Math.exp(-ki * 0.008) * 0.5f : 0;
            write(b, i, (saw + pulse + kick) * 10000);
        }
    }

    private void victoryBGM(byte[] b, int n) {
        int[] chord = {523, 659, 784, 1047};
        for (int i = 0; i < n; i++) {
            float t = (float) i / SR, v = 0;
            for (int f : chord) v += Math.sin(2 * Math.PI * f * t) * 0.18f;
            write(b, i, v * (float) Math.exp(-t * 0.25) * 14000);
        }
    }

    private void storyBGM(byte[] b, int n) {
        int[] pad = {196, 247, 294, 370};
        for (int i = 0; i < n; i++) {
            float t = (float) i / SR, v = 0;
            for (int j = 0; j < pad.length; j++)
                v += Math.sin(2 * Math.PI * pad[j] * t + j * 0.3) * 0.14f;
            v *= (0.8 + 0.2 * Math.sin(2 * Math.PI * 3 * t));
            write(b, i, v * 11000);
        }
    }

    // ── SFX generators ───────────────────────────────────────────────────────
    private byte[] buildSFX(String name) {
        switch (name) {
            case "ATTACK":      return sfxPunch();
            case "SKILL":       return sfxRise(300, 0.4, 18000);
            case "ULTIMATE":    return sfxBoom();
            case "HURT":        return sfxNoise(400, 0.15, 14000);
            case "VICTORY_SFX": return sfxFanfare(new int[]{523, 659, 784, 1047}, SR / 6);
            case "DEFEAT":      return sfxDescend();
            case "GACHA":       return sfxGacha();
            case "FREEZE":      return sfxCrystal();
            case "CLICK":       return sfxTick();
            case "RUN":         return sfxRise(600, 0.25, 13000);
            case "EGG":         return sfxCrack();
            case "UNLOCK":      return sfxFanfare(new int[]{784, 880, 1047, 1319}, SR / 8);
            default:            return sfxTick();
        }
    }

    private byte[] sfxPunch() {
        int d = SR / 5; byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            float t = (float) i / SR;
            float v = (float)(Math.sin(2 * Math.PI * 80 * t) + rnd() * 0.3)
                    * (float) Math.exp(-i * 0.015);
            write(b, i, v * 20000);
        }
        return b;
    }

    private byte[] sfxBoom() {
        int d = SR * 3 / 4; byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            float t = (float) i / SR;
            float v = (float)(Math.sin(2 * Math.PI * 60 * t) * 0.45
                    + Math.sin(2 * Math.PI * 120 * t) * 0.3 + rnd() * 0.35)
                    * (float) Math.exp(-i * 0.005);
            write(b, i, v * 22000);
        }
        return b;
    }

    private byte[] sfxRise(float startHz, double sec, float amp) {
        int d = (int)(SR * sec); byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            float t = (float) i / SR;
            float f = startHz + i * 0.8f;
            float v = (float)(Math.sin(2 * Math.PI * f * t))
                    * (float) Math.exp(-i * 0.008);
            write(b, i, v * amp);
        }
        return b;
    }

    private byte[] sfxNoise(float hz, double sec, float amp) {
        int d = (int)(SR * sec); byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            float t = (float) i / SR;
            float v = (float)(Math.sin(2 * Math.PI * hz * t) + rnd() * 0.5)
                    * (float) Math.exp(-i * 0.02);
            write(b, i, v * amp);
        }
        return b;
    }

    private byte[] sfxFanfare(int[] notes, int nd) {
        // Gunakan nd minimal SR/6 (≈7350 sample per not)
        nd = Math.max(nd, SR / 6);
        int d = notes.length * nd;
        byte[] b = new byte[d * 2];

        for (int noteIdx = 0; noteIdx < notes.length; noteIdx++) {
            double freq  = notes[noteIdx];
            // Phase accumulator per not — tidak ada t*f yang overflow
            double phase1 = 0, phase2 = 0, phase3 = 0;
            double inc1 = 2 * Math.PI * freq       / SR;
            double inc2 = 2 * Math.PI * freq * 2.0 / SR;
            double inc3 = 2 * Math.PI * freq * 3.0 / SR;

            for (int s = 0; s < nd; s++) {
                int globalIdx = noteIdx * nd + s;
                // Decay: 0->1 (attack 20ms) lalu turun perlahan
                float env = (float) Math.exp(-s * 3.5 / SR);
                if (s < 882) env *= (s / 882f); // attack 20ms
                float v = (float)(Math.sin(phase1) * 0.60
                                + Math.sin(phase2) * 0.25
                                + Math.sin(phase3) * 0.10) * env;
                write(b, globalIdx, v * 20000);
                phase1 += inc1; phase2 += inc2; phase3 += inc3;
                // Wrap phase agar tidak ke infinity
                if (phase1 > 2 * Math.PI) { phase1 -= 2 * Math.PI; phase2 -= 4 * Math.PI; phase3 -= 6 * Math.PI; }
            }
        }
        return b;
    }

    private byte[] sfxGacha() {
        // Whoosh naik lalu chime akhir — pakai phase accumulator
        int d = (int)(SR * 1.2); byte[] b = new byte[d * 2];
        double phase = 0;
        double chimePhase = 0;
        double chimeInc = 2 * Math.PI * 1047.0 / SR;

        for (int i = 0; i < d; i++) {
            float t = (float) i / SR;
            // Frekuensi whoosh: 150 -> 600 Hz dalam 0.6 detik lalu berhenti
            double freq = Math.min(600, 150 + i * 0.012);
            double inc  = 2 * Math.PI * freq / SR;
            phase += inc;
            if (phase > 2 * Math.PI) phase -= 2 * Math.PI;

            float envWhoosh = (float) Math.exp(-t * 1.8f);
            float whoosh = (float) Math.sin(phase) * 0.5f * envWhoosh;

            // Chime mulai di 0.5 detik
            float chime = 0;
            if (i > SR / 2) {
                chimePhase += chimeInc;
                if (chimePhase > 2 * Math.PI) chimePhase -= 2 * Math.PI;
                float ct = t - 0.5f;
                chime = (float)(Math.sin(chimePhase) * Math.exp(-ct * 5.0) * 0.7f);
            }

            write(b, i, (whoosh + chime) * 20000);
        }
        return b;
    }

    private byte[] sfxDescend() {
        int d = SR * 3 / 4; byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            float t = (float) i / SR;
            float f = Math.max(80, 420 - i * 0.22f);
            float v = (float) Math.sin(2 * Math.PI * f * t)
                    * (float) Math.exp(-i * 0.004);
            write(b, i, v * 16000);
        }
        return b;
    }

    private byte[] sfxCrystal() {
        int d = SR / 3; byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            float t = (float) i / SR;
            float v = (rnd() * 0.55f + (float) Math.sin(2 * Math.PI * 2000 * t) * 0.4f)
                    * (float) Math.exp(-i * 0.01);
            write(b, i, v * 12000);
        }
        return b;
    }

    private byte[] sfxTick() {
        int d = SR / 20; byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            write(b, i, rnd() * (float) Math.exp(-i * 0.04) * 10000);
        }
        return b;
    }

    private byte[] sfxCrack() {
        int d = SR / 5; byte[] b = new byte[d * 2];
        for (int i = 0; i < d; i++) {
            float v = (rnd() * 0.8f + (float) Math.sin(2 * Math.PI * 150 * i / SR) * 0.3f)
                    * (float) Math.exp(-i * 0.025);
            write(b, i, v * 18000);
        }
        return b;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private float rnd() { return (float)(Math.random() - 0.5) * 2; }

    private float envelope(int pos, int total) {
        float attack  = Math.min(1f, pos / 800f);
        float release = 1f - Math.min(1f, (float)(pos - total + 600) / 600f);
        return Math.min(attack, release);
    }

    private void write(byte[] b, int i, float v) {
        // Guard: NaN atau Infinity dari kalkulasi sin/exp -> tulis 0 agar tidak corrupt
        if (!Float.isFinite(v)) v = 0f;
        short s = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
        b[i * 2]     = (byte)(s & 0xFF);
        b[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
    }

    /**
     * openClip – coba 3 strategi berurutan agar kompatibel di semua driver Windows.
     *
     * Strategi 1: AudioSystem.getClip() langsung (paling kompatibel di Windows)
     * Strategi 2: DataLine.Info eksplisit (fallback JVM lain)
     * Strategi 3: Format stereo 16-bit (beberapa driver tidak support mono)
     *
     * Error "null" terjadi ketika LineUnavailableException.getMessage() == null —
     * sekarang kita log class name sebagai gantinya.
     */
    private Clip openClip(byte[] pcm) {
        AudioFormat fmtMono   = new AudioFormat(SR, 16, 1, true, false);
        AudioFormat fmtStereo = new AudioFormat(SR, 16, 2, true, false);

        // Strategi 1: getClip() langsung — paling kompatibel Windows
        try {
            Clip c = AudioSystem.getClip();
            c.open(new AudioInputStream(new ByteArrayInputStream(pcm), fmtMono, pcm.length / 2L));
            return c;
        } catch (Exception e1) {
            String msg1 = e1.getMessage() != null ? e1.getMessage() : e1.getClass().getSimpleName();

            // Strategi 2: DataLine.Info eksplisit
            try {
                DataLine.Info info = new DataLine.Info(Clip.class, fmtMono);
                if (AudioSystem.isLineSupported(info)) {
                    Clip c = (Clip) AudioSystem.getLine(info);
                    c.open(new AudioInputStream(new ByteArrayInputStream(pcm), fmtMono, pcm.length / 2L));
                    return c;
                }
            } catch (Exception e2) { /* lanjut ke strategi 3 */ }

            // Strategi 3: stereo (duplikasi channel mono ke stereo)
            try {
                byte[] stereo = monoToStereo(pcm);
                DataLine.Info info2 = new DataLine.Info(Clip.class, fmtStereo);
                if (AudioSystem.isLineSupported(info2)) {
                    Clip c = (Clip) AudioSystem.getLine(info2);
                    c.open(new AudioInputStream(new ByteArrayInputStream(stereo), fmtStereo, stereo.length / 4L));
                    return c;
                }
            } catch (Exception e3) { /* semua gagal */ }

            System.err.println("[Audio] Semua strategi gagal: " + msg1);
            return null;
        }
    }

    /** Duplikasi channel mono (16-bit LE) menjadi stereo interleaved. */
    private byte[] monoToStereo(byte[] mono) {
        byte[] stereo = new byte[mono.length * 2];
        for (int i = 0; i < mono.length / 2; i++) {
            // Setiap sample 2 byte -> copy ke L dan R
            stereo[i * 4]     = mono[i * 2];
            stereo[i * 4 + 1] = mono[i * 2 + 1];
            stereo[i * 4 + 2] = mono[i * 2];
            stereo[i * 4 + 3] = mono[i * 2 + 1];
        }
        return stereo;
    }

    /**
     * FIX: setVol menggunakan konversi linear->dB yang benar.
     *
     * MASTER_GAIN bekerja dalam desibel (dB), bukan linear.
     * Rumus yang benar: dB = 20 * log10(volume), di-clamp ke [min, max] Clip.
     *
     * Rumus lama: min + (max-min)*v  -> SALAH, menghasilkan volume terlalu kecil.
     */
    private void setVol(Clip c, float vol) {
        if (c == null) return;
        try {
            FloatControl fc = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            float min = fc.getMinimum();  // biasanya -80 dB
            float max = fc.getMaximum();  // biasanya  6 dB

            float dB;
            if (vol <= 0f) {
                dB = min;
            } else {
                // Konversi linear (0.0-1.0) ke desibel
                dB = (float)(20.0 * Math.log10(vol));
                // Clamp ke range yang didukung hardware
                dB = Math.max(min, Math.min(max, dB));
            }
            fc.setValue(dB);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Beberapa driver tidak support MASTER_GAIN, coba VOLUME
            try {
                FloatControl fc2 = (FloatControl) c.getControl(FloatControl.Type.VOLUME);
                fc2.setValue(Math.max(fc2.getMinimum(), Math.min(fc2.getMaximum(), vol)));
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    // ── Settings ──────────────────────────────────────────────────────────────
    public void setSFXEnabled(boolean en) { sfxOn = en; }
    public void setBGMEnabled(boolean en) {
        bgmOn = en;
        if (!en) stopBGM();
    }
    public boolean isSFXEnabled()  { return sfxOn; }
    public boolean isBGMEnabled()  { return bgmOn; }
    public void setBGMVolume(float v) {
        bgmVol = clamp(v);
        // Update volume BGM yang sedang diputar secara langsung
        if (bgmClip != null) setVol(bgmClip, bgmVol);
    }
    public void setSFXVolume(float v) { sfxVol = clamp(v); }
    public float getBGMVolume() { return bgmVol; }
    public float getSFXVolume() { return sfxVol; }
    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
