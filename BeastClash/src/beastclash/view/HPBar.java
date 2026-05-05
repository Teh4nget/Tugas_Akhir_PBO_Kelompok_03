package beastclash.view;

import javax.swing.*;
import java.awt.*;

public class HPBar extends JPanel {
    private int current, max;
    private Color fillColor;
    private String label;

    public HPBar(String label, int current, int max, Color fillColor) {
        this.label = label;
        this.current = current;
        this.max = max;
        this.fillColor = fillColor;
        setPreferredSize(new Dimension(180, 18));
        setOpaque(false);
    }

    public void update(int current, int max) {
        this.current = current;
        this.max = max;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // Background track
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(0, 2, w, h - 4, 8, 8);

        // Fill
        double ratio = max > 0 ? (double) current / max : 0;
        int fillW = (int)(w * ratio);
        if (fillW > 0) {
            // Color transition: green > yellow > red
            Color c = ratio > 0.5 ? fillColor :
                      ratio > 0.25 ? new Color(220, 180, 0) :
                      new Color(200, 50, 50);
            g2.setColor(c);
            g2.fillRoundRect(0, 2, fillW, h - 4, 8, 8);

            // Shine
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRoundRect(2, 3, fillW - 4, (h - 8) / 2, 4, 4);
        }

        // Label
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.setColor(Color.WHITE);
        String text = label + ": " + current + "/" + max;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2 + fm.getAscent() / 2 - 1);
    }
}
