package emergency.ui.panels;

import emergency.model.Edge;
import emergency.model.NetworkModel;
import emergency.model.Node;
import emergency.routing.DijkstraRouter;
import emergency.routing.DistanceVectorRouter;
import emergency.routing.Route;
import emergency.ui.Theme;
import emergency.ui.components.ModernButton;
import emergency.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Java2D Network Topology visualization.
 * Supports direct mouse interaction (click link to toggle, click node to select),
 * route path illumination, link failure toggling, and real-time topology synchronization.
 */
public class TopologyPanel extends JPanel {
    private final NetworkModel network;
    private final JComboBox<String> sourceCombo = new JComboBox<>();
    private final JComboBox<String> destCombo = new JComboBox<>();
    private final JComboBox<String> linkCombo = new JComboBox<>();
    private final JLabel routeStatusLabel = new JLabel("Route: Calculating...");
    private final TopologyCanvas canvas;
    private Route currentRoute = null;

    public TopologyPanel(NetworkModel network) {
        super(new BorderLayout(10, 10));
        this.network = network;
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        canvas = new TopologyCanvas();

        // Populate dropdowns
        for (String node : network.nodes()) {
            sourceCombo.addItem(node);
            destCombo.addItem(node);
        }
        sourceCombo.setSelectedItem("Control Center");
        destCombo.setSelectedItem("Shelter B");

        refreshLinkCombo();

        // Top Control Strip
        JPanel topStrip = createTopStrip();
        add(topStrip, BorderLayout.NORTH);

        // Center: Canvas + Legend
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(canvas, BorderLayout.CENTER);
        centerPanel.add(createLegendPanel(), BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Synchronize on model changes
        network.addListener(() -> {
            refreshLinkCombo();
            recalculateRoute();
            canvas.repaint();
        });

        // Event listeners
        sourceCombo.addActionListener(e -> recalculateRoute());
        destCombo.addActionListener(e -> recalculateRoute());

        recalculateRoute();
    }

    private JPanel createTopStrip() {
        RoundedPanel strip = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 10, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);

        JLabel srcLbl = new JLabel("Source:");
        srcLbl.setFont(Theme.FONT_SECTION);
        srcLbl.setForeground(Theme.TEXT_SECONDARY);

        JLabel dstLbl = new JLabel("Destination:");
        dstLbl.setFont(Theme.FONT_SECTION);
        dstLbl.setForeground(Theme.TEXT_SECONDARY);

        JLabel lnkLbl = new JLabel("Link:");
        lnkLbl.setFont(Theme.FONT_SECTION);
        lnkLbl.setForeground(Theme.TEXT_SECONDARY);

        ModernButton btnDijkstra = new ModernButton("Dijkstra Route", ModernButton.Style.PRIMARY);
        btnDijkstra.addActionListener(e -> recalculateRoute());

        ModernButton btnToggle = new ModernButton("Toggle Link", ModernButton.Style.DANGER);
        btnToggle.addActionListener(e -> {
            int idx = linkCombo.getSelectedIndex();
            if (idx >= 0 && idx < network.edges().size()) {
                Edge edge = network.edgeAt(idx);
                network.toggleEdge(edge);
            }
        });

        ModernButton btnRestore = new ModernButton("Restore All Links", ModernButton.Style.SUCCESS);
        btnRestore.addActionListener(e -> network.restoreAllEdges());

        styleComboBox(sourceCombo);
        styleComboBox(destCombo);
        styleComboBox(linkCombo);

        routeStatusLabel.setFont(Theme.FONT_REGULAR);
        routeStatusLabel.setForeground(Theme.CYAN_ACCENT);

        strip.add(srcLbl);
        strip.add(sourceCombo);
        strip.add(dstLbl);
        strip.add(destCombo);
        strip.add(btnDijkstra);
        strip.add(Box.createHorizontalStrut(10));
        strip.add(lnkLbl);
        strip.add(linkCombo);
        strip.add(btnToggle);
        strip.add(btnRestore);
        strip.add(Box.createHorizontalStrut(10));
        strip.add(routeStatusLabel);

        return strip;
    }

    private void styleComboBox(JComboBox<String> cb) {
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setFont(Theme.FONT_REGULAR);
        cb.setFocusable(false);
    }

    private void refreshLinkCombo() {
        linkCombo.removeAllItems();
        for (int i = 0; i < network.edges().size(); i++) {
            Edge e = network.edgeAt(i);
            linkCombo.addItem(String.format("[%d] %s -- %s (%dms) %s",
                    i, e.a, e.b, e.cost, e.active ? "UP" : "DOWN"));
        }
    }

