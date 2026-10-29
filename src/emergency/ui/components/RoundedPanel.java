package emergency.ui.components;

import emergency.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable container panel featuring smooth anti-aliased rounded corners
 * and tactical borders.
 */
public class RoundedPanel extends JPanel {
    private final int cornerRadius;
    private Color backgroundColor;
    private Color borderColor;

    public RoundedPanel(LayoutManager layout, int radius, Color bg, Color border) {
        super(layout);
        this.cornerRadius = radius;
        this.backgroundColor = bg != null ? bg : Theme.BG_CARD;
        this.borderColor = border != null ? border : Theme.BORDER_SUBTLE;
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout, int radius) {
        this(layout, radius, Theme.BG_CARD, Theme.BORDER_SUBTLE);
    }

    public RoundedPanel(int radius) {
        this(new BorderLayout(), radius, Theme.BG_CARD, Theme.BORDER_SUBTLE);
    }

    public RoundedPanel() {
        this(new BorderLayout(), 12, Theme.BG_CARD, Theme.BORDER_SUBTLE);
    }

    public void setBackgroundColor(Color bg) {
        this.backgroundColor = bg;
        repaint();
    }

    public void setBorderColor(Color border) {
        this.borderColor = border;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        // Border
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
