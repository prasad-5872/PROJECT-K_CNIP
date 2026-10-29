package emergency.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Rigorously computed performance metrics from a completed simulation run.
 * Contains no fabricated or hardcoded numbers; all fields derive strictly
 * from simulation telemetry.
 */
public final class SimulationMetrics {
    public int generated = 0;
    public int bucketAccepted = 0;
    public int bucketDropped = 0;
    public int unroutable = 0;
    public int delivered = 0;
    public int lostArq = 0;
    public int transmittedFrames = 0;
    public int corruptedFrames = 0;
    public int retransmissions = 0;
    public int peakQueue = 0;
    public int overflowEvents = 0;

    public double totalDelayMs = 0.0;
    public double averageDelayMs = 0.0;
    public double throughputMbps = 0.0;
    public double deliveryRatio = 0.0;
    public double dropRatio = 0.0;
    public double simulatedTimeSec = 0.0;

    public final List<Integer> queueTimeline = new ArrayList<>();
    public final List<Double> throughputTimeline = new ArrayList<>();
    public final List<Double> delayTimeline = new ArrayList<>();

    public void computeDerived(int payloadBytesPerFrame, int framesPerMsg) {
        // Safe delivery ratio (delivered / generated)
        this.deliveryRatio = (generated > 0) ? ((double) delivered / generated) : 0.0;
        int totalDropsAndLoss = (generated - delivered);
        this.dropRatio = (generated > 0) ? ((double) totalDropsAndLoss / generated) : 0.0;

        // Safe average delay (total delay of delivered messages / delivered count)
        this.averageDelayMs = (delivered > 0) ? (totalDelayMs / delivered) : 0.0;

        // Safe throughput: Delivered payload bits / simulated elapsed time in seconds
        // Each message contains framesPerMsg * payloadBytesPerFrame * 8 bits
        double totalDeliveredBits = (double) delivered * framesPerMsg * payloadBytesPerFrame * 8.0;
        double effectiveTimeSec = Math.max(0.001, totalDelayMs / 1000.0);
        this.simulatedTimeSec = effectiveTimeSec;
        this.throughputMbps = (delivered > 0 && effectiveTimeSec > 0)
                ? (totalDeliveredBits / (effectiveTimeSec * 1_000_000.0))
                : 0.0;
    }

    public List<Integer> getQueueTimeline() {
        return Collections.unmodifiableList(queueTimeline);
    }

    public List<Double> getThroughputTimeline() {
        return Collections.unmodifiableList(throughputTimeline);
    }

    public List<Double> getDelayTimeline() {
        return Collections.unmodifiableList(delayTimeline);
    }

    public String summary() {
        return String.format(Locale.US,
                "Generated: %d | Delivered: %d (%.1f%%) | Ingress Drops: %d | Unroutable: %d | ARQ Loss: %d | " +
                "Throughput: %.3f Mbps | Avg Delay: %.2f ms | Retransmissions: %d | Corrupt Frames: %d | Peak Queue: %d",
                generated, delivered, deliveryRatio * 100.0, bucketDropped, unroutable, lostArq,
                throughputMbps, averageDelayMs, retransmissions, corruptedFrames, peakQueue);
    }

    /**
     * Metric definitions and units dictionary for display in the UI and documentation.
     */
    public static final class MetricDefinition {
        public final String name;
        public final String unit;
        public final String formula;
        public final String description;

        public MetricDefinition(String name, String unit, String formula, String description) {
            this.name = name;
            this.unit = unit;
            this.formula = formula;
            this.description = description;
        }
    }

    public static List<MetricDefinition> getDefinitions() {
        return List.of(
                new MetricDefinition("Generated Messages", "messages", "M_total", "Total emergency dispatch messages offered to the system during the run."),
                new MetricDefinition("Delivered Messages", "messages", "M_delivered", "Total messages successfully received, CRC-verified, and acknowledged by destination."),
                new MetricDefinition("Delivery Ratio", "percentage (%)", "(M_delivered / M_total) * 100", "Proportion of generated messages that successfully arrived at the destination."),
                new MetricDefinition("Ingress Bucket Drops", "packets", "Packets rejected when Queue == Capacity", "Congestion drops discarded at the Leaky Bucket buffer when incoming burst exceeds capacity."),
                new MetricDefinition("Unroutable Messages", "messages", "Messages dropped when Path == INF", "Messages rejected because link failures severed all active paths between source and destination."),
                new MetricDefinition("ARQ Retransmissions", "frames", "Sum of GBN window retries", "Additional frames retransmitted by Go-Back-N following frame corruption or channel loss."),
                new MetricDefinition("Corrupted Frames", "frames", "Frames where CRC8(payload) != received_crc", "Frames detected with transmission errors using CRC-8 polynomial 0x07."),
                new MetricDefinition("Average Delay", "milliseconds (ms)", "Sum(Latency_i) / M_delivered", "Mean end-to-end latency per delivered message, including queueing, transmission, and ARQ timeouts."),
                new MetricDefinition("Throughput", "Megabits/sec (Mbps)", "(Delivered Bits) / (Elapsed Time * 10^6)", "Effective payload transfer rate over the duration of the communication session."),
                new MetricDefinition("Peak Queue Size", "packets", "max(Queue_t)", "Maximum buffer occupancy observed in the Leaky Bucket shaper across all simulation ticks.")
        );
    }
}
