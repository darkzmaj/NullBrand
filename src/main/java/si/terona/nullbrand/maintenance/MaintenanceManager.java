package si.terona.nullbrand.maintenance;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Runtime maintenance mode toggle.
 * Decoupled from config so it can be changed via command or API without a reload.
 */
public final class MaintenanceManager {
    private final AtomicBoolean active;
    public MaintenanceManager(boolean initialState) {
        this.active = new AtomicBoolean(initialState);
    }
    public boolean isActive() {
        return active.get();
    }
    public void setActive(boolean state) {
        active.set(state);
    }
    public boolean toggle() {
        boolean newState = !active.get();
        active.set(newState);
        return newState;
    }
}
