package beastclash.view;

import beastclash.audio.SoundManager;
import beastclash.controller.GameState;
import beastclash.database.DatabaseManager;
import beastclash.gacha.GachaSystem;
import beastclash.model.Beast;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * GachaPanel – layar gacha Beast Clash.
 *
 * FIX:
 *  - Guard userId <= 0 (mode offline): tombol pull dinonaktifkan.
 *  - refreshEggCount() dan refreshOwnedCount() benar-benar query DB.
 *  - Setelah pull berhasil, GameState.invalidateBeastCache() dipanggil
 *    agar BeastSelectPanel mendapat daftar beast terbaru.
 *  - lblOwned diperbarui setelah setiap pull.
 *  - Pity count disimpan per sesi (tidak hilang saat refresh panel).
 */
public class GachaPanel extends JPanel implements MainFrame.Cleanable {

    private MainFrame  frame;
    private GachaSystem gacha;
    private int userId;

    // UI
    private JLabel lblEggs;
    private JLabel lblPity;
    private JLabel lblResult;
    private JLabel lblOwned;
    private JButton btnPull;
    private JButton btnBack;
    private JPanel  revealPanel;

    // Animasi
    private Timer animTimer;
    private float eggPhase    = 0;
    private float particleAng = 0;
    private boolean showReveal = false;
    private Beast   revealedBeast = null;
    private List<float[]> particles = new ArrayList<>();

    // Pity counter per sesi
    private int[] pityCount = {0};

