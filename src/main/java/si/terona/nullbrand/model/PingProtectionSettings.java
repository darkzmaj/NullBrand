package si.terona.nullbrand.model;
/** Immutable configuration snapshot for ping-rate limiting. */
public final class PingProtectionSettings {
    public static final PingProtectionSettings DISABLED =
            new PingProtectionSettings(false, 1000L, 20);
    private final boolean enabled;
    private final long cooldownMs;
    private final int maxPerSecond;
    public PingProtectionSettings(boolean enabled, long cooldownMs, int maxPerSecond) {
        this.enabled = enabled;
        this.cooldownMs = Math.max(0L, cooldownMs);
        this.maxPerSecond = Math.max(1, maxPerSecond);
    }
    public boolean isEnabled() { return enabled; }
    public long getCooldownMs() { return cooldownMs; }
    public int getMaxPerSecond() { return maxPerSecond; }
}
