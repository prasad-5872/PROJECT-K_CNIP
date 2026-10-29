package emergency.routing;

import emergency.model.Edge;
import emergency.model.NetworkModel;

import java.util.*;

/**
 * Iterative Distance Vector routing algorithm based on the distributed Bellman-Ford equation:
 *   D_x(y) = min_v { c(x, v) + D_v(y) }
 *
 * Simulates synchronous periodic distance-vector exchanges until convergence or max iterations.
 */
public final class DistanceVectorRouter {
    private DistanceVectorRouter() {}

    public static final class DVResult {
        public final Map<String, Map<String, Integer>> costMatrix; // u -> dest -> cost
        public final Map<String, Map<String, String>> nextHopMatrix; // u -> dest -> nextHop
        public final int iterationsToConverge;
        public final boolean converged;

        public DVResult(Map<String, Map<String, Integer>> costMatrix,
                        Map<String, Map<String, String>> nextHopMatrix,
                        int iterationsToConverge, boolean converged) {
            this.costMatrix = costMatrix;
            this.nextHopMatrix = nextHopMatrix;
            this.iterationsToConverge = iterationsToConverge;
            this.converged = converged;
        }
    }

    /**
     * Executes iterative distance vector exchange until convergence.
     */
    public static DVResult computeDistanceVector(NetworkModel network) {
        if (network == null || network.nodes().isEmpty()) {
            return new DVResult(Map.of(), Map.of(), 0, true);
        }

        Set<String> nodes = network.nodes();
        Map<String, Map<String, Integer>> costs = new LinkedHashMap<>();
        Map<String, Map<String, String>> nextHops = new LinkedHashMap<>();

        // Initialize table: 0 to self, c(u, v) for direct active neighbors, INF otherwise
        for (String u : nodes) {
            Map<String, Integer> rowCost = new LinkedHashMap<>();
            Map<String, String> rowHop = new LinkedHashMap<>();
            for (String dest : nodes) {
                if (u.equals(dest)) {
                    rowCost.put(dest, 0);
                    rowHop.put(dest, u);
                } else {
                    rowCost.put(dest, Route.INF);
                    rowHop.put(dest, null);
                }
            }
            for (Edge e : network.edgesFrom(u)) {
                if (e.active) {
                    String v = e.other(u);
                    rowCost.put(v, e.cost);
                    rowHop.put(v, v);
                }
            }
            costs.put(u, rowCost);
            nextHops.put(u, rowHop);
        }

        int maxIterations = nodes.size() + 2;
        int iterations = 0;
        boolean converged = false;

        for (int iter = 0; iter < maxIterations; iter++) {
            iterations = iter + 1;
            boolean anyChanged = false;
            Map<String, Map<String, Integer>> nextCosts = new LinkedHashMap<>();
            Map<String, Map<String, String>> nextNextHops = new LinkedHashMap<>();

            for (String u : nodes) {
                Map<String, Integer> rowCost = new LinkedHashMap<>(costs.get(u));
                Map<String, String> rowHop = new LinkedHashMap<>(nextHops.get(u));

                for (Edge e : network.edgesFrom(u)) {
                    if (!e.active) continue;
                    String v = e.other(u);
                    int edgeCost = e.cost;

                    for (String dest : nodes) {
                        int vCost = costs.get(v).get(dest);
                        if (vCost < Route.INF) {
                            int totalThroughV = edgeCost + vCost;
                            if (totalThroughV < rowCost.get(dest)) {
                                rowCost.put(dest, totalThroughV);
                                rowHop.put(dest, v);
                                anyChanged = true;
                            }
                        }
                    }
                }
                nextCosts.put(u, rowCost);
                nextNextHops.put(u, rowHop);
            }

            costs = nextCosts;
            nextHops = nextNextHops;

            if (!anyChanged) {
                converged = true;
                break;
            }
        }

        return new DVResult(costs, nextHops, iterations, converged);
    }

    /**
     * Resolves end-to-end path using the converged next-hop routing tables.
     */
    public static Route findRoute(NetworkModel network, String source, String destination) {
        if (network == null || source == null || destination == null) {
            return Route.unreachable(source, destination, "Distance Vector", "Invalid arguments.");
        }
        if (!network.nodes().contains(source) || !network.nodes().contains(destination)) {
            return Route.unreachable(source, destination, "Distance Vector", "Node not in network.");
        }
        if (source.equals(destination)) {
            return new Route(source, destination, List.of(source), 0, "Distance Vector", "Source and destination are identical.");
        }

        DVResult dv = computeDistanceVector(network);
        int cost = dv.costMatrix.getOrDefault(source, Map.of()).getOrDefault(destination, Route.INF);

        if (cost >= Route.INF) {
            return Route.unreachable(source, destination, "Distance Vector",
                    "Destination is unreachable according to Distance Vector tables.");
        }

        List<String> path = new ArrayList<>();
        path.add(source);
        String current = source;
        Set<String> visited = new HashSet<>();
        visited.add(source);

        while (!current.equals(destination)) {
            String next = dv.nextHopMatrix.get(current).get(destination);
            if (next == null || visited.contains(next)) {
                // Loop or broken next hop detected
                return Route.unreachable(source, destination, "Distance Vector",
                        "Forwarding loop or missing next-hop detected along path.");
            }
            path.add(next);
            visited.add(next);
            current = next;
            if (path.size() > network.nodes().size()) {
                return Route.unreachable(source, destination, "Distance Vector", "Routing loop detected.");
            }
        }

        String explanation = "Distance Vector converged in " + dv.iterationsToConverge +
                " iterations. Next-hop from " + source + " is " + (path.size() > 1 ? path.get(1) : "None") +
                ". Total distributed cost: " + cost;

        return new Route(source, destination, path, cost, "Distance Vector", explanation);
    }

    /**
     * Generates a list of RoutingTableEntry objects for a given node.
     */
    public static List<RoutingTableEntry> buildRoutingTable(NetworkModel network, String source) {
        List<RoutingTableEntry> entries = new ArrayList<>();
        DVResult dv = computeDistanceVector(network);
        Map<String, Integer> costRow = dv.costMatrix.getOrDefault(source, Map.of());
        Map<String, String> hopRow = dv.nextHopMatrix.getOrDefault(source, Map.of());

        for (String dest : network.nodes()) {
            if (dest.equals(source)) {
                entries.add(new RoutingTableEntry(dest, "Local Loop", 0));
                continue;
            }
            int cost = costRow.getOrDefault(dest, Route.INF);
            String nextHop = hopRow.get(dest);
            entries.add(new RoutingTableEntry(dest, nextHop != null ? nextHop : "None", cost));
        }
        entries.sort(Comparator.comparing(e -> e.destination));
        return entries;
    }
}
