package emergency.model;

import java.util.Objects;

/**
 * Mutable undirected weighted communication link between two network nodes.
 */
public final class Edge {
    public final String a;
    public final String b;
    public final int cost;
    public boolean active = true;

    public Edge(String a, String b, int cost) {
        this.a = Objects.requireNonNull(a, "Node A cannot be null");
        this.b = Objects.requireNonNull(b, "Node B cannot be null");
        this.cost = cost;
    }

    public Edge(String a, String b, int cost, boolean active) {
        this(a, b, cost);
        this.active = active;
    }

    /**
     * Given one endpoint of this edge, returns the other endpoint.
     */
    public String other(String node) {
        if (node.equals(a)) return b;
        if (node.equals(b)) return a;
        throw new IllegalArgumentException("Node '" + node + "' is not an endpoint of edge (" + a + " -- " + b + ")");
    }

    /**
     * Checks if this edge connects the specified pair of nodes (undirected).
     */
    public boolean connects(String u, String v) {
        return (a.equals(u) && b.equals(v)) || (a.equals(v) && b.equals(u));
    }

    public String id() {
        return a.compareTo(b) <= 0 ? (a + "<->" + b) : (b + "<->" + a);
    }

    @Override
    public String toString() {
        return a + " -- " + b + " (" + cost + "ms)" + (active ? " [UP]" : " [DOWN]");
    }
}
