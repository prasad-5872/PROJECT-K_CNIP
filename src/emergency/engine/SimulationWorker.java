package emergency.engine;

import emergency.congestion.LeakyBucket;
import emergency.model.NetworkModel;
import emergency.protocol.GoBackNProtocol;
import emergency.protocol.TransferResult;
import emergency.routing.DijkstraRouter;
import emergency.routing.DistanceVectorRouter;
import emergency.routing.Route;

import javax.swing.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background worker thread (SwingWorker) that executes the simulation without
 * blocking the Swing Event Dispatch Thread (EDT).
 * Supports real-time pause, resume, cancellation, and progress updates.
 */
public final class SimulationWorker extends SwingWorker<SimulationMetrics, SimulationWorker.ProgressUpdate> {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static final class ProgressUpdate {
        public final int processed;
        public final int total;
        public final int delivered;
        public final int currentQueue;
        public final String status;
        public final String logMessage;
        public final String logLevel;

        public ProgressUpdate(int processed, int total, int delivered, int currentQueue, String status, String logMessage, String logLevel) {
            this.processed = processed;
            this.total = total;
            this.delivered = delivered;
            this.currentQueue = currentQueue;
            this.status = status;
            this.logMessage = logMessage;
            this.logLevel = logLevel;
        }
    }

    private final NetworkModel network;
    private final SimulationConfig config;
    private final SimulationListener listener;

    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Object pauseLock = new Object();

    public SimulationWorker(NetworkModel network, SimulationConfig config, SimulationListener listener) {
        this.network = network;
        this.config = config;
        this.listener = listener;
    }

    public void pause() {
        paused.set(true);
        if (listener != null) listener.onStateChanged(SimulationListener.State.PAUSED);
    }

    public void resumeSimulation() {
        synchronized (pauseLock) {
            paused.set(false);
            pauseLock.notifyAll();
        }
        if (listener != null) listener.onStateChanged(SimulationListener.State.RUNNING);
    }

    public boolean isPaused() {
        return paused.get();
    }

    public void stopSimulation() {
        cancelled.set(true);
        resumeSimulation();
        cancel(true);
        if (listener != null) listener.onStateChanged(SimulationListener.State.CANCELLED);
    }

