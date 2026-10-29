package emergency.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tactical navigation sidebar allowing one-click access across all
 * emergency operations modules.
 */
public class SidebarNavigation extends JPanel {
    public static final class NavItem {
        public final String id;
        public final String label;
        public final String iconSymbol;

        public NavItem(String id, String label, String iconSymbol) {
            this.id = id;
            this.label = label;
            this.iconSymbol = iconSymbol;
        }
    }

    private final List<NavItemButton> buttons = new ArrayList<>();
    private final Consumer<String> onSelectCallback;
    private String activeId = "topology";

    public SidebarNavigation(Consumer<String> onSelect) {
        super();
        this.onSelectCallback = onSelect;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG_SIDEBAR);
        setPreferredSize(new Dimension(210, 700));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER_SUBTLE),
                new EmptyBorder(16, 10, 16, 10)
        ));

        JLabel title = new JLabel("TACTICAL MODULES");
        title.setFont(Theme.FONT_SMALL);
        title.setForeground(Theme.TEXT_MUTED);
        title.setBorder(new EmptyBorder(0, 12, 10, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);

        addNavButton("topology",    "Topology Map",      "[#]");
        addNavButton("routing",     "Routing Tables",    "[->]");
        addNavButton("simulation",  "Simulation Panel",  "[>]");
        addNavButton("analytics",   "Analytics & Charts","[%]");
        addNavButton("protocol",    "Protocol & CRC Lab","[~]");
        addNavButton("experiments", "Experiments",      "[*]");
        addNavButton("logs",        "Event Log",         "[=]");
        addNavButton("settings",    "Settings & Docs",   "[?]");

        add(Box.createVerticalGlue());

        // Footer version info
        JLabel vLabel = new JLabel("EOC v2.0-LTS");
        vLabel.setFont(Theme.FONT_SMALL);
        vLabel.setForeground(Theme.TEXT_MUTED);
        vLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        vLabel.setBorder(new EmptyBorder(10, 12, 0, 0));
        add(vLabel);
    }

    private void addNavButton(String id, String label, String iconSymbol) {
        NavItemButton btn = new NavItemButton(id, label, iconSymbol);
        buttons.add(btn);
        add(btn);
        add(Box.createVerticalStrut(4));
    }

    public void selectTab(String id) {
        this.activeId = id;
        for (NavItemButton b : buttons) {
            b.setActive(b.id.equals(id));
        }
        if (onSelectCallback != null) {
            onSelectCallback.accept(id);
        }
    }

    private class NavItemButton extends JPanel {
        final String id;
        final String label;
        final String icon;
        boolean isActive = false;
        boolean isHovered = false;

        NavItemButton(String id, String label, String icon) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setMaximumSize(new Dimension(200, 38));
            setPreferredSize(new Dimension(190, 38));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(6, 12, 6, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (id.equals("topology")) isActive = true;

            JLabel iconLbl = new JLabel(icon);
            iconLbl.setFont(Theme.FONT_MONO);
            iconLbl.setForeground(Theme.BLUE_ACCENT);

            JLabel textLbl = new JLabel(label);
            textLbl.setFont(Theme.FONT_REGULAR);
            textLbl.setForeground(Theme.TEXT_PRIMARY);

            add(iconLbl, BorderLayout.WEST);
            add(textLbl, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    selectTab(id);
                }
            });
        }

        void setActive(boolean active) {
            this.isActive = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                g2.setColor(new Color(6, 182, 212, 35));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.setColor(Theme.CYAN_ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                // Left marker stripe
                g2.fillRoundRect(2, 6, 4, h - 12, 2, 2);
            } else if (isHovered) {
                g2.setColor(Theme.BG_CARD_HOVER);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
