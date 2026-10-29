package emergency;

import java.util.*;

public final class SimulationEngine {
    public static final class Metrics {
        public int generated, delivered, dropped, retransmissions, corrupted;
        public double throughputMbps, averageDelayMs, deliveryRatio;
        public int bucketAccepted, bucketDropped, peakQueue;
        public String summary() {
            return String.format(Locale.US,
                "Generated: %d | Delivered: %d | Dropped: %d | Delivery ratio: %.1f%% | Throughput: %.3f Mbps | Avg delay: %.1f ms | Retransmissions: %d | Corrupt frames: %d | Bucket drops: %d",
                generated, delivered, dropped, deliveryRatio*100, throughputMbps, averageDelayMs, retransmissions, corrupted, bucketDropped);
        }
    }
    private final Random random = new Random();
    private final NetworkModel network;
    public SimulationEngine(NetworkModel network) { this.network=network; }

    public Metrics run(int messageCount, double errorRate, int arrivalsPerTick, int bucketCapacity, int leakRate) {
        Metrics m=new Metrics();
        m.generated=messageCount;
        int[] arrivals=new int[20];
        for (int i=0;i<arrivals.length;i++) arrivals[i]=arrivalsPerTick;
        ProtocolSimulator.BucketResult bucket=ProtocolSimulator.leakyBucket(arrivals,bucketCapacity,leakRate);
        m.bucketAccepted=bucket.accepted; m.bucketDropped=bucket.dropped; m.peakQueue=bucket.peakQueue;

        RoutingAlgorithms.Route route=RoutingAlgorithms.dijkstra(network,"Control Center","Shelter B");
        if (route.path.isEmpty()) {
            m.dropped=messageCount; m.deliveryRatio=0; m.averageDelayMs=0; return m;
        }
        double delayTotal=0;
        for (int i=0;i<messageCount;i++) {
            ProtocolSimulator.TransferResult t=ProtocolSimulator.goBackN(5,4,errorRate,random);
            m.retransmissions+=t.retransmissions; m.corrupted+=t.corruptedFrames;
            if (t.delivered) {
                m.delivered++;
                // Model per-link propagation/processing delay plus retransmission penalty.
                delayTotal += route.cost*8.0 + t.retransmissions*12.0 + random.nextDouble()*8.0;
            } else m.dropped++;
        }
        m.deliveryRatio = messageCount == 0 ? 0 : (double)m.delivered/messageCount;
        m.averageDelayMs = m.delivered == 0 ? 0 : delayTotal/m.delivered;
        m.throughputMbps = (m.delivered*5*1024.0*8.0)/(Math.max(1,delayTotal)*1000.0);
        // Congestion drops are reported separately; the bucket model does not silently alter delivery counts.
        return m;
    }
}
