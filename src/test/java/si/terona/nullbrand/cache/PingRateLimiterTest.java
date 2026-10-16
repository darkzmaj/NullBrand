package si.terona.nullbrand.cache;
import org.junit.jupiter.api.Test;
import si.terona.nullbrand.model.PingProtectionSettings;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
class PingRateLimiterTest {
    @Test void acceptsFirstPingAndRecoversWithoutExtendingBlockedWindow() {
        AtomicLong now = new AtomicLong(0);
        PingRateLimiter limiter = new PingRateLimiter(new PingProtectionSettings(true, 500, 10), now::get);
        assertFalse(limiter.isBlocked("client"));
        now.set(100); assertTrue(limiter.isBlocked("client"));
        now.set(500); assertFalse(limiter.isBlocked("client"));
        assertFalse(limiter.isBlocked("another"));
    }
}
