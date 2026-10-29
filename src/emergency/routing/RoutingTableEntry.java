package emergency.routing;

/**
 * Single forwarding table entry for a router.
 */
public final class RoutingTableEntry {
    public final String destination;
    public final String nextHop;
    public final int cost;
    public final boolean reachable;

    public RoutingTableEntry(String destination, String nextHop, int cost) {
        this.destination = destination;
        this.nextHop = nextHop;
        this.cost = cost;
        this.reachable = cost < Route.INF && nextHop != null && !nextHop.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("%-16s -> NextHop: %-16s Cost: %s",
                destination,
                reachable ? nextHop : "None",
                cost >= Route.INF ? "INF" : String.valueOf(cost));
    }
}
