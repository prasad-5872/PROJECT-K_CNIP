package emergency;

import java.util.*;

/** Educational protocol models; values are simulated rather than sent over real sockets. */
public final class ProtocolSimulator {
    private ProtocolSimulator() {}
    public static final class TransferResult {
        public int sentFrames, retransmissions, corruptedFrames, deliveredFrames;
        public boolean delivered;
        public String detail;
    }

    /** CRC-8 using polynomial x^8 + x^2 + x + 1 (0x07). */
    public static int crc8(byte[] data) {
        int crc=0;
        for (byte value : data) {
            crc ^= (value & 0xff);
            for (int i=0;i<8;i++) crc = ((crc & 0x80) != 0) ? ((crc << 1) ^ 0x07) : (crc << 1);
            crc &= 0xff;
        }
        return crc;
    }
    public static boolean crcValid(byte[] data, int receivedCrc) { return crc8(data) == (receivedCrc & 0xff); }

    /**
     * Simplified Go-Back-N ARQ model. Random loss/corruption triggers a timeout;
     * the outstanding window is retransmitted from the oldest unacknowledged frame.
     */
    public static TransferResult goBackN(int frameCount, int windowSize, double errorRate, Random random) {
        TransferResult r=new TransferResult();
        int base=0, attempts=0, maxAttempts=Math.max(20, frameCount*8);
        while (base < frameCount && attempts < maxAttempts) {
            int end=Math.min(base+windowSize,frameCount);
            int firstFailure=-1;
            for (int i=base;i<end;i++) {
                r.sentFrames++; attempts++;
                double x=random.nextDouble();
                if (x < errorRate) {
                    r.corruptedFrames++;
                    firstFailure=i;
                    break;
                } else {
                    r.deliveredFrames++;
                }
            }
            if (firstFailure >= 0) {
                r.retransmissions += end-firstFailure;
                base=firstFailure;
            } else {
                base=end;
            }
        }
        r.delivered=base>=frameCount;
        r.detail = r.delivered ? "All frames acknowledged." : "Retry limit reached; transfer incomplete.";
        return r;
    }

    /** Leaky bucket: drains at a fixed rate; returns accepted and dropped packets. */
    public static final class BucketResult {
        public int accepted, dropped, peakQueue;
        public List<Integer> queueSamples = new ArrayList<>();
    }
    public static BucketResult leakyBucket(int[] arrivalsPerTick, int capacity, int leakPerTick) {
        BucketResult r=new BucketResult();
        int queue=0;
        for (int arrivals : arrivalsPerTick) {
            int accepted=Math.min(arrivals,Math.max(0,capacity-queue));
            r.accepted += accepted; r.dropped += arrivals-accepted;
            queue += accepted;
            r.peakQueue=Math.max(r.peakQueue,queue);
            queue=Math.max(0,queue-leakPerTick);
            r.queueSamples.add(queue);
        }
        return r;
    }
}
