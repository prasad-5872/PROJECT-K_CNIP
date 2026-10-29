package emergency.experiment;

import emergency.engine.SimulationConfig;
import emergency.engine.SimulationMetrics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An immutable record of a single completed simulation experiment.
 * Holds exact input parameters, empirical outputs, and timestamp.
 */
public final class ExperimentRecord {
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public final String runId;
    public final String name;
    public final String timestamp;
    public final SimulationConfig config;
    public final SimulationMetrics metrics;
    public final String notes;

    public ExperimentRecord(String runId, String name, SimulationConfig config, SimulationMetrics metrics, String notes) {
        this.runId = runId;
        this.name = (name == null || name.trim().isEmpty()) ? "Unnamed Experiment" : name.trim();
        this.timestamp = LocalDateTime.now().format(ISO_FMT);
        this.config = config;
        this.metrics = metrics;
        this.notes = notes != null ? notes : "";
    }

    public ExperimentRecord(String runId, String name, String timestamp, SimulationConfig config, SimulationMetrics metrics, String notes) {
        this.runId = runId;
        this.name = name;
        this.timestamp = timestamp;
        this.config = config;
        this.metrics = metrics;
        this.notes = notes != null ? notes : "";
    }
}
