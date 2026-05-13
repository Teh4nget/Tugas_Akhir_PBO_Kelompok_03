package beastclash.view;

import beastclash.audio.SoundManager;
import beastclash.controller.GameState;
import beastclash.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenuPanel extends JPanel {

    private JButton btnMulai, btnPlay, btnCredit, btnGacha, btnSettings, btnExit;
    private MainFrame frame;
    private float cloudOffset = 0;
    private Timer animTimer;

    public MainMenuPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(null);
        setPreferredSize(new Dimension(480, 560));
        initComponents();
        startAnimation();
    }

    private void initComponents() {
        // User info label
        int uid = GameState.getInstance().getCurrentUserId();
        String username = (uid > 0) ? DatabaseManager.getInstance().getUsername(uid) : "Offline";
        int eggs = (uid > 0) ? DatabaseManager.getInstance().getEggs(uid) : 0;

        JLabel lblUser = new JLabel("👤 " + username + "   🥚 " + eggs + " Telur", SwingConstants.RIGHT);
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblUser.setForeground(new Color(40, 40, 40));
        lblUser.setBounds(0, 10, 460, 20);
        add(lblUser);

        // MULAI
        btnMulai = createMenuButton("MULAI", 175, 230, 130, 40);
        btnMulai.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(btnMulai);

        // PLAY
        btnPlay = createMenuButton("PLAY", 185, 290, 110, 32);
        btnPlay.setVisible(false);
        add(btnPlay);

        // GACHA
        btnGacha = createMenuButton("✨ GACHA", 185, 330, 110, 32);
        btnGacha.setVisible(false);
        btnGacha.setForeground(new Color(100, 40, 180));
        add(btnGacha);

        // CREDIT
        btnCredit = createMenuButton("CREDIT", 185, 370, 110, 32);
        btnCredit.setVisible(false);
        add(btnCredit);

        // SETTINGS
        btnSettings = createMenuButton("⚙ AUDIO", 185, 408, 110, 32);
        btnSettings.setVisible(false);
        add(btnSettings);

        // EXIT
        btnExit = createMenuButton("EXIT", 185, 446, 110, 32);
        btnExit.setVisible(false);
        add(btnExit);

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
            int confirm = JOptionPane.showConfirmDialog(this,
                "Yakin ingin keluar dari Beast Clash?", "Exit", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                SoundManager.getInstance().stopBGM();
                DatabaseManager.getInstance().disconnect();
                System.exit(0);
            }
        });
    }

    private JButton createMenuButton(String text, int x, int y, int w, int h) {
        JButton btn = new JButton(text);
        btn.setBounds(x, y, w, h);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(230, 245, 255)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showCredit() {
        SoundManager.getInstance().playSFX("CLICK");
        String credits =
            "╔══════════════════════════════╗\n" +
            "║       BEAST CLASH            ║\n" +
            "║       Tim Pengembang         ║\n" +
            "╠══════════════════════════════╣\n" +
            "║  • Agung Wahyu Niti Wijaya               ║\n" +
            "║  • Raga Deva Bela Negara              ║\n" +
            "║  • Ahmad Dziqro Attayu Setio Damar                ║\n" +
            "║  • Nova Salwa Safitri              ║\n" +
            "║  • Septi Lailatul Fitria              ║\n" +
            "╠══════════════════════════════╣\n" +
            "║  © 2025 Beast Clash Team     ║\n" +
            "╚══════════════════════════════╝";
        JOptionPane.showMessageDialog(this, credits, "Credits", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAudioSettings() {
        SoundManager sm = SoundManager.getInstance();
        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JCheckBox cbBGM = new JCheckBox("BGM", sm.isBGMEnabled());
        JSlider   slBGM = new JSlider(0, 100, (int)(sm.getBGMVolume() * 100));
        JCheckBox cbSFX = new JCheckBox("SFX", sm.isSFXEnabled());
        JSlider   slSFX = new JSlider(0, 100, (int)(sm.getSFXVolume() * 100));

        panel.add(cbBGM); panel.add(slBGM);
        panel.add(cbSFX); panel.add(slSFX);
        panel.add(new JLabel("Volume BGM:")); panel.add(new JLabel("Volume SFX:"));

        int res = JOptionPane.showConfirmDialog(this, panel, "⚙ Audio Settings",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            sm.setBGMEnabled(cbBGM.isSelected());
            sm.setSFXEnabled(cbSFX.isSelected());
            sm.setBGMVolume(slBGM.getValue() / 100f);
            sm.setSFXVolume(slSFX.getValue() / 100f);
            if (cbBGM.isSelected()) sm.playBGM("MENU");
        }
    }

    private void startAnimation() {
        animTimer = new Timer(50, e -> { cloudOffset = (cloudOffset + 0.5f) % getWidth(); repaint(); });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 195, 235), 0, h * 0.65f, new Color(170, 225, 245));
        g2.setPaint(sky); g2.fillRect(0, 0, w, h);
        drawCloud(g2, (int)(50  + cloudOffset) % (w+100) - 50, 60,  80, 35);
        drawCloud(g2, (int)(200 + cloudOffset) % (w+100) - 50, 40, 100, 40);
        drawCloud(g2, (int)(350 + cloudOffset) % (w+100) - 50, 80,  70, 30);
        g2.setColor(new Color(80, 170, 200)); g2.fillRect(0, (int)(h*0.62), w, (int)(h*0.1));
        g2.setColor(new Color(80, 160, 70));  g2.fillRect(0, (int)(h*0.72), w, (int)(h*0.28));
        drawPixelGrass(g2, w, (int)(h*0.72));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        String title = "BEAST CLASH";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0,0,0,80)); g2.drawString(title, tx+2, 202);
        g2.setColor(new Color(40,40,40));  g2.drawString(title, tx,   200);
    }

    private void drawCloud(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(255,255,255,200));
        g2.fillRoundRect(x, y, w, h, 20, 20);
        g2.fillRoundRect(x+10, y-h/3, (int)(w*0.6), (int)(h*0.7), 18, 18);
    }

    private void drawPixelGrass(Graphics2D g2, int w, int groundY) {
        g2.setColor(new Color(60, 180, 60));
        for (int x = 0; x < w; x += 8) {
            int h = 6 + (int)(Math.sin(x*0.3)*3);
            g2.fillRect(x, groundY-h, 6, h+2);
        }
    }
}
