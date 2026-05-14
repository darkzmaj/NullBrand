package si.terona.nullbrand.config;
import si.terona.nullbrand.model.*;
import java.util.Map;
/**
 * Immutable snapshot of the entire NullBrand configuration.
 * Replaced atomically on each reload.
 */
public final class BrandConfig {
    private final boolean debug;
    private final boolean motdEnabled;
    private final String activeProfile;
    private final String globalFaviconName;
    private final MaintenanceSettings maintenanceSettings;
    private final FakePlayerSettings globalFakePlayerSettings;
    private final MaxPlayersSettings globalMaxPlayersSettings;
    private final PlayerCountSettings globalPlayerCountSettings;
    private final RotationSettings rotationSettings;
    private final PingProtectionSettings pingProtectionSettings;
    private final LangSettings langSettings;
    private final Map<String, MotdProfile> profiles;
    public BrandConfig(boolean debug,
                       boolean motdEnabled,
                       String activeProfile,
                       String globalFaviconName,
                       MaintenanceSettings maintenanceSettings,
                       FakePlayerSettings globalFakePlayerSettings,
                       MaxPlayersSettings globalMaxPlayersSettings,
                       PlayerCountSettings globalPlayerCountSettings,
                       RotationSettings rotationSettings,
                       PingProtectionSettings pingProtectionSettings,
                       LangSettings langSettings,
                       Map<String, MotdProfile> profiles) {
        this.debug = debug;
        this.motdEnabled = motdEnabled;
        this.activeProfile = activeProfile;
        this.globalFaviconName = globalFaviconName;
        this.maintenanceSettings = maintenanceSettings;
        this.globalFakePlayerSettings = globalFakePlayerSettings;
        this.globalMaxPlayersSettings = globalMaxPlayersSettings;
        this.globalPlayerCountSettings = globalPlayerCountSettings;
        this.rotationSettings = rotationSettings;
        this.pingProtectionSettings = pingProtectionSettings;
        this.langSettings = langSettings;
        this.profiles = profiles;
    }
    public boolean isDebug() { return debug; }
    public boolean isMotdEnabled() { return motdEnabled; }
    public String getActiveProfile() { return activeProfile; }
    public String getGlobalFaviconName() { return globalFaviconName; }
    public MaintenanceSettings getMaintenanceSettings() { return maintenanceSettings; }
    public FakePlayerSettings getGlobalFakePlayerSettings() { return globalFakePlayerSettings; }
    public MaxPlayersSettings getGlobalMaxPlayersSettings() { return globalMaxPlayersSettings; }
    public PlayerCountSettings getGlobalPlayerCountSettings() { return globalPlayerCountSettings; }
    public RotationSettings getRotationSettings() { return rotationSettings; }
    public PingProtectionSettings getPingProtectionSettings() { return pingProtectionSettings; }
    public LangSettings getLangSettings() { return langSettings; }
    public Map<String, MotdProfile> getProfiles() { return profiles; }
    public MotdProfile getProfile(String name) {
        MotdProfile p = profiles.get(name);
        if (p != null) return p;
        return profiles.getOrDefault("global",
                new si.terona.nullbrand.model.MotdProfile(
                        java.util.List.of("&5Null&dBrand"),
                        java.util.Collections.emptyList(), false));
    }
}
