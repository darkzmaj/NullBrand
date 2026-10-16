package si.terona.nullbrand.config;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import si.terona.nullbrand.model.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
/**
 * SnakeYAML-backed configuration loader.
 * Builds an immutable {@link BrandConfig} from config.yml.
 * Never throws — errors are captured and surfaced via {@link #getWarnings()}.
 */
public final class ConfigLoader {
    private final List<String> warnings = new ArrayList<>();
    public BrandConfig load(Path dataDir, ClassLoader classLoader) {
        Path configPath = dataDir.resolve("config.yml");
        ensureDirectories(dataDir);
        ensureDefaultConfig(configPath, classLoader);
        Map<String, Object> root = parseYaml(configPath);
        if (root == null) {
            warnings.add("config.yml is empty or malformed — using all defaults.");
            root = Collections.emptyMap();
        }
        boolean debug = bool(root, "debug", false);
        Map<String, Object> motdSection = section(root, "motd");
        boolean motdEnabled = bool(motdSection, "enabled", true);
        String activeProfile = str(motdSection, "profile", "default");
        String globalFavicon = str(motdSection, "favicon", "");
        MaintenanceSettings maintenance = loadMaintenance(section(root, "maintenance"));
        FakePlayerSettings globalFake = loadFakePlayer(section(root, "fakeplayers"), "global");
        MaxPlayersSettings globalMax = loadMaxPlayers(section(root, "maxplayers"), "global");
        PlayerCountSettings globalPlayerCount = loadPlayerCount(section(root, "playercount"), "global");
        RotationSettings rotation = loadRotation(section(root, "rotation"));
        PingProtectionSettings pingProt = loadPingProtection(section(root, "pingprotection"));
        LangSettings lang = loadLang(section(root, "lang"));
        Map<String, MotdProfile> profiles = loadProfiles(section(root, "profiles"),
                globalFake, globalMax, globalPlayerCount);
        // Ensure global fallback exists
        if (!profiles.containsKey("global")) {
            profiles.put("global", defaultGlobalProfile());
        }
        return new BrandConfig(debug, motdEnabled, activeProfile, globalFavicon,
                maintenance, globalFake, globalMax, globalPlayerCount,
                rotation, pingProt, lang, Collections.unmodifiableMap(profiles));
    }
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
    // -----------------------------------------------------------------------
    // Section loaders
    // -----------------------------------------------------------------------
    private MaintenanceSettings loadMaintenance(Map<String, Object> s) {
        boolean enabled = bool(s, "enabled", false);
        String profile = str(s, "profile", "maintenance");
        return new MaintenanceSettings(enabled, profile);
    }
    private FakePlayerSettings loadFakePlayer(Map<String, Object> s, String context) {
        boolean enabled = bool(s, "enabled", false);
        if (!enabled) return FakePlayerSettings.DISABLED;
        String modeStr = str(s, "mode", "STATIC").toUpperCase(Locale.ROOT);
        FakePlayerMode mode;
        try { mode = FakePlayerMode.valueOf(modeStr); }
        catch (IllegalArgumentException e) {
            warnings.add("Invalid fakeplayers.mode '" + modeStr + "' in " + context + " — defaulting to STATIC.");
            mode = FakePlayerMode.STATIC;
        }
        int amount = integer(s, "amount", 250);
        int min = integer(s, "min", 100);
        int max = integer(s, "max", 300);
        double multiplier = dbl(s, "multiplier", 1.5);
        int minimum = integer(s, "minimum", 50);
        int cacheTicks = integer(s, "cache-ticks", 20);
        if (min > max) {
            warnings.add("fakeplayers.min > max in " + context + " — swapping.");
            int tmp = min; min = max; max = tmp;
        }
        return new FakePlayerSettings(true, mode, amount, min, max, multiplier, minimum, cacheTicks);
    }
    private MaxPlayersSettings loadMaxPlayers(Map<String, Object> s, String context) {
        boolean enabled = bool(s, "enabled", false);
        if (!enabled) return MaxPlayersSettings.DISABLED;
        int amount = integer(s, "amount", 9999);
        if (amount < 0) {
            warnings.add("maxplayers.amount must be >= 0 in " + context + " — using 9999.");
            amount = 9999;
        }
        return new MaxPlayersSettings(true, amount);
    }
    private PlayerCountSettings loadPlayerCount(Map<String, Object> s, String context) {
        boolean enabled = bool(s, "enabled", false);
        if (!enabled) return PlayerCountSettings.DISABLED;
        Map<String, Object> textSection = section(s, "text");
        boolean textEnabled = bool(textSection, "enabled", false);
        String textValue = str(textSection, "value", "");
        AnimationSettings anim = loadAnimation(section(s, "animation"), context + ".playercount");
        return new PlayerCountSettings(true, textEnabled, textValue, anim);
    }
    private AnimationSettings loadAnimation(Map<String, Object> s, String context) {
        boolean enabled = bool(s, "enabled", false);
        if (!enabled) return AnimationSettings.DISABLED;
        int interval = integer(s, "interval", 20);
        List<String> frames = stringList(s, "frames");
        if (frames.isEmpty()) {
            warnings.add("animation.frames is empty in " + context + " — animation disabled.");
            return AnimationSettings.DISABLED;
        }
        return new AnimationSettings(true, interval, frames);
    }
    private RotationSettings loadRotation(Map<String, Object> s) {
        boolean enabled = bool(s, "enabled", false);
        int interval = integer(s, "interval", 300);
        List<String> profiles = stringList(s, "profiles");
        return new RotationSettings(enabled, interval, profiles);
    }
    private PingProtectionSettings loadPingProtection(Map<String, Object> s) {
        boolean enabled = bool(s, "enabled", true);
        long cooldown = (long) integer(s, "cooldown-ms", 500);
        int max = integer(s, "max-per-second", 10);
        return new PingProtectionSettings(enabled, cooldown, max);
    }
    private LangSettings loadLang(Map<String, Object> s) {
        LangSettings def = LangSettings.defaults();
        return new LangSettings(
                str(s, "reload", def.getReload()),
                str(s, "no-permission", def.getNoPermission()),
                str(s, "profile-switched", def.getProfileSwitched()),
                str(s, "maintenance-on", def.getMaintenanceOn()),
                str(s, "maintenance-off", def.getMaintenanceOff()),
                str(s, "unknown-profile", def.getUnknownProfile()),
                str(s, "fakeplayers-set", def.getFakePlayersSet()),
                str(s, "status", def.getStatus())
        );
    }
    private Map<String, MotdProfile> loadProfiles(Map<String, Object> s,
                                                   FakePlayerSettings gFake,
                                                   MaxPlayersSettings gMax,
                                                   PlayerCountSettings gPc) {
        Map<String, MotdProfile> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : s.entrySet()) {
            String name = entry.getKey();
            if (!(entry.getValue() instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> ps = (Map<String, Object>) entry.getValue();
            try {
                result.put(name, loadProfile(name, ps, gFake, gMax, gPc));
            } catch (Exception e) {
                warnings.add("Failed to load profile '" + name + "': " + e.getMessage());
            }
        }
        return result;
    }
    private MotdProfile loadProfile(String name, Map<String, Object> s,
                                     FakePlayerSettings gFake, MaxPlayersSettings gMax,
                                     PlayerCountSettings gPc) {
        List<String> lines = stringList(s, "lines");
        if (lines.isEmpty()) {
            warnings.add("Profile '" + name + "' has no lines — using placeholder.");
            lines = List.of("&5Null&dBrand");
        }
        Map<String, Object> hoverSection = section(s, "hover");
        boolean hoverEnabled = bool(hoverSection, "enabled", false);
        boolean hoverRandom = bool(hoverSection, "random", false);
        List<String> hoverLines = stringList(hoverSection, "lines");
        AnimationSettings motdAnim = loadAnimation(section(s, "animation"), "profiles." + name);
        FakePlayerSettings fake = s.containsKey("fakeplayers")
                ? loadFakePlayer(section(s, "fakeplayers"), "profiles." + name) : gFake;
        MaxPlayersSettings max = s.containsKey("maxplayers")
                ? loadMaxPlayers(section(s, "maxplayers"), "profiles." + name) : gMax;
        PlayerCountSettings pc = s.containsKey("playercount")
                ? loadPlayerCount(section(s, "playercount"), "profiles." + name) : gPc;
        String faviconName = str(s, "favicon", null);
        return new MotdProfile(name, lines, hoverLines, hoverEnabled, hoverRandom,
                motdAnim, fake, max, pc, faviconName);
    }
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(Path path) {
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            org.yaml.snakeyaml.LoaderOptions options = new org.yaml.snakeyaml.LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(20);
            options.setCodePointLimit(1_048_576);
            Yaml yaml = new Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(options));
            Object loaded = yaml.load(reader);
            if (loaded instanceof Map) return (Map<String, Object>) loaded;
            if (loaded == null) return Collections.emptyMap();
            throw new IllegalArgumentException("config.yml must contain a mapping");
        } catch (YAMLException e) {
            throw new IllegalArgumentException("Invalid YAML in config.yml; previous configuration retained", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read config.yml; previous configuration retained", e);
        }
    }
    private void ensureDirectories(Path dir) {
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path favDir = dir.resolve("favicons");
            if (!Files.exists(favDir)) Files.createDirectories(favDir);
        } catch (IOException e) {
            warnings.add("Cannot create plugin directories: " + e.getMessage());
        }
    }
    private void ensureDefaultConfig(Path configPath, ClassLoader cl) {
        if (Files.exists(configPath)) return;
        try (InputStream in = cl.getResourceAsStream("config.yml")) {
            if (in != null) {
                Files.copy(in, configPath);
                return;
            }
        } catch (IOException e) {
            warnings.add("Cannot copy default config.yml: " + e.getMessage());
        }
    }
    private static boolean bool(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return def;
    }
    private static String str(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v != null ? v.toString() : def;
    }
    private static int integer(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) { try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {} }
        return def;
    }
    private static double dbl(Map<String, Object> map, String key, double def) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) { try { return Double.parseDouble((String) v); } catch (NumberFormatException ignored) {} }
        return def;
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Map) return (Map<String, Object>) v;
        return Collections.emptyMap();
    }
    @SuppressWarnings("unchecked")
    private static List<String> stringList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) v) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        return new ArrayList<>();
    }
    private static MotdProfile defaultGlobalProfile() {
        return new MotdProfile(
                List.of("&7Welcome to our server", "&fHave fun!"),
                Collections.emptyList(),
                false
        );
    }
}
