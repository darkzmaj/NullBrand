package si.terona.nullbrand.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigUtil {

    private final Path dataDirectory;
    private final Path configPath;
    private volatile boolean brandingEnabled = true;
    private volatile String brandingTextColored = "NullBrand";
    private volatile boolean motdEnabled = true;
    private volatile List<String> motdLinesColored = List.of("NullBrand");

    public ConfigUtil(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve("config.yml");
    }

    public synchronized void load(ClassLoader classLoader) throws IOException {
        ensureDefault(classLoader);
        parse(Files.readAllLines(configPath, StandardCharsets.UTF_8));
    }

    public boolean isBrandingEnabled() {
        return brandingEnabled;
    }

    public String getBrandingTextColored() {
        return brandingTextColored;
    }

    public boolean isMotdEnabled() {
        return motdEnabled;
    }

    public List<String> getMotdLinesColored() {
        return motdLinesColored;
    }

    public String getMotdJoinedColored() {
        return String.join("\n", motdLinesColored);
    }

    private void ensureDefault(ClassLoader classLoader) throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }
        if (!Files.exists(configPath)) {
            try (InputStream in = classLoader.getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                    return;
                }
            }
            writeDefault();
        }
    }

    private void writeDefault() throws IOException {
        String defaultConfig =
                "branding:\n"
                        + "  enabled: true\n"
                        + "  text: \"<#6affff>NullBrand</#6affff> &7| &fInvisible Proxy\"\n"
                        + "\n"
                        + "motd:\n"
                        + "  enabled: true\n"
                        + "  lines:\n"
                        + "    - \"<#6affff>NullBrand</#6affff>\"\n"
                        + "    - \"&7Secure \\u2022 Fast \\u2022 Invisible\"\n";
        Files.writeString(configPath, defaultConfig, StandardCharsets.UTF_8);
    }

    private void parse(List<String> lines) {
        boolean brandingEnabled = true;
        String brandingText = "NullBrand";
        boolean motdEnabled = true;
        String motdText = null;
        List<String> motdLines = new ArrayList<>();
        boolean inBranding = false;
        boolean inMotd = false;
        boolean readingMotdLines = false;

        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indent = countLeadingSpaces(raw);
            if (indent == 0 && trimmed.endsWith(":")) {
                String section = trimmed.substring(0, trimmed.length() - 1).toLowerCase();
                inBranding = section.equals("branding");
                inMotd = section.equals("motd");
                readingMotdLines = false;
                continue;
            }
            if (indent == 2 && trimmed.contains(":")) {
                int split = trimmed.indexOf(':');
                String key = trimmed.substring(0, split).trim().toLowerCase();
                String value = trimmed.substring(split + 1).trim();
                value = unquote(value);
                if (inBranding) {
                    if (key.equals("enabled")) {
                        brandingEnabled = parseBoolean(value, brandingEnabled);
                    } else if (key.equals("text")) {
                        brandingText = value;
                    }
                } else if (inMotd) {
                    if (key.equals("enabled")) {
                        motdEnabled = parseBoolean(value, motdEnabled);
                    } else if (key.equals("text")) {
                        motdText = value;
                    } else if (key.equals("lines")) {
                        readingMotdLines = true;
                        motdLines.clear();
                    } else {
                        readingMotdLines = false;
                    }
                }
                if (!key.equals("lines")) {
                    readingMotdLines = false;
                }
                continue;
            }
            if (inMotd && readingMotdLines && trimmed.startsWith("-")) {
                String item = trimmed.substring(1).trim();
                motdLines.add(unquote(item));
            }
        }

        if (motdLines.isEmpty()) {
            if (motdText != null) {
                motdLines.add(motdText);
            } else {
                motdLines.add("");
            }
        }

        List<String> motdLinesColored = new ArrayList<>(motdLines.size());
        for (String line : motdLines) {
            motdLinesColored.add(ColorUtil.translate(line));
        }

        this.brandingEnabled = brandingEnabled;
        this.brandingTextColored = ColorUtil.translate(brandingText);
        this.motdEnabled = motdEnabled;
        this.motdLinesColored = Collections.unmodifiableList(motdLinesColored);
    }

    private static int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String unquote(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static boolean parseBoolean(String value, boolean def) {
        if (value == null || value.isEmpty()) {
            return def;
        }
        String v = value.toLowerCase();
        if (v.equals("true") || v.equals("yes") || v.equals("on")) {
            return true;
        }
        if (v.equals("false") || v.equals("no") || v.equals("off")) {
            return false;
        }
        return def;
    }
}
