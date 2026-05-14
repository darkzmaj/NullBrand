package si.terona.nullbrand.animation;
import si.terona.nullbrand.model.AnimationSettings;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Manages all active text animations by maintaining per-animation tick counters.
 * Frame advancement is driven externally by the BrandEngine scheduler (every 50 ms = 1 tick).
 * All operations are lock-free and thread-safe.
 */
public final class AnimationEngine {
    /** State for a single animation sequence. */
    private static final class AnimState {
        final int frameCount;
        final int interval;
        final AtomicInteger frameIndex = new AtomicInteger(0);
        final AtomicLong ticksSinceAdvance = new AtomicLong(0);
        AnimState(int frameCount, int interval) {
            this.frameCount = frameCount;
            this.interval = interval;
        }
        void tick() {
            long ticks = ticksSinceAdvance.incrementAndGet();
            if (ticks >= interval) {
                ticksSinceAdvance.set(0);
                frameIndex.updateAndGet(i -> (i + 1) % frameCount);
            }
        }
        int currentFrame() {
            return frameIndex.get();
        }
    }
    private final ConcurrentHashMap<String, AnimState> states = new ConcurrentHashMap<>();
    /**
     * Register an animation by a unique key.
     * Safe to call multiple times — re-registers reset the frame index.
     */
    public void register(String key, AnimationSettings settings) {
        if (settings == null || !settings.isEnabled() || settings.getFrames().isEmpty()) {
            states.remove(key);
            return;
        }
        states.put(key, new AnimState(settings.getFrames().size(), settings.getInterval()));
    }
    /**
     * Advance all registered animations by one tick (50 ms).
     * Called by the BrandEngine scheduler thread.
     */
    public void tick() {
        for (AnimState state : states.values()) {
            state.tick();
        }
    }
    /**
     * Get the current frame text for a registered animation.
     *
     * @param key    animation key
     * @param frames the frames list (from AnimationSettings)
     * @return current frame string, or null if not registered / no frames
     */
    public String currentFrame(String key, List<String> frames) {
        AnimState state = states.get(key);
        if (state == null || frames == null || frames.isEmpty()) return null;
        int idx = state.currentFrame();
        if (idx < 0 || idx >= frames.size()) return frames.get(0);
        return frames.get(idx);
    }
    /** Remove all registered animations (called on reload/shutdown). */
    public void clear() {
        states.clear();
    }
}
