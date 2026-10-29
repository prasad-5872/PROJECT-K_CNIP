package emergency;

import java.util.*;

public final class ProjectSelfTest {
    public static void main(String[] args) {
        int passed=0;
        NetworkModel n=NetworkModel.demo();
        RoutingAlgorithms.Route r=RoutingAlgorithms.dijkstra(n,"Control Center","Shelter B");
        check(!r.path.isEmpty() && r.cost==7,"Dijkstra finds minimum-cost path"); passed++;
        NetworkModel.Edge edge=n.edges().get(0); edge.active=false;
        check(!RoutingAlgorithms.dijkstra(n,"Control Center","Shelter B").path.isEmpty(),"Alternate route survives one link failure"); passed++;
        Map<String,Map<String,Integer>> dv=RoutingAlgorithms.distanceVector(n);
        check(dv.get("Control Center").get("Shelter B")<RoutingAlgorithms.INF,"Distance Vector converges"); passed++;
        byte[] payload="MAYDAY".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int crc=ProtocolSimulator.crc8(payload);
        check(ProtocolSimulator.crcValid(payload,crc),"CRC accepts unmodified payload"); passed++;
        payload[0]^=1;
        check(!ProtocolSimulator.crcValid(payload,crc),"CRC detects modified payload"); passed++;
        ProtocolSimulator.TransferResult transfer=ProtocolSimulator.goBackN(10,4,0.0,new Random(1));
        check(transfer.delivered,"Go-Back-N delivers with zero error rate"); passed++;
        ProtocolSimulator.BucketResult bucket=ProtocolSimulator.leakyBucket(new int[]{10,10,10},5,2);
        check(bucket.dropped>0 && bucket.peakQueue<=5,"Leaky Bucket enforces capacity"); passed++;
        System.out.println("PASS: "+passed+" checks");
    }
    private static void check(boolean ok,String label) {
        if(!ok) throw new AssertionError("FAILED: "+label);
        System.out.println("PASS: "+label);
    }
}
