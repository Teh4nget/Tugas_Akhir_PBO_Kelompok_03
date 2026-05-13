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
 * Klik map yang terbuka → set selectedMap di GameState → tampilkan BeastSelectPanel.
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

        JLabel title = new JLabel("🗺 Pilih Map", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(255, 220, 80));
        header.add(title, BorderLayout.CENTER);

        JButton btnBack = new JButton("← Kembali");
        styleSecondaryBtn(btnBack);
        btnBack.addActionListener(e -> {
            SoundManager.getInstance().playSFX("CLICK");
            frame.showMainMenu();
        });
        header.add(btnBack, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Grid map
        List<GameMap> maps = state.getMaps();
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        for (int i = 0; i < maps.size(); i++) {
            GameMap map = maps.get(i);
            grid.add(buildMapCard(map, i + 1));
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

    private JPanel buildMapCard(GameMap map, int mapNumber) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                map.isUnlocked() ? new Color(100, 160, 255) : new Color(80, 80, 80), 2),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        Color bg = map.isUnlocked()
                ? new Color(40, 55, 90)
                : new Color(35, 35, 45);
        card.setBackground(bg);
        card.setOpaque(true);

        // Nomor & nama
        JLabel numLbl = new JLabel("Map " + mapNumber, SwingConstants.CENTER);
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        numLbl.setForeground(map.isUnlocked() ? new Color(255, 220, 80) : new Color(100, 100, 100));
        numLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(numLbl);

        JLabel nameLbl = new JLabel(map.getName(), SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLbl.setForeground(map.isUnlocked() ? Color.WHITE : new Color(90, 90, 90));
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLbl);

        card.add(Box.createVerticalStrut(6));

        // Ikon elemen
        JLabel elemLbl = new JLabel(getElementEmoji(map.getEnemyElement())
                + " " + map.getEnemyElement(), SwingConstants.CENTER);
        elemLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        elemLbl.setForeground(new Color(180, 210, 255));
        elemLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(elemLbl);

        card.add(Box.createVerticalStrut(6));

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
            // Badge SELESAI jika semua level sudah ditamatkan
            if (map.isFullyCompleted()) {
                JLabel doneLbl = new JLabel("✅ SELESAI", SwingConstants.CENTER);
                doneLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                doneLbl.setForeground(new Color(100, 220, 100));
                doneLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(doneLbl);
                card.add(Box.createVerticalStrut(4));
            }

            // FIX: level dimulai dari 1 (replay) jika sudah selesai,
            // atau comp+1 jika belum. Pastikan tidak melebihi maxLevels.
            int startLevel = map.isFullyCompleted()
                ? 1
                : Math.min(comp + 1, map.getMaxLevels());

            String btnLabel = map.isFullyCompleted() ? "🔄 ULANG" : "▶ MAIN";
            JButton btnPlay = new JButton(btnLabel);
            stylePrimaryBtn(btnPlay);
            if (map.isFullyCompleted()) {
                btnPlay.setBackground(new Color(40, 110, 60));
                btnPlay.setToolTipText("Main ulang dari Level 1");
            }
            btnPlay.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnPlay.addActionListener(e -> {
                SoundManager.getInstance().playSFX("CLICK");
                state.setSelectedMap(map);
                state.setCurrentLevel(startLevel);
                frame.showBeastSelect();
            });
            card.add(btnPlay);
        } else {
            JLabel locked = new JLabel("🔒 TERKUNCI", SwingConstants.CENTER);
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
            case "Api":    return "🔥";
            case "Air":    return "💧";
            case "Tanah":  return "🪨";
            case "Daun":   return "🌿";
            case "Cahaya": return "✨";
            case "Gelap":  return "🌑";
            default:       return "❓";
        }
    }

    private void stylePrimaryBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(new Color(70, 130, 220));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 100, 180), 1),
            BorderFactory.createEmptyBorder(5, 16, 5, 16)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(140, 30));
    }

    private void styleSecondaryBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(new Color(50, 50, 70));
        btn.setForeground(new Color(200, 200, 220));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 100), 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
