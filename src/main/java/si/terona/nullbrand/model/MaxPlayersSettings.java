package si.terona.nullbrand.model;
/** Immutable configuration snapshot for max-player slot override. */
public final class MaxPlayersSettings {
    public static final MaxPlayersSettings DISABLED = new MaxPlayersSettings(false, 0);
    private final boolean enabled;
    private final int amount;
    public MaxPlayersSettings(boolean enabled, int amount) {
        this.enabled = enabled;
        this.amount = amount;
    }
    public boolean isEnabled() { return enabled; }
    public int getAmount() { return amount; }
}
