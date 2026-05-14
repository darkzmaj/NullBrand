package si.terona.nullbrand.util;

public final class ColorUtil {

    private static final char SECTION = '\u00A7';
    private static final int HEX_RGB_LEN = 6;

    private ColorUtil() {
    }

    public static String translate(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String value = replaceHexTags(input);
        value = replaceHexCodes(value);
        return translateLegacy(value);
    }

    /**
     * Hover text must not support hex colors. We strip common hex syntaxes and only translate legacy palette
     * colors (&0 - &f) to section sign formatting.
     */
    public static String translateHover(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String stripped = stripHexEverywhere(input);
        return translatePaletteColors(stripped);
    }

    private static String replaceHexTags(String input) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '<' && i + 8 < input.length() && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                if (isHex(hex) && input.charAt(i + 8) == '>') {
                    out.append(toLegacyHex(hex));
                    i += 9;
                    continue;
                }
            }
            if (c == '<' && i + 3 < input.length() && input.charAt(i + 1) == '/' && input.charAt(i + 2) == '#') {
                int end = input.indexOf('>', i + 3);
                if (end != -1) {
                    out.append(SECTION).append('r');
                    i = end + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String replaceHexCodes(String input) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '&' && i + 7 < input.length() && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                if (isHex(hex)) {
                    out.append(toLegacyHex(hex));
                    i += 8;
                    continue;
                }
            }
            if (c == '#' && i + 6 < input.length()) {
                String hex = input.substring(i + 1, i + 7);
                if (isHex(hex)) {
                    out.append(toLegacyHex(hex));
                    i += 7;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String translateLegacy(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char code = input.charAt(i + 1);
                if (isLegacy(code)) {
                    out.append(SECTION).append(Character.toLowerCase(code));
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String translatePaletteColors(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char code = input.charAt(i + 1);
                if (isPaletteColor(code)) {
                    out.append(SECTION).append(Character.toLowerCase(code));
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String stripHexEverywhere(String input) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            // &#RRGGBB
            if (c == '&' && i + 1 + 1 + HEX_RGB_LEN - 1 < input.length() && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 2 + HEX_RGB_LEN);
                if (isHex(hex)) {
                    i += 2 + HEX_RGB_LEN;
                    continue;
                }
            }

            // #RRGGBB
            if (c == '#' && i + HEX_RGB_LEN < input.length()) {
                String hex = input.substring(i + 1, i + 1 + HEX_RGB_LEN);
                if (isHex(hex)) {
                    i += 1 + HEX_RGB_LEN;
                    continue;
                }
            }

            // <#RRGGBB> and </#...>
            if (c == '<') {
                if (i + 1 + 1 + HEX_RGB_LEN < input.length() && input.charAt(i + 1) == '#') {
                    String hex = input.substring(i + 2, i + 2 + HEX_RGB_LEN);
                    if (isHex(hex) && input.charAt(i + 2 + HEX_RGB_LEN) == '>') {
                        i += 3 + HEX_RGB_LEN;
                        continue;
                    }
                }
                if (i + 3 < input.length() && input.charAt(i + 1) == '/' && input.charAt(i + 2) == '#') {
                    int end = input.indexOf('>', i + 3);
                    if (end != -1) {
                        i = end + 1;
                        continue;
                    }
                }
            }

            // §x§R§R§G§G§B§B
            if (c == SECTION && i + 13 < input.length() && (input.charAt(i + 1) == 'x' || input.charAt(i + 1) == 'X')) {
                boolean ok = true;
                for (int j = 0; j < 6; j++) {
                    int base = i + 2 + (j * 2);
                    if (input.charAt(base) != SECTION || !isHexChar(input.charAt(base + 1))) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    i += 14;
                    continue;
                }
            }

            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static String toLegacyHex(String hex) {
        StringBuilder out = new StringBuilder(14);
        out.append(SECTION).append('x');
        for (int i = 0; i < hex.length(); i++) {
            out.append(SECTION).append(Character.toLowerCase(hex.charAt(i)));
        }
        return out.toString();
    }

    private static boolean isHex(String hex) {
        if (hex.length() != 6) {
            return false;
        }
        for (int i = 0; i < hex.length(); i++) {
            if (!isHexChar(hex.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private static boolean isPaletteColor(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    private static boolean isLegacy(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F')
                || (c >= 'k' && c <= 'o')
                || (c >= 'K' && c <= 'O')
                || c == 'r'
                || c == 'R';
    }
}
