package emergency.routing;

import emergency.model.Edge;
import emergency.model.NetworkModel;

import java.util.*;

/**
 * Dijkstra's Single-Source Shortest Path algorithm for link-state emergency routing.
 * Respects active/inactive link states and computes optimal paths.
 */
public final class DijkstraRouter {
    private DijkstraRouter() {}

    /**
     * Computes the shortest path from source to destination.
     */
    public static Route findRoute(NetworkModel network, String source, String destination) {
        if (network == null || source == null || destination == null) {
            return Route.unreachable(source, destination, "Dijkstra", "Invalid input parameters.");
        }
        if (!network.nodes().contains(source) || !network.nodes().contains(destination)) {
            return Route.unreachable(source, destination, "Dijkstra", "Source or destination node not in network.");
        }
        if (source.equals(destination)) {
            return new Route(source, destination, List.of(source), 0, "Dijkstra", "Source and destination are identical.");
        }

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();

        for (String node : network.nodes()) {
            dist.put(node, Route.INF);
        }
        dist.put(source, 0);

        PriorityQueue<NodeDist> pq = new PriorityQueue<>(Comparator.comparingInt(nd -> nd.distance));
        pq.add(new NodeDist(source, 0));

        while (!pq.isEmpty()) {
            NodeDist current = pq.poll();
            String u = current.node;

            if (visited.contains(u)) continue;
            visited.add(u);

            if (u.equals(destination)) break;

            for (Edge edge : network.edgesFrom(u)) {
                if (!edge.active) continue; // Skip failed links
                String v = edge.other(u);
                if (visited.contains(v)) continue;

                int alt = dist.get(u) + edge.cost;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.add(new NodeDist(v, alt));
                }
            }
        }

        if (dist.get(destination) >= Route.INF || !prev.containsKey(destination)) {
            return Route.unreachable(source, destination, "Dijkstra",
                    "No active path exists between " + source + " and " + destination + " (network partitioned).");
        }

        LinkedList<String> path = new LinkedList<>();
        String curr = destination;
        while (curr != null) {
            path.addFirst(curr);
            curr = prev.get(curr);
        }

        if (!path.getFirst().equals(source)) {
            return Route.unreachable(source, destination, "Dijkstra", "Path trace failed to reach source.");
        }

        int totalCost = dist.get(destination);
        StringBuilder exp = new StringBuilder();
        exp.append("Dijkstra selected path via ");
        for (int i = 0; i < path.size() - 1; i++) {
            Edge e = network.findEdge(path.get(i), path.get(i + 1));
            exp.append(path.get(i)).append("-[cost ").append(e != null ? e.cost : "?").append("]->");
        }
        exp.append(destination).append(". Total cost: ").append(totalCost).append(" (optimal minimum cost).");

        return new Route(source, destination, path, totalCost, "Dijkstra", exp.toString());
    }

    /**
     * Builds the complete forwarding table for a single router using Dijkstra.
     */
    public static List<RoutingTableEntry> buildRoutingTable(NetworkModel network, String source) {
        List<RoutingTableEntry> table = new ArrayList<>();
        if (network == null || source == null || !network.nodes().contains(source)) {
            return table;
        }

        for (String dest : network.nodes()) {
            if (dest.equals(source)) {
                table.add(new RoutingTableEntry(dest, "Local Loop", 0));
                continue;
            }
            Route r = findRoute(network, source, dest);
            if (r.reachable) {
                table.add(new RoutingTableEntry(dest, r.nextHop, r.cost));
            } else {
                table.add(new RoutingTableEntry(dest, "Unreachable", Route.INF));
            }
        }
        table.sort(Comparator.comparing(e -> e.destination));
        return table;
    }

    private static class NodeDist {
        final String node;
        final int distance;
        NodeDist(String node, int distance) {
            this.node = node;
            this.distance = distance;
        }
    }
}
