package emergency.routing;

import java.util.Collections;
import java.util.List;

/**
 * Represents the computed routing solution between a source and destination.
 */
public final class Route {
    public static final int INF = 1_000_000;

    public final String source;
    public final String destination;
    public final List<String> path;
    public final int cost;
    public final String nextHop;
    public final boolean reachable;
    public final String algorithm;
    public final String explanation;

    public Route(String source, String destination, List<String> path, int cost, String algorithm, String explanation) {
        this.source = source;
        this.destination = destination;
        this.path = path == null ? List.of() : Collections.unmodifiableList(path);
        this.cost = cost;
        this.reachable = !this.path.isEmpty() && cost < INF;
        this.nextHop = this.reachable && this.path.size() > 1 ? this.path.get(1) : (this.reachable && this.path.size() == 1 ? source : "None (Unreachable)");
        this.algorithm = algorithm;
        this.explanation = explanation;
    }

    public static Route unreachable(String source, String destination, String algorithm, String reason) {
        return new Route(source, destination, List.of(), INF, algorithm, reason);
    }

    public int hopCount() {
        return path.isEmpty() ? 0 : Math.max(0, path.size() - 1);
    }

    @Override
    public String toString() {
        if (!reachable) {
            return "NO ROUTE (" + source + " -> " + destination + ") [Cost: INF]";
        }
        return String.join(" -> ", path) + " (Cost: " + cost + ", Hops: " + hopCount() + ", Next Hop: " + nextHop + ")";
    }
}
