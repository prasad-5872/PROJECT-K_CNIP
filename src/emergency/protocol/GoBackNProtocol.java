package emergency.protocol;

import java.util.Random;

/**
 * Go-Back-N Sliding Window ARQ protocol simulator.
 *
 * Implements sliding window transmission where the sender can transmit up to N
 * unacknowledged frames. If a frame in the window is corrupted (failing CRC-8) or
 * lost due to channel error, the receiver discards it, a timeout occurs at the sender,
 * and the sender retransmits all frames starting from the oldest unacknowledged frame (base).
 */
public final class GoBackNProtocol {
    private GoBackNProtocol() {}

    /**
     * Simulates transfer of frameCount frames with windowSize N and channel error rate.
     */
    public static TransferResult simulate(int frameCount, int windowSize, double errorRate, Random random) {
        if (random == null) random = new Random();
        TransferResult result = new TransferResult();

        if (frameCount <= 0) {
            result.delivered = true;
            result.detail = "Zero frames requested; no-op.";
            return result;
        }

        int base = 0;
        int attempts = 0;
        int maxAttempts = Math.max(30, frameCount * 12);
        int round = 0;

        while (base < frameCount && attempts < maxAttempts) {
            round++;
            result.totalRounds = round;
            int windowEnd = Math.min(base + windowSize, frameCount);

            result.addTrace(String.format("Round %d: Window [%d..%d], Base=%d", round, base, windowEnd - 1, base));

            int firstFailure = -1;
            for (int i = base; i < windowEnd; i++) {
                result.sentFrames++;
                attempts++;

                double roll = random.nextDouble();
                if (roll < errorRate) {
                    // Frame corrupted or lost
                    if (roll < errorRate * 0.7) {
                        result.corruptedFrames++;
                        result.addTrace(String.format("  Frame #%d transmitted -> CRC-8 MISMATCH (Corrupted)", i));
                    } else {
                        result.lostFrames++;
                        result.addTrace(String.format("  Frame #%d transmitted -> PACKET LOST in channel", i));
                    }
                    firstFailure = i;
                    break; // In Go-Back-N, receiver drops out-of-order packets after first failure
                } else {
                    result.deliveredFrames++;
                    result.addTrace(String.format("  Frame #%d transmitted -> CRC-8 VALID -> ACK received", i));
                }
            }

            if (firstFailure >= 0) {
                // Timeout on unacknowledged frame!
                int retransCount = windowEnd - firstFailure;
                result.retransmissions += retransCount;
                result.addTrace(String.format("  TIMEOUT: Frame #%d unacknowledged. Go-Back-N rolls back base to %d (+%d retransmissions)",
                        firstFailure, firstFailure, retransCount));
                base = firstFailure; // Roll back base to first unacknowledged frame
            } else {
                // Entire window acknowledged; advance base
                base = windowEnd;
                result.addTrace(String.format("  Window acknowledged. Base advances to %d", base));
            }
        }

        result.delivered = (base >= frameCount);
        if (result.delivered) {
            result.detail = "All " + frameCount + " frames acknowledged and delivered successfully.";
        } else {
            result.detail = "Retry limit (" + maxAttempts + " attempts) reached. Transfer aborted due to severe channel noise.";
        }

        return result;
    }
}
