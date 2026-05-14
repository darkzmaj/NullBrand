package si.terona.nullbrand.cache;
import si.terona.nullbrand.model.PingProtectionSettings;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Lightweight per-IP ping rate limiter using a lock-free concurrent map.
 * Entries older than the cooldown window are lazily cleaned up.
 */
public final class PingRateLimiter {
    private static final long CLEANUP_INTERVAL_MS = 30_000L;
    private final PingProtectionSettings settings;
    private final ConcurrentHashMap<String, long[]> lastPing = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();
    public PingRateLimiter(PingProtectionSettings settings) {
        this.settings = settings;
    }
    /**
     * Record a ping attempt and decide whether it should be blocked.
     *
     * @param ip IP address of the pinging client
     * @return true if this ping should be blocked (rate-limited), false if it may proceed
     */
    public boolean isBlocked(String ip) {
        if (!settings.isEnabled() || ip == null) return false;
        long now = System.currentTimeMillis();
        maybePurge(now);
        long[] entry = lastPing.compute(ip, (k, v) -> {
            if (v == null) return new long[]{now};
            v[0] = now;
            return v;
        });
        // entry[0] is last-seen; block if within cooldown window
        // (compute always returns updated value; re-fetch for comparison)
        long[] existing = lastPing.get(ip);
        if (existing == null) return false;
        // We use a simpler per-IP cooldown: if the gap since last ping < cooldownMs, block.
        // We track the *previous* timestamp in a separate slot.
        long[] trackEntry = lastPing.compute(ip, (k, v) -> {
            if (v == null || v.length < 2) return new long[]{now, Long.MIN_VALUE};
            long prev = v[0];
            v[0] = now;
            v[1] = prev;
            return v;
        });
        long prev = trackEntry.length > 1 ? trackEntry[1] : Long.MIN_VALUE;
        if (prev == Long.MIN_VALUE) return false;
        return (now - prev) < settings.getCooldownMs();
    }
    private void maybePurge(long now) {
        if ((now - lastCleanup) < CLEANUP_INTERVAL_MS) return;
        lastCleanup = now;
        long threshold = now - settings.getCooldownMs() * 10;
        lastPing.entrySet().removeIf(e -> e.getValue()[0] < threshold);
    }
}
