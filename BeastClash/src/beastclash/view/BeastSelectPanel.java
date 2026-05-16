package beastclash.view;

import beastclash.controller.GameState;
import beastclash.audio.SoundManager;
import beastclash.data.BeastData;
import beastclash.database.DatabaseManager;
import beastclash.model.Beast;
import beastclash.model.GameMap;
import beastclash.resources.ResourceManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * BeastSelectPanel – pilih 5 Beast untuk tim.
 * Menampilkan semua 24 beast; beast yang belum dimiliki ditampilkan
 * dengan overlay "TERKUNCI" dan tidak bisa dipilih.
 */
public class BeastSelectPanel extends JPanel {

    private MainFrame   frame;
    private List<Beast> ownedBeasts;   // beast yang dimiliki player
    private List<Beast> allBeasts;     // semua 24 beast (untuk tampilan terkunci)
    private List<Beast> selectedBeasts = new ArrayList<>();
    private JPanel[]    beastCards     = new JPanel[25];
    private JPanel      teamPreviewPanel;
    private JLabel      statusLabel;
    private JLabel      collectionLabel; // label "X / 24 Beast"
    private String      filterElement  = "Semua";
    private GameMap     currentMap;
    private JPanel      gridPanel;
    private JWindow     detailPopup;

    public BeastSelectPanel(MainFrame frame) {
        this.frame       = frame;
        this.ownedBeasts = GameState.getInstance().getAvailableBeasts();
        this.allBeasts   = BeastData.getAllBeasts();
        this.currentMap  = GameState.getInstance().getSelectedMap();
        setLayout(new BorderLayout());
        setBackground(new Color(14, 14, 28));
        buildUI();
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUI() {
        // NORTH: header
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setBackground(new Color(22, 22, 44));
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton btnBack = makeBtn("<- Kembali", new Color(50, 50, 80));
        btnBack.addActionListener(e -> { closePopup(); frame.showMapSelect(); });

        JPanel titleCenter = new JPanel(new GridLayout(2, 1, 0, 1));
        titleCenter.setOpaque(false);

        JLabel title = new JLabel("PILIH BEAST", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(200, 220, 255));

        // Label koleksi: X / 24 Beast
        collectionLabel = new JLabel(ownedBeasts.size() + " / 24 Beast dimiliki",
            SwingConstants.CENTER);
        collectionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        collectionLabel.setForeground(new Color(160, 180, 220));

        titleCenter.add(title);
        titleCenter.add(collectionLabel);

        String ee = currentMap != null ? currentMap.getEnemyElement() : "?";
        JLabel enemyInfo = new JLabel("Musuh: " + ee,
            SwingConstants.RIGHT);
        enemyInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        enemyInfo.setForeground(ElementColor.getColor(ee));

        top.add(btnBack,     BorderLayout.WEST);
        top.add(titleCenter, BorderLayout.CENTER);
        top.add(enemyInfo,   BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // CENTER: filter + grid
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(14, 14, 28));

        // Filter bar — gunakan GridLayout agar tombol sama lebar & rapat
        String[] elems = {"Semua","Api","Air","Tanah","Daun","Cahaya","Gelap"};
        JPanel filterBar = new JPanel(new GridLayout(1, elems.length, 3, 0));
        filterBar.setBackground(new Color(20, 20, 38));
        filterBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        for (String elem : elems) {
            JButton fb = new JButton(elem);
            fb.setFont(new Font("Segoe UI", Font.BOLD, 10));
            fb.setBackground(filterElement.equals(elem)
                ? new Color(60, 60, 110) : new Color(30, 30, 55));
            fb.setForeground(elem.equals("Semua")
                ? Color.WHITE : ElementColor.getColor(elem).brighter());
            fb.setBorderPainted(false);
            fb.setFocusPainted(false);
            fb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            fb.addActionListener(e -> {
                filterElement = elem;
                // Reset semua warna tombol
                for (Component c : filterBar.getComponents()) {
                    if (c instanceof JButton) {
                        c.setBackground(new Color(30, 30, 55));
                    }
                }
                fb.setBackground(new Color(60, 60, 110));
                rebuildGrid();
            });
            filterBar.add(fb);
        }
        center.add(filterBar, BorderLayout.NORTH);

        gridPanel = new JPanel();
        gridPanel.setBackground(new Color(14, 14, 28));
        rebuildGrid();

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(new Color(14, 14, 28));
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // SOUTH: preview + status + begin
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(new Color(18, 18, 38));
        south.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 120)),
            BorderFactory.createEmptyBorder(8, 12, 10, 12)));

        statusLabel = new JLabel("Pilih 5 Beast untuk tim kamu! (0/5)",
            SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(180, 200, 255));

        teamPreviewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        teamPreviewPanel.setBackground(new Color(18, 18, 38));
        for (int i = 0; i < 5; i++) teamPreviewPanel.add(createEmptySlot(i));

        JButton btnBegin = makeBtn("MULAI BATTLE", new Color(40, 160, 80));
        btnBegin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBegin.addActionListener(e -> onBegin());

        JPanel bottomRow = new JPanel(new BorderLayout(8, 0));
        bottomRow.setBackground(new Color(18, 18, 38));

        JPanel leftSouth = new JPanel(new BorderLayout(0, 2));
        leftSouth.setOpaque(false);
        leftSouth.add(statusLabel,      BorderLayout.NORTH);
        leftSouth.add(teamPreviewPanel, BorderLayout.CENTER);

        bottomRow.add(leftSouth, BorderLayout.CENTER);
        bottomRow.add(btnBegin,  BorderLayout.EAST);
        south.add(bottomRow, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    // ── Grid ──────────────────────────────────────────────────────────────────
    private void rebuildGrid() {
        gridPanel.removeAll();

        // Kumpulkan beast yang perlu ditampilkan (semua 24, filter elemen)
        List<Beast> toShow = new ArrayList<>();
        for (Beast b : allBeasts) {
            if (filterElement.equals("Semua") || b.getElement().equals(filterElement))
                toShow.add(b);
        }

        int cols = 4;
        gridPanel.setLayout(new GridLayout(0, cols, 12, 12));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (Beast beast : toShow) {
            boolean owned = isOwned(beast);
            JPanel card = owned ? createOwnedCard(beast) : createLockedCard(beast);
            if (beast.getId() >= 1 && beast.getId() <= 24)
                beastCards[beast.getId()] = card;
            gridPanel.add(card);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private boolean isOwned(Beast beast) {
        return ownedBeasts.stream().anyMatch(b -> b.getId() == beast.getId());
    }

    // ── Kartu beast yang dimiliki ─────────────────────────────────────────────
    private JPanel createOwnedCard(Beast beast) {
        boolean selected = selectedBeasts.stream()
            .anyMatch(b -> b.getId() == beast.getId());
        Color ec = ElementColor.getColor(beast.getElement());

        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = selected
                    ? new Color(ec.getRed()/2, ec.getGreen()/2, ec.getBlue()/2, 220)
                    : new Color(22, 22, 44, 230);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(selected ? ec : new Color(60, 60, 100));
                g2.setStroke(new BasicStroke(selected ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(155, 200));
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 5, 6));

        // Gambar beast
        JLabel imgLabel = makeBeastImageLabel(beast, selected, true);
        imgLabel.setPreferredSize(new Dimension(135, 135));

        // Info
        JLabel nameLabel = new JLabel(truncate(beast.getName(), 11), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        nameLabel.setForeground(selected ? Color.WHITE : new Color(200, 210, 240));

        JLabel elemLabel = new JLabel(beast.getElement(), SwingConstants.CENTER);
        elemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        elemLabel.setForeground(ec.brighter());

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 1));
        info.setOpaque(false);
        info.add(nameLabel);
        info.add(elemLabel);

        card.add(imgLabel, BorderLayout.CENTER);
        card.add(info,     BorderLayout.SOUTH);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { toggleBeast(beast); }
            public void mouseEntered(MouseEvent e) { showDetailPopup(beast, card, true); }
            public void mouseExited(MouseEvent e)  { closePopup(); }
        });
        return card;
    }

    // ── Kartu beast yang belum dimiliki (terkunci) ────────────────────────────
    private JPanel createLockedCard(Beast beast) {
        Color ec = ElementColor.getColor(beast.getElement());
        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                // Background gelap
                g2.setColor(new Color(18, 18, 30, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(50, 50, 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(155, 200));
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 5, 6));

        // Gambar beast — grayscale + overlay gelap
        JLabel imgLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                BufferedImage img = ResourceManager.getInstance().getBeastImg(beast.getName());
                if (img != null) {
                    // Konversi ke grayscale
                    BufferedImage gray = toGrayscale(img);
                    g2.drawImage(gray, 0, 0, getWidth(), getHeight(), null);
                    // Overlay gelap
                    g2.setColor(new Color(0, 0, 0, 130));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                }
                // Gembok
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                g2.setColor(new Color(200, 200, 220, 200));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("?", (getWidth() - fm.stringWidth("?"))/2, getHeight()/2 + 8);
            }
        };
        imgLabel.setPreferredSize(new Dimension(135, 135));

        JLabel nameLabel = new JLabel("???", SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        nameLabel.setForeground(new Color(100, 100, 120));

        JLabel lockedLabel = new JLabel("TERKUNCI", SwingConstants.CENTER);
        lockedLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lockedLabel.setForeground(new Color(180, 80, 80));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 1));
        info.setOpaque(false);
        info.add(nameLabel);
        info.add(lockedLabel);

        card.add(imgLabel, BorderLayout.CENTER);
        card.add(info,     BorderLayout.SOUTH);
        // Tidak ada cursor hand, tidak bisa diklik
        card.setCursor(Cursor.getDefaultCursor());
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { showDetailPopup(beast, card, false); }
            public void mouseExited(MouseEvent e)  { closePopup(); }
        });
        return card;
    }

    // ── Beast image label helper ──────────────────────────────────────────────
    private JLabel makeBeastImageLabel(Beast beast, boolean selected, boolean owned) {
        Color ec = ElementColor.getColor(beast.getElement());
        return new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                BufferedImage img = ResourceManager.getInstance().getBeastImg(beast.getName());
                if (img != null) {
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                    // Overlay elemen tipis
                    g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                } else {
                    g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 80));
                    g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
                }
                // Centang jika dipilih
                if (selected) {
                    g2.setColor(new Color(0, 0, 0, 140));
                    g2.fillOval(getWidth()-17, 1, 15, 15);
                    g2.setColor(new Color(60, 220, 100));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.drawString("v", getWidth()-13, 12);
                }
            }
        };
    }

    // ── Grayscale helper ──────────────────────────────────────────────────────
    private BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb  = src.getRGB(x, y);
                int a     = (argb >> 24) & 0xFF;
                int r     = (argb >> 16) & 0xFF;
                int g     = (argb >>  8) & 0xFF;
                int b     = argb & 0xFF;
                int lum   = (int)(0.299*r + 0.587*g + 0.114*b);
                gray.setRGB(x, y, (a << 24) | (lum << 16) | (lum << 8) | lum);
            }
        }
        return gray;
    }

    // ── Toggle pilih beast ────────────────────────────────────────────────────
    private void toggleBeast(Beast beast) {
        SoundManager.getInstance().playSFX("CLICK");
        boolean already = selectedBeasts.stream().anyMatch(b -> b.getId() == beast.getId());
        if (already) {
            selectedBeasts.removeIf(b -> b.getId() == beast.getId());
        } else {
            if (selectedBeasts.size() >= 5) {
                statusLabel.setText("Maksimal 5 Beast!");
                statusLabel.setForeground(new Color(255, 120, 80));
                return;
            }
            selectedBeasts.add(beast);
        }
        updateTeamPreview();
        rebuildGrid();
    }

    // ── Preview tim ───────────────────────────────────────────────────────────
    private void updateTeamPreview() {
        statusLabel.setText("Pilih 5 Beast untuk tim kamu! ("
            + selectedBeasts.size() + "/5)");
        statusLabel.setForeground(selectedBeasts.size() == 5
            ? new Color(100, 220, 100) : new Color(180, 200, 255));

        teamPreviewPanel.removeAll();
        for (int i = 0; i < 5; i++) {
            teamPreviewPanel.add(i < selectedBeasts.size()
                ? createPreviewSlot(selectedBeasts.get(i))
                : createEmptySlot(i));
        }
        teamPreviewPanel.revalidate();
        teamPreviewPanel.repaint();
    }

    private JPanel createPreviewSlot(Beast b) {
        Color ec = ElementColor.getColor(b.getElement());
        JPanel slot = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(ec.getRed()/3, ec.getGreen()/3, ec.getBlue()/3, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(ec);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                BufferedImage img = ResourceManager.getInstance().getBeastForPlayer(b.getName());
                if (img != null) g2.drawImage(img, 2, 2, getWidth()-4, getHeight()-18, null);
            }
        };
        slot.setPreferredSize(new Dimension(68, 70));
        slot.setOpaque(false);
        JLabel nm = new JLabel(truncate(b.getName(), 8), SwingConstants.CENTER);
        nm.setFont(new Font("Segoe UI", Font.BOLD, 7));
        nm.setForeground(Color.WHITE);
        slot.add(nm, BorderLayout.SOUTH);
        return slot;
    }

    private JPanel createEmptySlot(int i) {
        JPanel slot = new JPanel(new BorderLayout());
        slot.setPreferredSize(new Dimension(68, 70));
        slot.setBackground(new Color(25, 25, 50));
        slot.setBorder(BorderFactory.createDashedBorder(
            new Color(80, 80, 120), 2, 4, 2, true));
        JLabel lbl = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(new Color(80, 80, 120));
        slot.add(lbl, BorderLayout.CENTER);
        return slot;
    }

    // ── Detail popup hover ────────────────────────────────────────────────────
    private void showDetailPopup(Beast beast, JPanel card, boolean owned) {
        closePopup();
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) return;
        detailPopup = new JWindow(owner);
        detailPopup.setLayout(new BorderLayout());

        JPanel pop = new JPanel(new BorderLayout(6, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                Color ec = ElementColor.getColor(beast.getElement());
                g2.setColor(new Color(14, 14, 32, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(owned ? ec : new Color(80, 80, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
            }
        };
        pop.setOpaque(false);
        pop.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Gambar
        JLabel imgLbl = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                BufferedImage img = ResourceManager.getInstance().getBeastImg(beast.getName());
                if (img != null) {
                    if (!owned) img = toGrayscale(img);
                    g2.drawImage(img, 0, 0, 80, 80, null);
                }
            }
        };
        imgLbl.setPreferredSize(new Dimension(80, 80));

        Color ec = ElementColor.getColor(beast.getElement());
        JPanel statPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        statPanel.setOpaque(false);

        if (owned) {
            for (String line : new String[]{
                "<b>" + beast.getName() + "</b>",
                beast.getElement(),
                "HP: " + beast.getMaxHP(),
                "MP: " + beast.getMaxMana(),
                "ATK: " + beast.getAttack(),
                "DEF: " + beast.getDefense(),
                "SPD: " + beast.getSpeed()
            }) {
                JLabel l = new JLabel("<html>" + line + "</html>");
                l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                l.setForeground(line.contains("<b>") ? ec.brighter() : new Color(200, 215, 255));
                statPanel.add(l);
            }
        } else {
            JLabel lock = new JLabel("<html><b>??? TERKUNCI ???</b></html>");
            lock.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lock.setForeground(new Color(180, 80, 80));
            statPanel.add(lock);
            JLabel hint = new JLabel("<html>Dapatkan Beast ini<br>melalui Gacha!</html>");
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            hint.setForeground(new Color(160, 160, 190));
            statPanel.add(hint);
        }

        pop.add(imgLbl,    BorderLayout.WEST);
        pop.add(statPanel, BorderLayout.CENTER);
        detailPopup.add(pop);
        detailPopup.pack();

        try {
            Point p = card.getLocationOnScreen();
            int px = p.x + card.getWidth() + 4;
            int py = p.y;
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            if (px + detailPopup.getWidth() > screen.width)
                px = p.x - detailPopup.getWidth() - 4;
            detailPopup.setLocation(px, py);
            detailPopup.setVisible(true);
        } catch (Exception ignored) {}
    }

    private void closePopup() {
        if (detailPopup != null) {
            detailPopup.setVisible(false);
            detailPopup.dispose();
            detailPopup = null;
        }
    }

    // ── Begin ─────────────────────────────────────────────────────────────────
    private void onBegin() {
        if (selectedBeasts.size() < 5) {
            statusLabel.setText("Pilih 5 Beast dulu! (" + selectedBeasts.size() + "/5)");
            statusLabel.setForeground(new Color(255, 120, 80));
            return;
        }
        closePopup();
        SoundManager.getInstance().playSFX("CLICK");
        GameState gs = GameState.getInstance();
        List<Beast> freshTeam = new ArrayList<>();
        for (Beast b : selectedBeasts)
            freshTeam.add(new Beast(b.getId(), b.getName(), b.getElement(),
                b.getMaxHP(), b.getMaxMana(), b.getAttack(), b.getDefense(), b.getSpeed()));
        gs.setPlayerTeam(freshTeam);
        String mapElem = currentMap != null ? currentMap.getEnemyElement() : "Api";
        gs.setEnemyTeam(BeastData.getEnemyTeam(mapElem, gs.getCurrentLevel()));
        gs.resetBattle();
        frame.showBattle();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isPressed()  ? getBackground().darker()  :
                          getModel().isRollover() ? getBackground().brighter() : getBackground();
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getBackground().brighter());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "..";
    }
}
