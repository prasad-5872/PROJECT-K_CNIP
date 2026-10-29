package emergency.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mutable undirected weighted graph representing the emergency communication infrastructure.
 * Serves as the single source of truth for topology visualization, routing calculations,
 * and transmission simulations.
 */
public class NetworkModel {
    private final Map<String, Node> nodeMap = new LinkedHashMap<>();
    private final Map<String, List<Edge>> adjacency = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<NetworkChangeListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(NetworkChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(NetworkChangeListener listener) {
        listeners.remove(listener);
    }

    public void fireTopologyChanged() {
        for (NetworkChangeListener listener : listeners) {
            listener.onTopologyChanged();
        }
    }

    public void addNode(Node node) {
        Objects.requireNonNull(node, "Node cannot be null");
        nodeMap.put(node.getName(), node);
        adjacency.computeIfAbsent(node.getName(), k -> new ArrayList<>());
    }

    public void addNode(String name) {
        addNode(new Node(name, Node.Role.COMMAND, 0.5, 0.5));
    }

    public void addEdge(String a, String b, int cost) {
        if (!nodeMap.containsKey(a)) addNode(a);
        if (!nodeMap.containsKey(b)) addNode(b);
        Edge e = new Edge(a, b, cost);
        edges.add(e);
        adjacency.get(a).add(e);
        adjacency.get(b).add(e);
        fireTopologyChanged();
    }

    public Set<String> nodes() {
        return Collections.unmodifiableSet(nodeMap.keySet());
    }

    public Collection<Node> getNodeObjects() {
        return Collections.unmodifiableCollection(nodeMap.values());
    }

    public Node getNode(String name) {
        return nodeMap.get(name);
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public List<Edge> edgesFrom(String node) {
        return Collections.unmodifiableList(adjacency.getOrDefault(node, List.of()));
    }

    public boolean connected(String a, String b) {
        for (Edge e : adjacency.getOrDefault(a, List.of())) {
            if (e.other(a).equals(b) && e.active) return true;
        }
        return false;
    }

    public Edge findEdge(String a, String b) {
        for (Edge e : adjacency.getOrDefault(a, List.of())) {
            if (e.other(a).equals(b)) return e;
        }
        return null;
    }

    public void toggleEdge(Edge edge) {
        if (edge != null) {
            edge.active = !edge.active;
            fireTopologyChanged();
        }
    }

    public void setEdge(Edge edge, boolean active) {
        if (edge != null && edge.active != active) {
            edge.active = active;
            fireTopologyChanged();
        }
    }

    public void restoreAllEdges() {
        boolean changed = false;
        for (Edge e : edges) {
            if (!e.active) {
                e.active = true;
                changed = true;
            }
        }
        if (changed) {
            fireTopologyChanged();
        }
    }

    public Edge edgeAt(int index) {
        if (index >= 0 && index < edges.size()) {
            return edges.get(index);
        }
        return null;
    }

    public int activeEdgeCount() {
        int count = 0;
        for (Edge e : edges) {
            if (e.active) count++;
        }
        return count;
    }

    public int totalEdgeCount() {
        return edges.size();
    }

    /**
     * Factory for standard disaster network topology as specified in requirements:
     * Control Center, Police, Fire Service, Ambulance, Field Team, Shelter A, Shelter B.
     */
    public static NetworkModel demo() {
        NetworkModel n = new NetworkModel();

        // Nodes with relative graphical layout coordinates (X, Y in [0, 1])
        n.addNode(new Node("Control Center", Node.Role.COMMAND,    0.50, 0.15));
        n.addNode(new Node("Police",         Node.Role.POLICE,     0.22, 0.35));
        n.addNode(new Node("Fire Service",   Node.Role.FIRE,       0.78, 0.35));
        n.addNode(new Node("Ambulance",      Node.Role.AMBULANCE,  0.50, 0.52));
        n.addNode(new Node("Field Team",     Node.Role.FIELD_TEAM, 0.22, 0.72));
        n.addNode(new Node("Shelter A",      Node.Role.SHELTER,    0.60, 0.82));
        n.addNode(new Node("Shelter B",      Node.Role.SHELTER,    0.85, 0.65));

        // Links with costs (weighted by metric, e.g. latency/link resistance in ms)
        n.addEdge("Control Center", "Police", 2);
        n.addEdge("Control Center", "Fire Service", 3);
        n.addEdge("Control Center", "Field Team", 4);
        n.addEdge("Police", "Ambulance", 2);
        n.addEdge("Police", "Field Team", 2);
        n.addEdge("Fire Service", "Ambulance", 2);
        n.addEdge("Fire Service", "Shelter A", 4);
        n.addEdge("Ambulance", "Shelter B", 3);
        n.addEdge("Field Team", "Shelter A", 2);
        n.addEdge("Shelter A", "Shelter B", 2);

        return n;
    }
}
