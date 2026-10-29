package emergency.ui.panels;

import emergency.engine.SimulationConfig;
import emergency.engine.SimulationMetrics;
import emergency.experiment.CsvExporter;
import emergency.experiment.ExperimentManager;
import emergency.experiment.ExperimentRecord;
import emergency.ui.Theme;
import emergency.ui.components.ModernButton;
import emergency.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Tactical Experiment Management & Reporting Panel.
 * Manages empirical trial runs, baseline comparisons, CSV data export,
 * and comprehensive project report generation.
 */
public class ExperimentPanel extends JPanel {
    private final ExperimentManager experimentManager;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{
                    "Run ID", "Name", "Timestamp", "Algo", "Seed", "Msgs",
                    "FER %", "Arr/Cap/Lk", "Delivered", "Ratio %", "Throughput", "Delay", "Retrans", "Drops"
            }, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextArea comparisonArea = new JTextArea();

    public ExperimentPanel(ExperimentManager experimentManager) {
        super(new BorderLayout(10, 10));
        this.experimentManager = experimentManager;
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(createTopToolBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createTableCard(), createComparisonCard());
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.65);

        add(split, BorderLayout.CENTER);

        refreshTable();
    }

    private JPanel createTopToolBar() {
        RoundedPanel bar = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 10, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);

        ModernButton btnSetBaseline = new ModernButton("Set as Baseline", ModernButton.Style.SECONDARY);
        btnSetBaseline.addActionListener(e -> setBaselineAction());

        ModernButton btnCompare = new ModernButton("Compare to Baseline", ModernButton.Style.PURPLE);
        btnCompare.addActionListener(e -> compareAction());

        ModernButton btnExportCsv = new ModernButton("Export CSV", ModernButton.Style.PRIMARY);
        btnExportCsv.addActionListener(e -> exportCsvAction());

        ModernButton btnReport = new ModernButton("Generate Full Report", ModernButton.Style.SUCCESS);
        btnReport.addActionListener(e -> generateReportAction());

        ModernButton btnDelete = new ModernButton("Delete Run", ModernButton.Style.DANGER);
        btnDelete.addActionListener(e -> deleteAction());

        ModernButton btnClear = new ModernButton("Clear All", ModernButton.Style.SECONDARY);
        btnClear.addActionListener(e -> clearAction());

        bar.add(btnSetBaseline);
        bar.add(btnCompare);
        bar.add(btnExportCsv);
        bar.add(btnReport);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(btnDelete);
        bar.add(btnClear);

        return bar;
    }

    private JPanel createTableCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("SAVED EXPERIMENTAL RUNS");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.CYAN_ACCENT);

        table.setBackground(Theme.BG_CARD);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(Theme.FONT_REGULAR);
        table.setRowHeight(24);
        table.setGridColor(Theme.BORDER_SUBTLE);
        table.getTableHeader().setBackground(Theme.BG_ELEVATED);
        table.getTableHeader().setForeground(Theme.BLUE_ACCENT);
        table.getTableHeader().setFont(Theme.FONT_SECTION);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, center);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                compareAction();
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Theme.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));

        card.add(title, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel createComparisonCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(6, 6), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("BASELINE COMPARISON & HYPOTHESIS DELTA");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.PURPLE_ANALYTICS);

        comparisonArea.setEditable(false);
        comparisonArea.setFont(Theme.FONT_MONO);
        comparisonArea.setBackground(Theme.BG_ELEVATED);
        comparisonArea.setForeground(Theme.TEXT_PRIMARY);
        comparisonArea.setText("Select an experiment run from the table above to compare against baseline.");

        JScrollPane sp = new JScrollPane(comparisonArea);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));

        card.add(title, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<ExperimentRecord> list = experimentManager.getRecords();
        ExperimentRecord baseline = experimentManager.getBaseline();

        for (ExperimentRecord r : list) {
            SimulationConfig c = r.config;
            SimulationMetrics m = r.metrics;
            String isBase = (r == baseline) ? " [BASELINE]" : "";

            tableModel.addRow(new Object[]{
                    r.runId + isBase,
                    r.name,
                    r.timestamp,
                    c.algorithm,
                    c.randomSeed != null ? c.randomSeed : "Random",
                    c.messageCount,
                    String.format(Locale.US, "%.0f%%", c.errorRate * 100.0),
                    c.arrivalsPerTick + "/" + c.bucketCapacity + "/" + c.leakRate,
                    m.delivered,
                    String.format(Locale.US, "%.1f%%", m.deliveryRatio * 100.0),
                    String.format(Locale.US, "%.3f M", m.throughputMbps),
                    String.format(Locale.US, "%.1f ms", m.averageDelayMs),
                    m.retransmissions,
                    m.bucketDropped
            });
        }
    }

    private void setBaselineAction() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a run to designate as the baseline.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ExperimentRecord rec = experimentManager.getRecords().get(row);
        experimentManager.setBaseline(rec);
        refreshTable();
        table.setRowSelectionInterval(row, row);
        compareAction();
    }

    private void compareAction() {
        int row = table.getSelectedRow();
        if (row < 0) {
            comparisonArea.setText("Select an experiment run from the table above to compare against baseline.");
            return;
        }
        ExperimentRecord rec = experimentManager.getRecords().get(row);
        ExperimentRecord base = experimentManager.getBaseline();
        comparisonArea.setText(ExperimentManager.compareWithBaseline(rec, base));
    }

    private void deleteAction() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        experimentManager.removeRecord(row);
        refreshTable();
        comparisonArea.setText("Record deleted.");
    }

    private void clearAction() {
        if (JOptionPane.showConfirmDialog(this, "Clear all saved experiments?", "Confirm Clear",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            experimentManager.clear();
            refreshTable();
            comparisonArea.setText("All records cleared.");
        }
    }

    private void exportCsvAction() {
        List<ExperimentRecord> records = experimentManager.getRecords();
        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No experiments to export. Run and save simulations first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("emergency_experiments.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                CsvExporter.exportExperimentsToCsv(records, fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "CSV exported successfully to:\n" + fc.getSelectedFile().getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void generateReportAction() {
        int row = table.getSelectedRow();
        if (row < 0 && !experimentManager.getRecords().isEmpty()) {
            row = 0;
        }
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "No experiments available to report.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ExperimentRecord rec = experimentManager.getRecords().get(row);
        String reportMd = CsvExporter.generateMarkdownReport(rec);

        JTextArea previewArea = new JTextArea(reportMd);
        previewArea.setFont(Theme.FONT_MONO);
        previewArea.setEditable(false);
        previewArea.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(previewArea);
        sp.setPreferredSize(new Dimension(650, 450));

        JPanel dlgPanel = new JPanel(new BorderLayout(8, 8));
        dlgPanel.add(new JLabel("Markdown Report Preview:"), BorderLayout.NORTH);
        dlgPanel.add(sp, BorderLayout.CENTER);

        int opt = JOptionPane.showOptionDialog(this, dlgPanel, "Experiment Report: " + rec.name,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Save Markdown File", "Close"}, "Save Markdown File");

        if (opt == 0) {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(rec.runId + "_report.md"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter fw = new FileWriter(fc.getSelectedFile(), StandardCharsets.UTF_8)) {
                    fw.write(reportMd);
                    JOptionPane.showMessageDialog(this, "Report saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error saving report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
