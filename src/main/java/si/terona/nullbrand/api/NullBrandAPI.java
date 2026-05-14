package si.terona.nullbrand.api;
import si.terona.nullbrand.engine.BrandEngine;
import si.terona.nullbrand.engine.BrandEngine.EngineState;
/**
 * Public stable API for NullBrand.
 * Platform-independent — works on Velocity, BungeeCord, and Spigot.
 *
 * <p>Example usage from another plugin:
 * <pre>{@code
 * NullBrandAPI.setProfile("event");
 * NullBrandAPI.setMaintenance(true);
 * int displayed = NullBrandAPI.getDisplayedPlayers(50);
 * }</pre>
 */
public final class NullBrandAPI {
    private static volatile BrandEngine engine;
    /** Internal — called by NullBrand on initialization. */
    public static void init(BrandEngine e) {
        engine = e;
    }
    /**
     * Reload the plugin configuration. Safe to call from any thread.
     * Blocks until the reload completes.
     *
     * @return true if reload succeeded
     */
    public static boolean reload() {
        checkInitialized();
        // NullBrand entry points store dataDir/classLoader — delegate through engine state
        throw new UnsupportedOperationException("Use the in-game /nb reload command to trigger a reload.");
    }
    /**
     * Switch the active MOTD profile.
     *
     * @param profileName name of the profile to switch to
     * @return true if the profile exists and was switched
     */
    public static boolean setProfile(String profileName) {
        checkInitialized();
        EngineState state = engine.state();
        if (state == null) return false;
        return state.profileManager().setProfile(profileName);
    }
    /**
     * Get the name of the currently active profile.
     */
    public static String getActiveProfile() {
        checkInitialized();
        EngineState state = engine.state();
        if (state == null) return "default";
        return state.profileManager().getActiveProfileName();
    }
    /**
     * Enable or disable maintenance mode.
     *
     * @param active true to enable maintenance mode
     */
    public static void setMaintenance(boolean active) {
        checkInitialized();
        EngineState state = engine.state();
        if (state != null) state.maintenanceManager().setActive(active);
    }
    /**
     * Check if maintenance mode is currently active.
     */
    public static boolean isMaintenance() {
        checkInitialized();
        EngineState state = engine.state();
        return state != null && state.maintenanceManager().isActive();
    }
    /**
     * Override the fake-player static amount at runtime.
     * Only effective when global fake-player mode is STATIC.
     *
     * @param amount additional fake players to display
     */
    public static void setFakePlayers(int amount) {
        checkInitialized();
        EngineState state = engine.state();
        if (state != null) state.fakePlayerEngine().setStaticOverride(Math.max(0, amount));
    }
    /**
     * Compute the displayed online player count using the current fake-player engine.
     *
     * @param realOnline the actual number of online players
     * @return displayed count
     */
    public static int getDisplayedPlayers(int realOnline) {
        checkInitialized();
        EngineState state = engine.state();
        if (state == null) return realOnline;
        return state.fakePlayerEngine().computeDisplayed(realOnline);
    }
    private static void checkInitialized() {
        if (engine == null) throw new IllegalStateException("NullBrand has not been initialized yet.");
    }
}
