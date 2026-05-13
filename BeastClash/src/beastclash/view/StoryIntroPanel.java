package beastclash.view;

import beastclash.audio.SoundManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * StoryIntroPanel – Prolog Beast Clash: Bangkitnya Zenith.
 *
 * Setiap halaman memiliki latar animasi berbeda:
 *   0 – Langit malam tenang (dunia Arcana)
 *   1 – Meteor-meteor jatuh dari langit
 *   2 – Siluet Zenith muncul dari kegelapan
 *   3 – Dunia retak, kristal Zenith tumbuh
 *   4 – Layar misi / call-to-action
 *   5 – Title card Beast Clash
 */
public class StoryIntroPanel extends JPanel {

    private MainFrame frame;
    private int pageIndex = 0;

    // ── Teks cerita ────────────────────────────────────────────────────────────
    private static final String[] TITLES = {
        "DUNIA ARCANA",
        "MALAM YANG TENANG ROBEK OLEH LANGIT",
        "ZENITH",
        "PECAHAN KEHANCURAN",
        "MISI TERAKHIR",
        "BEAST CLASH"
    };

    private static final String[] PAGES = {
        // Hal. 1 – Dunia normal
        "Di dunia Arcana, makhluk-makhluk legendaris yang disebut BEAST\n"
        + "hidup berdampingan dengan manusia selama ribuan tahun.\n\n"
        + "Enam elemen menjaga keseimbangan alam:\n"
        + "🔥 Api · 💧 Air · 🪨 Tanah · 🌿 Daun · ☀ Cahaya · 🌑 Gelap\n\n"
        + "Para Pelatih Beast menjaga kedamaian bersama sahabat mereka.\n"
        + "Selama berabad-abad, Arcana hidup dalam harmoni...",

        // Hal. 2 – Meteor jatuh
        "Hingga malam itu tiba.\n\n"
        + "Ratusan meteor menyobek langit malam Arcana.\n"
        + "Bumi berguncang. Lautan mendidih. Hutan terbakar.\n\n"
        + "Bukan sembarang meteor — ini adalah PECAHAN.\n"
        + "Pecahan dari sesuatu yang jauh lebih besar.\n"
        + "Sesuatu yang telah lama tertidur di antara bintang-bintang.",

        // Hal. 3 – Zenith diperkenalkan
        "ZENITH.\n\n"
        + "Senjata pemusnah massal berbentuk bintang raksasa yang diciptakan\n"
        + "oleh peradaban kuno yang telah punah.\n\n"
        + "Dirancang untuk menghancurkan dunia-dunia yang dianggap\n"
        + "\"tidak layak\" oleh penciptanya.\n\n"
        + "Setelah ribuan tahun melayang di kegelapan antariksa,\n"
        + "Zenith kini terbangun — dan orbitnya mengarah ke Arcana.",

        // Hal. 4 – Bahaya pecahan
        "Setiap pecahan meteor yang jatuh membawa KRISTAL ZENITH.\n\n"
        + "Kristal ini memancarkan energi korup yang mengubah Beast-Beast\n"
        + "menjadi liar dan menyerang manusia tanpa henti.\n\n"
        + "Jika semua kristal terkumpul di satu titik,\n"
        + "Zenith akan bangkit sepenuhnya dan menghancurkan Arcana\n"
        + "dalam sekejap mata.",

        // Hal. 5 – Misi
        "Kamu adalah seorang Pelatih Beast.\n\n"
        + "Organisasi penjaga Arcana — \"The Wardens\" — telah memilihmu\n"
        + "untuk mengemban satu tugas yang tidak bisa gagal:\n\n"
        + "✦  Hancurkan kristal-kristal Zenith yang tersebar\n"
        + "✦  Kalahkan Beast yang telah terkontaminasi\n"
        + "✦  Cegah Zenith dari kebangkitannya\n\n"
        + "Dunia Arcana bergantung padamu.",

        // Hal. 6 – Title card
        "BEAST CLASH\n\n"
        + "Kumpulkan Beast terkuat.\n"
        + "Jelajahi setiap penjuru Arcana.\n"
        + "Hadapi Zenith — sebelum semuanya berakhir.\n\n"
        + "Petualanganmu dimulai sekarang."
    };

    // ── UI components ─────────────────────────────────────────────────────────
    private JLabel    lblTitle;
    private JTextArea storyText;
    private JButton   btnNext;
    private JLabel    pageLabel;
    private Timer     typeTimer;
    private int       charIdx = 0;

