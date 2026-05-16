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
 * EndingPanel – ditampilkan setelah player mengumpulkan semua 24 Beast.
 * Zenith tidak jadi bangkit, Arcana selamat. The End.
 */
public class EndingPanel extends JPanel {

    private final MainFrame frame;
    private Timer animTimer;
    private float phase     = 0f;
    private float textPhase = 0f;

    // Partikel cahaya
    private final List<float[]> sparks = new ArrayList<>();
    private final Random rng = new Random(7);

    // Teks ending
    private static final String[] LINES = {
        "Semua kristal Zenith telah dihancurkan.",
        "Beast-beast Arcana kembali hidup dalam damai.",
        "Zenith tidak jadi bangkit.",
        "Dunia Arcana terselamatkan.",
        "",
        "Di bawah langit biru yang tenang,",
        "manusia dan Beast kembali berdampingan —",
        "seperti ribuan tahun sebelumnya.",
        "",
        "Kamu, sang Pelatih, kini menjadi legenda.",
        "The Wardens menorehkan namamu",
        "di Menara Bintang Arcana untuk selamanya.",
        "",
        "— T H E   E N D —"
    };

    private int visibleLines = 0;
    private Timer lineTimer;
    private int charIdx = 0;
    private String currentDisplayText = "";
    private JTextArea textArea;
    private StringBuilder fullText = new StringBuilder();

