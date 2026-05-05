package beastclash.view;

import beastclash.controller.GameState;
import beastclash.model.GameMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MapSelectPanel extends JPanel {

    private MainFrame frame;
    private List<GameMap> maps;

    public MapSelectPanel(MainFrame frame) {
        this.frame = frame;
        this.maps = GameState.getInstance().getMaps();
        setLayout(null);
        setBackground(new Color(245, 240, 230));
        setPreferredSize(new Dimension(480, 560));
        buildUI();
    }

    private void buildUI() {
        removeAll();

        // Title
        JLabel title = new JLabel("PILIH MAP", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBounds(0, 20, 480, 40);
        add(title);

        // Back button
        JButton btnBack = new JButton("← Kembali");
        btnBack.setBounds(10, 15, 100, 30);
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> frame.showMainMenu());
        add(btnBack);

        // Map grid (2x2)
        String[] mapNames = {"Grass Land", "Blizzard", "Volcano", "Desert"};
        int[] gridX = {40, 260, 40, 260};
        int[] gridY = {90, 90, 290, 290};

        for (int i = 0; i < maps.size(); i++) {
            GameMap map = maps.get(i);
            add(createMapCard(map, gridX[i], gridY[i], i));
        }

        // Element info
        JLabel hint = new JLabel("Selesaikan semua level di map sebelumnya untuk membuka map baru!", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(new Color(120, 80, 30));
        hint.setBounds(10, 490, 460, 20);
        add(hint);

        revalidate();
        repaint();
    }

    private JPanel createMapCard(GameMap map, int x, int y, int index) {
        JPanel card = new JPanel(null);
        card.setBounds(x, y, 180, 170);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 160, 80), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        if (map.isUnlocked()) {
            card.setBackground(new Color(255, 252, 240));

            // Map name
            JLabel nameLabel = new JLabel(map.getName(), SwingConstants.CENTER);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setBounds(0, 10, 180, 24);
            card.add(nameLabel);

            // Element badge
            JLabel elemLabel = new JLabel("Musuh: " + map.getEnemyElement(), SwingConstants.CENTER);
            elemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            elemLabel.setForeground(ElementColor.getColor(map.getEnemyElement()));
            elemLabel.setBounds(0, 38, 180, 18);
            card.add(elemLabel);

            // Map icon (colored rectangle)
            JPanel icon = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    drawMapIcon(g2, map.getName(), getWidth(), getHeight());
                }
            };
            icon.setBounds(30, 62, 120, 70);
            icon.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            card.add(icon);

            // Progress
            JLabel progress = new JLabel(map.getProgressText(), SwingConstants.CENTER);
            progress.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            progress.setForeground(new Color(80, 80, 80));
            progress.setBounds(0, 140, 180, 16);
            card.add(progress);

            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    GameState.getInstance().setSelectedMap(map);
                    GameState.getInstance().setCurrentLevel(map.getCompletedLevels() + 1);
                    frame.showBeastSelect();
                }
                public void mouseEntered(MouseEvent e) { card.setBackground(new Color(230, 245, 255)); }
                public void mouseExited(MouseEvent e) { card.setBackground(new Color(255, 252, 240)); }
            });

        } else {
            card.setBackground(new Color(200, 200, 200));

            // Lock icon
            JLabel lock = new JLabel("🔒", SwingConstants.CENTER);
            lock.setFont(new Font("Segoe UI", Font.PLAIN, 36));
            lock.setBounds(0, 45, 180, 50);
            card.add(lock);

            JLabel nameLabel = new JLabel(map.getName(), SwingConstants.CENTER);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLabel.setForeground(new Color(120, 120, 120));
            nameLabel.setBounds(0, 10, 180, 24);
            card.add(nameLabel);

            JLabel lockedLabel = new JLabel("TERKUNCI", SwingConstants.CENTER);
            lockedLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lockedLabel.setForeground(new Color(150, 100, 100));
            lockedLabel.setBounds(0, 110, 180, 20);
            card.add(lockedLabel);
        }

        return card;
    }

    private void drawMapIcon(Graphics2D g2, String mapName, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (mapName) {
            case "Grass Land":
                g2.setColor(new Color(100, 200, 100));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(60, 160, 60));
                for (int x = 0; x < w; x += 8) g2.fillRect(x, 0, 4, h / 3);
                g2.setColor(new Color(150, 200, 255));
                g2.fillOval(w/4, h/4, w/2, h/3);
                break;
            case "Blizzard":
                g2.setColor(new Color(200, 230, 255));
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.WHITE);
                for (int i = 0; i < 8; i++) g2.fillOval((i*15)%w, (i*10)%h, 8, 8);
                break;
            case "Volcano":
                g2.setColor(new Color(60, 60, 60));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(220, 80, 20));
                int[] vx = {w/2, w/6, 5*w/6};
                int[] vy = {5, h-5, h-5};
                g2.fillPolygon(vx, vy, 3);
                g2.setColor(new Color(255, 180, 0));
                g2.fillOval(w/2-8, 0, 16, 12);
                break;
            case "Desert":
                g2.setColor(new Color(240, 200, 100));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(200, 160, 60));
                for (int i = 0; i < 3; i++) {
                    int dw = 20 + i * 15;
                    g2.fillArc(i * 35 + 5, h/2, dw, 15, 0, 180);
                }
                break;
        }
    }
}
