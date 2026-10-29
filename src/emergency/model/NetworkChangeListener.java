package emergency.model;

/**
 * Observer interface notified when the network topology changes
 * (e.g., link toggled UP/DOWN, links restored, or weights updated).
 */
@FunctionalInterface
public interface NetworkChangeListener {
    void onTopologyChanged();
}
