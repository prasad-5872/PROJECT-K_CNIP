package emergency.ui.panels;

import emergency.model.NetworkModel;
import emergency.ui.Theme;
import emergency.ui.components.ModernButton;
import emergency.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Tactical Settings & Built-in System Documentation.
 * Provides user configuration for simulation tick rates, random seed controls,
 * network model reset, and full on-board reference documentation.
 */
public class SettingsPanel extends JPanel {
    private final NetworkModel network;
    private final JSpinner defaultSeedSpinner = new JSpinner(new SpinnerNumberModel(42, 1, 999999, 1));
    private final JSlider speedSlider = new JSlider(0, 50, 10);
    private final JLabel speedLabel = new JLabel("Animation Delay: 10 ms (Smooth)");
    private final JTextArea docsArea = new JTextArea();

    public SettingsPanel(NetworkModel network) {
        super(new BorderLayout(12, 12));
        this.network = network;
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("SYSTEM SETTINGS & TACTICAL MANUAL");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.CYAN_ACCENT);

        topBar.add(title, BorderLayout.NORTH);
        add(topBar, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createConfigCard(), createDocsCard());
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.35);

        add(split, BorderLayout.CENTER);

        loadDocumentationText();
    }

    private JPanel createConfigCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("SIMULATION PREFERENCES");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.BLUE_ACCENT);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        // Seed
        JLabel seedLbl = new JLabel("Default Fixed Random Seed:");
        seedLbl.setFont(Theme.FONT_SECTION);
        seedLbl.setForeground(Theme.TEXT_SECONDARY);
        form.add(seedLbl);
        form.add(Box.createVerticalStrut(4));
        form.add(defaultSeedSpinner);
        form.add(Box.createVerticalStrut(10));

        // Speed
        speedLabel.setFont(Theme.FONT_SECTION);
        speedLabel.setForeground(Theme.TEXT_SECONDARY);
        speedSlider.setOpaque(false);
        speedSlider.setMajorTickSpacing(10);
        speedSlider.setPaintTicks(true);
        speedSlider.setForeground(Theme.TEXT_MUTED);
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            if (val == 0) speedLabel.setText("Animation Delay: 0 ms (Instant Batch)");
            else speedLabel.setText("Animation Delay: " + val + " ms");
        });

        form.add(speedLabel);
        form.add(speedSlider);
        form.add(Box.createVerticalStrut(15));

        // Actions
        ModernButton btnRestoreNet = new ModernButton("Restore All Network Links", ModernButton.Style.SUCCESS);
        btnRestoreNet.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRestoreNet.addActionListener(e -> {
            network.restoreAllEdges();
            JOptionPane.showMessageDialog(this, "All 10 emergency network links restored to UP state.", "Notice", JOptionPane.INFORMATION_MESSAGE);
        });

        ModernButton btnResetDefaults = new ModernButton("Reset Preferences to Defaults", ModernButton.Style.SECONDARY);
        btnResetDefaults.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnResetDefaults.addActionListener(e -> {
            defaultSeedSpinner.setValue(42);
            speedSlider.setValue(10);
            JOptionPane.showMessageDialog(this, "Preferences restored to defaults.", "Notice", JOptionPane.INFORMATION_MESSAGE);
        });

        form.add(btnRestoreNet);
        form.add(Box.createVerticalStrut(8));
        form.add(btnResetDefaults);

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);

        return card;
    }

    private JPanel createDocsCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel header = new JLabel("OPERATIONAL ARCHITECTURE & ALGORITHM SPECIFICATION");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.CYAN_ACCENT);

        docsArea.setEditable(false);
        docsArea.setFont(Theme.FONT_REGULAR);
        docsArea.setBackground(Theme.BG_ELEVATED);
        docsArea.setForeground(Theme.TEXT_PRIMARY);
        docsArea.setCaretColor(Theme.CYAN_ACCENT);
        docsArea.setLineWrap(true);
        docsArea.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(docsArea);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));
        sp.getViewport().setBackground(Theme.BG_ELEVATED);

        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private void loadDocumentationText() {
        docsArea.setText(
                "================================================================================\n" +
                "RELIABLE EMERGENCY COMMUNICATION NETWORK - TECHNICAL MANUAL (v2.0)\n" +
                "================================================================================\n\n" +
                "1. SYSTEM OVERVIEW\n" +
                "In emergency operations (natural disasters, search and rescue, power outages),\n" +
                "communication links suffer from physical destruction, RF interference, and severe\n" +
                "traffic surges. This tactical desktop suite models end-to-end message delivery under\n" +
                "these adverse conditions.\n\n" +
                "2. ROUTING ARCHITECTURE\n" +
                "- Dijkstra's Link-State Algorithm: Global view shortest-path algorithm. Evaluates the\n" +
                "  active graph topology and produces minimum-cost paths with zero routing loops.\n" +
                "  Complexity: O(|V|^2) or O(|E| log |V|).\n" +
                "- Distance Vector Routing: Distributed Bellman-Ford equation: D_x(y) = min_v { c(x,v) + D_v(y) }.\n" +
                "  Exchanges routing vectors between adjacent nodes synchronously until convergence.\n\n" +
                "3. ERROR DETECTION & RELIABILITY\n" +
                "- CRC-8 Checksum: Standard generator polynomial 0x07 (x^8 + x^2 + x + 1). Protects each\n" +
                "  individual frame against bit errors. Receiver re-computes CRC; any bit flip is flagged.\n" +
                "- Go-Back-N ARQ Protocol: Sliding window size N=4. Transmits up to N unacknowledged frames.\n" +
                "  Upon CRC failure or loss, sender triggers a timeout and retransmits all frames from the\n" +
                "  oldest unacknowledged frame (base).\n\n" +
                "4. CONGESTION CONTROL\n" +
                "- Leaky Bucket Traffic Shaper: Ingress buffer of capacity C with constant drain rate L pkts/tick.\n" +
                "  Disaster traffic bursts exceeding remaining capacity are dropped immediately at the ingress\n" +
                "  buffer, protecting network links from collapse.\n\n" +
                "5. METRICS & TELEMETRY DEFINITIONS\n" +
                "- Delivery Ratio (%) = (Delivered Messages / Generated Messages) * 100\n" +
                "- Throughput (Mbps) = (Delivered Payload Bits) / (Elapsed Simulated Seconds * 10^6)\n" +
                "- Average Delay (ms) = Mean end-to-end latency per delivered message\n" +
                "- Peak Queue Size = Maximum buffer occupancy observed in Leaky Bucket\n\n" +
                "6. REPRODUCIBILITY\n" +
                "Enable 'Fixed Seed (42)' to guarantee bit-for-bit reproducible trials across runs.\n"
        );
        docsArea.setCaretPosition(0);
    }

    public int getAnimationDelayMs() {
        return speedSlider.getValue();
    }
}
