package beastclash.view;

import beastclash.audio.SoundManager;
import beastclash.controller.GameState;
import beastclash.model.GameMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * MapSelectPanel – layar pemilihan map sebelum masuk BeastSelectPanel.
 *
 * Menampilkan semua GameMap dari GameState. Map terkunci ditampilkan greyed-out.
 * Klik map yang terbuka -> set selectedMap di GameState -> tampilkan BeastSelectPanel.
 */
public class MapSelectPanel extends JPanel {

    private MainFrame frame;
    private GameState state;

    public MapSelectPanel(MainFrame frame) {
        this.frame = frame;
        this.state = GameState.getInstance();
        setLayout(new BorderLayout());
        setBackground(new Color(30, 40, 60));
        buildUI();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        JLabel title = new JLabel("Pilih Map", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(255, 220, 80));
        header.add(title, BorderLayout.CENTER);

        JButton btnBack = new JButton("<- Kembali");
        styleSecondaryBtn(btnBack);
        btnBack.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            frame.showMainMenu();
        });
        header.add(btnBack, BorderLayout.WEST);

        // Spacer kanan agar judul benar-benar di tengah (seimbang dengan tombol kiri)
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(btnBack.getPreferredSize());
        header.add(spacer, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Grid map
        List<GameMap> maps = state.getMaps();
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        for (int i = 0; i < maps.size(); i++) {
            GameMap map = maps.get(i);
            boolean isLastMap = (i == maps.size() - 1);
            grid.add(buildMapCard(map, i + 1, isLastMap));
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Info footer
        JLabel info = new JLabel(
            "Selesaikan semua level di setiap map untuk membuka map berikutnya",
            SwingConstants.CENTER);
        info.setForeground(new Color(160, 160, 180));
        info.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        info.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));
        add(info, BorderLayout.SOUTH);
    }

    private JPanel buildMapCard(GameMap map, int mapNumber, boolean isLastMap) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                map.isUnlocked() ? new Color(100, 160, 255) : new Color(80, 80, 80), 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        Color bg = map.isUnlocked() ? new Color(40, 55, 90) : new Color(35, 35, 45);
        card.setBackground(bg);
        card.setOpaque(true);

        // Nomor & nama
        JLabel numLbl = new JLabel("Map " + mapNumber, SwingConstants.CENTER);
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        numLbl.setForeground(map.isUnlocked() ? new Color(255, 220, 80) : new Color(100, 100, 100));
        numLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(numLbl);

        JLabel nameLbl = new JLabel(map.getName(), SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(map.isUnlocked() ? Color.WHITE : new Color(90, 90, 90));
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLbl);

        card.add(Box.createVerticalStrut(5));

        // ── Thumbnail gambar map ─────────────────────────────────────────────
        JLabel imgLbl = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                java.awt.image.BufferedImage bg2 =
                    beastclash.resources.ResourceManager.getInstance().getMapBg(map.getName());
                if (bg2 != null) {
                    g2.drawImage(bg2, 0, 0, getWidth(), getHeight(), null);
                    // Overlay gelap jika terkunci
                    if (!map.isUnlocked()) {
                        g2.setColor(new Color(0, 0, 0, 160));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                        FontMetrics fmk = g2.getFontMetrics();
                        String kunci = "TERKUNCI";
                        g2.drawString(kunci, (getWidth() - fmk.stringWidth(kunci)) / 2, getHeight()/2 + 6);
                    }
                } else {
                    g2.setColor(new Color(30, 30, 50));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                // Border tipis
                g2.setColor(map.isUnlocked() ? new Color(100,160,255,120) : new Color(60,60,60));
                g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
            }
        };
        imgLbl.setPreferredSize(new Dimension(170, 80));
        imgLbl.setMaximumSize(new Dimension(999, 80));
        imgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(imgLbl);

        card.add(Box.createVerticalStrut(5));

        card.add(Box.createVerticalStrut(4));

        // Progress bar
        int comp = map.getCompletedLevels();
        int max  = map.getMaxLevels();
        JProgressBar progress = new JProgressBar(0, max);
        progress.setValue(comp);
        progress.setStringPainted(true);
        progress.setString(comp + " / " + max + " Level");
        progress.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        progress.setMaximumSize(new Dimension(160, 16));
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (map.isUnlocked()) {
            progress.setForeground(new Color(80, 200, 80));
        } else {
            progress.setForeground(new Color(80, 80, 80));
        }
        card.add(progress);

        card.add(Box.createVerticalStrut(8));

        // Tombol main / terkunci / selesai
        if (map.isUnlocked()) {
            if (map.isFullyCompleted()) {
                if (isLastMap) {
                    // Map terakhir yang selesai: bisa diulang
                    JLabel doneLbl = new JLabel("SELESAI", SwingConstants.CENTER);
                    doneLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    doneLbl.setForeground(new Color(100, 220, 100));
                    doneLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                    card.add(doneLbl);
                    card.add(Box.createVerticalStrut(4));

                    JButton btnPlay = new JButton("ULANG");
                    stylePrimaryBtn(btnPlay);
                    btnPlay.setBackground(new Color(40, 110, 60));
                    btnPlay.setToolTipText("Main ulang dari Level 1");
                    btnPlay.setAlignmentX(Component.CENTER_ALIGNMENT);
                    btnPlay.addActionListener(e -> {
                        SoundManager.getInstance().playSFX("CLICK");
                        state.setSelectedMap(map);
                        state.setCurrentLevel(1);
                        frame.showBeastSelect();
                    });
                    card.add(btnPlay);
                } else {
                    // Map bukan terakhir yang sudah selesai: DIKUNCI permanen
                    JLabel doneLbl = new JLabel("SELESAI", SwingConstants.CENTER);
                    doneLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    doneLbl.setForeground(new Color(100, 220, 100));
                    doneLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                    card.add(doneLbl);
                    card.add(Box.createVerticalStrut(4));

                    JLabel lockedDone = new JLabel("Tidak bisa diulang", SwingConstants.CENTER);
                    lockedDone.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    lockedDone.setForeground(new Color(160, 160, 160));
                    lockedDone.setAlignmentX(Component.CENTER_ALIGNMENT);
                    card.add(lockedDone);
                }
            } else {
                // Map belum selesai: bisa dimainkan
                int startLevel = Math.min(comp + 1, map.getMaxLevels());
                JButton btnPlay = new JButton("> MAIN");
                stylePrimaryBtn(btnPlay);
                btnPlay.setAlignmentX(Component.CENTER_ALIGNMENT);
                btnPlay.addActionListener(e -> {
                    SoundManager.getInstance().playSFX("CLICK");
                    state.setSelectedMap(map);
                    state.setCurrentLevel(startLevel);
                    frame.showBeastSelect();
                });
                card.add(btnPlay);
            }
        } else {
            JLabel locked = new JLabel("TERKUNCI", SwingConstants.CENTER);
            locked.setFont(new Font("Segoe UI", Font.BOLD, 12));
            locked.setForeground(new Color(120, 120, 120));
            locked.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(locked);
        }

        // Hover effect hanya untuk yang unlocked
        if (map.isUnlocked()) {
            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    card.setBackground(new Color(55, 75, 120));
                }
                public void mouseExited(MouseEvent e) {
                    card.setBackground(bg);
                }
            });
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        return card;
    }

    private String getElementEmoji(String element) {
        switch (element) {
            case "Api":    return "Api";
            case "Air":    return "Air";
            case "Tanah":  return "Tanah";
            case "Daun":   return "Daun";
            case "Cahaya": return "*";
            case "Gelap":  return "Gelap";
            default:       return "?";
        }
    }

    private void stylePrimaryBtn(JButton btn) {
        applyCustomPaint(btn, new Color(70, 130, 220), Color.WHITE, 12);
        btn.setMaximumSize(new Dimension(160, 32));
    }

    private void styleSecondaryBtn(JButton btn) {
        applyCustomPaint(btn, new Color(50, 55, 90), new Color(200, 210, 240), 12);
    }

    private void applyCustomPaint(JButton btn, Color bg, Color fg, int fontSize) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Override paint agar selalu terlihat
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                JButton b = (JButton) c;
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = b.getBackground();
                Color fill = b.getModel().isPressed()  ? base.darker()  :
                             b.getModel().isRollover() ? base.brighter() : base;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), 8, 8);
                g2.setColor(base.brighter());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, b.getWidth()-1, b.getHeight()-1, 8, 8);
                g2.setFont(b.getFont());
                g2.setColor(b.getForeground());
                FontMetrics fm = g2.getFontMetrics();
                String t = b.getText();
                g2.drawString(t,
                    (b.getWidth()  - fm.stringWidth(t)) / 2,
                    (b.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint gp = new GradientPaint(
            0, 0, new Color(20, 30, 55),
            0, getHeight(), new Color(40, 55, 80));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
