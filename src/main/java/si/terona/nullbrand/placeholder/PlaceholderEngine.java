package si.terona.nullbrand.placeholder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * Fast, recursive-safe placeholder resolver.
 * All placeholder values are passed in via {@link PingContext} at ping time with no global state.
 */
public final class PlaceholderEngine {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final String platform;
    public PlaceholderEngine(String platform) {
        this.platform = platform;
    }
    /**
     * Resolve all known placeholders in the given text.
     *
     * @param text        raw text (may contain {@code %placeholder%} tokens)
     * @param context     current ping context
     * @return text with all placeholders replaced
     */
    public String resolve(String text, PingContext context) {
        if (text == null || text.isEmpty() || !text.contains("%")) return text;
        LocalDateTime now = LocalDateTime.now();
        return text
                .replace("%online%", String.valueOf(context.realOnline()))
                .replace("%max_players%", String.valueOf(context.realMax()))
                .replace("%fake_online%", String.valueOf(context.fakeOnline()))
                .replace("%display_online%", String.valueOf(context.displayOnline()))
                .replace("%display_max%", String.valueOf(context.displayMax()))
                .replace("%motd_name%", safe(context.profileName()))
                .replace("%proxy%", platform)
                .replace("%platform%", platform)
                .replace("%time%", now.format(TIME_FMT))
                .replace("%date%", now.format(DATE_FMT))
                .replace("%newline%", "\n")
                .replace("%profile%", safe(context.profileName()))
                .replace("%maintenance%", String.valueOf(context.maintenance()))
                .replace("%fakeplayers%", context.fakePlayersInfo());
    }
    private static String safe(String s) {
        return s == null ? "" : s;
    }
    /**
     * Immutable snapshot of all data available during a single ping event.
     */
    public record PingContext(
            int realOnline,
            int realMax,
            int fakeOnline,
            int displayOnline,
            int displayMax,
            String profileName,
            boolean maintenance,
            String fakePlayersInfo
    ) {}
}