    public EndingPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        buildUI();
        spawnSparks(60);
        startAnim();
        SoundManager.getInstance().playBGM("VICTORY");
        startRevealText();
    }

    private void buildUI() {
        // Overlay teks di tengah
        JPanel center = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) { /* transparan */ }
        };
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(60, 60, 40, 60));

        textArea = new JTextArea();
        textArea.setOpaque(false);
        textArea.setEditable(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        textArea.setForeground(new Color(220, 230, 255));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // Tombol kembali (muncul setelah semua teks tampil)
        JButton btnBack = new JButton("<- Kembali ke Menu");
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBack.setBackground(new Color(40, 60, 120));
        btnBack.setForeground(Color.WHITE);
        btnBack.setBorderPainted(false);
        btnBack.setFocusPainted(false);
        btnBack.setVisible(false);
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> {
            if (animTimer != null) animTimer.stop();
            SoundManager.getInstance().playBGM("MENU");
            frame.showMainMenu();
        });

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(btnBack);
        add(south, BorderLayout.SOUTH);

        // Klik layar untuk skip reveal
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (lineTimer != null && lineTimer.isRunning()) {
                    lineTimer.stop();
                    fullText.setLength(0);
                    for (String l : LINES) fullText.append(l).append("\n");
                    textArea.setText(fullText.toString());
                    btnBack.setVisible(true);
                }
            }
        });

        // Jadwalkan munculnya tombol
        new Timer(LINES.length * 800 + 3000, e -> {
            btnBack.setVisible(true);
            ((Timer)e.getSource()).stop();
        }).start();
    }

    private void startRevealText() {
        lineTimer = new Timer(800, null);
        lineTimer.addActionListener(e -> {
            if (visibleLines < LINES.length) {
                fullText.append(LINES[visibleLines]).append("\n");
                textArea.setText(fullText.toString());
                textArea.setCaretPosition(0);
                visibleLines++;
            } else {
                lineTimer.stop();
            }
        });
        lineTimer.start();
    }

    private void spawnSparks(int n) {
        for (int i = 0; i < n; i++) {
            sparks.add(new float[]{
                rng.nextFloat() * 600,          // x
                rng.nextFloat() * 500,          // y
                (rng.nextFloat() - .5f) * .8f,  // vx
                -(rng.nextFloat() * 1.5f + .3f),// vy
                rng.nextFloat() * 4 + 2,        // size
                rng.nextFloat(),                // alpha
                rng.nextFloat() * 3             // hue offset
            });
        }
    }

    private void startAnim() {
        animTimer = new Timer(30, e -> {
            phase     += 0.015f;
            textPhase += 0.025f;
            for (float[] s : sparks) {
                s[0] += s[2];
                s[1] += s[3];
                s[5] -= 0.005f;
                if (s[5] <= 0 || s[1] < -10) resetSpark(s);
            }
            repaint();
        });
        animTimer.start();
    }

    private void resetSpark(float[] s) {
        s[0] = rng.nextFloat() * getWidth();
        s[1] = getHeight() + 10;
        s[2] = (rng.nextFloat() - .5f) * .8f;
        s[3] = -(rng.nextFloat() * 1.5f + .3f);
        s[5] = rng.nextFloat() * .8f + .2f;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int W = getWidth(), H = getHeight();

        // ── Latar: langit fajar biru-emas ─────────────────────────────────────
        GradientPaint sky = new GradientPaint(
            0, 0,    new Color(5, 10, 30),
            0, H*.6f, new Color(15, 30, 80));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);

        // Horizon hangat
        GradientPaint horizon = new GradientPaint(
            0, (int)(H*.55f), new Color(255, 180, 60, 0),
            0, (int)(H*.75f), new Color(255, 140, 20, 100));
        g2.setPaint(horizon);
        g2.fillRect(0, (int)(H*.55f), W, H);

        // Langit bawah (daratan)
        GradientPaint land = new GradientPaint(
            0, (int)(H*.70f), new Color(30, 60, 20),
            0, H,              new Color(10, 30, 10));
        g2.setPaint(land);
        g2.fillRect(0, (int)(H*.70f), W, H - (int)(H*.70f));

        // Bintang fajar (pelan memudar)
        Random sr = new Random(42);
        for (int i = 0; i < 80; i++) {
            float a = Math.max(0f, Math.min(1f,
                (float)(0.3 + 0.3 * Math.sin(phase + i * .4))));
            g2.setColor(new Color(1f, 1f, 1f, a * .6f));
            g2.fillOval(sr.nextInt(W), sr.nextInt((int)(H*.55f)), 2, 2);
        }

        // Matahari terbit kecil
        float sunPulse = (float)(.7 + .3 * Math.sin(phase * .6));
        int sx = W / 2, sy = (int)(H * .62f);
        for (int i = 5; i >= 0; i--) {
            g2.setColor(new Color(1f, .85f, .3f, .06f * sunPulse * (6-i)));
            int sr2 = 30 + i * 14;
            g2.fillOval(sx - sr2, sy - sr2/2, sr2*2, sr2);
        }
        g2.setColor(new Color(255, 230, 100, (int)(220 * sunPulse)));
        g2.fillOval(sx - 22, sy - 11, 44, 22);

        // Partikel cahaya naik (seperti kunang-kunang damai)
        for (float[] s : sparks) {
            float a = Math.max(0f, Math.min(1f, s[5]));
            float hue = (.12f + s[6] * .08f) % 1f;
            Color sc = Color.getHSBColor(hue, .6f, 1f);
            g2.setColor(new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), (int)(a * 200)));
            g2.fillOval((int)s[0], (int)s[1], (int)s[4], (int)s[4]);
        }

        // Aurora tenang di atas
        for (int i = 0; i < 4; i++) {
            float a = Math.max(0f, (float)(.04 + .02 * Math.sin(phase + i)));
            Color ac = Color.getHSBColor(.52f + i * .05f, .7f, .9f);
            g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), (int)(a*255)));
            g2.fillRect(0, i * 40, W, 60);
        }

        // ── Overlay gelap di bawah teks ────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(50, 45, W - 100, H - 130, 16, 16);

        // ── Judul "ARCANA DAMAI" ──────────────────────────────────────────────
        g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
        float tp = (float)(.7 + .3 * Math.sin(textPhase));
        GradientPaint titleGrad = new GradientPaint(
            W/2f - 150, 0, new Color(255, 220, 100),
            W/2f + 150, 0, new Color(180, 220, 255));
        g2.setPaint(titleGrad);
        String title = "* ARCANA DAMAI *";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (W - fm.stringWidth(title)) / 2, 42);
    }
}
