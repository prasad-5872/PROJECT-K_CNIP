package emergency.ui.components;

import emergency.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Compact pill-shaped status indicator badge with LED dot and label.
 */
public class StatusBadge extends JPanel {
    private String text;
    private Color indicatorColor;

    public StatusBadge(String text, Color indicatorColor) {
        super(new FlowLayout(FlowLayout.CENTER, 6, 2));
        this.text = text;
        this.indicatorColor = indicatorColor != null ? indicatorColor : Theme.GREEN_OPERATIONAL;
        setOpaque(false);
    }

    public void setStatus(String text, Color color) {
        this.text = text;
        this.indicatorColor = color;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Theme.FONT_SMALL);
        int w = fm.stringWidth(text) + 26;
        return new Dimension(w, 22);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        // Background pill
        g2.setColor(new Color(indicatorColor.getRed(), indicatorColor.getGreen(), indicatorColor.getBlue(), 35));
        g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);

        g2.setColor(new Color(indicatorColor.getRed(), indicatorColor.getGreen(), indicatorColor.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        // Indicator LED
        g2.setColor(indicatorColor);
        g2.fillOval(7, (h - 7) / 2, 7, 7);

        // Text
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_PRIMARY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, 18, (h + fm.getAscent() - fm.getDescent()) / 2);

        g2.dispose();
    }
}
