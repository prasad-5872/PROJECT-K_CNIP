package emergency.model;

/**
 * Represents an operational node within the emergency communication network.
 */
public final class Node {
    public enum Role {
        COMMAND("Control Center", "Central Command & Tactical Dispatch", "HQ"),
        POLICE("Police Service", "Law Enforcement & Perimeter Security", "POL"),
        FIRE("Fire Service", "Fire Suppression & Search/Rescue", "FIRE"),
        AMBULANCE("Ambulance", "Emergency Medical Response & Triage", "MED"),
        FIELD_TEAM("Field Team", "Mobile Disaster Assessment Unit", "FLD"),
        SHELTER("Civilian Shelter", "Civilian Evacuation & Relief Center", "SHT");

        public final String displayName;
        public final String description;
        public final String badge;

        Role(String displayName, String description, String badge) {
            this.displayName = displayName;
            this.description = description;
            this.badge = badge;
        }
    }

    private final String name;
    private final Role role;
    private final double relX;
    private final double relY;

    public Node(String name, Role role, double relX, double relY) {
        this.name = name;
        this.role = role;
        this.relX = relX;
        this.relY = relY;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public double getRelX() {
        return relX;
    }

    public double getRelY() {
        return relY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return name.equals(node.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name + " [" + role.badge + "]";
    }
}
