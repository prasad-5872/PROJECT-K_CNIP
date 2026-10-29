package emergency.ui.panels;

import emergency.engine.SimulationConfig;
import emergency.engine.SimulationListener;
import emergency.engine.SimulationMetrics;
import emergency.engine.SimulationWorker;
import emergency.experiment.ExperimentManager;
import emergency.model.NetworkModel;
import emergency.ui.Theme;
import emergency.ui.components.ModernButton;
import emergency.ui.components.RoundedPanel;
import emergency.ui.components.StatCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Interactive Simulation Execution & Metrics Dashboard.
 * Houses validated simulation controls, preset configurations, non-blocking
 * execution controls (Run, Pause, Resume, Stop), and real-time telemetry KPI cards.
 */
public class SimulationPanel extends JPanel {
    private final NetworkModel network;
    private final ExperimentManager experimentManager;
    private final Consumer<String> logConsumer;
    private final Consumer<SimulationMetrics> onSimulationCompleteCallback;

    // Controls
    private final JSpinner messagesSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 10));
    private final JSlider errorSlider = new JSlider(0, 50, 10);
    private final JLabel errorLabel = new JLabel("Frame Error Rate: 10%");
    private final JSpinner arrivalsSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 100, 1));
    private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(20, 1, 500, 1));
    private final JSpinner leakSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private final JComboBox<String> sourceCombo = new JComboBox<>();
    private final JComboBox<String> destCombo = new JComboBox<>();
    private final JComboBox<String> algoCombo = new JComboBox<>(new String[]{"Dijkstra", "Distance Vector"});
    private final JCheckBox fixedSeedCheck = new JCheckBox("Fixed Seed (42)", true);

    // Action buttons
    private final ModernButton btnRun = new ModernButton("Run Simulation", ModernButton.Style.PRIMARY);
    private final ModernButton btnPause = new ModernButton("Pause", ModernButton.Style.SECONDARY);
    private final ModernButton btnStop = new ModernButton("Stop", ModernButton.Style.DANGER);
    private final ModernButton btnReset = new ModernButton("Reset Defaults", ModernButton.Style.SECONDARY);
    private final ModernButton btnRestore = new ModernButton("Restore Links", ModernButton.Style.SUCCESS);
    private final ModernButton btnSaveExp = new ModernButton("Save Run to Experiments", ModernButton.Style.PURPLE);

    // Progress
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusText = new JLabel("System Ready. Configure parameters and run simulation.");

    // KPI Stat Cards
    private final StatCard cardDelivered = new StatCard("Delivered", "0", "msgs", Theme.GREEN_OPERATIONAL);
    private final StatCard cardRatio = new StatCard("Delivery Ratio", "0.0", "%", Theme.CYAN_ACCENT);
    private final StatCard cardThroughput = new StatCard("Throughput", "0.000", "Mbps", Theme.PURPLE_ANALYTICS);
    private final StatCard cardDelay = new StatCard("Average Delay", "0.0", "ms", Theme.BLUE_ACCENT);
    private final StatCard cardBucketDrops = new StatCard("Bucket Drops", "0", "pkts", Theme.RED_FAILURE);
    private final StatCard cardRetrans = new StatCard("Retransmissions", "0", "frames", Theme.AMBER_WARNING);
    private final StatCard cardCorrupt = new StatCard("CRC Errors", "0", "frames", Theme.RED_FAILURE);
    private final StatCard cardUnroutable = new StatCard("Unroutable", "0", "msgs", Theme.RED_FAILURE);
    private final StatCard cardPeakQueue = new StatCard("Peak Queue", "0", "pkts", Theme.AMBER_WARNING);

    private SimulationWorker activeWorker = null;
    private SimulationMetrics lastMetrics = null;
    private SimulationConfig lastConfig = null;

    public SimulationPanel(NetworkModel network, ExperimentManager expMgr,
                           Consumer<String> logConsumer, Consumer<SimulationMetrics> onComplete) {
        super(new BorderLayout(12, 12));
        this.network = network;
        this.experimentManager = expMgr;
        this.logConsumer = logConsumer;
        this.onSimulationCompleteCallback = onComplete;

        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        populateCombos();

        add(createControlSidebar(), BorderLayout.WEST);
        add(createDashboardCenter(), BorderLayout.CENTER);

        setupListeners();
    }

    private void populateCombos() {
        sourceCombo.removeAllItems();
        destCombo.removeAllItems();
        for (String node : network.nodes()) {
            sourceCombo.addItem(node);
            destCombo.addItem(node);
        }
        sourceCombo.setSelectedItem("Control Center");
        destCombo.setSelectedItem("Shelter B");

        styleCombo(sourceCombo);
        styleCombo(destCombo);
        styleCombo(algoCombo);

        fixedSeedCheck.setOpaque(false);
        fixedSeedCheck.setFont(Theme.FONT_SMALL);
        fixedSeedCheck.setForeground(Theme.TEXT_SECONDARY);
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setFont(Theme.FONT_REGULAR);
        cb.setFocusable(false);
    }

    private JPanel createControlSidebar() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        panel.setPreferredSize(new Dimension(340, 650));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("SIMULATION PARAMETERS");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.CYAN_ACCENT);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        form.add(createField("Source Node:", sourceCombo));
        form.add(Box.createVerticalStrut(5));
        form.add(createField("Destination Node:", destCombo));
        form.add(Box.createVerticalStrut(5));
        form.add(createField("Routing Algorithm:", algoCombo));
        form.add(Box.createVerticalStrut(8));

        form.add(createField("Messages to Generate:", messagesSpinner));
        form.add(Box.createVerticalStrut(8));

        errorLabel.setFont(Theme.FONT_SECTION);
        errorLabel.setForeground(Theme.TEXT_SECONDARY);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorSlider.setOpaque(false);
        errorSlider.setMajorTickSpacing(10);
        errorSlider.setMinorTickSpacing(5);
        errorSlider.setPaintTicks(true);
        errorSlider.setPaintLabels(true);
        errorSlider.setForeground(Theme.TEXT_MUTED);
        errorSlider.setFont(Theme.FONT_SMALL);
        errorSlider.addChangeListener(e -> errorLabel.setText("Frame Error Rate: " + errorSlider.getValue() + "%"));

        form.add(errorLabel);
        form.add(errorSlider);
        form.add(Box.createVerticalStrut(8));

        form.add(createField("Ingress Arrival Rate (pkts/tick):", arrivalsSpinner));
        form.add(Box.createVerticalStrut(5));
        form.add(createField("Leaky Bucket Capacity:", capacitySpinner));
        form.add(Box.createVerticalStrut(5));
        form.add(createField("Leaky Rate (pkts/tick):", leakSpinner));
        form.add(Box.createVerticalStrut(8));

        form.add(fixedSeedCheck);
        form.add(Box.createVerticalStrut(10));

        // Presets
        JLabel presetLbl = new JLabel("Experiment Presets:");
        presetLbl.setFont(Theme.FONT_SECTION);
        presetLbl.setForeground(Theme.TEXT_SECONDARY);
        form.add(presetLbl);
        form.add(Box.createVerticalStrut(4));

        JPanel presetBar = new JPanel(new GridLayout(2, 2, 6, 6));
        presetBar.setOpaque(false);

        ModernButton pBaseline = new ModernButton("Baseline Normal", ModernButton.Style.SECONDARY);
        pBaseline.setFont(Theme.FONT_SMALL);
        pBaseline.addActionListener(e -> applyPreset(100, 10, 8, 20, 5));

        ModernButton pCongestion = new ModernButton("High Burst", ModernButton.Style.SECONDARY);
        pCongestion.setFont(Theme.FONT_SMALL);
        pCongestion.addActionListener(e -> applyPreset(150, 10, 25, 20, 5));

        ModernButton pNoise = new ModernButton("High Error (35%)", ModernButton.Style.SECONDARY);
        pNoise.setFont(Theme.FONT_SMALL);
        pNoise.addActionListener(e -> applyPreset(100, 35, 8, 20, 5));

        ModernButton pRestore = new ModernButton("Restore Links", ModernButton.Style.SUCCESS);
        pRestore.setFont(Theme.FONT_SMALL);
        pRestore.addActionListener(e -> network.restoreAllEdges());

        presetBar.add(pBaseline);
        presetBar.add(pCongestion);
        presetBar.add(pNoise);
        presetBar.add(pRestore);
        form.add(presetBar);

        form.add(Box.createVerticalStrut(12));

        // Execution Buttons
        JPanel actionPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        actionPanel.setOpaque(false);

        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        btnSaveExp.setEnabled(false);

        actionPanel.add(btnRun);
        actionPanel.add(btnPause);
        actionPanel.add(btnStop);
        actionPanel.add(btnReset);

        form.add(actionPanel);
        form.add(Box.createVerticalStrut(6));
        btnSaveExp.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(btnSaveExp);

        JScrollPane scrollForm = new JScrollPane(form);
        scrollForm.setOpaque(false);
        scrollForm.getViewport().setOpaque(false);
        scrollForm.setBorder(null);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollForm, BorderLayout.CENTER);

        return panel;
    }

    private void applyPreset(int msgs, int err, int arr, int cap, int lk) {
        messagesSpinner.setValue(msgs);
        errorSlider.setValue(err);
        arrivalsSpinner.setValue(arr);
        capacitySpinner.setValue(cap);
        leakSpinner.setValue(lk);
    }

    private JPanel createField(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(320, 55));
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_SECTION);
        l.setForeground(Theme.TEXT_SECONDARY);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel createDashboardCenter() {
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);

        // Top: Progress & Execution Status Bar
        RoundedPanel progressPanel = new RoundedPanel(new BorderLayout(10, 6), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        progressPanel.setBorder(new EmptyBorder(10, 14, 10, 14));

        statusText.setFont(Theme.FONT_REGULAR);
        statusText.setForeground(Theme.TEXT_PRIMARY);

        progressBar.setStringPainted(true);
        progressBar.setBackground(Theme.BG_INPUT);
        progressBar.setForeground(Theme.CYAN_ACCENT);
        progressBar.setFont(Theme.FONT_SMALL);
        progressBar.setPreferredSize(new Dimension(200, 20));

        progressPanel.add(statusText, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        center.add(progressPanel, BorderLayout.NORTH);

        // Middle: Telemetry KPI Stat Cards Grid
        RoundedPanel cardsContainer = new RoundedPanel(new BorderLayout(), 10, Theme.BG_PAGE, null);
        JPanel grid = new JPanel(new GridLayout(3, 3, 10, 10));
        grid.setOpaque(false);

        grid.add(cardDelivered);
        grid.add(cardRatio);
        grid.add(cardThroughput);
        grid.add(cardDelay);
        grid.add(cardBucketDrops);
        grid.add(cardRetrans);
        grid.add(cardCorrupt);
        grid.add(cardUnroutable);
        grid.add(cardPeakQueue);

        cardsContainer.add(grid, BorderLayout.CENTER);
        center.add(cardsContainer, BorderLayout.CENTER);

        // Bottom: Operational Pipeline Flow Explanation
        RoundedPanel flowExpl = new RoundedPanel(new BorderLayout(), 8, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        flowExpl.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel flowTitle = new JLabel("CONNECTED SIMULATION FLOW ARCHITECTURE");
        flowTitle.setFont(Theme.FONT_SECTION);
        flowTitle.setForeground(Theme.CYAN_ACCENT);

        JTextArea flowText = new JTextArea(
                "1. Traffic Generator: Creates burst messages offered to Leaky Bucket ingress queue.\n" +
                "2. Leaky Bucket: Packets exceeding buffer capacity are dropped immediately (Ingress Drops). Conforming traffic leaks at constant service rate.\n" +
                "3. Routing Layer: Evaluates active topology via Dijkstra or Distance Vector. If path cost == INF (severed links), packets drop as Unroutable.\n" +
                "4. Transport & CRC-8: Conforming routed messages are split into frames and transmitted via Go-Back-N ARQ. Channel FER causes bit errors detected by CRC-8, triggering retransmissions."
        );
        flowText.setEditable(false);
        flowText.setFont(Theme.FONT_SMALL);
        flowText.setForeground(Theme.TEXT_SECONDARY);
        flowText.setBackground(Theme.BG_CARD);
        flowText.setOpaque(false);

        flowExpl.add(flowTitle, BorderLayout.NORTH);
        flowExpl.add(flowText, BorderLayout.CENTER);
        center.add(flowExpl, BorderLayout.SOUTH);

        return center;
    }

    private void setupListeners() {
        btnRun.addActionListener(e -> startSimulation());
        btnPause.addActionListener(e -> togglePause());
        btnStop.addActionListener(e -> stopSimulation());
        btnReset.addActionListener(e -> applyPreset(100, 10, 8, 20, 5));
        btnSaveExp.addActionListener(e -> saveCurrentExperiment());
    }

    private void startSimulation() {
        int msgs = (Integer) messagesSpinner.getValue();
        double fer = errorSlider.getValue() / 100.0;
        int arr = (Integer) arrivalsSpinner.getValue();
        int cap = (Integer) capacitySpinner.getValue();
        int lk = (Integer) leakSpinner.getValue();
        String src = (String) sourceCombo.getSelectedItem();
        String dst = (String) destCombo.getSelectedItem();
        String algo = (String) algoCombo.getSelectedItem();
        Long seed = fixedSeedCheck.isSelected() ? 42L : null;

        SimulationConfig config = new SimulationConfig(msgs, fer, arr, cap, lk, src, dst, algo, seed, 8, 5, 4);
        this.lastConfig = config;

        setControlsRunning(true);
        progressBar.setValue(0);
        statusText.setText("Simulation Initializing...");

        activeWorker = new SimulationWorker(network, config, new SimulationListener() {
            @Override
            public void onStateChanged(State newState) {
                SwingUtilities.invokeLater(() -> {
                    switch (newState) {
                        case RUNNING -> {
                            btnPause.setText("Pause");
                            statusText.setText("Simulation Running...");
                        }
                        case PAUSED -> {
                            btnPause.setText("Resume");
                            statusText.setText("Simulation PAUSED. Click Resume to continue.");
                        }
                        case COMPLETED, CANCELLED, ERROR -> setControlsRunning(false);
                    }
                });
            }

            @Override
            public void onProgress(int current, int total, double percent, String status) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue((int) percent);
                    statusText.setText(String.format(Locale.US, "[%.0f%%] %s", percent, status));
                });
            }

            @Override
            public void onLog(String timestamp, String message, String level) {
                if (logConsumer != null) {
                    logConsumer.accept(String.format("[%s] [%s] %s", timestamp, level, message));
                }
            }

            @Override
            public void onCompleted(SimulationMetrics metrics) {
                SwingUtilities.invokeLater(() -> {
                    lastMetrics = metrics;
                    updateDashboardCards(metrics);
                    btnSaveExp.setEnabled(true);
                    progressBar.setValue(100);
                    statusText.setText("Simulation Finished: " + metrics.summary());
                    if (onSimulationCompleteCallback != null) {
                        onSimulationCompleteCallback.accept(metrics);
                    }
                });
            }
        });

        activeWorker.execute();
    }

    private void togglePause() {
        if (activeWorker == null) return;
        if (activeWorker.isPaused()) {
            activeWorker.resumeSimulation();
        } else {
            activeWorker.pause();
        }
    }

    private void stopSimulation() {
        if (activeWorker != null) {
            activeWorker.stopSimulation();
            statusText.setText("Simulation Stopped by user.");
            setControlsRunning(false);
        }
    }

    private void setControlsRunning(boolean running) {
        btnRun.setEnabled(!running);
        btnPause.setEnabled(running);
        btnStop.setEnabled(running);
        messagesSpinner.setEnabled(!running);
        errorSlider.setEnabled(!running);
        arrivalsSpinner.setEnabled(!running);
        capacitySpinner.setEnabled(!running);
        leakSpinner.setEnabled(!running);
        sourceCombo.setEnabled(!running);
        destCombo.setEnabled(!running);
        algoCombo.setEnabled(!running);
        fixedSeedCheck.setEnabled(!running);
    }

    private void updateDashboardCards(SimulationMetrics m) {
        cardDelivered.setValue(String.valueOf(m.delivered));
        cardRatio.setValue(String.format(Locale.US, "%.1f", m.deliveryRatio * 100.0));
        cardThroughput.setValue(String.format(Locale.US, "%.3f", m.throughputMbps));
        cardDelay.setValue(String.format(Locale.US, "%.1f", m.averageDelayMs));
        cardBucketDrops.setValue(String.valueOf(m.bucketDropped));
        cardRetrans.setValue(String.valueOf(m.retransmissions));
        cardCorrupt.setValue(String.valueOf(m.corruptedFrames));
        cardUnroutable.setValue(String.valueOf(m.unroutable));
        cardPeakQueue.setValue(String.valueOf(m.peakQueue));
    }

    private void saveCurrentExperiment() {
        if (lastMetrics == null || lastConfig == null) {
            JOptionPane.showMessageDialog(this, "No completed simulation to save.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Enter experiment name:", "Run " + lastConfig.algorithm + " FER " + (int)(lastConfig.errorRate*100) + "%");
        if (name != null && !name.trim().isEmpty()) {
            experimentManager.saveRun(name, lastConfig, lastMetrics, "Saved from Simulation Panel");
            JOptionPane.showMessageDialog(this, "Experiment saved to Experiment Manager!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public SimulationMetrics getLastMetrics() {
        return lastMetrics;
    }
}
