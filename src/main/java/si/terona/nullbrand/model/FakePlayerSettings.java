package si.terona.nullbrand.model;
/** Immutable configuration snapshot for the fake player engine. */
public final class FakePlayerSettings {
    public static final FakePlayerSettings DISABLED =
            new FakePlayerSettings(false, FakePlayerMode.STATIC, 0, 0, 0, 1.0, 0, 20);
    private final boolean enabled;
    private final FakePlayerMode mode;
    private final int amount;
    private final int min;
    private final int max;
    private final double multiplier;
    private final int minimum;
    private final int cacheTicks;
    public FakePlayerSettings(boolean enabled, FakePlayerMode mode, int amount,
                               int min, int max, double multiplier, int minimum, int cacheTicks) {
        this.enabled = enabled;
        this.mode = mode;
        this.amount = amount;
        this.min = min;
        this.max = max;
        this.multiplier = multiplier;
        this.minimum = minimum;
        this.cacheTicks = Math.max(1, cacheTicks);
    }
    public boolean isEnabled() { return enabled; }
    public FakePlayerMode getMode() { return mode; }
    public int getAmount() { return amount; }
    public int getMin() { return min; }
    public int getMax() { return max; }
    public double getMultiplier() { return multiplier; }
    public int getMinimum() { return minimum; }
    public int getCacheTicks() { return cacheTicks; }
}