    public void recalculateRoute() {
        String src = (String) sourceCombo.getSelectedItem();
        String dst = (String) destCombo.getSelectedItem();
        if (src == null || dst == null) return;

        currentRoute = DijkstraRouter.findRoute(network, src, dst);
        if (currentRoute.reachable) {
            routeStatusLabel.setText(String.format("Route: %s (Cost: %d ms, Hops: %d)",
                    String.join(" -> ", currentRoute.path), currentRoute.cost, currentRoute.hopCount()));
            routeStatusLabel.setForeground(Theme.CYAN_ACCENT);
        } else {
            routeStatusLabel.setText("Route: NO ACTIVE PATH (Partitioned)");
            routeStatusLabel.setForeground(Theme.RED_FAILURE);
        }
        canvas.repaint();
    }

    public Route getCurrentRoute() {
        return currentRoute;
    }

    private JPanel createLegendPanel() {
        RoundedPanel legend = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 20, 6), 8, Theme.BG_CARD, Theme.BORDER_SUBTLE);

        legend.add(createLegendItem("Active Link", Theme.BORDER_LIGHT));
        legend.add(createLegendItem("Selected Route", Theme.CYAN_ACCENT));
        legend.add(createLegendItem("Failed Link [DOWN]", Theme.RED_FAILURE));
        legend.add(createLegendItem("Source Node", Theme.GREEN_OPERATIONAL));
        legend.add(createLegendItem("Destination Node", Theme.PURPLE_ANALYTICS));

        JLabel hint = new JLabel("(Tip: Click any link on canvas to toggle UP/DOWN. Left-click node for Source, Right-click for Dest)");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_MUTED);
        legend.add(hint);

        return legend;
    }

    private JPanel createLegendItem(String label, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyAntiAliasing(g2);
                g2.setColor(color);
                g2.fillOval(1, 1, 10, 10);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(12, 12));

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_SECONDARY);

        p.add(dot);
        p.add(lbl);
        return p;
    }

    /**
     * Java2D Interactive Graph Canvas.
     */
    private class TopologyCanvas extends JPanel {
        private Edge hoveredEdge = null;
        private Node hoveredNode = null;

        TopologyCanvas() {
            setBackground(Theme.BG_PAGE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    Node n = findNodeAt(p);
                    Edge ed = findEdgeNear(p);
                    if (n != hoveredNode || ed != hoveredEdge) {
                        hoveredNode = n;
                        hoveredEdge = ed;
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    Point p = e.getPoint();
                    Node clickedNode = findNodeAt(p);
                    if (clickedNode != null) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            destCombo.setSelectedItem(clickedNode.getName());
                        } else {
                            sourceCombo.setSelectedItem(clickedNode.getName());
                        }
                        recalculateRoute();
                        return;
                    }

                    Edge clickedEdge = findEdgeNear(p);
                    if (clickedEdge != null) {
                        network.toggleEdge(clickedEdge);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private Point getNodePoint(Node node) {
            int w = getWidth();
            int h = getHeight();
            int x = (int) (node.getRelX() * w);
            int y = (int) (node.getRelY() * h);
            return new Point(x, y);
        }

        private Node findNodeAt(Point p) {
            for (Node n : network.getNodeObjects()) {
                Point np = getNodePoint(n);
                if (p.distance(np) <= 28) return n;
            }
            return null;
        }

        private Edge findEdgeNear(Point p) {
            for (Edge e : network.edges()) {
                Node nA = network.getNode(e.a);
                Node nB = network.getNode(e.b);
                if (nA == null || nB == null) continue;
                Point pA = getNodePoint(nA);
                Point pB = getNodePoint(nB);
                double dist = Line2D.ptSegDist(pA.x, pA.y, pB.x, pB.y, p.x, p.y);
                if (dist <= 10.0) return e;
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            // Background subtle grid
            g2.setColor(new Color(255, 255, 255, 6));
            for (int x = 0; x < w; x += 40) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 40) g2.drawLine(0, y, w, y);

            List<String> routePath = (currentRoute != null && currentRoute.reachable) ? currentRoute.path : List.of();

            // 1. Draw Links
            for (Edge edge : network.edges()) {
                Node nA = network.getNode(edge.a);
                Node nB = network.getNode(edge.b);
                if (nA == null || nB == null) continue;

                Point pA = getNodePoint(nA);
                Point pB = getNodePoint(nB);

                boolean isOnRoute = isEdgeOnRoute(edge, routePath);
                boolean isHovered = (edge == hoveredEdge);

                // Line styling
                if (!edge.active) {
                    // Down link: dashed red
                    float[] dash = {6.0f, 6.0f};
                    g2.setStroke(new BasicStroke(isHovered ? 4.0f : 2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                    g2.setColor(Theme.RED_FAILURE);
                } else if (isOnRoute) {
                    // Active route link: glowing cyan/blue
                    g2.setStroke(new BasicStroke(isHovered ? 6.0f : 4.5f));
                    g2.setColor(Theme.CYAN_ACCENT);
                } else {
                    // Regular active link
                    g2.setStroke(new BasicStroke(isHovered ? 3.5f : 2.0f));
                    g2.setColor(isHovered ? Theme.BLUE_ACCENT : new Color(71, 85, 105, 180));
                }

                g2.drawLine(pA.x, pA.y, pB.x, pB.y);

                // Cost / status badge on link midpoint
                int midX = (pA.x + pB.x) / 2;
                int midY = (pA.y + pB.y) / 2;
                drawLinkBadge(g2, edge, midX, midY, isOnRoute);
            }

            // 2. Draw Nodes
            String curSrc = (String) sourceCombo.getSelectedItem();
            String curDst = (String) destCombo.getSelectedItem();

            for (Node node : network.getNodeObjects()) {
                Point p = getNodePoint(node);
                boolean isSource = node.getName().equals(curSrc);
                boolean isDest = node.getName().equals(curDst);
                boolean isHovered = (node == hoveredNode);
                boolean isOnRoute = routePath.contains(node.getName());

                drawNode(g2, node, p.x, p.y, isSource, isDest, isOnRoute, isHovered);
            }

            g2.dispose();
        }

        private boolean isEdgeOnRoute(Edge e, List<String> route) {
            for (int i = 0; i < route.size() - 1; i++) {
                if (e.connects(route.get(i), route.get(i + 1))) return true;
            }
            return false;
        }

        private void drawLinkBadge(Graphics2D g2, Edge edge, int x, int y, boolean onRoute) {
            String text = edge.active ? (edge.cost + " ms") : "DOWN";
            Font font = Theme.FONT_SMALL;
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getHeight();

            int bx = x - tw / 2 - 6;
            int by = y - th / 2 - 2;
            int bw = tw + 12;
            int bh = th + 4;

            // Pill background
            Color bg = edge.active ? (onRoute ? new Color(13, 148, 136) : Theme.BG_CARD) : Theme.RED_FAILURE;
            g2.setColor(bg);
            g2.fillRoundRect(bx, by, bw, bh, 8, 8);

            g2.setColor(edge.active ? (onRoute ? Theme.CYAN_ACCENT : Theme.BORDER_SUBTLE) : Color.WHITE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(bx, by, bw, bh, 8, 8);

            g2.setColor(edge.active ? (onRoute ? Color.WHITE : Theme.TEXT_SECONDARY) : Color.WHITE);
            g2.drawString(text, bx + 6, by + fm.getAscent() + 1);
        }

        private void drawNode(Graphics2D g2, Node node, int x, int y, boolean isSrc, boolean isDst, boolean onRoute, boolean isHover) {
            int radius = isHover ? 28 : 25;

            // Outer pulse/halo for Source or Dest
            if (isSrc) {
                g2.setColor(new Color(16, 185, 129, 60));
                g2.fillOval(x - radius - 7, y - radius - 7, (radius + 7) * 2, (radius + 7) * 2);
            } else if (isDst) {
                g2.setColor(new Color(139, 92, 246, 60));
                g2.fillOval(x - radius - 7, y - radius - 7, (radius + 7) * 2, (radius + 7) * 2);
            } else if (onRoute) {
                g2.setColor(new Color(6, 182, 212, 45));
                g2.fillOval(x - radius - 4, y - radius - 4, (radius + 4) * 2, (radius + 4) * 2);
            }

            // Node Body
            Color bodyBg = isSrc ? new Color(6, 78, 59) : (isDst ? new Color(76, 29, 149) : (onRoute ? new Color(15, 23, 42) : Theme.BG_CARD));
            g2.setColor(bodyBg);
            g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);

            // Ring Stroke
            Color ringColor = isSrc ? Theme.GREEN_OPERATIONAL : (isDst ? Theme.PURPLE_ANALYTICS : (onRoute ? Theme.CYAN_ACCENT : Theme.BORDER_LIGHT));
            g2.setColor(ringColor);
            g2.setStroke(new BasicStroke(isHover ? 3.0f : 2.0f));
            g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);

            // Badge text inside node (e.g. HQ, POL, FIRE, MED, FLD, SHT)
            g2.setFont(Theme.FONT_SECTION);
            g2.setColor(Theme.TEXT_PRIMARY);
            FontMetrics fmBadge = g2.getFontMetrics();
            String badge = node.getRole().badge;
            g2.drawString(badge, x - fmBadge.stringWidth(badge) / 2, y + fmBadge.getAscent() / 2 - 2);

            // Node Name Label underneath
            g2.setFont(Theme.FONT_SECTION);
            FontMetrics fmName = g2.getFontMetrics();
            String name = node.getName();
            int nw = fmName.stringWidth(name);

            // Label background tag
            g2.setColor(new Color(10, 15, 26, 210));
            g2.fillRoundRect(x - nw / 2 - 6, y + radius + 5, nw + 12, fmName.getHeight() + 4, 6, 6);
            g2.setColor(Theme.BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(x - nw / 2 - 6, y + radius + 5, nw + 12, fmName.getHeight() + 4, 6, 6);

            g2.setColor(isSrc ? Theme.GREEN_OPERATIONAL : (isDst ? Theme.PURPLE_ANALYTICS : Theme.TEXT_PRIMARY));
            g2.drawString(name, x - nw / 2, y + radius + 5 + fmName.getAscent() + 1);
        }
    }
}
