package beastclash.view;

import beastclash.controller.GameState;
import beastclash.data.BeastData;
import beastclash.model.Beast;
import beastclash.model.GameMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class BeastSelectPanel extends JPanel {

    private MainFrame frame;
    private List<Beast> allBeasts;
    private List<Beast> selectedBeasts = new ArrayList<>();
    private JPanel[] beastCards;
    private JPanel teamPreviewPanel;
    private JLabel statusLabel;
    private String filterElement = "Semua";
    private GameMap currentMap;

    public BeastSelectPanel(MainFrame frame) {
        this.frame = frame;
        this.allBeasts = BeastData.getAllBeasts();
        this.currentMap = GameState.getInstance().getSelectedMap();
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 248));
        buildUI();
    }

    private void buildUI() {
        // === TOP PANEL ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(60, 60, 100));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton btnBack = new JButton("← Back");
        btnBack.setForeground(Color.WHITE);
        btnBack.setBackground(new Color(80, 80, 130));
        btnBack.setBorderPainted(false);
        btnBack.setFocusPainted(false);
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> frame.showMapSelect());

        JLabel title = new JLabel("PILIH CHARACTER", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        String enemyElem = currentMap != null ? currentMap.getEnemyElement() : "?";
        JLabel enemyInfo = new JLabel("Musuh: " + ElementColor.getEmoji(enemyElem) + " " + enemyElem, SwingConstants.RIGHT);
        enemyInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        enemyInfo.setForeground(ElementColor.getColor(enemyElem));

        topPanel.add(btnBack, BorderLayout.WEST);
        topPanel.add(title, BorderLayout.CENTER);
        topPanel.add(enemyInfo, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // === CENTER: Filter + Grid ===
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(240, 240, 248));

        // Filter bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        filterPanel.setBackground(new Color(220, 220, 235));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        String[] elements = {"Semua", "Api", "Air", "Tanah", "Daun", "Cahaya", "Gelap"};
        for (String elem : elements) {
            JButton fb = new JButton(elem.equals("Semua") ? elem : ElementColor.getEmoji(elem) + " " + elem);
            fb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            fb.setFocusPainted(false);
            if (!elem.equals("Semua")) fb.setForeground(ElementColor.getColor(elem));
            fb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            fb.addActionListener(e -> { filterElement = elem; rebuildGrid(gridPanel); });
            filterPanel.add(fb);
        }
        centerPanel.add(filterPanel, BorderLayout.NORTH);

        // Beast grid
        gridPanel = new JPanel();
        gridPanel.setBackground(new Color(240, 240, 248));
        rebuildGrid(gridPanel);

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(scroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // === BOTTOM: Team preview + Begin ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(50, 50, 80));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        statusLabel = new JLabel("Pilih 5 Beast untuk tim kamu! (0/5)", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(Color.WHITE);

        teamPreviewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        teamPreviewPanel.setBackground(new Color(50, 50, 80));
        for (int i = 0; i < 5; i++) {
            JPanel slot = createEmptySlot(i);
            teamPreviewPanel.add(slot);
        }

        JButton btnBegin = new JButton("BEGIN →");
        btnBegin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnBegin.setBackground(new Color(50, 180, 80));
        btnBegin.setForeground(Color.WHITE);
        btnBegin.setBorderPainted(false);
        btnBegin.setFocusPainted(false);
        btnBegin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBegin.addActionListener(e -> {
            if (selectedBeasts.size() < 5) {
                JOptionPane.showMessageDialog(this, "Pilih 5 beast dulu!", "Info", JOptionPane.WARNING_MESSAGE);
            } else {
                GameState state = GameState.getInstance();
                // Clone beasts so they reset fresh
                List<Beast> freshTeam = new ArrayList<>();
                for (Beast b : selectedBeasts) {
                    freshTeam.add(new Beast(b.getId(), b.getName(), b.getElement(),
                        b.getMaxHP(), b.getMaxMana(), b.getAttack(), b.getDefense(), b.getSpeed()));
                }
                state.setPlayerTeam(freshTeam);
                // Set enemy team
                String mapElem = currentMap != null ? currentMap.getEnemyElement() : "Api";
                state.setEnemyTeam(BeastData.getEnemyTeam(mapElem, state.getCurrentLevel()));
                state.resetBattle();
                frame.showBattle();
            }
        });

        JPanel bottomBottom = new JPanel(new BorderLayout());
        bottomBottom.setBackground(new Color(50, 50, 80));
        bottomBottom.add(statusLabel, BorderLayout.NORTH);
        bottomBottom.add(teamPreviewPanel, BorderLayout.CENTER);
        bottomBottom.add(btnBegin, BorderLayout.EAST);

        bottomPanel.add(bottomBottom, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel gridPanel;

    private void rebuildGrid(JPanel panel) {
        panel.removeAll();
        List<Beast> filtered = new ArrayList<>();
        for (Beast b : allBeasts) {
            if (filterElement.equals("Semua") || b.getElement().equals(filterElement)) filtered.add(b);
        }
        int cols = 6;
        panel.setLayout(new GridLayout(0, cols, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        beastCards = new JPanel[allBeasts.size()];

        for (Beast beast : filtered) {
            JPanel card = createBeastCard(beast);
            beastCards[beast.getId() - 1] = card;
            panel.add(card);
        }

        panel.revalidate();
        panel.repaint();
    }

    private JPanel createBeastCard(Beast beast) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(72, 86));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        card.setBackground(new Color(255, 255, 255));

        // Beast icon (colored circle with initial)
        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color ec = ElementColor.getColor(beast.getElement());
                g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 40));
                g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
                g2.setColor(ec);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(2, 2, getWidth()-4, getHeight()-4);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                String initial = beast.getName().substring(0, 1);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(initial)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initial, tx, ty);
            }
        };
        icon.setPreferredSize(new Dimension(46, 46));
        icon.setOpaque(false);

        JLabel nameLabel = new JLabel(beast.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 8));

        JLabel elemLabel = new JLabel(ElementColor.getEmoji(beast.getElement()), SwingConstants.CENTER);
        elemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 0));
        info.setOpaque(false);
        info.add(nameLabel);
        info.add(elemLabel);

        card.add(icon, BorderLayout.CENTER);
        card.add(info, BorderLayout.SOUTH);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { toggleBeast(beast, card); }
            public void mouseEntered(MouseEvent e) {
                if (!selectedBeasts.contains(beast)) card.setBackground(new Color(230, 240, 255));
                showBeastTooltip(beast, card);
            }
            public void mouseExited(MouseEvent e) {
                if (!selectedBeasts.contains(beast)) card.setBackground(Color.WHITE);
            }
        });

        if (selectedBeasts.contains(beast)) {
            card.setBackground(new Color(180, 240, 180));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 180, 80), 2),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
            ));
        }

        return card;
    }

    private void toggleBeast(Beast beast, JPanel card) {
        if (selectedBeasts.contains(beast)) {
            selectedBeasts.remove(beast);
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 200), 1),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
            ));
        } else {
            if (selectedBeasts.size() >= 5) {
                JOptionPane.showMessageDialog(this, "Maksimal 5 beast!", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            selectedBeasts.add(beast);
            card.setBackground(new Color(180, 240, 180));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 180, 80), 2),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
            ));
        }
        updateTeamPreview();
    }

    private void updateTeamPreview() {
        statusLabel.setText("Pilih 5 Beast untuk tim kamu! (" + selectedBeasts.size() + "/5)");
        teamPreviewPanel.removeAll();
        for (int i = 0; i < 5; i++) {
            if (i < selectedBeasts.size()) {
                Beast b = selectedBeasts.get(i);
                JPanel slot = new JPanel(new BorderLayout());
                slot.setPreferredSize(new Dimension(72, 56));
                slot.setBackground(ElementColor.getColor(b.getElement()).darker());
                slot.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));

                JLabel nm = new JLabel(b.getName(), SwingConstants.CENTER);
                nm.setFont(new Font("Segoe UI", Font.BOLD, 8));
                nm.setForeground(Color.WHITE);

                JLabel el = new JLabel(ElementColor.getEmoji(b.getElement()), SwingConstants.CENTER);
                el.setFont(new Font("Segoe UI", Font.PLAIN, 14));

                slot.add(el, BorderLayout.CENTER);
                slot.add(nm, BorderLayout.SOUTH);
                teamPreviewPanel.add(slot);
            } else {
                teamPreviewPanel.add(createEmptySlot(i));
            }
        }
        teamPreviewPanel.revalidate();
        teamPreviewPanel.repaint();
    }

    private JPanel createEmptySlot(int i) {
        JPanel slot = new JPanel();
        slot.setPreferredSize(new Dimension(72, 56));
        slot.setBackground(new Color(80, 80, 110));
        slot.setBorder(BorderFactory.createDashedBorder(new Color(150, 150, 180), 2, 5, 3, true));
        JLabel lbl = new JLabel((i + 1) + "", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(new Color(150, 150, 180));
        slot.add(lbl);
        return slot;
    }

    private void showBeastTooltip(Beast beast, Component comp) {
        // Tooltip shows stats
        String tip = "<html><b>" + beast.getName() + "</b><br/>"
            + "Elemen: " + beast.getElement() + "<br/>"
            + "HP: " + beast.getMaxHP() + " | MP: " + beast.getMaxMana() + "<br/>"
            + "ATK: " + beast.getAttack() + " | DEF: " + beast.getDefense() + "<br/>"
            + "SPD: " + beast.getSpeed() + "</html>";
        if (comp instanceof JComponent) {
            ((JComponent) comp).setToolTipText(tip);
        }
    }
}
