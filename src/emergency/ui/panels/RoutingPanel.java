package emergency.ui.panels;

import emergency.model.NetworkModel;
import emergency.routing.*;
import emergency.ui.Theme;
import emergency.ui.components.ModernButton;
import emergency.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tactical Routing Analysis Panel.
 * Demonstrates and compares Dijkstra Link-State and Distance Vector (Bellman-Ford) routing.
 * Provides live forwarding tables, full distance-vector cost matrix, and educational analysis.
 */
public class RoutingPanel extends JPanel {
    private final NetworkModel network;

    private final JComboBox<String> sourceCombo = new JComboBox<>();
    private final JComboBox<String> destCombo = new JComboBox<>();
    private final JComboBox<String> inspectorNodeCombo = new JComboBox<>();

    private final JLabel dijkstraPathLabel = new JLabel();
    private final JLabel dijkstraCostLabel = new JLabel();
    private final JLabel dijkstraNextHopLabel = new JLabel();
    private final JTextArea dijkstraExplanation = new JTextArea();

    private final JLabel dvPathLabel = new JLabel();
    private final JLabel dvCostLabel = new JLabel();
    private final JLabel dvNextHopLabel = new JLabel();
    private final JLabel dvConvergenceLabel = new JLabel();
    private final JTextArea dvExplanation = new JTextArea();

    private final JTable forwardingTable = new JTable();
    private final DefaultTableModel forwardingModel = new DefaultTableModel(
            new String[]{"Destination", "Next Hop", "Cost (ms)", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final JTable dvMatrixTable = new JTable();
    private final DefaultTableModel dvMatrixModel = new DefaultTableModel();

    public RoutingPanel(NetworkModel network) {
        super(new BorderLayout(12, 12));
        this.network = network;
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        populateNodeSelectors();

        add(createTopControlPanel(), BorderLayout.NORTH);
        add(createMainSplitPanel(), BorderLayout.CENTER);

        network.addListener(() -> SwingUtilities.invokeLater(this::recomputeAll));

        sourceCombo.addActionListener(e -> recomputeAll());
        destCombo.addActionListener(e -> recomputeAll());
        inspectorNodeCombo.addActionListener(e -> updateInspectorTable());

        recomputeAll();
    }

    private void populateNodeSelectors() {
        sourceCombo.removeAllItems();
        destCombo.removeAllItems();
        inspectorNodeCombo.removeAllItems();

        for (String node : network.nodes()) {
            sourceCombo.addItem(node);
            destCombo.addItem(node);
            inspectorNodeCombo.addItem(node);
        }
        sourceCombo.setSelectedItem("Control Center");
        destCombo.setSelectedItem("Shelter B");
        inspectorNodeCombo.setSelectedItem("Control Center");
    }

    private JPanel createTopControlPanel() {
        RoundedPanel top = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 12, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);

        JLabel src = new JLabel("Route Source:");
        src.setFont(Theme.FONT_SECTION);
        src.setForeground(Theme.TEXT_SECONDARY);

        JLabel dst = new JLabel("Route Destination:");
        dst.setFont(Theme.FONT_SECTION);
        dst.setForeground(Theme.TEXT_SECONDARY);

        JLabel insp = new JLabel("Inspect Node Table:");
        insp.setFont(Theme.FONT_SECTION);
        insp.setForeground(Theme.CYAN_ACCENT);

        ModernButton recomputeBtn = new ModernButton("Recompute Algorithms", ModernButton.Style.PRIMARY);
        recomputeBtn.addActionListener(e -> recomputeAll());

        styleCombo(sourceCombo);
        styleCombo(destCombo);
        styleCombo(inspectorNodeCombo);

        top.add(src);
        top.add(sourceCombo);
        top.add(dst);
        top.add(destCombo);
        top.add(recomputeBtn);
        top.add(Box.createHorizontalStrut(15));
        top.add(insp);
        top.add(inspectorNodeCombo);

        return top;
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setFont(Theme.FONT_REGULAR);
        cb.setFocusable(false);
    }

    private JComponent createMainSplitPanel() {
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setOpaque(false);
        mainSplit.setDividerSize(6);
        mainSplit.setBorder(null);

        // Left: Dijkstra & Distance Vector Comparison Cards
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        leftPanel.add(createDijkstraCard());
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(createDistanceVectorCard());

        // Right: Forwarding Table & Full Distance Vector Matrix
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        rightPanel.add(createForwardingTableCard());
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(createDvMatrixCard());

        mainSplit.setLeftComponent(new JScrollPane(leftPanel));
        mainSplit.setRightComponent(new JScrollPane(rightPanel));
        mainSplit.setResizeWeight(0.5);

        return mainSplit;
    }

    private JPanel createDijkstraCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("DIJKSTRA SHORTEST-PATH (LINK-STATE)");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.GREEN_OPERATIONAL);

