package si.terona.nullbrand.model;
import java.util.Collections;
import java.util.List;
/** Immutable configuration snapshot for a text animation. */
public final class AnimationSettings {
    public static final AnimationSettings DISABLED =
            new AnimationSettings(false, 20, Collections.emptyList());
    private final boolean enabled;
    private final int interval;
    private final List<String> frames;
    public AnimationSettings(boolean enabled, int interval, List<String> frames) {
        this.enabled = enabled;
        this.interval = Math.max(1, interval);
        this.frames = Collections.unmodifiableList(frames);
    }
    public boolean isEnabled() { return enabled; }
    public int getInterval() { return interval; }
    public List<String> getFrames() { return frames; }
    public boolean hasFrames() { return !frames.isEmpty(); }
}
