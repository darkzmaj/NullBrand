package si.terona.nullbrand.model;
/** Immutable configuration snapshot for custom player-count text display. */
public final class PlayerCountSettings {
    public static final PlayerCountSettings DISABLED =
            new PlayerCountSettings(false, false, "", AnimationSettings.DISABLED);
    private final boolean enabled;
    private final boolean textEnabled;
    private final String textValue;
    private final AnimationSettings animation;
    public PlayerCountSettings(boolean enabled, boolean textEnabled, String textValue,
                                AnimationSettings animation) {
        this.enabled = enabled;
        this.textEnabled = textEnabled;
        this.textValue = textValue == null ? "" : textValue;
        this.animation = animation == null ? AnimationSettings.DISABLED : animation;
    }
    public boolean isEnabled() { return enabled; }
    public boolean isTextEnabled() { return textEnabled; }
    public String getTextValue() { return textValue; }
    public AnimationSettings getAnimation() { return animation; }
}
