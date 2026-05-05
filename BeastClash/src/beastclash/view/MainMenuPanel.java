package beastclash.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenuPanel extends JPanel {

    private JButton btnMulai, btnPlay, btnCredit, btnExit;
    private MainFrame frame;

    // Sky animation
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
        // MULAI button
        btnMulai = createMenuButton("MULAI", 175, 230, 130, 40);
        btnMulai.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(btnMulai);

        // PLAY button
        btnPlay = createMenuButton("PLAY", 185, 290, 110, 32);
        btnPlay.setVisible(false);
        add(btnPlay);

        // CREDIT button
        btnCredit = createMenuButton("CREDIT", 185, 330, 110, 32);
        btnCredit.setVisible(false);
        add(btnCredit);

        // EXIT button
        btnExit = createMenuButton("EXIT", 185, 370, 110, 32);
        btnExit.setVisible(false);
        add(btnExit);

        // Listeners
        btnMulai.addActionListener(e -> {
            btnMulai.setVisible(false);
            btnPlay.setVisible(true);
            btnCredit.setVisible(true);
            btnExit.setVisible(true);
        });

        btnPlay.addActionListener(e -> frame.showMapSelect());

        btnCredit.addActionListener(e -> showCredit());

        btnExit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Yakin ingin keluar dari Beast Clash?", "Exit",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
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
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(230, 245, 255)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void showCredit() {
        String credits =
            "╔══════════════════════════════╗\n" +
            "║       BEAST CLASH                                                           ║\n" +
            "║       Tim Pengembang                                                    ║\n" +
            "╠══════════════════════════════╣\n" +
            "║  • Agung Wahyu Niti Wijaya                                        ║\n" +
            "║  • Raga Deva Bela Negara                                             ║\n" +
            "║  • Ahmad Dziqro Attayu Setio Damar                         ║\n" +
            "║  • Nova Salwa Safitri                                                      ║\n" +
	    "║  • Septi Lailatul Fitria                                                     ║\n" +
            "╠══════════════════════════════╣\n" +
            "║  © 2025 Beast Clash Team                                            ║\n" +
            "╚══════════════════════════════╝";
        JOptionPane.showMessageDialog(this, credits, "Credits", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startAnimation() {
        animTimer = new Timer(50, e -> {
            cloudOffset = (cloudOffset + 0.5f) % getWidth();
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 195, 235), 0, h * 0.65f, new Color(170, 225, 245));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, h);

        // Clouds (pixel-art style)
        drawCloud(g2, (int)(50 + cloudOffset) % (w + 100) - 50, 60, 80, 35);
        drawCloud(g2, (int)(200 + cloudOffset) % (w + 100) - 50, 40, 100, 40);
        drawCloud(g2, (int)(350 + cloudOffset) % (w + 100) - 50, 80, 70, 30);
        drawCloud(g2, (int)(cloudOffset * 0.7f + 130) % (w + 100) - 50, 110, 90, 38);

        // Ocean/horizon
        g2.setColor(new Color(80, 170, 200));
        g2.fillRect(0, (int)(h * 0.62), w, (int)(h * 0.1));
        g2.setColor(new Color(60, 140, 180));
        g2.fillRect(0, (int)(h * 0.66), w, (int)(h * 0.06));

        // Ground strip
        g2.setColor(new Color(80, 160, 70));
        g2.fillRect(0, (int)(h * 0.72), w, (int)(h * 0.28));

        // Pixel grass
        drawPixelGrass(g2, w, (int)(h * 0.72));

        // Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        String title = "BEAST CLASH";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(title)) / 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(title, tx + 2, 202);

        // Main title
        g2.setColor(new Color(40, 40, 40));
        g2.drawString(title, tx, 200);
    }

    private void drawCloud(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect(x, y, w, h, 20, 20);
        g2.fillRoundRect(x + 10, y - h / 3, (int)(w * 0.6), (int)(h * 0.7), 18, 18);
        g2.fillRoundRect(x + w - (int)(w * 0.5), y - h / 4, (int)(w * 0.45), (int)(h * 0.6), 15, 15);
    }

    private void drawPixelGrass(Graphics2D g2, int w, int groundY) {
        g2.setColor(new Color(60, 180, 60));
        for (int x = 0; x < w; x += 8) {
            int h = 6 + (int)(Math.sin(x * 0.3) * 3);
            g2.fillRect(x, groundY - h, 6, h + 2);
        }
        // Darker grass patches
        g2.setColor(new Color(40, 140, 40));
        for (int x = 4; x < w; x += 12) {
            int h = 4 + (int)(Math.cos(x * 0.2) * 2);
            g2.fillRect(x, groundY - h, 5, h + 2);
        }
    }
}
