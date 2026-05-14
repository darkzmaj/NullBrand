package si.terona.nullbrand.model;
import java.util.Collections;
import java.util.List;
/**
 * Immutable snapshot of a fully-loaded MOTD profile.
 * Raw strings preserve MiniMessage / legacy formatting tags for runtime processing.
 */
public final class MotdProfile {
    private final String name;
    /** Raw (unresolved) MOTD lines — placeholders and formatting preserved. */
    private final List<String> lines;
    /** Raw hover lines. */
    private final List<String> hoverLines;
    private final boolean hoverEnabled;
    private final boolean hoverRandom;
    /** MOTD animation. When enabled, lines are replaced by animation frames. */
    private final AnimationSettings motdAnimation;
    /** Profile-level fake-player override (inherits global if DISABLED). */
    private final FakePlayerSettings fakePlayerSettings;
    /** Profile-level max-players override (inherits global if DISABLED). */
    private final MaxPlayersSettings maxPlayersSettings;
    /** Profile-level playercount display settings. */
    private final PlayerCountSettings playerCountSettings;
    /** Profile-specific favicon name (null = inherit global). */
    private final String faviconName;
    /** Pre-joined static MOTD (only set when animation is disabled). */
    private final String joinedStatic;
    public MotdProfile(String name,
                       List<String> lines,
                       List<String> hoverLines,
                       boolean hoverEnabled,
                       boolean hoverRandom,
                       AnimationSettings motdAnimation,
                       FakePlayerSettings fakePlayerSettings,
                       MaxPlayersSettings maxPlayersSettings,
                       PlayerCountSettings playerCountSettings,
                       String faviconName) {
        this.name = name;
        this.lines = Collections.unmodifiableList(lines);
        this.hoverLines = Collections.unmodifiableList(hoverLines);
        this.hoverEnabled = hoverEnabled;
        this.hoverRandom = hoverRandom;
        this.motdAnimation = motdAnimation != null ? motdAnimation : AnimationSettings.DISABLED;
        this.fakePlayerSettings = fakePlayerSettings != null ? fakePlayerSettings : FakePlayerSettings.DISABLED;
        this.maxPlayersSettings = maxPlayersSettings != null ? maxPlayersSettings : MaxPlayersSettings.DISABLED;
        this.playerCountSettings = playerCountSettings != null ? playerCountSettings : PlayerCountSettings.DISABLED;
        this.faviconName = faviconName;
        this.joinedStatic = String.join("\n", lines);
    }
    /** Legacy constructor for backward compatibility (global fallback profile). */
    public MotdProfile(List<String> lines, List<String> hoverLines, boolean hoverEnabled) {
        this("_legacy_", lines, hoverLines, hoverEnabled, false,
             AnimationSettings.DISABLED, FakePlayerSettings.DISABLED,
             MaxPlayersSettings.DISABLED, PlayerCountSettings.DISABLED, null);
    }
    public String getName() { return name; }
    public List<String> getLines() { return lines; }
    public List<String> getHoverLines() { return hoverLines; }
    public boolean isHoverEnabled() { return hoverEnabled; }
    public boolean isHoverRandom() { return hoverRandom; }
    public AnimationSettings getMotdAnimation() { return motdAnimation; }
    public FakePlayerSettings getFakePlayerSettings() { return fakePlayerSettings; }
    public MaxPlayersSettings getMaxPlayersSettings() { return maxPlayersSettings; }
    public PlayerCountSettings getPlayerCountSettings() { return playerCountSettings; }
    public String getFaviconName() { return faviconName; }
    /** Pre-joined MOTD text for static (non-animated) profiles. */
    public String getJoined() { return joinedStatic; }
}