    public GachaPanel(MainFrame frame) {
        this.frame  = frame;
        this.gacha  = new GachaSystem();
        this.userId = GameState.getInstance().getCurrentUserId();
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(580, 580));
        setBackground(new Color(8, 8, 25));
        buildUI();
        initParticles(40);
        startAnim();
        refreshEggCount();
        refreshOwnedCount();
    }

    private void buildUI() {
        // ── NORTH: header bar ─────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));

        btnBack = new JButton("<- Kembali") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? getBackground().brighter() : getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnBack.setBackground(new Color(40, 40, 70));
        btnBack.setForeground(new Color(200, 200, 240));
        btnBack.setFocusPainted(false);
        btnBack.setBorderPainted(false);
        btnBack.setContentAreaFilled(false);
        btnBack.setOpaque(false);
        btnBack.setPreferredSize(new Dimension(110, 32));
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> { SoundManager.getInstance().playSFX("CLICK"); frame.showMainMenu(); });

        JLabel title = new JLabel("GACHA BEAST", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(220, 180, 255));

        JPanel infoRight = new JPanel(new GridLayout(2, 1, 0, 0));
        infoRight.setOpaque(false);
        lblEggs = new JLabel("Telur: 0", SwingConstants.RIGHT);
        lblEggs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblEggs.setForeground(new Color(255, 220, 100));
        lblPity = new JLabel("Pity: 0/10", SwingConstants.RIGHT);
        lblPity.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPity.setForeground(new Color(180, 140, 220));
        infoRight.add(lblEggs);
        infoRight.add(lblPity);

        header.add(btnBack, BorderLayout.WEST);
        header.add(title,   BorderLayout.CENTER);
        header.add(infoRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── CENTER: area telur/reveal (transparan, dikuasai paintComponent) ───
        JPanel centerArea = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) { /* transparan */ }
        };
        centerArea.setOpaque(false);

        // revealPanel di tengah CENTER
        revealPanel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (revealedBeast == null) return;
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawReveal(g2, getWidth(), getHeight());
            }
        };
        revealPanel.setOpaque(false);
        revealPanel.setPreferredSize(new Dimension(300, 340));
        revealPanel.setVisible(false);

        JPanel revealWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        revealWrapper.setOpaque(false);
        revealWrapper.add(revealPanel);
        centerArea.add(revealWrapper, BorderLayout.CENTER);

        lblResult = new JLabel("", SwingConstants.CENTER);
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblResult.setForeground(Color.WHITE);
        lblResult.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        centerArea.add(lblResult, BorderLayout.SOUTH);

        add(centerArea, BorderLayout.CENTER);

        // ── SOUTH: tombol pull + info ─────────────────────────────────────────
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(4, 20, 12, 20));

        // Tombol PULL custom paint
        btnPull = new JButton("PULL  (1 Telur)") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getBackground();
                g2.setColor(getModel().isPressed()  ? base.darker()  :
                            getModel().isRollover() ? base.brighter() : base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(base.brighter());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? getForeground() : Color.GRAY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btnPull.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnPull.setBackground(new Color(100, 40, 180));
        btnPull.setForeground(Color.WHITE);
        btnPull.setFocusPainted(false);
        btnPull.setBorderPainted(false);
        btnPull.setContentAreaFilled(false);
        btnPull.setOpaque(false);
        btnPull.setPreferredSize(new Dimension(220, 50));
        btnPull.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPull.addActionListener(e -> doPull());
        // Pull tersedia di semua mode (online & offline)

        JPanel pullRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pullRow.setOpaque(false);
        pullRow.add(btnPull);
        south.add(pullRow, BorderLayout.CENTER);

        JPanel infoBottom = new JPanel(new GridLayout(3, 1, 0, 2));
        infoBottom.setOpaque(false);

        lblOwned = new JLabel("", SwingConstants.CENTER);
        lblOwned.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblOwned.setForeground(new Color(160, 160, 210));
        infoBottom.add(lblOwned);

        {
            JLabel lr = new JLabel(
                "Beast baru: Api/Air/Tanah/Daun (biasa) · Cahaya/Gelap (langka) · Pity ke-10 dijamin langka",
                SwingConstants.CENTER);
            lr.setFont(new Font("Segoe UI", Font.ITALIC, 9));
            lr.setForeground(new Color(120, 120, 160));
            infoBottom.add(lr);
        }
        infoBottom.add(new JLabel("", SwingConstants.CENTER)); // spacer
        south.add(infoBottom, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    // ── Pull logic ───────────────────────────────────────────────────────────
    private void doPull() {
        // Pull tersedia online dan offline

        SoundManager.getInstance().playSFX("EGG");
        btnPull.setEnabled(false);
        showReveal    = false;
        revealedBeast = null;
        revealPanel.setVisible(false);
        lblResult.setText("Telur Menetas...");

        Timer delay = new Timer(1500, e -> {
            GachaSystem.PullResult res = gacha.pull(userId, pityCount);
            if (res == null) {
                // Cek sisa telur sesuai mode
                int eggs = (userId <= 0)
                    ? GameState.getInstance().getOfflineEggs()
                    : DatabaseManager.getInstance().getEggs(userId);
                lblResult.setText(eggs <= 0
                    ? "Telur habis! Menangkan battle untuk mendapat telur."
                    : "Gagal pull. Coba lagi.");
                lblResult.setForeground(new Color(255, 100, 100));
                btnPull.setEnabled(eggs > 0);
            } else {
                Beast got = res.beast;
                revealedBeast = got;
                showReveal    = true;
                revealPanel.setVisible(true);

                if (res.isDuplicate) {
                    lblResult.setForeground(new Color(180, 180, 180));
                    lblResult.setText("Duplikat: " + got.getName()
                        + " [" + got.getElement() + "] — Beast sudah dimiliki.");
                    SoundManager.getInstance().playSFX("UNLOCK");
                    initParticles(20);
                } else {
                    lblResult.setForeground(ElementColor.getColor(got.getElement()));
                    lblResult.setText("! Beast Baru! " + got.getName()
                        + " [" + got.getElement() + "] berhasil didapat!");
                    SoundManager.getInstance().playSFX("GACHA");
                    initParticles(60);
                    GameState.getInstance().invalidateBeastCache();

                    // Cek apakah semua 24 beast sudah dimiliki -> tampilkan ending
                    // Hanya untuk mode online (offline tidak persisten)
                    if (userId > 0 && DatabaseManager.getInstance().isConnected()) {
                        int owned = DatabaseManager.getInstance().getOwnedBeastIds(userId).size();
                        if (owned >= 24) {
                            new Timer(2000, ev -> frame.showEnding()) {{ setRepeats(false); start(); }};
                        }
                    }
                }

                refreshEggCount();
                refreshOwnedCount();
                int eggsLeft = (userId <= 0)
                    ? GameState.getInstance().getOfflineEggs()
                    : DatabaseManager.getInstance().getEggs(userId);
                btnPull.setEnabled(eggsLeft > 0);
            }
            lblPity.setText("Pity: " + pityCount[0] + "/10");
        });
        delay.setRepeats(false);
        delay.start();
    }

    // ── Draw reveal card ─────────────────────────────────────────────────────
    private void drawReveal(Graphics2D g2, int w, int h) {
        Beast b = revealedBeast;
        Color ec = ElementColor.getColor(b.getElement());
        boolean isRare = b.getElement().equals("Cahaya") || b.getElement().equals("Gelap");

        // ── Background kartu ──────────────────────────────────────────────────
        GradientPaint cardBg = new GradientPaint(0, 0,
            new Color(ec.getRed()/4, ec.getGreen()/4, ec.getBlue()/4),
            0, h, new Color(ec.getRed()/6, ec.getGreen()/6, ec.getBlue()/6));
        g2.setPaint(cardBg);
        g2.fillRoundRect(0, 0, w, h, 20, 20);

        // Border (emas jika langka)
        if (isRare) {
            g2.setColor(new Color(255, 215, 0, 200));
            g2.setStroke(new BasicStroke(3));
        } else {
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 200));
            g2.setStroke(new BasicStroke(2));
        }
        g2.drawRoundRect(1, 1, w-2, h-2, 20, 20);
        g2.setStroke(new BasicStroke(1));

        // Badge LANGKA
        if (isRare) {
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fmr = g2.getFontMetrics();
            String badge = "* LANGKA *";
            g2.drawString(badge, (w - fmr.stringWidth(badge)) / 2, 20);
        }

        // ── Gambar beast asli dari aset ───────────────────────────────────────
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        java.awt.image.BufferedImage beastImg =
            beastclash.resources.ResourceManager.getInstance().getBeastImg(b.getName());

        int imgY    = isRare ? 24 : 10;
        int imgSize = h - imgY - 75; // sisakan ruang untuk teks stat bawah
        int imgX    = (w - imgSize) / 2;

        if (beastImg != null) {
            // Glow elemen di belakang gambar
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 40));
            g2.fillOval(imgX - 10, imgY - 10, imgSize + 20, imgSize + 20);
            // Clip supaya gambar tidak meluber keluar kartu
            Shape oldClip = g2.getClip();
            g2.setClip(1, 1, w-2, h-2);
            g2.drawImage(beastImg, imgX, imgY, imgSize, imgSize, null);
            g2.setClip(oldClip);
        } else {
            // Fallback lingkaran elemen
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 80));
            g2.fillOval(imgX, imgY, imgSize, imgSize);
            g2.setColor(ec);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
        }

        // ── Nama beast ────────────────────────────────────────────────────────
        int textY = imgY + imgSize + 16;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(b.getName(), (w - fm.stringWidth(b.getName())) / 2, textY);

        // Elemen
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(ec.brighter());
        String elemStr = b.getElement();
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(elemStr, (w - fm2.stringWidth(elemStr)) / 2, textY + 16);

        // Stat
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(new Color(200, 220, 255));
        String stat1 = "HP:" + b.getMaxHP() + "  ATK:" + b.getAttack() + "  DEF:" + b.getDefense();
        String stat2 = "MP:" + b.getMaxMana() + "  SPD:" + b.getSpeed();
        FontMetrics fm3 = g2.getFontMetrics();
        g2.drawString(stat1, (w - fm3.stringWidth(stat1)) / 2, textY + 30);
        g2.drawString(stat2, (w - fm3.stringWidth(stat2)) / 2, textY + 43);

        // Partikel
        for (float[] p : particles) {
            float a = Math.max(0f, Math.min(1f, p[5]));
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), (int)(a * 200)));
            g2.fillOval((int)p[0], (int)p[1], (int)p[4], (int)p[4]);
        }
    }

    // ── Animasi telur ─────────────────────────────────────────────────────────
    private void drawEggAnimation(Graphics2D g2) {
        int cx = getWidth() / 2, cy = getHeight() / 2 - 20;
        float wobble = (float)(Math.sin(eggPhase * 8) * 12);

        g2.setColor(new Color(255, 220, 100, 60));
        g2.fillOval(cx - 70, cy - 85, 140, 140);

        g2.setColor(new Color(240, 230, 210));
        g2.fillOval(cx - 45 + (int)wobble/2, cy - 65, 90, 115);

        if (eggPhase > 3) {
            g2.setColor(new Color(180, 160, 140));
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(cx + 10, cy - 40, cx + 25, cy - 10);
            g2.drawLine(cx - 5, cy - 30, cx - 20, cy);
            g2.setStroke(new BasicStroke(1));
        }

        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillOval(cx - 30, cy - 55, 20, 28);
    }

    private void startAnim() {
        animTimer = new Timer(30, e -> {
            eggPhase    += 0.05f;
            particleAng += 0.03f;
            for (float[] p : particles) {
                p[0] += p[2]; p[1] += p[3];
                p[5] -= 0.01f;
                if (p[5] <= 0 || p[1] > 400) resetParticle(p);
            }
            repaint();
        });
        animTimer.start();
    }

    private void initParticles(int n) {
        particles.clear();
        for (int i = 0; i < n; i++) {
            float[] p = new float[6];
            resetParticle(p);
            p[1] = (float)(Math.random() * 400);
            particles.add(p);
        }
    }

    private void resetParticle(float[] p) {
        p[0] = 150 + (float)(Math.random() * 220);
        p[1] = 150 + (float)(Math.random() * 80);
        double a = Math.random() * Math.PI * 2;
        float spd = (float)(Math.random() * 2 + 0.5);
        p[2] = (float)(Math.cos(a) * spd);
        p[3] = (float)(Math.sin(a) * spd) - 1.5f;
        p[4] = (float)(Math.random() * 5 + 2);
        p[5] = (float)(Math.random() * 0.8 + 0.2);
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────
    private void refreshEggCount() {
        if (userId <= 0) {
            // Mode offline: tampilkan telur in-memory
            int eggs = GameState.getInstance().getOfflineEggs();
            lblEggs.setText("Telur: " + eggs + " (Offline)");
            btnPull.setEnabled(eggs > 0);
            return;
        }
        int eggs = DatabaseManager.getInstance().getEggs(userId);
        lblEggs.setText("Telur: " + eggs);
        btnPull.setEnabled(eggs > 0 && DatabaseManager.getInstance().isConnected());
    }

    private void refreshOwnedCount() {
        if (userId <= 0) {
            // Offline: tampilkan starter count
            lblOwned.setText("Beast dimiliki: " + GameState.getInstance().getAvailableBeasts().size() + "/24 (Offline)");
            return;
        }
        int count = DatabaseManager.getInstance().getOwnedBeastIds(userId).size();
        lblOwned.setText("Beast dimiliki: " + count + "/24");
    }

    @Override
    public void cleanup() {
        if (animTimer != null) animTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint bg = new GradientPaint(0, 0, new Color(8, 8, 25), 0, getHeight(), new Color(20, 10, 40));
        g2.setPaint(bg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        for (int i = 0; i < 60; i++) {
            // FIX: clamp alpha ke [0.0, 1.0] — sin() bisa negatif sehingga 0.3+0.5*sin < 0
            float alpha = Math.max(0f, Math.min(1f, (float)(0.3 + 0.5 * Math.sin(eggPhase + i * 0.4))));
            g2.setColor(new Color(1f, 1f, 1f, alpha));
            g2.fillOval((i * 89 + 3) % getWidth(), (i * 47 + 5) % (getHeight() - 60), 2, 2);
        }

        if (!showReveal) drawEggAnimation(g2);
    }
}