    @Override
    protected SimulationMetrics doInBackground() throws Exception {
        if (listener != null) listener.onStateChanged(SimulationListener.State.RUNNING);

        SimulationMetrics metrics = new SimulationMetrics();
        metrics.generated = config.messageCount;

        Random random = (config.randomSeed != null) ? new Random(config.randomSeed) : new Random();
        LeakyBucket bucket = new LeakyBucket(config.bucketCapacity, config.leakRate);

        publishLog("Simulation started: " + config.messageCount + " messages from " +
                config.sourceNode + " to " + config.destinationNode + " via " + config.algorithm +
                (config.randomSeed != null ? " [Seed: " + config.randomSeed + "]" : " [Random Seed]"), "INFO");

        Route route = "Distance Vector".equalsIgnoreCase(config.algorithm)
                ? DistanceVectorRouter.findRoute(network, config.sourceNode, config.destinationNode)
                : DijkstraRouter.findRoute(network, config.sourceNode, config.destinationNode);

        if (route.reachable) {
            publishLog("Active Route selected: " + route.toString(), "INFO");
        } else {
            publishLog("WARNING: Destination " + config.destinationNode + " is UNREACHABLE! " + route.explanation, "WARN");
        }

        int remaining = config.messageCount;
        int admittedMessages = 0;
        int tick = 0;

        while ((remaining > 0 || bucket.getCurrentQueue() > 0) && !cancelled.get()) {
            // Handle pause state
            synchronized (pauseLock) {
                while (paused.get() && !cancelled.get()) {
                    pauseLock.wait();
                }
            }
            if (cancelled.get()) break;

            tick++;
            int arrivalsThisTick = Math.min(remaining, config.arrivalsPerTick);
            remaining -= arrivalsThisTick;

            LeakyBucket.StepResult step = bucket.tick(arrivalsThisTick);
            metrics.bucketAccepted += step.accepted;
            metrics.bucketDropped += step.dropped;
            metrics.peakQueue = Math.max(metrics.peakQueue, step.queueAfterAdmission);

            if (step.dropped > 0) {
                metrics.overflowEvents++;
                publishLog("Tick " + tick + ": Ingress Buffer Overflow! Dropped " + step.dropped +
                        " packets. Queue at capacity (" + config.bucketCapacity + ").", "WARN");
            }

            metrics.queueTimeline.add(step.queueAfterLeak);

            int msgsToTransmit = step.leaked;
            for (int m = 0; m < msgsToTransmit; m++) {
                if (cancelled.get()) break;
                if (admittedMessages >= metrics.bucketAccepted) break;
                admittedMessages++;

                if (!route.reachable) {
                    metrics.unroutable++;
                    publish(new ProgressUpdate(admittedMessages, config.messageCount, metrics.delivered,
                            step.queueAfterLeak, "Dropping packet: No route available", null, null));
                    continue;
                }

                TransferResult transfer = GoBackNProtocol.simulate(
                        config.framesPerMessage, config.windowSize, config.errorRate, random);

                metrics.transmittedFrames += transfer.sentFrames;
                metrics.retransmissions += transfer.retransmissions;
                metrics.corruptedFrames += transfer.corruptedFrames;

                if (transfer.delivered) {
                    metrics.delivered++;
                    double queueDelay = ((double) step.queueAfterAdmission / Math.max(1, config.leakRate)) * 12.0;
                    double pathDelay = route.cost * 8.5;
                    double arqPenalty = transfer.retransmissions * 18.0;
                    double jitter = (random.nextDouble() - 0.5) * 6.0;
                    double msgDelay = Math.max(5.0, queueDelay + pathDelay + arqPenalty + jitter);

                    metrics.totalDelayMs += msgDelay;
                    metrics.delayTimeline.add(msgDelay);

                    double sampleThroughput = (config.framesPerMessage * 1024.0 * 8.0) / (msgDelay * 1000.0);
                    metrics.throughputTimeline.add(sampleThroughput);

                    if (transfer.retransmissions > 0) {
                        publishLog("Msg #" + admittedMessages + ": Delivered after " +
                                transfer.retransmissions + " ARQ retransmission(s). Delay: " +
                                String.format("%.1f", msgDelay) + "ms", "INFO");
                    }
                } else {
                    metrics.lostArq++;
                    publishLog("Msg #" + admittedMessages + ": Go-Back-N ARQ failed after max retries (" +
                            transfer.corruptedFrames + " corrupt frames). Message lost.", "ERROR");
                }

                publish(new ProgressUpdate(admittedMessages, config.messageCount, metrics.delivered,
                        step.queueAfterLeak, "Transmitting message " + admittedMessages + "/" + config.messageCount, null, null));
            }

            // Animation delay if enabled
            if (config.tickDelayMs > 0) {
                Thread.sleep(config.tickDelayMs);
            }
        }

        metrics.computeDerived(1024, config.framesPerMessage);
        return metrics;
    }

    private void publishLog(String msg, String level) {
        publish(new ProgressUpdate(0, config.messageCount, 0, 0, null, msg, level));
    }

    @Override
    protected void process(List<ProgressUpdate> chunks) {
        if (chunks.isEmpty() || listener == null) return;
        ProgressUpdate latest = chunks.get(chunks.size() - 1);

        for (ProgressUpdate chunk : chunks) {
            if (chunk.logMessage != null) {
                listener.onLog(LocalTime.now().format(TIME_FMT), chunk.logMessage, chunk.logLevel);
            }
        }

        if (latest.status != null) {
            double pct = config.messageCount > 0 ? ((double) latest.processed / config.messageCount) * 100.0 : 0.0;
            listener.onProgress(latest.processed, latest.total, pct, latest.status);
        }
    }

    @Override
    protected void done() {
        if (cancelled.get()) {
            if (listener != null) listener.onStateChanged(SimulationListener.State.CANCELLED);
            return;
        }
        try {
            SimulationMetrics result = get();
            if (listener != null) {
                listener.onLog(LocalTime.now().format(TIME_FMT), "Simulation Completed: " + result.summary(), "INFO");
                listener.onStateChanged(SimulationListener.State.COMPLETED);
                listener.onCompleted(result);
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onLog(LocalTime.now().format(TIME_FMT), "Simulation Error: " + e.getMessage(), "ERROR");
                listener.onStateChanged(SimulationListener.State.ERROR);
            }
        }
    }
}
