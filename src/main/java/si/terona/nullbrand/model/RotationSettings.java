package si.terona.nullbrand.model;
import java.util.Collections;
import java.util.List;
/** Immutable configuration snapshot for profile rotation. */
public final class RotationSettings {
    public static final RotationSettings DISABLED =
            new RotationSettings(false, 300, Collections.emptyList());
    private final boolean enabled;
    private final int interval;
    private final List<String> profiles;
    public RotationSettings(boolean enabled, int interval, List<String> profiles) {
        this.enabled = enabled;
        this.interval = Math.max(10, interval);
        this.profiles = List.copyOf(profiles);
    }
    public boolean isEnabled() { return enabled && profiles.size() > 1; }
    public int getInterval() { return interval; }
    public List<String> getProfiles() { return profiles; }
}
