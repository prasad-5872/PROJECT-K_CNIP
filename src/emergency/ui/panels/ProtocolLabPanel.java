package emergency.ui.panels;

import emergency.protocol.Crc8;
import emergency.protocol.GoBackNProtocol;
import emergency.protocol.TransferResult;
import emergency.ui.Theme;
import emergency.ui.components.ModernButton;
import emergency.ui.components.RoundedPanel;
import emergency.ui.components.StatusBadge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Interactive Protocol & CRC Laboratory.
 * Provides live bit-mutation testing for CRC-8 (polynomial 0x07),
 * polynomial division bit trace, and visual Go-Back-N ARQ sliding window inspection.
 * Explicitly labeled as an educational micro-lab.
 */
public class ProtocolLabPanel extends JPanel {
    // CRC Components
    private final JTextField payloadField = new JTextField("MAYDAY: Ambulance required at Shelter B");
    private final JLabel crcDisplayLabel = new JLabel("Computed CRC-8: 0x00");
    private final StatusBadge crcStatusBadge = new StatusBadge("CHECKSUM VALID", Theme.GREEN_OPERATIONAL);
    private final JTextArea crcTraceArea = new JTextArea();
    private byte[] originalBytes;
    private byte[] currentBytes;
    private int originalCrc;

    // GBN Components
    private final JSpinner gbnFramesSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 30, 1));
    private final JSpinner gbnWindowSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
    private final JSlider gbnErrorSlider = new JSlider(0, 50, 15);
    private final JLabel gbnErrorLabel = new JLabel("Channel Error Rate: 15%");
    private final JTextArea gbnTraceArea = new JTextArea();
    private final JLabel gbnResultLabel = new JLabel("ARQ Session: Ready");

    public ProtocolLabPanel() {
        super(new BorderLayout(12, 12));
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        originalBytes = payloadField.getText().getBytes(StandardCharsets.UTF_8);
        currentBytes = originalBytes.clone();
        originalCrc = Crc8.compute(originalBytes);

        // Header
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("PROTOCOL & ERROR CONTROL LABORATORY");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.CYAN_ACCENT);

        JLabel banner = new JLabel("Educational Micro-Lab: Isolates CRC-8 integrity verification and Go-Back-N ARQ window mechanics.");
        banner.setFont(Theme.FONT_SMALL);
        banner.setForeground(Theme.AMBER_WARNING);

        topBar.add(title, BorderLayout.NORTH);
        topBar.add(banner, BorderLayout.SOUTH);
        add(topBar, BorderLayout.NORTH);

        // Split view: Left = CRC Lab, Right = Go-Back-N Lab
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createCrcLabCard(), createGbnLabCard());
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.5);

        add(split, BorderLayout.CENTER);

        updateCrcState();
    }

    private JPanel createCrcLabCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("CRC-8 CHECKSUM & BIT-MUTATION LAB (Poly: 0x07)");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.CYAN_ACCENT);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setOpaque(false);

        JLabel prompt = new JLabel("Frame Payload String:");
        prompt.setFont(Theme.FONT_SECTION);
        prompt.setForeground(Theme.TEXT_SECONDARY);

        payloadField.setBackground(Theme.BG_INPUT);
        payloadField.setForeground(Theme.TEXT_PRIMARY);
        payloadField.setCaretColor(Theme.CYAN_ACCENT);
        payloadField.setFont(Theme.FONT_MONO);
        payloadField.addActionListener(e -> resetCrcPayload());

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusRow.setOpaque(false);
        crcDisplayLabel.setFont(Theme.FONT_SECTION);
        crcDisplayLabel.setForeground(Theme.BLUE_ACCENT);
        statusRow.add(crcDisplayLabel);
        statusRow.add(crcStatusBadge);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnRow.setOpaque(false);

        ModernButton btnFlipBit0 = new ModernButton("Flip Bit 0 (Byte 0)", ModernButton.Style.DANGER);
        btnFlipBit0.addActionListener(e -> mutateBit(0, 0));

        ModernButton btnFlipRandom = new ModernButton("Flip Random Bit", ModernButton.Style.DANGER);
        btnFlipRandom.addActionListener(e -> {
            Random r = new Random();
            int b = r.nextInt(Math.max(1, currentBytes.length));
            int bit = r.nextInt(8);
            mutateBit(b, bit);
        });

        ModernButton btnRestore = new ModernButton("Restore Clean Payload", ModernButton.Style.SUCCESS);
        btnRestore.addActionListener(e -> resetCrcPayload());

        ModernButton btnTrace = new ModernButton("Calculate Bit Trace", ModernButton.Style.PRIMARY);
        btnTrace.addActionListener(e -> crcTraceArea.setText(Crc8.generateTrace(currentBytes)));

        btnRow.add(btnFlipBit0);
        btnRow.add(btnFlipRandom);
        btnRow.add(btnRestore);
        btnRow.add(btnTrace);

        controls.add(prompt);
        controls.add(Box.createVerticalStrut(4));
        controls.add(payloadField);
        controls.add(Box.createVerticalStrut(6));
        controls.add(statusRow);
        controls.add(Box.createVerticalStrut(6));
        controls.add(btnRow);

        // Trace Output
        crcTraceArea.setEditable(false);
        crcTraceArea.setFont(Theme.FONT_MONO);
        crcTraceArea.setBackground(Theme.BG_ELEVATED);
        crcTraceArea.setForeground(Theme.TEXT_PRIMARY);
        crcTraceArea.setRows(14);
        JScrollPane sp = new JScrollPane(crcTraceArea);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE), "Bitwise Execution Trace",
                0, 0, Theme.FONT_SMALL, Theme.TEXT_MUTED));

        card.add(header, BorderLayout.NORTH);
        card.add(controls, BorderLayout.CENTER);
        card.add(sp, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createGbnLabCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(8, 8), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("GO-BACK-N SLIDING WINDOW ARQ INSPECTOR");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.PURPLE_ANALYTICS);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setOpaque(false);

        JPanel paramGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        paramGrid.setOpaque(false);

        paramGrid.add(createLabeledComp("Frames to Transmit:", gbnFramesSpinner));
        paramGrid.add(createLabeledComp("Sliding Window Size (N):", gbnWindowSpinner));

        gbnErrorSlider.setOpaque(false);
        gbnErrorSlider.setMajorTickSpacing(10);
        gbnErrorSlider.setPaintTicks(true);
        gbnErrorSlider.setPaintLabels(true);
        gbnErrorSlider.setForeground(Theme.TEXT_MUTED);
        gbnErrorSlider.addChangeListener(e -> gbnErrorLabel.setText("Channel Error Rate: " + gbnErrorSlider.getValue() + "%"));

        paramGrid.add(createLabeledComp("Error Rate:", gbnErrorSlider));

        ModernButton btnRunGbn = new ModernButton("Simulate ARQ Transfer", ModernButton.Style.PURPLE);
        btnRunGbn.addActionListener(e -> executeGbnSimulation());
        paramGrid.add(createLabeledComp("Action:", btnRunGbn));

        controls.add(paramGrid);
        controls.add(Box.createVerticalStrut(8));

        gbnResultLabel.setFont(Theme.FONT_SECTION);
        gbnResultLabel.setForeground(Theme.TEXT_PRIMARY);
        controls.add(gbnResultLabel);

        // Trace Area
        gbnTraceArea.setEditable(false);
        gbnTraceArea.setFont(Theme.FONT_MONO);
        gbnTraceArea.setBackground(Theme.BG_ELEVATED);
        gbnTraceArea.setForeground(Theme.TEXT_PRIMARY);
        gbnTraceArea.setRows(14);
        JScrollPane sp = new JScrollPane(gbnTraceArea);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE), "Sliding Window Execution Trace",
                0, 0, Theme.FONT_SMALL, Theme.TEXT_MUTED));

        card.add(header, BorderLayout.NORTH);
        card.add(controls, BorderLayout.CENTER);
        card.add(sp, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createLabeledComp(String text, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(2, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_SECONDARY);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void resetCrcPayload() {
        originalBytes = payloadField.getText().getBytes(StandardCharsets.UTF_8);
        currentBytes = originalBytes.clone();
        originalCrc = Crc8.compute(originalBytes);
        updateCrcState();
    }

    private void mutateBit(int byteIdx, int bitIdx) {
        currentBytes = Crc8.flipBit(currentBytes, byteIdx, bitIdx);
        updateCrcState();
    }

    private void updateCrcState() {
        int currentCrc = Crc8.compute(currentBytes);
        boolean isValid = (currentCrc == originalCrc);

        crcDisplayLabel.setText(String.format("Expected: %s | Computed: %s",
                Crc8.toHex(originalCrc), Crc8.toHex(currentCrc)));

        if (isValid) {
            crcStatusBadge.setStatus("VALID CHECKSUM (PASS)", Theme.GREEN_OPERATIONAL);
        } else {
            crcStatusBadge.setStatus("CRC MISMATCH DETECTED (FAIL)", Theme.RED_FAILURE);
        }

        crcTraceArea.setText(String.format(
                "Payload Status: %s\nPayload ASCII: \"%s\"\nExpected CRC-8: %s | Recalculated CRC-8: %s\nResult: %s",
                isValid ? "Clean / Unmodified" : "CORRUPTED (Bit mutated)",
                new String(currentBytes, StandardCharsets.UTF_8),
                Crc8.toHex(originalCrc),
                Crc8.toHex(currentCrc),
                isValid ? "PASS - Frame accepted by emergency receiver." : "REJECTED - Frame flagged as corrupt by CRC-8 check."
        ));
    }

    private void executeGbnSimulation() {
        int frames = (Integer) gbnFramesSpinner.getValue();
        int win = (Integer) gbnWindowSpinner.getValue();
        double err = gbnErrorSlider.getValue() / 100.0;

        TransferResult res = GoBackNProtocol.simulate(frames, win, err, new Random(42));

        gbnResultLabel.setText(String.format(
                "Result: %s | Sent: %d frames | Retransmissions: %d | Corrupt/Lost: %d | Delivered: %d",
                res.delivered ? "SUCCESS" : "RETRY LIMIT REACHED",
                res.sentFrames, res.retransmissions, res.corruptedFrames + res.lostFrames, res.deliveredFrames
        ));
        gbnResultLabel.setForeground(res.delivered ? Theme.GREEN_OPERATIONAL : Theme.RED_FAILURE);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Go-Back-N ARQ Session (Window N=").append(win).append(", Error Rate=").append((int)(err*100)).append("%) ===\n");
        for (String line : res.getEventTrace()) {
            sb.append(line).append("\n");
        }
        sb.append("\nSummary: ").append(res.detail).append("\n");
        gbnTraceArea.setText(sb.toString());
        gbnTraceArea.setCaretPosition(0);
    }
}
