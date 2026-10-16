package si.terona.nullbrand.cache;

import si.terona.nullbrand.model.PingProtectionSettings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;

/** Bounded per-IP cooldown cache. Blocked attempts do not extend the cooldown. */
public final class PingRateLimiter {
    private static final int MAX_ENTRIES = 16_384;
    private final PingProtectionSettings settings;
    private final LongSupplier clock;
    private final Map<String, Long> lastPing = new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public PingRateLimiter(PingProtectionSettings settings) {
        this(settings, () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
    }

    PingRateLimiter(PingProtectionSettings settings, LongSupplier clock) {
        this.settings = settings;
        this.clock = clock;
    }

    public synchronized boolean isBlocked(String ip) {
        if (!settings.isEnabled() || ip == null) return false;
        long now = clock.getAsLong();
        Long previous = lastPing.get(ip);
        if (previous != null && now - previous < settings.getCooldownMs()) return true;
        lastPing.put(ip, now);
        return false;
    }
}
