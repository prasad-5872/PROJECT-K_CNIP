package emergency.ui;

import emergency.model.NetworkModel;
import emergency.ui.components.StatusBadge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Top tactical header banner displaying title, operational readiness,
 * and live network health indicators.
 */
public class HeaderPanel extends JPanel {
    private final JLabel titleLabel = new JLabel("RELIABLE EMERGENCY COMMUNICATION NETWORK");
    private final JLabel subtitleLabel = new JLabel("Tactical Disaster Response & Congestion Control Operations Center");
    private final StatusBadge systemStatusBadge = new StatusBadge("SYSTEM READY", Theme.GREEN_OPERATIONAL);
    private final StatusBadge linkStatusBadge = new StatusBadge("10/10 LINKS UP", Theme.GREEN_OPERATIONAL);
    private final StatusBadge simStatusBadge = new StatusBadge("SIM: IDLE", Theme.CYAN_ACCENT);

    public HeaderPanel(NetworkModel network) {
        super(new BorderLayout(15, 0));
        setBackground(Theme.BG_HEADER);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                new EmptyBorder(12, 18, 12, 18)
        ));

        titleLabel.setFont(Theme.FONT_HEADER);
        titleLabel.setForeground(Theme.BLUE_ACCENT);

        subtitleLabel.setFont(Theme.FONT_SMALL);
        subtitleLabel.setForeground(Theme.TEXT_SECONDARY);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.add(titleLabel);
        leftPanel.add(Box.createVerticalStrut(3));
        leftPanel.add(subtitleLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightPanel.setOpaque(false);
        rightPanel.add(linkStatusBadge);
        rightPanel.add(simStatusBadge);
        rightPanel.add(systemStatusBadge);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        if (network != null) {
            updateNetworkStatus(network);
            network.addListener(() -> updateNetworkStatus(network));
        }
    }

    public void updateNetworkStatus(NetworkModel network) {
        int active = network.activeEdgeCount();
        int total = network.totalEdgeCount();
        if (active == total) {
            linkStatusBadge.setStatus(active + "/" + total + " LINKS UP", Theme.GREEN_OPERATIONAL);
            systemStatusBadge.setStatus("FULL READINESS", Theme.GREEN_OPERATIONAL);
        } else if (active > 0) {
            linkStatusBadge.setStatus(active + "/" + total + " LINKS ACTIVE", Theme.AMBER_WARNING);
            systemStatusBadge.setStatus("DEGRADED TOPOLOGY", Theme.AMBER_WARNING);
        } else {
            linkStatusBadge.setStatus("ALL LINKS DOWN", Theme.RED_FAILURE);
            systemStatusBadge.setStatus("NETWORK SEVERED", Theme.RED_FAILURE);
        }
    }

    public void setSimulationState(String text, Color color) {
        simStatusBadge.setStatus("SIM: " + text, color);
    }
}
