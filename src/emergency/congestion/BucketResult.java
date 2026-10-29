package emergency.congestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the performance metrics and timeline telemetry of the Leaky Bucket traffic shaper.
 */
public final class BucketResult {
    public int accepted = 0;
    public int dropped = 0;
    public int peakQueue = 0;
    public int overflowEvents = 0;
    public int totalLeaked = 0;

    public final List<Integer> queueTimeline = new ArrayList<>();
    public final List<Integer> acceptedTimeline = new ArrayList<>();
    public final List<Integer> droppedTimeline = new ArrayList<>();
    public final List<Integer> leakedTimeline = new ArrayList<>();

    public List<Integer> getQueueTimeline() {
        return Collections.unmodifiableList(queueTimeline);
    }

    public double getDropRate() {
        int total = accepted + dropped;
        return total == 0 ? 0.0 : (double) dropped / total;
    }

    public double getUtilization(int capacity) {
        return capacity <= 0 ? 0.0 : (double) peakQueue / capacity;
    }

    @Override
    public String toString() {
        return String.format("LeakyBucket [Accepted: %d, Dropped: %d, PeakQueue: %d, Overflows: %d, Leaked: %d]",
                accepted, dropped, peakQueue, overflowEvents, totalLeaked);
    }
}
