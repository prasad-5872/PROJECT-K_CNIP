package emergency.engine;

import emergency.congestion.LeakyBucket;
import emergency.model.NetworkModel;
import emergency.protocol.GoBackNProtocol;
import emergency.protocol.TransferResult;
import emergency.routing.DijkstraRouter;
import emergency.routing.DistanceVectorRouter;
import emergency.routing.Route;

import java.util.Random;

/**
 * Executes the integrated emergency communication simulation.
 * Connects traffic generation, Leaky Bucket ingress queueing,
 * dynamic shortest-path/distance-vector routing, and Go-Back-N ARQ
 * frame transmission over active network links.
 */
public final class SimulationEngine {
    private final NetworkModel network;

    public SimulationEngine(NetworkModel network) {
        this.network = network;
    }

    /**
     * Executes synchronous end-to-end simulation according to the given configuration.
     */
    public SimulationMetrics run(SimulationConfig config) {
        if (config == null) config = SimulationConfig.defaultBaseline();

        SimulationMetrics metrics = new SimulationMetrics();
        metrics.generated = config.messageCount;

        Random random = (config.randomSeed != null) ? new Random(config.randomSeed) : new Random();
        LeakyBucket bucket = new LeakyBucket(config.bucketCapacity, config.leakRate);

        // 1. Resolve active route from Source to Destination
        Route route = "Distance Vector".equalsIgnoreCase(config.algorithm)
                ? DistanceVectorRouter.findRoute(network, config.sourceNode, config.destinationNode)
                : DijkstraRouter.findRoute(network, config.sourceNode, config.destinationNode);

        // 2. Traffic Generation & Leaky Bucket Ingress Shaper
        // Generate message arrivals across discrete time ticks
        int remainingMessages = config.messageCount;
        int tick = 0;
        int admittedMessages = 0;

        while (remainingMessages > 0 || bucket.getCurrentQueue() > 0) {
            tick++;
            int arrivalsThisTick = Math.min(remainingMessages, config.arrivalsPerTick);
            remainingMessages -= arrivalsThisTick;

            LeakyBucket.StepResult step = bucket.tick(arrivalsThisTick);
            metrics.bucketAccepted += step.accepted;
            metrics.bucketDropped += step.dropped;
            metrics.peakQueue = Math.max(metrics.peakQueue, step.queueAfterAdmission);
            if (step.dropped > 0) {
                metrics.overflowEvents++;
            }

            metrics.queueTimeline.add(step.queueAfterLeak);

            // 3. Process leaked/conforming messages through routing & transport
            int msgsToTransmit = step.leaked;
            for (int m = 0; m < msgsToTransmit; m++) {
                if (admittedMessages >= metrics.bucketAccepted) break;
                admittedMessages++;

                if (!route.reachable) {
                    // Partition drop: route unavailable due to severed links
                    metrics.unroutable++;
                    continue;
                }

                // 4. Multi-hop transport with Go-Back-N ARQ and CRC-8 frame verification
                TransferResult transfer = GoBackNProtocol.simulate(
                        config.framesPerMessage,
                        config.windowSize,
                        config.errorRate,
                        random
                );

                metrics.transmittedFrames += transfer.sentFrames;
                metrics.retransmissions += transfer.retransmissions;
                metrics.corruptedFrames += transfer.corruptedFrames;

                if (transfer.delivered) {
                    metrics.delivered++;
                    // Calculate per-message end-to-end delay:
                    // Ingress queuing delay + hop propagation delay + ARQ retry penalty + physical jitter
                    double queueDelay = ((double) step.queueAfterAdmission / Math.max(1, config.leakRate)) * 12.0;
                    double pathDelay = route.cost * 8.5;
                    double arqPenalty = transfer.retransmissions * 18.0;
                    double jitter = (random.nextDouble() - 0.5) * 6.0;
                    double msgDelay = Math.max(5.0, queueDelay + pathDelay + arqPenalty + jitter);

                    metrics.totalDelayMs += msgDelay;
                    metrics.delayTimeline.add(msgDelay);

                    // Running throughput sample in Mbps (1024 bytes payload per frame)
                    double sampleThroughput = (config.framesPerMessage * 1024.0 * 8.0) / (msgDelay * 1000.0);
                    metrics.throughputTimeline.add(sampleThroughput);
                } else {
                    metrics.lostArq++;
                }
            }

            if (remainingMessages == 0 && bucket.getCurrentQueue() == 0) {
                break;
            }
            if (tick > 50000) break; // Safety guard
        }

        // Account for any remaining undelivered messages that were dropped at bucket
        // (already accounted in bucketDropped)

        // 5. Finalize metrics
        metrics.computeDerived(1024, config.framesPerMessage);
        return metrics;
    }
}
