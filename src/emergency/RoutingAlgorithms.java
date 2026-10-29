package emergency;

import java.util.*;

public final class RoutingAlgorithms {
    private RoutingAlgorithms() {}
    public static final int INF = 1_000_000;

    public static final class Route {
        public final List<String> path;
        public final int cost;
        public Route(List<String> path, int cost) { this.path=path; this.cost=cost; }
        @Override public String toString() { return path.isEmpty() ? "No route" : String.join(" -> ", path) + " (cost " + cost + ")"; }
    }

    /** Dijkstra's shortest path over currently active links. */
    public static Route dijkstra(NetworkModel n, String source, String destination) {
        Map<String,Integer> dist = new HashMap<>();
        Map<String,String> prev = new HashMap<>();
        Set<String> done = new HashSet<>();
        for (String node : n.nodes()) dist.put(node, INF);
        if (!n.nodes().contains(source) || !n.nodes().contains(destination)) return new Route(List.of(), INF);
        dist.put(source,0);
        while (done.size() < n.nodes().size()) {
            String u = null; int best = INF;
            for (String node : n.nodes()) if (!done.contains(node) && dist.get(node) < best) {
                u=node; best=dist.get(node);
            }
            if (u == null) break;
            if (u.equals(destination)) break;
            done.add(u);
            for (NetworkModel.Edge e : n.edgesFrom(u)) {
                if (!e.active) continue;
                String v=e.other(u);
                int alt=dist.get(u)+e.cost;
                if (alt < dist.get(v)) { dist.put(v,alt); prev.put(v,u); }
            }
        }
        if (dist.get(destination) >= INF) return new Route(List.of(), INF);
        LinkedList<String> path = new LinkedList<>();
        String cur=destination;
        while (cur != null) { path.addFirst(cur); cur=prev.get(cur); }
        if (!path.getFirst().equals(source)) return new Route(List.of(), INF);
        return new Route(path,dist.get(destination));
    }

    /** Iterative synchronous distance-vector updates; returns router -> destination -> cost. */
    public static Map<String, Map<String,Integer>> distanceVector(NetworkModel n) {
        Map<String,Map<String,Integer>> dv = new LinkedHashMap<>();
        for (String node : n.nodes()) {
            Map<String,Integer> row = new LinkedHashMap<>();
            for (String dest : n.nodes()) row.put(dest, node.equals(dest) ? 0 : INF);
            for (NetworkModel.Edge e : n.edgesFrom(node)) if (e.active) row.put(e.other(node),e.cost);
            dv.put(node,row);
        }
        for (int iteration=0; iteration<n.nodes().size(); iteration++) {
            boolean changed=false;
            Map<String,Map<String,Integer>> next = new LinkedHashMap<>();
            for (String u : n.nodes()) {
                Map<String,Integer> row = new LinkedHashMap<>(dv.get(u));
                for (NetworkModel.Edge e : n.edgesFrom(u)) {
                    if (!e.active) continue;
                    String v=e.other(u);
                    for (String dest : n.nodes()) {
                        int vCost=dv.get(v).get(dest);
                        if (vCost < INF && e.cost + vCost < row.get(dest)) {
                            row.put(dest,e.cost+vCost); changed=true;
                        }
                    }
                }
                next.put(u,row);
            }
            dv=next;
            if (!changed) break;
        }
        return dv;
    }
}
