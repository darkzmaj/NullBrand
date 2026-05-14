package si.terona.nullbrand.engine;
import si.terona.nullbrand.animation.AnimationEngine;
import si.terona.nullbrand.cache.PingRateLimiter;
import si.terona.nullbrand.config.BrandConfig;
import si.terona.nullbrand.config.ConfigLoader;
import si.terona.nullbrand.favicon.FaviconStore;
import si.terona.nullbrand.maintenance.MaintenanceManager;
import si.terona.nullbrand.model.MotdProfile;
import si.terona.nullbrand.placeholder.PlaceholderEngine;
import si.terona.nullbrand.playercount.FakePlayerEngine;
import si.terona.nullbrand.profile.ProfileManager;
import si.terona.nullbrand.util.LogUtil;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
/**
 * Core NullBrand engine. Owns the canonical plugin state and all background tasks.
 *
 * <p>Thread safety model:
 * <ul>
 *   <li>State is held in a single {@link AtomicReference} of {@link EngineState}.</li>
 *   <li>On reload, a new EngineState is built and swapped atomically.</li>
 *   <li>If the new config is invalid, the old state is preserved.</li>
 *   <li>Background tasks (animation tick, profile rotation) run on a single daemon thread.</li>
 * </ul>
 */
public final class BrandEngine {
    // -----------------------------------------------------------------------
    // Inner engine state — fully immutable, replaced atomically on reload
    // -----------------------------------------------------------------------
    public static final class EngineState {
        final BrandConfig config;
        final FaviconStore globalFavicon;
        final Map<String, FaviconStore> profileFavicons;
        final ProfileManager profileManager;
        final AnimationEngine animationEngine;
        final FakePlayerEngine fakePlayerEngine;
        final PlaceholderEngine placeholderEngine;
        final MaintenanceManager maintenanceManager;
        final PingRateLimiter rateLimiter;
        EngineState(BrandConfig config,
                    FaviconStore globalFavicon,
                    Map<String, FaviconStore> profileFavicons,
                    ProfileManager profileManager,
                    AnimationEngine animationEngine,
                    FakePlayerEngine fakePlayerEngine,
                    PlaceholderEngine placeholderEngine,
                    MaintenanceManager maintenanceManager,
                    PingRateLimiter rateLimiter) {
            this.config = config;
            this.globalFavicon = globalFavicon;
            this.profileFavicons = profileFavicons;
            this.profileManager = profileManager;
            this.animationEngine = animationEngine;
            this.fakePlayerEngine = fakePlayerEngine;
            this.placeholderEngine = placeholderEngine;
            this.maintenanceManager = maintenanceManager;
            this.rateLimiter = rateLimiter;
        }
        public BrandConfig config() { return config; }
        public FaviconStore globalFavicon() { return globalFavicon; }
        public Map<String, FaviconStore> profileFavicons() { return profileFavicons; }
        public ProfileManager profileManager() { return profileManager; }
        public AnimationEngine animationEngine() { return animationEngine; }
        public FakePlayerEngine fakePlayerEngine() { return fakePlayerEngine; }
        public PlaceholderEngine placeholderEngine() { return placeholderEngine; }
        public MaintenanceManager maintenanceManager() { return maintenanceManager; }
        public PingRateLimiter rateLimiter() { return rateLimiter; }
        /**
         * Get the best applicable favicon for the given profile.
         * Profile-specific favicon overrides the global one.
         */
        public FaviconStore faviconFor(String profileName) {
            FaviconStore ps = profileFavicons.get(profileName);
            if (ps != null && ps.hasFavicon()) return ps;
            if (globalFavicon.hasFavicon()) return globalFavicon;
            return FaviconStore.EMPTY;
        }
        boolean hasAnimations() {
            for (MotdProfile p : config.getProfiles().values()) {
                if (p.getMotdAnimation().isEnabled() && p.getMotdAnimation().hasFrames()) return true;
                if (p.getPlayerCountSettings().getAnimation().isEnabled()) return true;
            }
            return false;
        }
        boolean hasFakePlayerRandom() {
            if (config.getGlobalFakePlayerSettings().isEnabled() &&
                config.getGlobalFakePlayerSettings().getMode() == si.terona.nullbrand.model.FakePlayerMode.RANDOM) return true;
            for (MotdProfile p : config.getProfiles().values()) {
                if (p.getFakePlayerSettings().isEnabled() &&
                    p.getFakePlayerSettings().getMode() == si.terona.nullbrand.model.FakePlayerMode.RANDOM) return true;
            }
            return false;
        }
    }
    // -----------------------------------------------------------------------
    // BrandEngine fields
    // -----------------------------------------------------------------------
    private final AtomicReference<EngineState> stateRef = new AtomicReference<>();
    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> tickTask;
    private volatile ScheduledFuture<?> rotationTask;
    private final LogUtil log;
    private final String platform;
    public BrandEngine(LogUtil log, String platform) {
        this.log = log;
        this.platform = platform;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NullBrand-Engine");
            t.setDaemon(true);
            return t;
        });
    }
    // -----------------------------------------------------------------------
    // Load / Reload
    // -----------------------------------------------------------------------
    /**
     * Load or reload the configuration and rebuild the engine state atomically.
     *
     * @param dataDir     plugin data directory
     * @param classLoader plugin classloader for default resource extraction
     * @return true if load succeeded (or partially succeeded with warnings)
     */
    public synchronized boolean load(Path dataDir, ClassLoader classLoader) {
        log.debug("Starting config load from: " + dataDir);
        ConfigLoader loader = new ConfigLoader();
        BrandConfig config;
        try {
            config = loader.load(dataDir, classLoader);
        } catch (Throwable t) {
            log.error("Fatal error loading config — keeping previous state.", t);
            return false;
        }
        // Print any warnings from config loading
        for (String warning : loader.getWarnings()) {
            log.warn(warning);
        }
        // Update debug flag on logger
        log.setDebug(config.isDebug());
        // Build favicon stores
        Path faviconDir = dataDir.resolve("favicons");
        FaviconStore globalFav = FaviconStore.load(faviconDir, config.getGlobalFaviconName());
        ConcurrentHashMap<String, FaviconStore> profileFavicons = new ConcurrentHashMap<>();
        for (Map.Entry<String, MotdProfile> entry : config.getProfiles().entrySet()) {
            String fn = entry.getValue().getFaviconName();
            if (fn != null && !fn.isEmpty()) {
                profileFavicons.put(entry.getKey(), FaviconStore.load(faviconDir, fn));
            }
        }
        // Build services
        ProfileManager profileManager = new ProfileManager(config);
        AnimationEngine animationEngine = new AnimationEngine();
        FakePlayerEngine fakePlayerEngine = new FakePlayerEngine(config.getGlobalFakePlayerSettings());
        PlaceholderEngine placeholderEngine = new PlaceholderEngine(platform);
        MaintenanceManager maintenanceManager = new MaintenanceManager(config.getMaintenanceSettings().isEnabled());
        PingRateLimiter rateLimiter = new PingRateLimiter(config.getPingProtectionSettings());
        // Register animations
        for (Map.Entry<String, MotdProfile> entry : config.getProfiles().entrySet()) {
            String name = entry.getKey();
            MotdProfile profile = entry.getValue();
            animationEngine.register("motd:" + name, profile.getMotdAnimation());
            animationEngine.register("pc:" + name, profile.getPlayerCountSettings().getAnimation());
        }
        // Build new state
        EngineState newState = new EngineState(config, globalFav,
                java.util.Collections.unmodifiableMap(profileFavicons),
                profileManager, animationEngine, fakePlayerEngine,
                placeholderEngine, maintenanceManager, rateLimiter);
        // Atomically replace state
        stateRef.set(newState);
        // Restart background tasks
        stopBackgroundTasks();
        startBackgroundTasks(newState);
        logStartup(config);
        return true;
    }
    /**
     * Get the current engine state. Never null after first successful load.
     */
    public EngineState state() {
        return stateRef.get();
    }
    /**
     * Shut down the engine and background threads cleanly.
     */
    public void shutdown() {
        stopBackgroundTasks();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2L, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.debug("Engine shut down.");
    }
    // -----------------------------------------------------------------------
    // Background tasks
    // -----------------------------------------------------------------------
    private void stopBackgroundTasks() {
        ScheduledFuture<?> tt = this.tickTask;
        if (tt != null) { tt.cancel(false); this.tickTask = null; }
        ScheduledFuture<?> rt = this.rotationTask;
        if (rt != null) { rt.cancel(false); this.rotationTask = null; }
    }
    private void startBackgroundTasks(EngineState state) {
        // Always tick for animations and fake player random refreshes
        tickTask = scheduler.scheduleAtFixedRate(this::tick, 50L, 50L, TimeUnit.MILLISECONDS);
        // Profile rotation
        if (state.config().getRotationSettings().isEnabled()) {
            long intervalSec = state.config().getRotationSettings().getInterval();
            rotationTask = scheduler.scheduleAtFixedRate(
                    this::rotateProfile, intervalSec, intervalSec, TimeUnit.SECONDS);
            log.debug("Profile rotation enabled, interval: " + intervalSec + "s");
        }
    }
    private void tick() {
        EngineState s = stateRef.get();
        if (s == null) return;
        s.animationEngine.tick();
        s.fakePlayerEngine.tick();
    }
    private void rotateProfile() {
        EngineState s = stateRef.get();
        if (s == null) return;
        s.profileManager.rotateNext();
        log.debug("Rotated to profile: " + s.profileManager.getActiveProfileName());
    }
    // -----------------------------------------------------------------------
    // Startup diagnostics
    // -----------------------------------------------------------------------
    private void logStartup(BrandConfig config) {
        log.info("Loaded " + config.getProfiles().size() + " profile(s).");
        log.info("Active profile: " + config.getActiveProfile());
        if (config.getGlobalFakePlayerSettings().isEnabled()) {
            log.info("Fake players active: mode=" + config.getGlobalFakePlayerSettings().getMode());
        }
        if (config.getMaintenanceSettings().isEnabled()) {
            log.info("Maintenance mode is ENABLED (profile: " + config.getMaintenanceSettings().getProfile() + ")");
        }
        if (config.getRotationSettings().isEnabled()) {
            log.info("Profile rotation: " + config.getRotationSettings().getProfiles().size() +
                     " profiles, interval=" + config.getRotationSettings().getInterval() + "s");
        }
        long animCount = config.getProfiles().values().stream()
                .filter(p -> p.getMotdAnimation().isEnabled()).count();
        if (animCount > 0) log.info("Animated profiles: " + animCount);
    }
}
