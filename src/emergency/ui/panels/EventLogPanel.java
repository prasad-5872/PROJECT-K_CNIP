package emergency.ui.panels;

import emergency.ui.Theme;
import emergency.ui.components.ModernButton;
import emergency.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Tactical Real-Time Event Log Panel.
 * Records timestamped network events, route mutations, CRC outcomes,
 * ARQ retransmissions, and buffer overflows. Features live filtering,
 * memory-bounded performance, search, and log file export.
 */
public class EventLogPanel extends JPanel {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_LOG_LINES = 5000;

    public static final class LogEntry {
        public final String timestamp;
        public final String message;
        public final String level;

        public LogEntry(String timestamp, String message, String level) {
            this.timestamp = timestamp;
            this.message = message;
            this.level = level != null ? level : "INFO";
        }

        @Override
        public String toString() {
            return String.format("[%s] [%-5s] %s", timestamp, level, message);
        }
    }

    private final List<LogEntry> allEntries = new ArrayList<>();
    private final JTextArea logArea = new JTextArea();
    private final JComboBox<String> filterCombo = new JComboBox<>(new String[]{"ALL LEVELS", "INFO", "WARN", "ERROR"});
    private final JTextField searchField = new JTextField(15);
    private final JCheckBox autoScrollCheck = new JCheckBox("Auto-Scroll", true);
    private final JLabel countLabel = new JLabel("Entries: 0");

    public EventLogPanel() {
        super(new BorderLayout(10, 10));
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(createToolBar(), BorderLayout.NORTH);
        add(createLogAreaCard(), BorderLayout.CENTER);

        log("Emergency Operations Center Event Logger initialized.", "INFO");
    }

    private JPanel createToolBar() {
        RoundedPanel bar = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 10, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);

        JLabel filterLbl = new JLabel("Filter Level:");
        filterLbl.setFont(Theme.FONT_SECTION);
        filterLbl.setForeground(Theme.TEXT_SECONDARY);

        filterCombo.setBackground(Theme.BG_INPUT);
        filterCombo.setForeground(Theme.TEXT_PRIMARY);
        filterCombo.setFont(Theme.FONT_REGULAR);
        filterCombo.setFocusable(false);
        filterCombo.addActionListener(e -> applyFilters());

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(Theme.FONT_SECTION);
        searchLbl.setForeground(Theme.TEXT_SECONDARY);

        searchField.setBackground(Theme.BG_INPUT);
        searchField.setForeground(Theme.TEXT_PRIMARY);
        searchField.setCaretColor(Theme.CYAN_ACCENT);
        searchField.setFont(Theme.FONT_MONO);
        searchField.addActionListener(e -> applyFilters());

        autoScrollCheck.setOpaque(false);
        autoScrollCheck.setFont(Theme.FONT_SMALL);
        autoScrollCheck.setForeground(Theme.TEXT_SECONDARY);

        ModernButton btnCopy = new ModernButton("Copy Log", ModernButton.Style.SECONDARY);
        btnCopy.addActionListener(e -> copyAction());

        ModernButton btnClear = new ModernButton("Clear Log", ModernButton.Style.SECONDARY);
        btnClear.addActionListener(e -> clearAction());

        ModernButton btnExport = new ModernButton("Export Log", ModernButton.Style.PRIMARY);
        btnExport.addActionListener(e -> exportAction());

        countLabel.setFont(Theme.FONT_SMALL);
        countLabel.setForeground(Theme.CYAN_ACCENT);

        bar.add(filterLbl);
        bar.add(filterCombo);
        bar.add(searchLbl);
        bar.add(searchField);
        bar.add(autoScrollCheck);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(btnCopy);
        bar.add(btnClear);
        bar.add(btnExport);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(countLabel);

        return bar;
    }

    private JPanel createLogAreaCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(8, 8, 8, 8));

        logArea.setEditable(false);
        logArea.setFont(Theme.FONT_MONO);
        logArea.setBackground(Theme.BG_ELEVATED);
        logArea.setForeground(Theme.TEXT_PRIMARY);
        logArea.setCaretColor(Theme.CYAN_ACCENT);

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE));
        sp.getViewport().setBackground(Theme.BG_ELEVATED);

        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    public synchronized void log(String message, String level) {
        String ts = LocalTime.now().format(TIME_FMT);
        LogEntry entry = new LogEntry(ts, message, level);

        if (allEntries.size() >= MAX_LOG_LINES) {
            allEntries.remove(0);
        }
        allEntries.add(entry);

        SwingUtilities.invokeLater(() -> {
            countLabel.setText("Entries: " + allEntries.size());
            if (matchesFilter(entry)) {
                logArea.append(entry.toString() + "\n");
                if (autoScrollCheck.isSelected()) {
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }
        });
    }

    private boolean matchesFilter(LogEntry entry) {
        String selectedFilter = (String) filterCombo.getSelectedItem();
        if (selectedFilter != null && !selectedFilter.equals("ALL LEVELS")) {
            if (!entry.level.equalsIgnoreCase(selectedFilter)) {
                return false;
            }
        }
        String query = searchField.getText().trim().toLowerCase();
        if (!query.isEmpty()) {
            return entry.message.toLowerCase().contains(query);
        }
        return true;
    }

    private synchronized void applyFilters() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : allEntries) {
            if (matchesFilter(e)) {
                sb.append(e.toString()).append("\n");
            }
        }
        logArea.setText(sb.toString());
        if (autoScrollCheck.isSelected()) {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private void copyAction() {
        String text = logArea.getText();
        if (text != null && !text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(this, "Log copied to clipboard.", "Notice", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private synchronized void clearAction() {
        allEntries.clear();
        logArea.setText("");
        countLabel.setText("Entries: 0");
        log("Log cleared by operator.", "INFO");
    }

    private synchronized void exportAction() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("emergency_network_event_log.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fc.getSelectedFile(), StandardCharsets.UTF_8)) {
                for (LogEntry e : allEntries) {
                    fw.write(e.toString() + "\n");
                }
                JOptionPane.showMessageDialog(this, "Log successfully exported to:\n" + fc.getSelectedFile().getAbsolutePath(),
                        "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export log: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
