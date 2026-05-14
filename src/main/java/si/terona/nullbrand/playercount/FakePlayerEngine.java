package si.terona.nullbrand.playercount;
import si.terona.nullbrand.model.FakePlayerSettings;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Computes the fake-player bonus to add to the real online count.
 * Each mode uses a different calculation strategy.
 * RANDOM mode caches computed values to avoid per-ping allocation pressure.
 */
public final class FakePlayerEngine {
    private static final Random RANDOM = new Random();
    private final FakePlayerSettings settings;
    /** Cached random value for RANDOM mode (refreshed every cacheTicks). */
    private final AtomicInteger cachedRandom = new AtomicInteger(0);
    private final AtomicLong randomTick = new AtomicLong(0);
    public FakePlayerEngine(FakePlayerSettings settings) {
        this.settings = settings;
        // Initialize random cache
        if (settings.isEnabled() && settings.getMode() == si.terona.nullbrand.model.FakePlayerMode.RANDOM) {
            refreshRandom();
        }
    }
    /**
     * Compute the displayed online player count based on the real count and configured mode.
     *
     * @param realOnline real online player count reported by the platform
     * @return displayed online count (always >= 0)
     */
    public int computeDisplayed(int realOnline) {
        if (!settings.isEnabled()) return realOnline;
        int bonus = switch (settings.getMode()) {
            case STATIC -> settings.getAmount();
            case RANDOM -> cachedRandom.get();
            case SMART -> {
                int computed = (int) (realOnline * settings.getMultiplier());
                yield Math.max(computed - realOnline, settings.getMinimum());
            }
            case TIME_BASED -> computeTimeBased(realOnline);
        };
        return Math.max(0, realOnline + bonus);
    }
    /**
     * Tick method — called by the BrandEngine scheduler. Refreshes cached random
     * value when the configured cache interval elapses.
     */
    public void tick() {
        if (!settings.isEnabled() || settings.getMode() != si.terona.nullbrand.model.FakePlayerMode.RANDOM) return;
        long tick = randomTick.incrementAndGet();
        if (tick >= settings.getCacheTicks()) {
            randomTick.set(0);
            refreshRandom();
        }
    }
    /** Override the static fake amount at runtime (API or command driven). */
    public void setStaticOverride(int amount) {
        cachedRandom.set(amount);
    }
    public FakePlayerSettings getSettings() {
        return settings;
    }
    private void refreshRandom() {
        int min = settings.getMin();
        int max = settings.getMax();
        if (max <= min) { cachedRandom.set(min); return; }
        cachedRandom.set(min + RANDOM.nextInt(max - min + 1));
    }
    private int computeTimeBased(int realOnline) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        // Peak hours: 17-22, night: 0-7
        double factor;
        if (hour >= 17 && hour < 22) factor = isWeekend ? 2.5 : 2.0;
        else if (hour >= 12 && hour < 17) factor = isWeekend ? 1.8 : 1.4;
        else if (hour >= 7 && hour < 12) factor = 1.2;
        else factor = 0.3; // night reduction
        int bonus = (int) (realOnline * (factor - 1.0));
        return Math.max(bonus, settings.getMinimum());
    }
}
