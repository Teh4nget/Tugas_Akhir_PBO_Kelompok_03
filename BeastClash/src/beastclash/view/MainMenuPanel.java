package beastclash.view;

import beastclash.audio.SoundManager;
import beastclash.controller.GameState;
import beastclash.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class MainMenuPanel extends JPanel implements MainFrame.Cleanable {

    private MainFrame frame;
    private float cloudOffset = 0;
    private Timer animTimer;

    // Tombol-tombol
    private JButton btnMulai, btnPlay, btnGacha, btnCredit, btnSettings, btnExit;

    public MainMenuPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(null);
        setPreferredSize(new Dimension(560, 600));
        initComponents();
        startAnimation();
        // BGM sudah dipanggil oleh MainFrame.showMainMenu() — tidak perlu dipanggil lagi di sini
    }

    private void initComponents() {
        int uid = GameState.getInstance().getCurrentUserId();
        String username = (uid > 0) ? DatabaseManager.getInstance().getUsername(uid) : "Offline";
        int eggs = (uid > 0)
            ? DatabaseManager.getInstance().getEggs(uid)
            : GameState.getInstance().getOfflineEggs();

        // Info user kanan atas
        JLabel lblUser = new JLabel("User: " + username + "   Telur: " + eggs, SwingConstants.RIGHT);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(30, 30, 60));
        lblUser.setBounds(0, 10, 545, 20);
        add(lblUser);

        // Posisi tengah tombol
        int cx = 215; // x mulai agar tombol 130px center di 560px panel
        int bw = 130, bh = 38;

        btnMulai   = btn("MULAI",   cx, 255, bw, bh, new Color(50, 120, 220));
        btnPlay    = btn("PLAY",    cx, 255, bw, bh, new Color(50, 120, 220));
        btnGacha   = btn("GACHA",   cx, 300, bw, bh, new Color(110, 40, 180));
        btnCredit  = btn("CREDIT",  cx, 345, bw, bh, new Color(40, 90, 150));
        btnSettings= btn("AUDIO",   cx, 390, bw, bh, new Color(40, 100, 80));
        btnExit    = btn("EXIT",    cx, 435, bw, bh, new Color(160, 40, 40));

        // Hanya MULAI yang terlihat di awal
        btnPlay.setVisible(false);
        btnGacha.setVisible(false);
        btnCredit.setVisible(false);
        btnSettings.setVisible(false);
        btnExit.setVisible(false);

        add(btnMulai); add(btnPlay); add(btnGacha);
        add(btnCredit); add(btnSettings); add(btnExit);

        // Listeners
        btnMulai.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            btnMulai.setVisible(false);
            btnPlay.setVisible(true);
            btnGacha.setVisible(true);
            btnCredit.setVisible(true);
            btnSettings.setVisible(true);
            btnExit.setVisible(true);
        });

        btnPlay.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            frame.showMapSelect();
        });

        btnGacha.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            frame.showGacha();
        });

        btnCredit.addActionListener(e -> showCredit());
        btnSettings.addActionListener(e -> showAudioSettings());

        btnExit.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            int c = JOptionPane.showConfirmDialog(this,
                "Yakin ingin keluar?", "Exit", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                SoundManager.getInstance().stopBGM();
                DatabaseManager.getInstance().disconnect();
                System.exit(0);
            }
        });
    }

    // ── Buat tombol dengan warna solid + teks putih tegas ────────────────────
    private JButton btn(String text, int x, int y, int w, int h, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Gambar background manual agar selalu terlihat
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // Border
                g2.setColor(getBackground().brighter());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                // Teks
                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
            }
        };
        b.setBounds(x, y, w, h);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false); // kita handle manual di paintComponent
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color hover = bg.brighter();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); b.repaint(); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg);    b.repaint(); }
        });
        return b;
    }

    // ── Credit & Audio ────────────────────────────────────────────────────────
    private void showCredit() {
        SoundManager.getInstance().playSFX("CLICK");
        String credits =
	    "╔══════════════════════════════╗\n" +
            "║       BEAST CLASH                                                           ║\n" +
            "║       Tim Pengembang                                                    ║\n" +
            "╠══════════════════════════════╣\n" +
<<<<<<< HEAD
            "║  • Agung Wahyu Niti Wijaya                                        ║\n" +
            "║  • Raga Deva Bela Negara                                             ║\n" +
            "║  • Ahmad Dziqro Attayu Setio Damar                         ║\n" +
            "║  • Nova Salwa Safitri                                                      ║\n" +
            "║  • Septi Lailatul Fitria                                                     ║\n" +
=======
            "║  • Agung Wahyu Niti Wijaya               ║\n" +
            "║  • Raga Deva Bela Negara              ║\n" +
            "║  • Ahmad Dziqro Attayu Setio Damar                ║\n" +
            "║  • Nova Salwa Safitri              ║\n" +
            "║  • Septi Lailatul Fitria              ║\n" +
>>>>>>> 236f08496782095adbaaca05a1c391f985184c31
            "╠══════════════════════════════╣\n" +
            "║  © 2026 Beast Clash Team                                            ║\n" +
            "╚══════════════════════════════╝";
	JOptionPane.showMessageDialog(this, credits, "Credits", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAudioSettings() {
        SoundManager sm = SoundManager.getInstance();
        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JCheckBox cbBGM = new JCheckBox("BGM Aktif", sm.isBGMEnabled());
        JSlider   slBGM = new JSlider(0, 100, (int)(sm.getBGMVolume()*100));
        JCheckBox cbSFX = new JCheckBox("SFX Aktif", sm.isSFXEnabled());
        JSlider   slSFX = new JSlider(0, 100, (int)(sm.getSFXVolume()*100));
        p.add(cbBGM); p.add(slBGM);
        p.add(cbSFX); p.add(slSFX);
        p.add(new JLabel("Volume BGM:")); p.add(new JLabel("Volume SFX:"));
        int res = JOptionPane.showConfirmDialog(this, p, "Pengaturan Audio",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            sm.setBGMEnabled(cbBGM.isSelected());
            sm.setSFXEnabled(cbSFX.isSelected());
            sm.setBGMVolume(slBGM.getValue()/100f);
            sm.setSFXVolume(slSFX.getValue()/100f);
            if (cbBGM.isSelected()) sm.playBGM("MENU");
        }
    }

    // ── Animasi ───────────────────────────────────────────────────────────────
    private void startAnimation() {
        animTimer = new Timer(40, e -> {
            cloudOffset = (cloudOffset + 0.4f) % (getWidth() + 150);
            repaint();
        });
        animTimer.start();
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
        int W = getWidth(), H = getHeight();

        // Langit biru
        GradientPaint sky = new GradientPaint(0, 0,
            new Color(80, 160, 230), 0, H * 0.65f, new Color(160, 215, 245));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H);

        // Awan
        drawCloud(g2, (int)(50  + cloudOffset) % (W+150) - 80, 55,  90, 38);
        drawCloud(g2, (int)(210 + cloudOffset) % (W+150) - 80, 38, 115, 44);
        drawCloud(g2, (int)(380 + cloudOffset) % (W+150) - 80, 78,  75, 32);
        drawCloud(g2, (int)(490 + cloudOffset) % (W+150) - 80, 50,  95, 40);

        // Lautan
        g2.setColor(new Color(60, 140, 190));
        g2.fillRect(0, (int)(H*0.62), W, (int)(H*0.10));

        // Tanah hijau
        GradientPaint ground = new GradientPaint(0, (int)(H*0.72),
            new Color(75, 165, 65), 0, H, new Color(40, 110, 30));
        g2.setPaint(ground);
        g2.fillRect(0, (int)(H*0.72), W, H);

        // Rumput pixel
        g2.setColor(new Color(55, 185, 55));
        for (int x = 0; x < W; x += 8) {
            int gh = 5 + (int)(Math.sin(x * 0.3) * 3);
            g2.fillRect(x, (int)(H*0.72) - gh, 7, gh + 2);
        }

        // Judul game — shadow lalu teks utama
        g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
        String title = "BEAST CLASH";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(title)) / 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(title, tx + 3, 213);

        // Teks putih dengan outline biru gelap agar terbaca di langit
        g2.setColor(new Color(20, 60, 120));
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                if (dx != 0 || dy != 0)
                    g2.drawString(title, tx + dx, 210 + dy);
        g2.setColor(Color.WHITE);
        g2.drawString(title, tx, 210);

        // Subtitle
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        String sub = "Cegah kebangkitan Zenith!";
        FontMetrics fm2 = g2.getFontMetrics();
        int sx = (W - fm2.stringWidth(sub)) / 2;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(sub, sx + 1, 232);
        g2.setColor(new Color(20, 50, 120));
        g2.drawString(sub, sx, 231);
    }

    private void drawCloud(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRoundRect(x,        y,        w,        h,        24, 24);
        g2.fillRoundRect(x + 12,   y - h/3,  (int)(w*0.6), (int)(h*0.75), 20, 20);
        g2.fillRoundRect(x + w/2,  y - h/4,  (int)(w*0.4), (int)(h*0.6),  18, 18);
    }
}