        JPanel details = new JPanel(new GridLayout(3, 1, 4, 4));
        details.setOpaque(false);

        dijkstraPathLabel.setFont(Theme.FONT_REGULAR);
        dijkstraCostLabel.setFont(Theme.FONT_REGULAR);
        dijkstraNextHopLabel.setFont(Theme.FONT_REGULAR);

        details.add(dijkstraPathLabel);
        details.add(dijkstraCostLabel);
        details.add(dijkstraNextHopLabel);

        dijkstraExplanation.setEditable(false);
        dijkstraExplanation.setWrapStyleWord(true);
        dijkstraExplanation.setLineWrap(true);
        dijkstraExplanation.setFont(Theme.FONT_SMALL);
        dijkstraExplanation.setBackground(Theme.BG_ELEVATED);
        dijkstraExplanation.setForeground(Theme.TEXT_SECONDARY);
        dijkstraExplanation.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        card.add(header, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);
        card.add(dijkstraExplanation, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createDistanceVectorCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("DISTANCE VECTOR (BELLMAN-FORD)");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.PURPLE_ANALYTICS);

        JPanel details = new JPanel(new GridLayout(4, 1, 4, 4));
        details.setOpaque(false);

        dvPathLabel.setFont(Theme.FONT_REGULAR);
        dvCostLabel.setFont(Theme.FONT_REGULAR);
        dvNextHopLabel.setFont(Theme.FONT_REGULAR);
        dvConvergenceLabel.setFont(Theme.FONT_REGULAR);

        details.add(dvPathLabel);
        details.add(dvCostLabel);
        details.add(dvNextHopLabel);
        details.add(dvConvergenceLabel);

        dvExplanation.setEditable(false);
        dvExplanation.setWrapStyleWord(true);
        dvExplanation.setLineWrap(true);
        dvExplanation.setFont(Theme.FONT_SMALL);
        dvExplanation.setBackground(Theme.BG_ELEVATED);
        dvExplanation.setForeground(Theme.TEXT_SECONDARY);
        dvExplanation.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        card.add(header, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);
        card.add(dvExplanation, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createForwardingTableCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("ROUTER FORWARDING TABLE");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.BLUE_ACCENT);

        forwardingTable.setModel(forwardingModel);
        setupTableStyle(forwardingTable);

