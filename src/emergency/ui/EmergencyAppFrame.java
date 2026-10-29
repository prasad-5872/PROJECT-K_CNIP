package emergency.ui;

import emergency.engine.SimulationMetrics;
import emergency.experiment.ExperimentManager;
import emergency.model.NetworkModel;
import emergency.ui.panels.*;

import javax.swing.*;
import java.awt.*;

/**
 * Main application frame for the Reliable Emergency Communication Network.
 * Integrates tactical navigation, live header status, interactive topology,
 * routing tables, simulation engine, analytics charts, protocol lab, experiment manager,
 * and event logging into a cohesive, high-DPI desktop suite.
 */
public class EmergencyAppFrame extends JFrame {
    private final NetworkModel network = NetworkModel.demo();
    private final ExperimentManager experimentManager = new ExperimentManager();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel centerCardPanel = new JPanel(cardLayout);

    private final HeaderPanel headerPanel;
    private final SidebarNavigation sidebar;

    private final TopologyPanel topologyPanel;
    private final RoutingPanel routingPanel;
    private final SimulationPanel simulationPanel;
    private final AnalyticsPanel analyticsPanel;
    private final ProtocolLabPanel protocolLabPanel;
    private final ExperimentPanel experimentPanel;
    private final EventLogPanel eventLogPanel;
    private final SettingsPanel settingsPanel;

    public EmergencyAppFrame() {
        super("Reliable Emergency Communication Network - Tactical Operations Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 780));
        setSize(1280, 840);
        setLocationRelativeTo(null);

        // Set tactical operations center window and taskbar icon
        setIconImage(IconGenerator.getAppIcon());

        // Dark background styling
        getContentPane().setBackground(Theme.BG_PAGE);

        headerPanel = new HeaderPanel(network);
        eventLogPanel = new EventLogPanel();
        analyticsPanel = new AnalyticsPanel();
        protocolLabPanel = new ProtocolLabPanel();
        experimentPanel = new ExperimentPanel(experimentManager);
        settingsPanel = new SettingsPanel(network);
        topologyPanel = new TopologyPanel(network);
        routingPanel = new RoutingPanel(network);

        simulationPanel = new SimulationPanel(
                network,
                experimentManager,
                msg -> eventLogPanel.log(msg, "SIM"),
                this::onSimulationCompleted
        );

        sidebar = new SidebarNavigation(this::switchTab);

        // Build Cards
        centerCardPanel.setOpaque(false);
        centerCardPanel.add(topologyPanel,    "topology");
        centerCardPanel.add(routingPanel,     "routing");
        centerCardPanel.add(simulationPanel,  "simulation");
        centerCardPanel.add(analyticsPanel,   "analytics");
        centerCardPanel.add(protocolLabPanel, "protocol");
        centerCardPanel.add(experimentPanel,  "experiments");
        centerCardPanel.add(eventLogPanel,    "logs");
        centerCardPanel.add(settingsPanel,    "settings");

        // Layout
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_PAGE);
        root.add(headerPanel, BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(centerCardPanel, BorderLayout.CENTER);

        setContentPane(root);

        // Network change event logging
        network.addListener(() -> {
            int up = network.activeEdgeCount();
            int total = network.totalEdgeCount();
            eventLogPanel.log("Topology update: " + up + "/" + total + " communication links active.", "TOPOLOGY");
        });

        eventLogPanel.log("Reliable Emergency Communication Network desktop application started successfully.", "INFO");
    }

    private void switchTab(String tabId) {
        cardLayout.show(centerCardPanel, tabId);
        if ("routing".equals(tabId)) {
            routingPanel.recomputeAll();
        } else if ("experiments".equals(tabId)) {
            experimentPanel.refreshTable();
        }
    }

    private void onSimulationCompleted(SimulationMetrics metrics) {
        analyticsPanel.updateMetrics(metrics, 20);
        headerPanel.setSimulationState("COMPLETED (" + String.format("%.0f%%", metrics.deliveryRatio * 100) + ")", Theme.GREEN_OPERATIONAL);
        eventLogPanel.log("Simulation finished: " + metrics.summary(), "INFO");
    }
}
