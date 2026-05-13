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
public class GachaPanel extends JPanel {

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
        setLayout(null);
        setPreferredSize(new Dimension(520, 520));
        setBackground(new Color(8, 8, 25));
        buildUI();
        initParticles(40);
        startAnim();
        refreshEggCount();
        refreshOwnedCount();
    }

    private void buildUI() {
        // ── Header ───────────────────────────────────────────────────────────
        JLabel title = new JLabel("✨ GACHA BEAST ✨", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(220, 180, 255));
        title.setBounds(0, 15, 520, 35);
        add(title);

        lblEggs = new JLabel("🥚 Telur: 0", SwingConstants.CENTER);
        lblEggs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEggs.setForeground(new Color(255, 220, 100));
        lblEggs.setBounds(0, 52, 260, 24);
        add(lblEggs);

        lblPity = new JLabel("Pity: 0/10", SwingConstants.CENTER);
        lblPity.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPity.setForeground(new Color(180, 140, 220));
        lblPity.setBounds(260, 52, 260, 24);
        add(lblPity);

        // ── Tombol pull ───────────────────────────────────────────────────────
        btnPull = new JButton("🥚  PULL  (1 Telur)");
        btnPull.setBounds(160, 420, 200, 48);
        btnPull.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPull.setBackground(new Color(100, 40, 180));
        btnPull.setForeground(Color.WHITE);
        btnPull.setBorderPainted(false);
        btnPull.setFocusPainted(false);
        btnPull.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPull.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnPull.setBackground(new Color(130, 60, 220)); }
            public void mouseExited(MouseEvent e)  { btnPull.setBackground(new Color(100, 40, 180)); }
        });
        btnPull.addActionListener(e -> doPull());

        // FIX: nonaktifkan pull jika mode offline
        if (userId <= 0) {
            btnPull.setEnabled(false);
            btnPull.setToolTipText("Gacha tidak tersedia di mode offline");
        }
        add(btnPull);

        // ── Kembali ────────────────────────────────────────────────────────────
        btnBack = new JButton("← Kembali");
        btnBack.setBounds(10, 10, 110, 32);
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnBack.setBackground(new Color(40, 40, 70));
        btnBack.setForeground(new Color(180, 180, 220));
        btnBack.setBorderPainted(false);
        btnBack.setFocusPainted(false);
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            frame.showMainMenu();
        });
        add(btnBack);

        // ── Reveal panel ─────────────────────────────────────────────────────
        revealPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (revealedBeast == null) return;
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawReveal(g2, getWidth(), getHeight());
            }
        };
        revealPanel.setOpaque(false);
        revealPanel.setBounds(110, 85, 300, 310);
        revealPanel.setVisible(false);
        add(revealPanel);

        // Label result
        lblResult = new JLabel("", SwingConstants.CENTER);
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblResult.setForeground(Color.WHITE);
        lblResult.setBounds(0, 390, 520, 22);
        add(lblResult);

        // FIX: lblOwned sebagai field agar bisa diupdate setelah pull
        lblOwned = new JLabel("", SwingConstants.CENTER);
        lblOwned.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblOwned.setForeground(new Color(150, 150, 200));
        lblOwned.setBounds(0, 480, 520, 18);
        add(lblOwned);

        // Mode offline notice
        if (userId <= 0) {
            JLabel lblOffline = new JLabel("⚠ Mode Offline – Gacha membutuhkan koneksi database", SwingConstants.CENTER);
            lblOffline.setFont(new Font("Segoe UI", Font.ITALIC, 10));
            lblOffline.setForeground(new Color(255, 180, 60));
            lblOffline.setBounds(10, 460, 500, 14);
            add(lblOffline);
        } else {
            // Info rate hanya muncul jika online
            JLabel lblRate = new JLabel(
                "Beast baru: Api/Air/Tanah/Daun (biasa) · Cahaya/Gelap (langka) · Duplikat → +3🥚 · Pity ke-10 dijamin langka",
                SwingConstants.CENTER);
            lblRate.setFont(new Font("Segoe UI", Font.ITALIC, 9));
            lblRate.setForeground(new Color(120, 120, 160));
            lblRate.setBounds(10, 465, 500, 14);
            add(lblRate);
        }
    }

    // ── Pull logic ───────────────────────────────────────────────────────────
    private void doPull() {
        // FIX: guard userId
        if (userId <= 0) {
            lblResult.setText("❌ Gacha tidak tersedia di mode offline.");
            return;
        }
        if (!DatabaseManager.getInstance().isConnected()) {
            lblResult.setText("❌ Database tidak terhubung!");
            return;
        }

        SoundManager.getInstance().playSFX("EGG");
        btnPull.setEnabled(false);
        showReveal    = false;
        revealedBeast = null;
        revealPanel.setVisible(false);
        lblResult.setText("🥚 Menetas...");

        Timer delay = new Timer(1500, e -> {
            GachaSystem.PullResult res = gacha.pull(userId, pityCount);
            if (res == null) {
                int eggs = DatabaseManager.getInstance().getEggs(userId);
                lblResult.setText(eggs <= 0
                    ? "❌ Telur habis! Menangkan battle untuk mendapat telur."
                    : "❌ Gagal pull. Coba lagi.");
                lblResult.setForeground(new Color(255, 100, 100));
                btnPull.setEnabled(eggs > 0);
            } else {
                Beast got = res.beast;
                revealedBeast = got;
                showReveal    = true;
                revealPanel.setVisible(true);

                if (res.isDuplicate) {
                    // Beast duplikat → tampilkan pesan shard
                    lblResult.setForeground(new Color(255, 200, 80));
                    lblResult.setText("🔄 Duplikat: " + got.getName()
                        + " → +" + res.shardReward + " 🥚 Telur dikembalikan!");
                    SoundManager.getInstance().playSFX("UNLOCK");
                    initParticles(30); // efek partikel lebih kecil untuk duplikat
                } else {
                    // Beast baru!
                    lblResult.setForeground(ElementColor.getColor(got.getElement()));
                    lblResult.setText("🎉 Beast Baru! " + got.getName()
                        + " [" + got.getElement() + "] berhasil didapat!");
                    SoundManager.getInstance().playSFX("GACHA");
                    initParticles(60);
                    // Invalidate cache agar BeastSelectPanel dapat beast terbaru
                    GameState.getInstance().invalidateBeastCache();
                }

                refreshEggCount();
                refreshOwnedCount();
                btnPull.setEnabled(DatabaseManager.getInstance().getEggs(userId) > 0);
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

        GradientPaint cardBg = new GradientPaint(0, 0,
            new Color(ec.getRed()/4, ec.getGreen()/4, ec.getBlue()/4),
            0, h, new Color(ec.getRed()/6, ec.getGreen()/6, ec.getBlue()/6));
        g2.setPaint(cardBg);
        g2.fillRoundRect(0, 0, w, h, 20, 20);

        if (isRare) {
            g2.setColor(new Color(255, 215, 0, 180));
            g2.setStroke(new BasicStroke(3));
        } else {
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 200));
            g2.setStroke(new BasicStroke(2));
        }
        g2.drawRoundRect(1, 1, w-2, h-2, 20, 20);
        g2.setStroke(new BasicStroke(1));

        if (isRare) {
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("★ LANGKA ★", w/2 - 38, 22);
        }

        g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 50));
        g2.fillOval(w/2 - 80, 30, 160, 160);

        g2.setColor(ec);
        g2.fillOval(w/2 - 55, 40, 110, 110);
        g2.setColor(new Color(255,255,255,120));
        g2.fillOval(w/2 - 50, 45, 40, 30);
        g2.setColor(Color.WHITE);
        g2.fillOval(w/2 - 20, 75, 16, 16);
        g2.fillOval(w/2 + 5,  75, 16, 16);
        g2.setColor(Color.BLACK);
        g2.fillOval(w/2 - 16, 79, 8, 8);
        g2.fillOval(w/2 + 9,  79, 8, 8);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 17));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(b.getName(), (w - fm.stringWidth(b.getName())) / 2, 175);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(ec.brighter());
        String elemStr = ElementColor.getEmoji(b.getElement()) + " " + b.getElement();
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(elemStr, (w - fm2.stringWidth(elemStr)) / 2, 198);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(200, 220, 255));
        g2.drawString("HP: " + b.getMaxHP() + "  |  ATK: " + b.getAttack() + "  |  DEF: " + b.getDefense(),
            w/2 - 80, 222);
        g2.drawString("MP: " + b.getMaxMana() + "  |  SPD: " + b.getSpeed(),
            w/2 - 48, 240);

        for (float[] p : particles) {
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), (int)(p[5] * 200)));
            g2.fillOval((int)p[0], (int)p[1], (int)p[4], (int)p[4]);
        }
    }

    // ── Animasi telur ─────────────────────────────────────────────────────────
    private void drawEggAnimation(Graphics2D g2) {
        int cx = 260, cy = 280;
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
            lblEggs.setText("🥚 Telur: -");
            btnPull.setEnabled(false);
            return;
        }
        int eggs = DatabaseManager.getInstance().getEggs(userId);
        lblEggs.setText("🥚 Telur: " + eggs);
        btnPull.setEnabled(eggs > 0 && DatabaseManager.getInstance().isConnected());
    }

    // FIX: method baru untuk update label owned beast
    private void refreshOwnedCount() {
        if (userId <= 0) {
            lblOwned.setText("Beast dimiliki: - (mode offline)");
            return;
        }
        int count = DatabaseManager.getInstance().getOwnedBeastIds(userId).size();
        lblOwned.setText("Beast dimiliki: " + count + "/24");
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
