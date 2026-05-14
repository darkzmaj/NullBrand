package si.terona.nullbrand.profile;
import si.terona.nullbrand.config.BrandConfig;
import si.terona.nullbrand.model.MotdProfile;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
/**
 * Manages the active profile with support for manual switching and time-based rotation.
 * Thread-safe: all state held in atomic references.
 */
public final class ProfileManager {
    private final BrandConfig config;
    /** Name of the currently display profile (may be overridden). */
    private final AtomicReference<String> activeProfileName;
    /** Rotation index into the rotation list. */
    private final AtomicInteger rotationIndex = new AtomicInteger(0);
    public ProfileManager(BrandConfig config) {
        this.config = config;
        this.activeProfileName = new AtomicReference<>(config.getActiveProfile());
    }
    /**
     * Resolve the profile to display during a ping.
     * Maintenance mode overrides the active profile if enabled.
     */
    public MotdProfile resolve() {
        if (config.getMaintenanceSettings().isEnabled()) {
            return config.getProfile(config.getMaintenanceSettings().getProfile());
        }
        return config.getProfile(activeProfileName.get());
    }
    /**
     * Switch to a named profile immediately.
     *
     * @param name profile name
     * @return true if the profile exists and was switched
     */
    public boolean setProfile(String name) {
        if (!config.getProfiles().containsKey(name)) return false;
        activeProfileName.set(name);
        return true;
    }
    public String getActiveProfileName() {
        return activeProfileName.get();
    }
    /**
     * Advance to the next profile in the rotation list.
     * Called by the BrandEngine rotation scheduler.
     */
    public void rotateNext() {
        List<String> rota = config.getRotationSettings().getProfiles();
        if (rota.isEmpty()) return;
        int idx = rotationIndex.updateAndGet(i -> (i + 1) % rota.size());
        String next = rota.get(idx);
        if (config.getProfiles().containsKey(next)) {
            activeProfileName.set(next);
        }
    }
}
