package emergency;

import java.util.*;

/** Mutable undirected weighted network used by the simulator. */
public final class NetworkModel {
    public static final class Edge {
        public final String a, b;
        public final int cost;
        public boolean active = true;
        public Edge(String a, String b, int cost) { this.a=a; this.b=b; this.cost=cost; }
        public String other(String node) { return node.equals(a) ? b : a; }
        @Override public String toString() { return a + " -- " + b + " (" + cost + ")" + (active ? "" : " [DOWN]"); }
    }

    private final Map<String, List<Edge>> adjacency = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    public void addNode(String name) { adjacency.computeIfAbsent(name, k -> new ArrayList<>()); }
    public void addEdge(String a, String b, int cost) {
        addNode(a); addNode(b);
        Edge e = new Edge(a,b,cost); edges.add(e);
        adjacency.get(a).add(e); adjacency.get(b).add(e);
    }
    public Set<String> nodes() { return Collections.unmodifiableSet(adjacency.keySet()); }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public List<Edge> edgesFrom(String node) { return Collections.unmodifiableList(adjacency.getOrDefault(node, List.of())); }
    public boolean connected(String a, String b) {
        for (Edge e : adjacency.getOrDefault(a, List.of()))
            if (e.other(a).equals(b) && e.active) return true;
        return false;
    }
    public void toggleEdge(Edge edge) { edge.active = !edge.active; }
    public void setEdge(Edge edge, boolean active) { edge.active = active; }
    public Edge edgeAt(int index) { return edges.get(index); }

    public static NetworkModel demo() {
        NetworkModel n = new NetworkModel();
        for (String s : List.of("Control Center","Police","Fire Service","Ambulance","Field Team","Shelter A","Shelter B"))
            n.addNode(s);
        n.addEdge("Control Center","Police",2);
        n.addEdge("Control Center","Fire Service",3);
        n.addEdge("Control Center","Field Team",4);
        n.addEdge("Police","Ambulance",2);
        n.addEdge("Police","Field Team",2);
        n.addEdge("Fire Service","Ambulance",2);
        n.addEdge("Fire Service","Shelter A",4);
        n.addEdge("Ambulance","Shelter B",3);
        n.addEdge("Field Team","Shelter A",2);
        n.addEdge("Shelter A","Shelter B",2);
        return n;
    }
}