    // ── Animasi ───────────────────────────────────────────────────────────────
    private Timer     animTimer;
    private float     animPhase  = 0f;
    private final List<float[]> meteors   = new ArrayList<>();
    private final List<float[]> particles = new ArrayList<>();
    private final List<float[]> crystals  = new ArrayList<>();
    private final Random        rng       = new Random(42);
    private float               zenithPulse = 0f;
    private float               crackAnim   = 0f;

    public StoryIntroPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(8, 8, 20));
        buildUI();
        startAnimTimer();
        showPage(0);
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUI() {
        // Header: judul halaman
        lblTitle = new JLabel("", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(new Color(255, 220, 80));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(22, 10, 8, 10));
        add(lblTitle, BorderLayout.NORTH);

        // Teks story — semi-transparan agar animasi bg terlihat
        storyText = new JTextArea() {
            @Override protected void paintComponent(Graphics g) {
                // Background semi-transparan
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(5, 5, 18, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
            }
        };
        storyText.setOpaque(false);
        storyText.setEditable(false);
        storyText.setForeground(new Color(220, 225, 255));
        storyText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        storyText.setLineWrap(true);
        storyText.setWrapStyleWord(true);
        storyText.setFocusable(false);
        storyText.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));

        JScrollPane scroll = new JScrollPane(storyText);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        add(scroll, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 22, 20));

        pageLabel = new JLabel("1 / " + PAGES.length, SwingConstants.LEFT);
        pageLabel.setForeground(new Color(120, 120, 160));
        pageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bottom.add(pageLabel, BorderLayout.WEST);

        // Skip
        JButton btnSkip = makeSmallBtn("Skip ⏭");
        btnSkip.setToolTipText("Lewati cerita dan langsung bermain");
        btnSkip.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            if (typeTimer != null) typeTimer.stop();
            if (animTimer != null) animTimer.stop();
            frame.showMainMenu();
        });
        bottom.add(btnSkip, BorderLayout.CENTER);

        // Lanjut / Mulai
        btnNext = new JButton("Lanjut ▶");
        styleMainBtn(btnNext);
        btnNext.addActionListener(e -> onNext());
        bottom.add(btnNext, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        // Klik area cerita = skip animasi ketik
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (typeTimer != null && typeTimer.isRunning()) {
                    typeTimer.stop();
                    storyText.setText(PAGES[pageIndex]);
                    btnNext.setEnabled(true);
                    btnNext.setText(pageIndex == PAGES.length - 1 ? "MULAI ▶" : "Lanjut ▶");
                }
            }
        });
    }

    // ── Logika halaman ────────────────────────────────────────────────────────
    private void showPage(int idx) {
        pageIndex = idx;
        pageLabel.setText((idx + 1) + " / " + PAGES.length);
        lblTitle.setText(TITLES[idx]);
        storyText.setText("");
        btnNext.setEnabled(false);
        charIdx = 0;

        // Reset animasi per halaman
        meteors.clear();
        particles.clear();
        crystals.clear();
        animPhase   = 0f;
        zenithPulse = 0f;
        crackAnim   = 0f;

        if (idx == 1) spawnMeteors(18);
        if (idx == 3) spawnCrystals(12);

        SoundManager.getInstance().playSFX(idx == 0 ? "CLICK" : idx == 2 ? "ULTIMATE" : "SKILL");

        String full = PAGES[idx];
        typeTimer = new Timer(22, e -> {
            if (charIdx <= full.length()) {
                storyText.setText(full.substring(0, charIdx++));
                storyText.setCaretPosition(0);
            } else {
                ((Timer) e.getSource()).stop();
                btnNext.setEnabled(true);
                btnNext.setText(idx == PAGES.length - 1 ? "✦ MULAI PETUALANGAN" : "Lanjut ▶");
            }
        });
        typeTimer.start();
    }

    private void onNext() {
        SoundManager.getInstance().playSFX("CLICK");
        if (pageIndex < PAGES.length - 1) showPage(pageIndex + 1);
        else { if (animTimer != null) animTimer.stop(); frame.showMainMenu(); }
    }

    // ── Animasi timer ─────────────────────────────────────────────────────────
    private void startAnimTimer() {
        animTimer = new Timer(30, e -> {
            animPhase   += 0.02f;
            zenithPulse += 0.04f;
            crackAnim   += 0.015f;
            updateMeteors();
            repaint();
        });
        animTimer.start();
    }

    private void spawnMeteors(int n) {
        for (int i = 0; i < n; i++) {
            float[] m = new float[7];
            resetMeteor(m, true);
            meteors.add(m);
        }
    }

    private void resetMeteor(float[] m, boolean randomY) {
        m[0] = rng.nextFloat() * 600;                       // x
        m[1] = randomY ? rng.nextFloat() * -300 : -30f;    // y
        m[2] = 2.5f + rng.nextFloat() * 4f;                // speed
        m[3] = 20f + rng.nextFloat() * 30f;                // length (trail)
        m[4] = 0.6f + rng.nextFloat() * 0.4f;              // brightness
        m[5] = rng.nextFloat() * 20 - 10;                  // dx (slight angle)
        m[6] = rng.nextFloat() * 6;                         // size
    }

    private void updateMeteors() {
        for (float[] m : meteors) {
            m[0] += m[5] * 0.05f;
            m[1] += m[2];
            if (m[1] > getHeight() + 50) {
                resetMeteor(m, false);
                // Spawn partikel impact
                for (int i = 0; i < 8; i++) {
                    float[] p = { m[0], getHeight(),
                        (rng.nextFloat() - 0.5f) * 6,
                        -rng.nextFloat() * 4,
                        rng.nextFloat() * 4 + 2,
                        1f };
                    particles.add(p);
                }
            }
        }
        particles.removeIf(p -> {
            p[0] += p[2]; p[1] += p[3]; p[3] += 0.2f; p[5] -= 0.04f;
            return p[5] <= 0;
        });
    }

    private void spawnCrystals(int n) {
        for (int i = 0; i < n; i++) {
            crystals.add(new float[]{
                50 + rng.nextFloat() * 500,
                100 + rng.nextFloat() * 300,
                20 + rng.nextFloat() * 40,
                rng.nextFloat() * (float)Math.PI,
                rng.nextFloat()
            });
        }
    }

    // ── paintComponent ────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int W = getWidth(), H = getHeight();
        switch (pageIndex) {
            case 0: drawStarField(g2, W, H, 0.3f);  break;
            case 1: drawMeteorScene(g2, W, H);       break;
            case 2: drawZenithScene(g2, W, H);       break;
            case 3: drawCrystalScene(g2, W, H);      break;
            case 4: drawMissionScene(g2, W, H);      break;
            case 5: drawTitleScene(g2, W, H);        break;
            default: drawStarField(g2, W, H, 0.3f); break;
        }
    }

    // ── Scene 0: Langit bintang tenang ────────────────────────────────────────
    private void drawStarField(Graphics2D g2, int W, int H, float brightness) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(5, 5, 20), 0, H, new Color(10, 8, 35));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);
        Random r = new Random(99);
        for (int i = 0; i < 120; i++) {
            float a = Math.max(0f, Math.min(1f,
                (float)(brightness + 0.4 * Math.sin(animPhase + i * 0.5))));
            g2.setColor(new Color(1f, 1f, 1f, a));
            int x = r.nextInt(W), y = r.nextInt(H);
            int s = (i % 10 == 0) ? 3 : 1;
            g2.fillOval(x, y, s, s);
        }
    }

    // ── Scene 1: Meteor jatuh ─────────────────────────────────────────────────
    private void drawMeteorScene(Graphics2D g2, int W, int H) {
        // Langit merah-oranye ominous
        GradientPaint sky = new GradientPaint(0, 0, new Color(8, 5, 25),
            0, H, new Color(40, 12, 8));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);
        drawStarField(g2, W, H, 0.15f);

        // Garis cakrawala
        g2.setColor(new Color(20, 10, 5));
        g2.fillRect(0, H - 60, W, 60);
        GradientPaint ground = new GradientPaint(0, H-60, new Color(60, 20, 5), 0, H, new Color(20, 8, 3));
        g2.setPaint(ground);
        g2.fillRect(0, H - 60, W, 60);

        // Partikel impact di tanah
        for (float[] p : particles) {
            float a = Math.max(0f, Math.min(1f, p[5]));
            g2.setColor(new Color(1f, 0.5f, 0.1f, a));
            g2.fillOval((int)p[0], (int)p[1], (int)p[4], (int)p[4]);
        }

        // Meteor trails
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (float[] m : meteors) {
            float b = Math.max(0f, Math.min(1f, m[4]));
            // Trail
            GradientPaint trail = new GradientPaint(
                m[0], m[1] - m[3], new Color(1f, 0.7f, 0.2f, b),
                m[0], m[1],         new Color(1f, 0.3f, 0.05f, 0f));
            g2.setPaint(trail);
            g2.draw(new Line2D.Float(m[0], m[1] - m[3], m[0], m[1]));
            // Kepala meteor
            g2.setColor(new Color(1f, 0.9f, 0.7f, b));
            float sz = m[6];
            g2.fillOval((int)(m[0] - sz/2), (int)(m[1] - sz/2), (int)sz, (int)sz);
        }
        g2.setStroke(new BasicStroke(1));

        // Kilatan cahaya di langit
        float flash = Math.max(0f, (float)Math.sin(animPhase * 3) * 0.08f);
        g2.setColor(new Color(1f, 0.6f, 0.1f, flash));
        g2.fillRect(0, 0, W, H);
    }

    // ── Scene 2: Siluet Zenith ────────────────────────────────────────────────
    private void drawZenithScene(Graphics2D g2, int W, int H) {
        // Latar kosmik ungu gelap
        GradientPaint cosmos = new GradientPaint(0, 0, new Color(3, 0, 15),
            W, H, new Color(12, 0, 30));
        g2.setPaint(cosmos);
        g2.fillRect(0, 0, W, H);
        drawStarField(g2, W, H, 0.2f);

        // Halo raksasa Zenith (dari luar layar / atas)
        float pulse = (float)(0.5 + 0.5 * Math.sin(zenithPulse));
        int cx = W / 2, cy = -60;
        int r1 = 280, r2 = 320;

        // Glow lapis luar
        for (int i = 0; i < 6; i++) {
            float a = Math.max(0f, (0.06f - i * 0.01f) * pulse);
            g2.setColor(new Color(0.5f, 0.0f, 1.0f, a));
            int r = r2 + i * 18;
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        // Inti
        RadialGradientPaint core = new RadialGradientPaint(
            new Point2D.Float(cx, cy), r1,
            new float[]{0f, 0.4f, 1f},
            new Color[]{
                new Color(200, 100, 255, 200),
                new Color(80, 0, 160, 120),
                new Color(0, 0, 0, 0)
            });
        g2.setPaint(core);
        g2.fillOval(cx - r1, cy - r1, r1 * 2, r1 * 2);

        // Silhouette: bintang bersudut 8 (Zenith)
        g2.setColor(new Color(5, 0, 15, 220));
        drawStarShape(g2, cx, cy, 180, 80, 8);

        // Ring berputar
        g2.setColor(new Color(180, 80, 255, (int)(80 * pulse)));
        g2.setStroke(new BasicStroke(2));
        float ra = animPhase;
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(ra);
        g2.drawOval(-220, -30, 440, 60);
        g2.rotate(-ra * 1.6);
        g2.drawOval(-190, -25, 380, 50);
        g2.setTransform(old);
        g2.setStroke(new BasicStroke(1));

        // Teks "ZENITH" samar di background
        g2.setFont(new Font("Segoe UI", Font.BOLD, 72));
        g2.setColor(new Color(100, 0, 200, (int)(30 * pulse)));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("ZENITH", (W - fm.stringWidth("ZENITH")) / 2, H / 2 + 40);
    }

    // ── Scene 3: Kristal Zenith merusak dunia ────────────────────────────────
    private void drawCrystalScene(Graphics2D g2, int W, int H) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(5, 2, 18),
            0, H, new Color(15, 5, 8));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);
        drawStarField(g2, W, H, 0.15f);

        // Retakan di tanah
        g2.setColor(new Color(150, 30, 200, 80));
        g2.setStroke(new BasicStroke(2));
        Random r = new Random(77);
        for (int i = 0; i < 14; i++) {
            int x1 = r.nextInt(W), y1 = H/2 + r.nextInt(H/2);
            int x2 = x1 + r.nextInt(100) - 50, y2 = y1 + r.nextInt(80);
            g2.drawLine(x1, y1, x2, y2);
            // Glow retakan
            g2.setColor(new Color(180, 60, 255, 30));
            g2.drawLine(x1-1, y1, x2-1, y2);
        }
        g2.setStroke(new BasicStroke(1));

        // Kristal-kristal
        for (float[] c : crystals) {
            float pulse = (float)(0.5 + 0.5 * Math.sin(crackAnim * 2 + c[4] * 3));
            drawCrystal(g2, (int)c[0], (int)c[1], (int)c[2], c[3], pulse);
        }

        // Aura ungu dari bawah
        GradientPaint aura = new GradientPaint(0, H, new Color(100, 0, 180, 120),
            0, H/2, new Color(0, 0, 0, 0));
        g2.setPaint(aura);
        g2.fillRect(0, 0, W, H);
    }

    // ── Scene 4: Misi ────────────────────────────────────────────────────────
    private void drawMissionScene(Graphics2D g2, int W, int H) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(5, 10, 25),
            0, H, new Color(8, 15, 40));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);
        drawStarField(g2, W, H, 0.25f);

        // Emblem The Wardens — lingkaran cahaya emas
        int cx = W / 2, cy = 85;
        float p = (float)(0.7 + 0.3 * Math.sin(animPhase * 1.5));
        for (int i = 4; i >= 0; i--) {
            g2.setColor(new Color(1f, 0.85f, 0.2f, 0.04f * p * (5 - i)));
            g2.fillOval(cx - 45 - i*6, cy - 45 - i*6, (90+i*12), (90+i*12));
        }
        g2.setColor(new Color(255, 200, 40, (int)(200 * p)));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(cx - 40, cy - 40, 80, 80);
        drawStarShape(g2, cx, cy, 28, 12, 5);
        g2.setStroke(new BasicStroke(1));

        // Label organisasi
        g2.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 11));
        g2.setColor(new Color(200, 170, 80, 180));
        FontMetrics fm = g2.getFontMetrics();
        String warden = "— THE WARDENS —";
        g2.drawString(warden, (W - fm.stringWidth(warden)) / 2, cy + 58);
    }

    // ── Scene 5: Title card ───────────────────────────────────────────────────
    private void drawTitleScene(Graphics2D g2, int W, int H) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(2, 5, 20),
            0, H, new Color(10, 20, 50));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);
        drawStarField(g2, W, H, 0.35f);

        // Aurora di bawah
        for (int i = 0; i < 5; i++) {
            float a = Math.max(0f, (float)(0.06 + 0.04 * Math.sin(animPhase + i)));
            float hue = (0.55f + i * 0.05f) % 1f;
            Color ac = Color.getHSBColor(hue, 0.8f, 0.9f);
            g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), (int)(a*255)));
            g2.fillRect(0, H - 80 - i * 25, W, 60);
        }

        // Halo logo
        float p = (float)(0.6 + 0.4 * Math.sin(zenithPulse * 0.8));
        int cx = W / 2, cy = H / 2 - 30;
        for (int i = 3; i >= 0; i--) {
            g2.setColor(new Color(0.3f, 0.6f, 1f, 0.05f * p));
            int rr = 120 + i * 20;
            g2.fillOval(cx - rr, cy - rr / 3, rr * 2, rr * 2 / 3);
        }

        // Teks "BEAST CLASH" besar
        g2.setFont(new Font("Segoe UI", Font.BOLD, 42));
        GradientPaint textGrad = new GradientPaint(
            cx - 150, cy - 30, new Color(100, 200, 255),
            cx + 150, cy + 10, new Color(200, 100, 255));
        g2.setPaint(textGrad);
        FontMetrics fm = g2.getFontMetrics();
        String t = "BEAST CLASH";
        g2.drawString(t, (W - fm.stringWidth(t)) / 2, cy);

        // Subtitle
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        g2.setColor(new Color(180, 200, 255, (int)(180 * p)));
        String sub = "Cegah kebangkitan Zenith";
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(sub, (W - fm2.stringWidth(sub)) / 2, cy + 28);
    }

    // ── Helper draw ───────────────────────────────────────────────────────────
    private void drawStarShape(Graphics2D g2, int cx, int cy, int outerR, int innerR, int points) {
        int[] xs = new int[points * 2], ys = new int[points * 2];
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI / points * i - Math.PI / 2;
            int r = (i % 2 == 0) ? outerR : innerR;
            xs[i] = cx + (int)(r * Math.cos(angle));
            ys[i] = cy + (int)(r * Math.sin(angle));
        }
        g2.fillPolygon(xs, ys, points * 2);
    }

    private void drawCrystal(Graphics2D g2, int cx, int cy, int h, float angle, float pulse) {
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(angle);
        int w = h / 3;
        // Glow
        g2.setColor(new Color(160, 40, 255, (int)(50 * pulse)));
        g2.fillOval(-w - 4, -h/2 - 4, (w + 4) * 2, h + 8);
        // Badan kristal
        int[] px = {0, w, w/2, 0, -w/2, -w};
        int[] py = {-h/2, 0, h/2, h/3, h/2, 0};
        g2.setColor(new Color(120, 20, 200, (int)(180 * pulse)));
        g2.fillPolygon(px, py, 6);
        g2.setColor(new Color(200, 100, 255, (int)(220 * pulse)));
        g2.setStroke(new BasicStroke(1));
        g2.drawPolygon(px, py, 6);
        g2.setTransform(old);
        g2.setStroke(new BasicStroke(1));
    }

    // ── Button helpers ────────────────────────────────────────────────────────
    private JButton makeSmallBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setBackground(new Color(35, 35, 55));
        b.setForeground(new Color(160, 160, 200));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleMainBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(new Color(255, 200, 40));
        btn.setForeground(new Color(30, 20, 0));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 150, 0), 1),
            BorderFactory.createEmptyBorder(6, 18, 6, 18)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
