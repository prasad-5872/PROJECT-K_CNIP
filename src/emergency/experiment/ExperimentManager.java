package emergency.experiment;

import emergency.engine.SimulationConfig;
import emergency.engine.SimulationMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Manages saved experiment runs, baseline scenario comparisons, and record history.
 */
public final class ExperimentManager {
    private final List<ExperimentRecord> records = new ArrayList<>();
    private int counter = 1;
    private ExperimentRecord baseline = null;

    public synchronized ExperimentRecord saveRun(String name, SimulationConfig config, SimulationMetrics metrics, String notes) {
        String runId = String.format("EXP-%03d", counter++);
        ExperimentRecord record = new ExperimentRecord(runId, name, config, metrics, notes);
        records.add(record);
        if (baseline == null) {
            baseline = record;
        }
        return record;
    }

    public synchronized List<ExperimentRecord> getRecords() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized ExperimentRecord getBaseline() {
        return baseline;
    }

    public synchronized void setBaseline(ExperimentRecord record) {
        this.baseline = record;
    }

    public synchronized void removeRecord(int index) {
        if (index >= 0 && index < records.size()) {
            ExperimentRecord removed = records.remove(index);
            if (baseline == removed) {
                baseline = records.isEmpty() ? null : records.get(0);
            }
        }
    }

    public synchronized void clear() {
        records.clear();
        baseline = null;
        counter = 1;
    }

    /**
     * Compares an experiment record against the baseline and produces a textual delta.
     */
    public static String compareWithBaseline(ExperimentRecord record, ExperimentRecord baseline) {
        if (record == null) return "No experiment selected.";
        if (baseline == null) return "No baseline designated for comparison.";
        if (record == baseline) return "This record is currently designated as the baseline.";

        double delivDelta = (record.metrics.deliveryRatio - baseline.metrics.deliveryRatio) * 100.0;
        double delayDelta = record.metrics.averageDelayMs - baseline.metrics.averageDelayMs;
        double tputDelta = record.metrics.throughputMbps - baseline.metrics.throughputMbps;
        int dropDelta = record.metrics.bucketDropped - baseline.metrics.bucketDropped;
        int retransDelta = record.metrics.retransmissions - baseline.metrics.retransmissions;

        return String.format(Locale.US,
                "Comparison: %s vs Baseline (%s)\n" +
                "  Delivery Ratio:  %.1f%%  (Delta: %+.1f%%)\n" +
                "  Average Delay:   %.2f ms (Delta: %+.2f ms)\n" +
                "  Throughput:      %.3f Mbps (Delta: %+.3f Mbps)\n" +
                "  Bucket Drops:    %d      (Delta: %+d)\n" +
                "  Retransmissions: %d      (Delta: %+d)\n" +
                "  Input Changes:   FER: %.0f%% -> %.0f%% | Arr: %d -> %d | Cap: %d -> %d",
                record.runId, baseline.runId,
                record.metrics.deliveryRatio * 100.0, delivDelta,
                record.metrics.averageDelayMs, delayDelta,
                record.metrics.throughputMbps, tputDelta,
                record.metrics.bucketDropped, dropDelta,
                record.metrics.retransmissions, retransDelta,
                baseline.config.errorRate * 100.0, record.config.errorRate * 100.0,
                baseline.config.arrivalsPerTick, record.config.arrivalsPerTick,
                baseline.config.bucketCapacity, record.config.bucketCapacity
        );
    }
}
