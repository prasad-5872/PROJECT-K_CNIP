package emergency.engine;

/**
 * Callback interface for receiving real-time simulation updates,
 * log messages, and completion notifications.
 */
public interface SimulationListener {
    enum State {
        IDLE, RUNNING, PAUSED, COMPLETED, CANCELLED, ERROR
    }

    void onStateChanged(State newState);
    void onProgress(int currentItem, int totalItems, double percent, String status);
    void onLog(String timestamp, String message, String level);
    void onCompleted(SimulationMetrics metrics);
}
