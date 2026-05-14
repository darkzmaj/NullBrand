package si.terona.nullbrand.model;
/** Immutable map of all user-facing messages. */
public final class LangSettings {
    private final String reload;
    private final String noPermission;
    private final String profileSwitched;
    private final String maintenanceOn;
    private final String maintenanceOff;
    private final String unknownProfile;
    private final String fakePlayersSet;
    private final String status;
    public LangSettings(String reload, String noPermission, String profileSwitched,
                         String maintenanceOn, String maintenanceOff, String unknownProfile,
                         String fakePlayersSet, String status) {
        this.reload = reload;
        this.noPermission = noPermission;
        this.profileSwitched = profileSwitched;
        this.maintenanceOn = maintenanceOn;
        this.maintenanceOff = maintenanceOff;
        this.unknownProfile = unknownProfile;
        this.fakePlayersSet = fakePlayersSet;
        this.status = status;
    }
    public static LangSettings defaults() {
        return new LangSettings(
                "&aNullBrand reloaded successfully.",
                "&cYou don't have permission.",
                "&aActive profile switched to: &e%profile%",
                "&aMaintenance mode &cenabled&a.",
                "&aMaintenance mode &adisabled&a.",
                "&cUnknown profile: &e%profile%",
                "&aFake players set to: &e%amount%",
                "&7--- &5NullBrand &7Status ---\n" +
                "&7Platform: &f%platform%\n" +
                "&7Active Profile: &f%profile%\n" +
                "&7Maintenance: &f%maintenance%\n" +
                "&7Fake Players: &f%fakeplayers%"
        );
    }
    public String getReload() { return reload; }
    public String getNoPermission() { return noPermission; }
    public String getProfileSwitched() { return profileSwitched; }
    public String getMaintenanceOn() { return maintenanceOn; }
    public String getMaintenanceOff() { return maintenanceOff; }
    public String getUnknownProfile() { return unknownProfile; }
    public String getFakePlayersSet() { return fakePlayersSet; }
    public String getStatus() { return status; }
}