        JScrollPane sp = new JScrollPane(forwardingTable);
        sp.setPreferredSize(new Dimension(380, 150));
        sp.getViewport().setBackground(Theme.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));

        card.add(title, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JPanel createDvMatrixCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("CONVERGED DISTANCE VECTOR COST MATRIX");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.PURPLE_ANALYTICS);

        dvMatrixTable.setModel(dvMatrixModel);
        setupTableStyle(dvMatrixTable);

        JScrollPane sp = new JScrollPane(dvMatrixTable);
        sp.setPreferredSize(new Dimension(380, 160));
        sp.getViewport().setBackground(Theme.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));

        card.add(title, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private void setupTableStyle(JTable table) {
        table.setBackground(Theme.BG_CARD);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(Theme.FONT_REGULAR);
        table.setRowHeight(22);
        table.setGridColor(Theme.BORDER_SUBTLE);
        table.getTableHeader().setBackground(Theme.BG_ELEVATED);
        table.getTableHeader().setForeground(Theme.CYAN_ACCENT);
        table.getTableHeader().setFont(Theme.FONT_SECTION);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);
    }

    public void recomputeAll() {
        String src = (String) sourceCombo.getSelectedItem();
        String dst = (String) destCombo.getSelectedItem();
        if (src == null || dst == null) return;

        // Dijkstra Calculation
        Route dijkstraRoute = DijkstraRouter.findRoute(network, src, dst);
        if (dijkstraRoute.reachable) {
            dijkstraPathLabel.setText("Computed Path: " + String.join(" -> ", dijkstraRoute.path));
            dijkstraPathLabel.setForeground(Theme.TEXT_PRIMARY);
            dijkstraCostLabel.setText(String.format("Total Path Cost: %d ms (%d hops)", dijkstraRoute.cost, dijkstraRoute.hopCount()));
            dijkstraCostLabel.setForeground(Theme.GREEN_OPERATIONAL);
            dijkstraNextHopLabel.setText("Next Hop Forwarder: " + dijkstraRoute.nextHop);
            dijkstraNextHopLabel.setForeground(Theme.CYAN_ACCENT);
        } else {
            dijkstraPathLabel.setText("Computed Path: NO ROUTE (Destination Unreachable)");
            dijkstraPathLabel.setForeground(Theme.RED_FAILURE);
            dijkstraCostLabel.setText("Total Path Cost: INF");
            dijkstraCostLabel.setForeground(Theme.RED_FAILURE);
            dijkstraNextHopLabel.setText("Next Hop: None");
            dijkstraNextHopLabel.setForeground(Theme.TEXT_MUTED);
        }
        dijkstraExplanation.setText(dijkstraRoute.explanation +
                "\nAlgorithm Note: Link-State Dijkstra constructs the shortest-path tree using global link metrics. Converges in O(|V|^2) without routing loops.");

        // Distance Vector Calculation
        Route dvRoute = DistanceVectorRouter.findRoute(network, src, dst);
        DistanceVectorRouter.DVResult dvResult = DistanceVectorRouter.computeDistanceVector(network);

        if (dvRoute.reachable) {
            dvPathLabel.setText("Computed Path: " + String.join(" -> ", dvRoute.path));
            dvPathLabel.setForeground(Theme.TEXT_PRIMARY);
            dvCostLabel.setText(String.format("Total Distributed Cost: %d ms (%d hops)", dvRoute.cost, dvRoute.hopCount()));
            dvCostLabel.setForeground(Theme.PURPLE_ANALYTICS);
            dvNextHopLabel.setText("Next Hop Forwarder: " + dvRoute.nextHop);
            dvNextHopLabel.setForeground(Theme.CYAN_ACCENT);
        } else {
            dvPathLabel.setText("Computed Path: NO ROUTE (Unreachable / Partitioned)");
            dvPathLabel.setForeground(Theme.RED_FAILURE);
            dvCostLabel.setText("Total Distributed Cost: INF");
            dvCostLabel.setForeground(Theme.RED_FAILURE);
            dvNextHopLabel.setText("Next Hop: None");
            dvNextHopLabel.setForeground(Theme.TEXT_MUTED);
        }
        dvConvergenceLabel.setText(String.format("Convergence Status: %s in %d exchange iterations",
                dvResult.converged ? "CONVERGED" : "PENDING", dvResult.iterationsToConverge));
        dvConvergenceLabel.setForeground(dvResult.converged ? Theme.GREEN_OPERATIONAL : Theme.AMBER_WARNING);

        dvExplanation.setText(dvRoute.explanation +
                "\nAlgorithm Note: Bellman-Ford computes D_x(y) = min_v { c(x,v) + D_v(y) } by exchanging vectors between direct neighbors. Link failures require multiple iterations to propagate.");

        updateInspectorTable();
        updateDvMatrixTable(dvResult);
    }

    private void updateInspectorTable() {
        String inspectorNode = (String) inspectorNodeCombo.getSelectedItem();
        if (inspectorNode == null) return;

        forwardingModel.setRowCount(0);
        List<RoutingTableEntry> entries = DijkstraRouter.buildRoutingTable(network, inspectorNode);
        for (RoutingTableEntry e : entries) {
            String status = e.cost == 0 ? "Local Node" : (e.reachable ? "Active / Reachable" : "Unreachable");
            forwardingModel.addRow(new Object[]{
                    e.destination,
                    e.nextHop,
                    e.cost >= Route.INF ? "INF" : String.valueOf(e.cost),
                    status
            });
        }
    }

    private void updateDvMatrixTable(DistanceVectorRouter.DVResult dv) {
        Set<String> nodes = network.nodes();
        String[] columns = new String[nodes.size() + 1];
        columns[0] = "Router \\ Dest";
        int idx = 1;
        for (String n : nodes) {
            columns[idx++] = n;
        }

        dvMatrixModel.setDataVector(new Object[0][0], columns);

        for (String u : nodes) {
            Object[] row = new Object[columns.length];
            row[0] = u;
            int cIdx = 1;
            Map<String, Integer> costs = dv.costMatrix.getOrDefault(u, Map.of());
            for (String v : nodes) {
                int cost = costs.getOrDefault(v, Route.INF);
                row[cIdx++] = (cost >= Route.INF) ? "INF" : String.valueOf(cost);
            }
            dvMatrixModel.addRow(row);
        }
    }
}
