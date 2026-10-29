package emergency.congestion;

import java.util.Objects;

/**
 * Leaky Bucket congestion control and traffic shaping mechanism.
 *
 * Incoming packets arrive into a finite buffer of size 'capacity'.
 * Any burst that exceeds the remaining capacity is dropped at the ingress buffer.
 * Packets are leaked/serviced into the transmission queue at a constant 'leakRate' per tick.
 */
public final class LeakyBucket {
    private final int capacity;
    private final int leakRate;
    private int queue = 0;
    private int peakQueue = 0;
    private int overflowEvents = 0;
    private int totalAccepted = 0;
    private int totalDropped = 0;
    private int totalLeaked = 0;

    public LeakyBucket(int capacity, int leakRate) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be >= 1, given: " + capacity);
        if (leakRate < 1) throw new IllegalArgumentException("Leak rate must be >= 1, given: " + leakRate);
        this.capacity = capacity;
        this.leakRate = leakRate;
    }

    public static final class StepResult {
        public final int arrivals;
        public final int accepted;
        public final int dropped;
        public final int leaked;
        public final int queueAfterAdmission;
        public final int queueAfterLeak;

        public StepResult(int arrivals, int accepted, int dropped, int leaked, int queueAfterAdmission, int queueAfterLeak) {
            this.arrivals = arrivals;
            this.accepted = accepted;
            this.dropped = dropped;
            this.leaked = leaked;
            this.queueAfterAdmission = queueAfterAdmission;
            this.queueAfterLeak = queueAfterLeak;
        }
    }

    /**
     * Executes a single discrete time tick with the specified number of arriving packets.
     */
    public synchronized StepResult tick(int arrivals) {
        if (arrivals < 0) arrivals = 0;

        int spaceAvailable = Math.max(0, capacity - queue);
        int accepted = Math.min(arrivals, spaceAvailable);
        int dropped = arrivals - accepted;

        if (dropped > 0) {
            overflowEvents++;
        }

        queue += accepted;
        if (queue > peakQueue) {
            peakQueue = queue;
        }

        int queueAfterAdmission = queue;

        // Drain / leak packets at constant rate
        int leaked = Math.min(queue, leakRate);
        queue -= leaked;

        totalAccepted += accepted;
        totalDropped += dropped;
        totalLeaked += leaked;

        return new StepResult(arrivals, accepted, dropped, leaked, queueAfterAdmission, queue);
    }

    public synchronized void reset() {
        queue = 0;
        peakQueue = 0;
        overflowEvents = 0;
        totalAccepted = 0;
        totalDropped = 0;
        totalLeaked = 0;
    }

    public int getCapacity() { return capacity; }
    public int getLeakRate() { return leakRate; }
    public synchronized int getCurrentQueue() { return queue; }
    public synchronized int getPeakQueue() { return peakQueue; }
    public synchronized int getOverflowEvents() { return overflowEvents; }
    public synchronized int getTotalAccepted() { return totalAccepted; }
    public synchronized int getTotalDropped() { return totalDropped; }
    public synchronized int getTotalLeaked() { return totalLeaked; }

    /**
     * Batch simulation utility running an array of arrivals per tick.
     */
    public static BucketResult simulate(int[] arrivalsPerTick, int capacity, int leakRate) {
        Objects.requireNonNull(arrivalsPerTick, "Arrivals array cannot be null");
        LeakyBucket bucket = new LeakyBucket(capacity, leakRate);
        BucketResult result = new BucketResult();

        for (int arr : arrivalsPerTick) {
            StepResult step = bucket.tick(arr);
            result.accepted += step.accepted;
            result.dropped += step.dropped;
            result.totalLeaked += step.leaked;
            if (step.dropped > 0) {
                result.overflowEvents++;
            }
            result.peakQueue = Math.max(result.peakQueue, step.queueAfterAdmission);

            result.queueTimeline.add(step.queueAfterLeak);
            result.acceptedTimeline.add(step.accepted);
            result.droppedTimeline.add(step.dropped);
            result.leakedTimeline.add(step.leaked);
        }

        return result;
    }
}
