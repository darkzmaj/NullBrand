package si.terona.nullbrand.render;
import si.terona.nullbrand.util.ColorUtil;
/**
 * Converts raw text (with legacy/hex/MiniMessage tags) into platform-consumable outputs.
 *
 * <p>Two output paths:
 * <ul>
 *   <li><b>Legacy</b> – section-sign colored string, for BungeeCord and Spigot MOTD.</li>
 *   <li><b>Component</b> – Adventure {@code Component}, for Velocity and Paper.</li>
 * </ul>
 *
 * <p>MiniMessage is used when the Adventure MiniMessage class is present on the classpath
 * (Velocity + Paper). Legacy text tags are always translated as a fallback.
 */
public final class MotdRenderer {
    private static final boolean MINI_MESSAGE_AVAILABLE;
    static {
        boolean available = false;
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            available = true;
        } catch (ClassNotFoundException ignored) {}
        MINI_MESSAGE_AVAILABLE = available;
    }
    // Lazy holder prevents loading Adventure classes on BungeeCord.
    private static final class Adventure {
    private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY_SERIALIZER =
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                    .character('\u00A7')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();
    }
    private MotdRenderer() {}
    /**
     * Convert raw text to a section-sign legacy string.
     * Handles: {@code &x}, {@code &#RRGGBB}, {@code <#RRGGBB>} and MiniMessage gradient/rainbow
     * (stripped on platforms that do not support them).
     *
     * @param raw raw text from config
     * @return legacy colored string suitable for BungeeCord/Spigot setDescription/setMotd
     */
    public static String toLegacy(String raw) {
        if (raw == null) return "";
        if (MINI_MESSAGE_AVAILABLE) return Adventure.LEGACY_SERIALIZER.serialize(toComponent(raw));
        return ColorUtil.translate(raw);
    }
    /**
     * Convert raw text to an Adventure {@link net.kyori.adventure.text.Component}.
     * Uses MiniMessage when available; falls back to legacy deserialization.
     *
     * @param raw raw text from config
     * @return Adventure Component
     */
    public static net.kyori.adventure.text.Component toComponent(String raw) {
        if (raw == null || raw.isEmpty()) {
            return net.kyori.adventure.text.Component.empty();
        }
        try {
            if (MINI_MESSAGE_AVAILABLE && (containsMiniMessageTags(raw) || containsHexTags(raw))) {
                // Pre-process &#RRGGBB and <#RRGGBB> into MiniMessage format
                String miniText = preprocessToMiniMessage(raw);
                return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(miniText);
            }
        } catch (Throwable ignored) {
            // Fall through to legacy
        }
        return Adventure.LEGACY_SERIALIZER.deserialize(ColorUtil.translate(raw));
    }
    /**
     * Convert raw text to an Adventure Component through MiniMessage only.
     * Use when the raw text already uses pure MiniMessage syntax.
     */
    public static net.kyori.adventure.text.Component toComponentMiniMessage(String raw) {
        if (!MINI_MESSAGE_AVAILABLE) return toComponent(raw);
        try {
            return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(raw);
        } catch (Throwable e) {
            return toComponent(raw);
        }
    }
    /** Convert a multi-line raw MOTD to a single Adventure Component with newlines. */
    public static net.kyori.adventure.text.Component toMotdComponent(String rawJoined) {
        if (rawJoined == null || rawJoined.isEmpty()) return net.kyori.adventure.text.Component.empty();
        // Each line separated by \n
        String[] parts = rawJoined.split("\n", -1);
        net.kyori.adventure.text.Component result = toComponent(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = result.append(net.kyori.adventure.text.Component.newline())
                           .append(toComponent(parts[i]));
        }
        return result;
    }
    private static boolean containsMiniMessageTags(String s) {
        return s.indexOf('<') >= 0 && s.indexOf('>') > s.indexOf('<');
    }
    private static boolean containsHexTags(String s) {
        return s.contains("<#") || s.contains("&#");
    }
    /**
     * Convert &#RRGGBB → <#RRGGBB> so MiniMessage can parse it.
     */
    private static String preprocessToMiniMessage(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '&' && i + 7 < input.length() && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                if (isHex(hex)) {
                    sb.append('<').append('#').append(hex).append('>');
                    i += 8;
                    continue;
                }
            }
            if ((c == '&' || c == '\u00a7') && i + 1 < input.length()) {
                String codes = "0123456789abcdefklmnor";
                String[] tags = {"black","dark_blue","dark_green","dark_aqua","dark_red","dark_purple","gold","gray","dark_gray","blue","green","aqua","red","light_purple","yellow","white","obfuscated","bold","strikethrough","underlined","italic","reset"};
                int code = codes.indexOf(Character.toLowerCase(input.charAt(i + 1)));
                if (code >= 0) { sb.append('<').append(tags[code]).append('>'); i += 2; continue; }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) return false;
        }
        return s.length() == 6;
    }
}
