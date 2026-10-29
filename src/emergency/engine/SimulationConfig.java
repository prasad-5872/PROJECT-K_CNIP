package emergency.engine;

import java.util.Objects;

/**
 * Immutable configuration parameters for the emergency network simulation.
 */
public final class SimulationConfig {
    public final int messageCount;
    public final double errorRate;
    public final int arrivalsPerTick;
    public final int bucketCapacity;
    public final int leakRate;
    public final String sourceNode;
    public final String destinationNode;
    public final String algorithm; // "Dijkstra" or "Distance Vector"
    public final Long randomSeed;  // null = non-deterministic
    public final int tickDelayMs;   // animation speed, 0 = fast batch
    public final int framesPerMessage;
    public final int windowSize;

    public SimulationConfig(int messageCount, double errorRate, int arrivalsPerTick,
                            int bucketCapacity, int leakRate, String sourceNode,
                            String destinationNode, String algorithm, Long randomSeed,
                            int tickDelayMs, int framesPerMessage, int windowSize) {
        validate(messageCount, errorRate, arrivalsPerTick, bucketCapacity, leakRate);
        this.messageCount = messageCount;
        this.errorRate = errorRate;
        this.arrivalsPerTick = arrivalsPerTick;
        this.bucketCapacity = bucketCapacity;
        this.leakRate = leakRate;
        this.sourceNode = Objects.requireNonNullElse(sourceNode, "Control Center");
        this.destinationNode = Objects.requireNonNullElse(destinationNode, "Shelter B");
        this.algorithm = (algorithm != null && algorithm.equalsIgnoreCase("Distance Vector")) ? "Distance Vector" : "Dijkstra";
        this.randomSeed = randomSeed;
        this.tickDelayMs = Math.max(0, tickDelayMs);
        this.framesPerMessage = Math.max(1, framesPerMessage);
        this.windowSize = Math.max(1, windowSize);
    }

    public static void validate(int messageCount, double errorRate, int arrivalsPerTick, int bucketCapacity, int leakRate) {
        if (messageCount < 1 || messageCount > 50000) {
            throw new IllegalArgumentException("Message count must be between 1 and 50,000 (provided: " + messageCount + ")");
        }
        if (errorRate < 0.0 || errorRate > 1.0) {
            throw new IllegalArgumentException("Frame error rate must be between 0.0 (0%) and 1.0 (100%) (provided: " + errorRate + ")");
        }
        if (arrivalsPerTick < 0 || arrivalsPerTick > 1000) {
            throw new IllegalArgumentException("Arrival rate must be between 0 and 1,000 pkts/tick (provided: " + arrivalsPerTick + ")");
        }
        if (bucketCapacity < 1 || bucketCapacity > 10000) {
            throw new IllegalArgumentException("Leaky bucket capacity must be between 1 and 10,000 pkts (provided: " + bucketCapacity + ")");
        }
        if (leakRate < 1 || leakRate > 10000) {
            throw new IllegalArgumentException("Leak rate must be between 1 and 10,000 pkts/tick (provided: " + leakRate + ")");
        }
    }

    public static SimulationConfig defaultBaseline() {
        return new SimulationConfig(100, 0.10, 8, 20, 5, "Control Center", "Shelter B", "Dijkstra", 42L, 10, 5, 4);
    }

    public SimulationConfig withSeed(Long seed) {
        return new SimulationConfig(messageCount, errorRate, arrivalsPerTick, bucketCapacity, leakRate,
                sourceNode, destinationNode, algorithm, seed, tickDelayMs, framesPerMessage, windowSize);
    }

    public SimulationConfig withAlgorithm(String newAlgo) {
        return new SimulationConfig(messageCount, errorRate, arrivalsPerTick, bucketCapacity, leakRate,
                sourceNode, destinationNode, newAlgo, randomSeed, tickDelayMs, framesPerMessage, windowSize);
    }
}
