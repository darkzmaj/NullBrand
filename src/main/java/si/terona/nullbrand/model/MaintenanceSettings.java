package si.terona.nullbrand.model;
/** Immutable configuration snapshot for maintenance-mode settings. */
public final class MaintenanceSettings {
    public static final MaintenanceSettings DISABLED = new MaintenanceSettings(false, "maintenance");
    private final boolean enabled;
    private final String profile;
    public MaintenanceSettings(boolean enabled, String profile) {
        this.enabled = enabled;
        this.profile = profile == null || profile.isEmpty() ? "maintenance" : profile;
    }
    public boolean isEnabled() { return enabled; }
    public String getProfile() { return profile; }
}
