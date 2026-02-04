package si.terona.nullbrand.util;

public final class ColorUtil {

    private static final char SECTION = '\u00A7';

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
