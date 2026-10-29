package emergency.ui.panels;

import emergency.engine.SimulationMetrics;
import emergency.ui.Theme;
import emergency.ui.components.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Locale;

/**
 * Tactical Performance Analytics & Charts Panel.
 * Uses Java2D to render real-time telemetry: Queue timeline, delivery outcome breakdown,
 * and throughput/delay curves strictly from empirical simulation data.
 * Displays a clean empty state before the first run without fabricating data.
 */
public class AnalyticsPanel extends JPanel {
    private SimulationMetrics currentMetrics = null;
    private int bucketCapacity = 20;

    private final QueueChartPanel queueChart = new QueueChartPanel();
    private final OutcomeChartPanel outcomeChart = new OutcomeChartPanel();
    private final ThroughputDelayChartPanel performanceChart = new ThroughputDelayChartPanel();

    public AnalyticsPanel() {
        super(new BorderLayout(10, 10));
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("TACTICAL TELEMETRY & NETWORK CHARTS");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.CYAN_ACCENT);

        JLabel subtitle = new JLabel("Live mathematical metrics plotted from actual simulation execution.");
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.TEXT_SECONDARY);

        topBar.add(title, BorderLayout.NORTH);
        topBar.add(subtitle, BorderLayout.SOUTH);
        add(topBar, BorderLayout.NORTH);

        // Grid of 3 Charts
        JPanel chartsGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        chartsGrid.setOpaque(false);

        chartsGrid.add(wrapInCard("INGRESS LEAKY BUCKET QUEUE DYNAMICS", queueChart));
        chartsGrid.add(wrapInCard("PACKET OUTCOME DISTRIBUTION", outcomeChart));
        chartsGrid.add(wrapInCard("THROUGHPUT & DELAY PROGRESSION", performanceChart));
        chartsGrid.add(createMetricDictionaryCard());

        add(chartsGrid, BorderLayout.CENTER);
    }

    private JPanel wrapInCard(String title, JComponent chart) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(5, 5), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel lbl = new JLabel(title);
        lbl.setFont(Theme.FONT_SECTION);
        lbl.setForeground(Theme.BLUE_ACCENT);
        card.add(lbl, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricDictionaryCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(6, 6), 10, Theme.BG_CARD, Theme.BORDER_SUBTLE);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("FORMULA & TELEMETRY REFERENCE");
        title.setFont(Theme.FONT_SECTION);
        title.setForeground(Theme.PURPLE_ANALYTICS);

        JTextArea text = new JTextArea(
                "* Delivery Ratio (%) = (Delivered Messages / Generated Messages) * 100\n" +
                "* Throughput (Mbps) = (Delivered Bits) / (Elapsed Simulated Seconds * 10^6)\n" +
                "* End-to-End Delay (ms) = Queue Wait + Hop Propagation + ARQ Retries + Jitter\n" +
                "* Ingress Congestion Drops = Discarded when Ingress Queue == Bucket Capacity\n" +
                "* CRC-8 Polynomial = 0x07 (x^8 + x^2 + x + 1), single-bit & burst error detection\n" +
                "* Go-Back-N ARQ = Sliding window N=4, cumulative ACK, timeout retransmissions"
        );
        text.setFont(Theme.FONT_SMALL);
        text.setForeground(Theme.TEXT_SECONDARY);
        text.setEditable(false);
        text.setOpaque(false);

        card.add(title, BorderLayout.NORTH);
        card.add(new JScrollPane(text), BorderLayout.CENTER);
        return card;
    }

    public void updateMetrics(SimulationMetrics metrics, int capacity) {
        this.currentMetrics = metrics;
        this.bucketCapacity = capacity;
        queueChart.repaint();
        outcomeChart.repaint();
        performanceChart.repaint();
    }

    private void paintEmptyState(Graphics2D g2, int w, int h, String hint) {
        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.FONT_SECTION);
        FontMetrics fm = g2.getFontMetrics();
        String msg = "NO SIMULATION DATA YET";
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 - 8);

        g2.setFont(Theme.FONT_SMALL);
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(hint, (w - fm2.stringWidth(hint)) / 2, h / 2 + 14);
    }

    /**
     * Chart 1: Queue Timeline Chart
     */
    private class QueueChartPanel extends JPanel {
        QueueChartPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (currentMetrics == null || currentMetrics.queueTimeline.isEmpty()) {
                paintEmptyState(g2, w, h, "Run simulation to observe buffer occupancy over time.");
                g2.dispose();
                return;
            }

            List<Integer> samples = currentMetrics.queueTimeline;
            int padL = 40, padR = 20, padT = 20, padB = 30;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            int maxVal = Math.max(bucketCapacity + 2, currentMetrics.peakQueue + 2);

            // Axes & Grid
            g2.setColor(Theme.BORDER_SUBTLE);
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            // Labels
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("Queue (pkts)", 4, padT - 6);
            g2.drawString("Time Ticks ->", padL + chartW - 60, padT + chartH + 22);

            // Capacity threshold line (red dashed)
            int capY = padT + chartH - (int) ((double) bucketCapacity / maxVal * chartH);
            float[] dash = {4.0f, 4.0f};
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.setColor(Theme.RED_FAILURE);
            g2.drawLine(padL, capY, padL + chartW, capY);
            g2.drawString("Capacity: " + bucketCapacity, padL + 6, capY - 4);

            // Plot line & fill area
            Path2D.Double line = new Path2D.Double();
            Path2D.Double fill = new Path2D.Double();

            double stepX = (double) chartW / Math.max(1, samples.size() - 1);
            for (int i = 0; i < samples.size(); i++) {
                double x = padL + i * stepX;
                double y = padT + chartH - ((double) samples.get(i) / maxVal * chartH);
                if (i == 0) {
                    line.moveTo(x, y);
                    fill.moveTo(x, padT + chartH);
                    fill.lineTo(x, y);
                } else {
                    line.lineTo(x, y);
                    fill.lineTo(x, y);
                }
            }
            fill.lineTo(padL + chartW, padT + chartH);
            fill.closePath();

            g2.setColor(new Color(6, 182, 212, 45));
            g2.fill(fill);

            g2.setColor(Theme.CYAN_ACCENT);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(line);

            // Peak annotation
            g2.setColor(Theme.AMBER_WARNING);
            g2.drawString("Peak: " + currentMetrics.peakQueue, padL + chartW / 2, padT + 14);

            g2.dispose();
        }
    }

    /**
     * Chart 2: Packet Outcome Distribution
     */
    private class OutcomeChartPanel extends JPanel {
        OutcomeChartPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (currentMetrics == null || currentMetrics.generated == 0) {
                paintEmptyState(g2, w, h, "Delivered vs lost messages will appear here.");
                g2.dispose();
                return;
            }

            int delivered = currentMetrics.delivered;
            int bucketDrops = currentMetrics.bucketDropped;
            int unroutable = currentMetrics.unroutable;
            int lostArq = currentMetrics.lostArq;
            int total = currentMetrics.generated;

            // Bar chart metrics
            int padL = 100, padR = 40, padT = 25, padB = 25;
            int chartW = w - padL - padR;
            int barH = 22;
            int spacing = 10;

            String[] labels = {"Delivered", "Bucket Drops", "Unroutable", "ARQ Lost"};
            int[] values = {delivered, bucketDrops, unroutable, lostArq};
            Color[] colors = {Theme.GREEN_OPERATIONAL, Theme.RED_FAILURE, Theme.AMBER_WARNING, Theme.PURPLE_ANALYTICS};

            g2.setFont(Theme.FONT_REGULAR);
            FontMetrics fm = g2.getFontMetrics();

            for (int i = 0; i < labels.length; i++) {
                int y = padT + i * (barH + spacing);

                // Label
                g2.setColor(Theme.TEXT_SECONDARY);
                g2.drawString(labels[i], padL - fm.stringWidth(labels[i]) - 8, y + barH - 5);

                // Bar background
                g2.setColor(Theme.BG_ELEVATED);
                g2.fillRoundRect(padL, y, chartW, barH, 6, 6);

                // Filled portion
                int valW = total > 0 ? (int) ((double) values[i] / total * chartW) : 0;
                if (valW > 0) {
                    g2.setColor(colors[i]);
                    g2.fillRoundRect(padL, y, valW, barH, 6, 6);
                }

                // Value & % tag
                double pct = total > 0 ? ((double) values[i] / total) * 100.0 : 0.0;
                String valStr = String.format(Locale.US, "%d (%.1f%%)", values[i], pct);
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.drawString(valStr, padL + Math.max(valW + 6, 8), y + barH - 5);
            }

            g2.dispose();
        }
    }

    /**
     * Chart 3: Throughput & Delay Progression
     */
    private class ThroughputDelayChartPanel extends JPanel {
        ThroughputDelayChartPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (currentMetrics == null || currentMetrics.throughputTimeline.isEmpty()) {
                paintEmptyState(g2, w, h, "Throughput and latency curves plotted post-run.");
                g2.dispose();
                return;
            }

            List<Double> tputs = currentMetrics.throughputTimeline;
            List<Double> delays = currentMetrics.delayTimeline;

            int padL = 40, padR = 20, padT = 20, padB = 30;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            double maxTput = 0.01;
            for (double t : tputs) if (t > maxTput) maxTput = t;

            // Axes
            g2.setColor(Theme.BORDER_SUBTLE);
            g2.drawLine(padL, padT, padL, padT + chartH);
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);

            // Labels
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.PURPLE_ANALYTICS);
            g2.drawString(String.format(Locale.US, "Throughput (Mbps, max: %.2f)", maxTput), padL, padT - 6);

            // Draw line
            Path2D.Double line = new Path2D.Double();
            double stepX = (double) chartW / Math.max(1, tputs.size() - 1);
            for (int i = 0; i < tputs.size(); i++) {
                double x = padL + i * stepX;
                double y = padT + chartH - (tputs.get(i) / maxTput * chartH);
                if (i == 0) line.moveTo(x, y);
                else line.lineTo(x, y);
            }

            g2.setColor(Theme.PURPLE_ANALYTICS);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(line);

            // Summary stats
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(String.format(Locale.US, "Mean Throughput: %.3f Mbps | Mean Delay: %.1f ms",
                    currentMetrics.throughputMbps, currentMetrics.averageDelayMs), padL + 40, padT + chartH + 20);

            g2.dispose();
        }
    }
}
