package emergency.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Results and execution trace of a Go-Back-N ARQ transmission session.
 */
public final class TransferResult {
    public int sentFrames = 0;
    public int retransmissions = 0;
    public int corruptedFrames = 0;
    public int lostFrames = 0;
    public int deliveredFrames = 0;
    public boolean delivered = false;
    public int totalRounds = 0;
    public String detail = "";
    private final List<String> eventTrace = new ArrayList<>();

    public void addTrace(String event) {
        eventTrace.add(event);
    }

    public List<String> getEventTrace() {
        return Collections.unmodifiableList(eventTrace);
    }

    @Override
    public String toString() {
        return String.format("GBN Result: %s | Sent: %d, Retrans: %d, Corrupt: %d, Delivered: %d",
                delivered ? "SUCCESS" : "FAILED", sentFrames, retransmissions, corruptedFrames, deliveredFrames);
    }
}
