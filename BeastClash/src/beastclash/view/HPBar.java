package beastclash.view;

import javax.swing.*;
import java.awt.*;

/**
 * HPBar – custom JPanel yang menampilkan bar HP atau Mana dengan label nilai.
 *
 * Konstruktor: HPBar(label, current, max, barColor)
 * Update      : update(current, max)
 */
public class HPBar extends JPanel {

    private final String label;
    private int current;
    private int max;
    private final Color barColor;

    private static final int BAR_HEIGHT = 10;
    private static final int PREF_W     = 140;
    private static final int PREF_H     = 22;

    public HPBar(String label, int current, int max, Color barColor) {
        this.label    = label;
        this.current  = current;
        this.max      = max;
        this.barColor = barColor;
        setOpaque(false);
        setPreferredSize(new Dimension(PREF_W, PREF_H));
    }

    /** Perbarui nilai dan repaint. */
    public void update(int current, int max) {
        this.current = Math.max(0, current);
        this.max     = Math.max(1, max);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int barY = PREF_H - BAR_HEIGHT - 2;

        // Label + angka
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(Color.WHITE);
        String txt = label + ": " + current + "/" + max;
        g2.drawString(txt, 0, barY - 2);

        // Background bar (abu gelap)
        g2.setColor(new Color(50, 50, 50));
        g2.fillRoundRect(0, barY, w, BAR_HEIGHT, 4, 4);

        // Fill bar
        if (max > 0 && current > 0) {
            int fw = (int)((double) current / max * w);
            // Gradasi warna: makin rendah makin merah
            Color fillColor = barColor;
            if (barColor.getGreen() > 100 && (double) current / max < 0.3) {
                fillColor = new Color(200, 60, 60); // merah saat kritis
            }
            g2.setColor(fillColor);
            g2.fillRoundRect(0, barY, fw, BAR_HEIGHT, 4, 4);
        }

        g2.dispose();
    }
}
