package emergency.ui.components;

import emergency.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Metric KPI card component displaying title, bold value, unit, and accent highlight.
 */
public class StatCard extends RoundedPanel {
    private final JLabel titleLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final JLabel unitLabel = new JLabel();
    private final Color accentColor;

    public StatCard(String title, String initialValue, String unit, Color accent) {
        super(new BorderLayout(5, 5), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        this.accentColor = accent != null ? accent : Theme.BLUE_ACCENT;

        setBorder(new EmptyBorder(10, 14, 10, 14));
        setPreferredSize(new Dimension(160, 85));

        titleLabel.setText(title.toUpperCase());
        titleLabel.setFont(Theme.FONT_SMALL);
        titleLabel.setForeground(Theme.TEXT_SECONDARY);

        valueLabel.setText(initialValue);
        valueLabel.setFont(Theme.FONT_STAT_VALUE);
        valueLabel.setForeground(accentColor);

        unitLabel.setText(unit);
        unitLabel.setFont(Theme.FONT_SMALL);
        unitLabel.setForeground(Theme.TEXT_MUTED);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        centerPanel.setOpaque(false);
        centerPanel.add(valueLabel);
        centerPanel.add(unitLabel);

        add(titleLabel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void setValue(String val) {
        valueLabel.setText(val);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Paint top accent line
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);
        g2.setColor(accentColor);
        g2.fillRoundRect(8, 0, getWidth() - 16, 3, 2, 2);
        g2.dispose();
    }
}
