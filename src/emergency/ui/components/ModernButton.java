package emergency.ui.components;

import emergency.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tactical styled button with rounded corners, subtle hover illumination,
 * and semantic color schemes.
 */
public class ModernButton extends JButton {
    public enum Style {
        PRIMARY(Theme.BLUE_ACCENT, new Color(14, 165, 233), Color.BLACK),
        SECONDARY(Theme.BG_ELEVATED, Theme.BORDER_SUBTLE, Theme.TEXT_PRIMARY),
        SUCCESS(Theme.GREEN_OPERATIONAL, new Color(5, 150, 105), Color.BLACK),
        DANGER(Theme.RED_FAILURE, new Color(220, 38, 38), Color.WHITE),
        PURPLE(Theme.PURPLE_ANALYTICS, new Color(124, 58, 237), Color.WHITE);

        final Color normal;
        final Color hover;
        final Color text;

        Style(Color normal, Color hover, Color text) {
            this.normal = normal;
            this.hover = hover;
            this.text = text;
        }
    }

    private final Style style;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public ModernButton(String text, Style style) {
        super(text);
        this.style = style != null ? style : Style.PRIMARY;
        setFont(Theme.FONT_SECTION);
        setForeground(this.style.text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(7, 14, 7, 14));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public ModernButton(String text) {
        this(text, Style.PRIMARY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        Color bg;
        if (!isEnabled()) {
            bg = Theme.BORDER_SUBTLE;
            setForeground(Theme.TEXT_MUTED);
        } else if (isPressed) {
            bg = style.hover.darker();
            setForeground(style.text);
        } else if (isHovered) {
            bg = style.hover;
            setForeground(style.text);
        } else {
            bg = style.normal;
            setForeground(style.text);
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

        g2.setColor(isEnabled() ? Theme.BORDER_LIGHT : Theme.BORDER_SUBTLE);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

        g2.dispose();
        super.paintComponent(g);
    }
}
