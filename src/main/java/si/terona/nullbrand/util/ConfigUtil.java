package si.terona.nullbrand.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import si.terona.nullbrand.model.MotdProfile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public final class ConfigUtil {

    private final Path dataDirectory;
    private final Path faviconDirectory;
    private volatile Path configPath;
    private volatile boolean debugEnabled = false;
    private volatile boolean motdEnabled = true;
    private volatile String activeProfileName = "default";
    private volatile Map<String, MotdProfile> profiles = new HashMap<>();
    private volatile String faviconName = null;
    private volatile String cachedFaviconBase64 = null;
    private volatile Object cachedFaviconVelocity = null;
    private volatile Object cachedFaviconBungee = null;
    private volatile Object cachedFaviconSpigot = null;

    public ConfigUtil(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve("config.yml");
        this.faviconDirectory = dataDirectory.resolve("favicons");
    }

    public synchronized void load(ClassLoader classLoader) throws IOException {
        ensureDirectories();
        ensureDefault(classLoader);
        byte[] bytes = Files.readAllBytes(configPath);
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        List<String> lines = Arrays.asList(content.split("\\R", -1));
        parse(lines);
        loadFavicon();
    }

    private void ensureDirectories() throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }
        if (!Files.exists(faviconDirectory)) {
            Files.createDirectories(faviconDirectory);
        }
    }

    private void loadFavicon() {
        if (faviconName == null || faviconName.isEmpty()) {
            cachedFaviconBase64 = null;
            cachedFaviconVelocity = null;
            cachedFaviconBungee = null;
            cachedFaviconSpigot = null;
            return;
        }

        Path faviconPath = faviconDirectory.resolve(faviconName);
        if (!Files.exists(faviconPath)) {
            cachedFaviconBase64 = null;
            cachedFaviconVelocity = null;
            cachedFaviconBungee = null;
            cachedFaviconSpigot = null;
            return;
        }

        try {
            BufferedImage image = ImageIO.read(faviconPath.toFile());
            if (image == null) {
                if (debugEnabled) {
                    System.out.println("[NullBrand] Failed to load favicon (invalid image): " + faviconName);
                }
                return;
            }

            if (image.getWidth() != 64 || image.getHeight() != 64) {
                if (debugEnabled) {
                    System.out.println("[NullBrand] Favicon must be 64x64! Current: " + image.getWidth() + "x" + image.getHeight());
                }
                return;
            }

            // Proxy Servers (Velocity, BungeeCord, Waterfall) require Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            this.cachedFaviconBase64 = "data:image/png;base64," + base64;

            if (debugEnabled) {
                System.out.println("[NullBrand] Loaded favicon: " + faviconName);
            }

            // Platform specific objects
            try {
                if (isClassAvailable("com.velocitypowered.api.util.Favicon")) {
                    this.cachedFaviconVelocity = com.velocitypowered.api.util.Favicon.create(image);
                }
            } catch (Throwable ignored) {}

            try {
                if (isClassAvailable("net.md_5.bungee.api.Favicon")) {
                    this.cachedFaviconBungee = net.md_5.bungee.api.Favicon.create(this.cachedFaviconBase64);
                }
            } catch (Throwable ignored) {}

            try {
                if (isClassAvailable("org.bukkit.Bukkit")) {
                    this.cachedFaviconSpigot = org.bukkit.Bukkit.loadServerIcon(image);
                }
            } catch (Throwable ignored) {}

        } catch (Throwable e) {
            if (debugEnabled) {
                System.out.println("[NullBrand] Error loading favicon: " + e.getMessage());
            }
        }
    }

    private boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public MotdProfile getActiveProfile() {
        MotdProfile profile = profiles.get(activeProfileName);
        if (profile == null) {
            if (debugEnabled && !activeProfileName.equalsIgnoreCase("global")) {
                System.out.println("[NullBrand] Profile '" + activeProfileName + "' not found, falling back to 'global'");
            }
            return profiles.getOrDefault("global", new MotdProfile(List.of("&5Null&dBrand"), List.of(), false));
        }
        return profile;
    }

    public String getFaviconBase64() {
        return cachedFaviconBase64;
    }

    public Object getCachedFaviconVelocity() {
        return cachedFaviconVelocity;
    }

    public Object getCachedFaviconBungee() {
        return cachedFaviconBungee;
    }

    public Object getCachedFaviconSpigot() {
        return cachedFaviconSpigot;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isMotdEnabled() {
        return motdEnabled;
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
                "# ---------------------------------------------------\n"
                        + "# NullBrand Configuration\n"
                        + "# Lightweight MOTD system\n"
                        + "# ---------------------------------------------------\n"
                        + "\n"
                        + "# Enables debug logging\n"
                        + "debug: false\n"
                        + "\n"
                        + "\n"
                        + "# ---------------------------------------------------\n"
                        + "# MOTD SETTINGS\n"
                        + "# ---------------------------------------------------\n"
                        + "motd:\n"
                        + "\n"
                        + "  # Enable MOTD control\n"
                        + "  enabled: true\n"
                        + "\n"
                        + "  # Active profile\n"
                        + "  profile: \"default\"\n"
                        + "\n"
                        + "  # Favicon file inside /plugins/NullBrand/favicons/\n"
                        + "  favicon: \"logo.png\"\n"
                        + "\n"
                        + "\n"
                        + "\n"
                        + "# ---------------------------------------------------\n"
                        + "# MOTD PROFILES\n"
                        + "# ---------------------------------------------------\n"
                        + "# NOTE: Hover (Works on Proxy Only)\n"
                        + "\n"
                        + "profiles:\n"
                        + "\n"
                        + "  # Global fallback profile\n"
                        + "  global:\n"
                        + "    lines:\n"
                        + "      - \"&7Welcome to our server\"\n"
                        + "      - \"&fHave fun!\"\n"
                        + "\n"
                        + "  # Default server MOTD\n"
                        + "  default:\n"
                        + "    lines:\n"
                        + "      - \"&5Null&dBrand\"\n"
                        + "      - \"&7This is line 2\"\n"
                        + "    hover:\n"
                        + "      enabled: false\n"
                        + "      lines:\n"
                        + "        - \"&7Welcome to &5Null&dBrand\"\n"
                        + "        - \"&7Running powerful infrastructure\"\n"
                        + "\n"
                        + "  # Example event profile\n"
                        + "  event:\n"
                        + "    lines:\n"
                        + "      - \"&6Special Event Live!\"\n"
                        + "      - \"&eJoin today!\"\n"
                        + "    hover:\n"
                        + "      enabled: true\n"
                        + "      lines:\n"
                        + "        - \"&6Limited time event\"\n"
                        + "        - \"&eDon't miss it!\"\n";
        Files.writeString(configPath, defaultConfig, StandardCharsets.UTF_8);
    }

    private void parse(List<String> lines) {
        boolean debugEnabled = false;
        boolean motdEnabled = true;
        String activeProfile = "default";
        String favicon = "";

        Map<String, List<String>> profileLines = new HashMap<>();
        Map<String, List<String>> profileHoverLines = new HashMap<>();
        Map<String, Boolean> profileHoverEnabled = new HashMap<>();

        String currentProfile = null;
        boolean inMotd = false;
        boolean inProfiles = false;
        boolean inProfileHover = false;
        boolean readingProfileLines = false;
        boolean readingProfileHoverLines = false;

        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indent = countLeadingSpaces(raw);

            if (indent == 0) {
                if (trimmed.contains(":")) {
                    int split = trimmed.indexOf(':');
                    String key = trimmed.substring(0, split).trim().toLowerCase();
                    String value = unquote(trimmed.substring(split + 1).trim());
                    if (key.equals("debug")) {
                        debugEnabled = parseBoolean(value, debugEnabled);
                    }
                }
                if (trimmed.endsWith(":")) {
                    String section = trimmed.substring(0, trimmed.length() - 1).toLowerCase();
                    inMotd = section.equals("motd");
                    inProfiles = section.equals("profiles");
                    currentProfile = null;
                    continue;
                }
            }

            if (inMotd && indent == 2 && trimmed.contains(":")) {
                int split = trimmed.indexOf(':');
                String key = trimmed.substring(0, split).trim().toLowerCase();
                String value = unquote(trimmed.substring(split + 1).trim());
                if (key.equals("enabled")) motdEnabled = parseBoolean(value, motdEnabled);
                else if (key.equals("profile")) activeProfile = value;
                else if (key.equals("favicon")) favicon = value;
                continue;
            }

            if (inProfiles) {
                if (indent == 2 && trimmed.endsWith(":")) {
                    currentProfile = trimmed.substring(0, trimmed.length() - 1).trim();
                    profileLines.put(currentProfile, new ArrayList<>());
                    profileHoverLines.put(currentProfile, new ArrayList<>());
                    profileHoverEnabled.put(currentProfile, false);
                    readingProfileLines = false;
                    inProfileHover = false;
                    readingProfileHoverLines = false;
                    continue;
                }

                if (currentProfile != null) {
                    if (indent == 4) {
                        if (trimmed.endsWith(":")) {
                            String sub = trimmed.substring(0, trimmed.length() - 1).trim().toLowerCase();
                            if (sub.equals("lines")) {
                                readingProfileLines = true;
                                readingProfileHoverLines = false;
                                inProfileHover = false;
                                continue;
                            } else if (sub.equals("hover")) {
                                inProfileHover = true;
                                readingProfileLines = false;
                                readingProfileHoverLines = false;
                                continue;
                            }
                        }
                    }

                    if (readingProfileLines && indent >= 4 && trimmed.startsWith("-")) {
                        profileLines.get(currentProfile).add(unquote(trimmed.substring(1).trim()));
                        continue;
                    }

                    if (inProfileHover && indent == 6) {
                        if (trimmed.contains(":")) {
                            int split = trimmed.indexOf(':');
                            String key = trimmed.substring(0, split).trim().toLowerCase();
                            String value = unquote(trimmed.substring(split + 1).trim());
                            if (key.equals("enabled")) {
                                profileHoverEnabled.put(currentProfile, parseBoolean(value, false));
                            } else if (key.equals("lines")) {
                                readingProfileHoverLines = true;
                            }
                            continue;
                        }
                    }

                    if (readingProfileHoverLines && indent >= 6 && trimmed.startsWith("-")) {
                        profileHoverLines.get(currentProfile).add(unquote(trimmed.substring(1).trim()));
                        continue;
                    }
                }
            }
        }

        // Processing
        Map<String, MotdProfile> parsedProfiles = new HashMap<>();
        for (String name : profileLines.keySet()) {
            List<String> lines_list = profileLines.get(name);
            List<String> hover_list = profileHoverLines.get(name);
            boolean hover_enabled = profileHoverEnabled.getOrDefault(name, false);

            List<String> coloredLines = new ArrayList<>();
            for (String s : lines_list) coloredLines.add(ColorUtil.translate(s));

            List<String> coloredHover = new ArrayList<>();
            for (String s : hover_list) coloredHover.add(ColorUtil.translateHover(s));

            parsedProfiles.put(name, new MotdProfile(coloredLines, coloredHover, hover_enabled));
        }

        this.debugEnabled = debugEnabled;
        this.motdEnabled = motdEnabled;
        this.activeProfileName = activeProfile;
        this.profiles = parsedProfiles;
        this.faviconName = favicon;

        if (debugEnabled) {
            System.out.println("[NullBrand] Debug mode enabled.");
            System.out.println("[NullBrand] Platform detected: " + detectPlatform());
            System.out.println("[NullBrand] Loaded " + profiles.size() + " MOTD profiles.");
            System.out.println("[NullBrand] Active profile: " + activeProfileName);
            MotdProfile active = getActiveProfile();
            System.out.println("[NullBrand] Hover lines count: " + active.getHoverLines().size());
        }
    }

    private String detectPlatform() {
        if (isClassAvailable("com.velocitypowered.api.proxy.ProxyServer")) return "Velocity";
        if (isClassAvailable("net.md_5.bungee.api.ProxyServer")) return "BungeeCord/Waterfall";
        if (isClassAvailable("org.bukkit.Bukkit")) return "Bukkit/Spigot/Paper";
        return "Unknown";
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
