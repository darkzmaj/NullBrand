package si.terona.nullbrand.command;
import si.terona.nullbrand.engine.BrandEngine;
import si.terona.nullbrand.engine.BrandEngine.EngineState;
import si.terona.nullbrand.model.LangSettings;
import si.terona.nullbrand.util.ColorUtil;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
/**
 * Full NullBrand command handler.
 * Subcommands: reload, profile, maintenance, fakeplayers, status
 *
 * <p>Permission nodes:
 * <ul>
 *   <li>{@code nullbrand.reload}        — /nb reload</li>
 *   <li>{@code nullbrand.profile.switch} — /nb profile &lt;name&gt;</li>
 *   <li>{@code nullbrand.maintenance}   — /nb maintenance &lt;on|off&gt;</li>
 *   <li>{@code nullbrand.fakeplayers}   — /nb fakeplayers &lt;amount&gt;</li>
 *   <li>{@code nullbrand.admin}         — /nb status (and all others)</li>
 * </ul>
 */
public final class NullBrandCommand {
    private NullBrandCommand() {}
    // -----------------------------------------------------------------------
    // Shared logic
    // -----------------------------------------------------------------------
    interface Sender {
        void send(String legacy);
        boolean hasPermission(String node);
    }
    static class Result {
        static final String USAGE = "/nb <reload|profile|maintenance|fakeplayers|status>";
    }
    /**
     * Dispatch a command. Returns the message to send to the sender.
     */
    static void dispatch(Sender sender, String[] args, BrandEngine engine, Path dataDir, ClassLoader cl) {
        if (args.length == 0) {
            sender.send("&7Usage: " + Result.USAGE);
            return;
        }
        EngineState state = engine.state();
        LangSettings lang = state != null ? state.config().getLangSettings() : LangSettings.defaults();
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("nullbrand.reload") && !sender.hasPermission("nullbrand.admin")) {
                    sender.send(lang.getNoPermission());
                    return;
                }
                boolean ok = engine.load(dataDir, cl);
                if (ok) sender.send(lang.getReload());
                else sender.send("&cReload failed — check console for errors.");
            }
            case "profile" -> {
                if (!sender.hasPermission("nullbrand.profile.switch") && !sender.hasPermission("nullbrand.admin")) {
                    sender.send(lang.getNoPermission());
                    return;
                }
                if (args.length < 2) { sender.send("&7Usage: /nb profile <name>"); return; }
                String name = args[1];
                EngineState s = engine.state();
                if (s == null) { sender.send("&cEngine not initialized."); return; }
                if (s.profileManager().setProfile(name)) {
                    sender.send(lang.getProfileSwitched().replace("%profile%", name));
                } else {
                    sender.send(lang.getUnknownProfile().replace("%profile%", name));
                }
            }
            case "maintenance" -> {
                if (!sender.hasPermission("nullbrand.maintenance") && !sender.hasPermission("nullbrand.admin")) {
                    sender.send(lang.getNoPermission());
                    return;
                }
                EngineState s = engine.state();
                if (s == null) { sender.send("&cEngine not initialized."); return; }
                if (args.length < 2) {
                    boolean toggled = s.maintenanceManager().toggle();
                    sender.send(toggled ? lang.getMaintenanceOn() : lang.getMaintenanceOff());
                } else {
                    boolean active = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                    s.maintenanceManager().setActive(active);
                    sender.send(active ? lang.getMaintenanceOn() : lang.getMaintenanceOff());
                }
            }
            case "fakeplayers" -> {
                if (!sender.hasPermission("nullbrand.fakeplayers") && !sender.hasPermission("nullbrand.admin")) {
                    sender.send(lang.getNoPermission());
                    return;
                }
                if (args.length < 2) { sender.send("&7Usage: /nb fakeplayers <amount>"); return; }
                EngineState s = engine.state();
                if (s == null) { sender.send("&cEngine not initialized."); return; }
                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount < 0) throw new NumberFormatException();
                    s.fakePlayerEngine().setStaticOverride(amount);
                    sender.send(lang.getFakePlayersSet().replace("%amount%", String.valueOf(amount)));
                } catch (NumberFormatException e) {
                    sender.send("&cInvalid amount. Must be a non-negative integer.");
                }
            }
            case "status" -> {
                if (!sender.hasPermission("nullbrand.admin")) {
                    sender.send(lang.getNoPermission());
                    return;
                }
                EngineState s = engine.state();
                if (s == null) { sender.send("&cEngine not initialized."); return; }
                boolean maint = s.maintenanceManager().isActive();
                boolean fake = s.config().getGlobalFakePlayerSettings().isEnabled();
                String fakeInfo = fake
                        ? s.config().getGlobalFakePlayerSettings().getMode().name()
                        : "disabled";
                String status = lang.getStatus()
                        .replace("%platform%", s.config().getActiveProfile())
                        .replace("%profile%", s.profileManager().getActiveProfileName())
                        .replace("%maintenance%", maint ? "ENABLED" : "disabled")
                        .replace("%fakeplayers%", fakeInfo);
                for (String line : status.split("\n")) {
                    sender.send(line);
                }
            }
            default -> sender.send("&7Usage: " + Result.USAGE);
        }
    }
    static List<String> suggest(String[] args) {
        if (args.length <= 1) {
            return Arrays.asList("reload", "profile", "maintenance", "fakeplayers", "status");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("maintenance")) {
            return Arrays.asList("on", "off");
        }
        return List.of();
    }
    // -----------------------------------------------------------------------
    // Velocity
    // -----------------------------------------------------------------------
    public static final class Velocity implements com.velocitypowered.api.command.SimpleCommand {
        private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY =
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                        .character('\u00A7').hexColors().build();
        private final BrandEngine engine;
        private final Path dataDir;
        private final ClassLoader cl;
        public Velocity(BrandEngine engine, Path dataDir, ClassLoader cl) {
            this.engine = engine;
            this.dataDir = dataDir;
            this.cl = cl;
        }
        @Override
        public void execute(Invocation inv) {
            Sender sender = new Sender() {
                public void send(String msg) {
                    inv.source().sendMessage(LEGACY.deserialize(ColorUtil.translate(msg)));
                }
                public boolean hasPermission(String perm) {
                    return inv.source().hasPermission(perm);
                }
            };
            dispatch(sender, inv.arguments(), engine, dataDir, cl);
        }
        @Override
        public boolean hasPermission(Invocation inv) {
            return inv.source().hasPermission("nullbrand.admin")
                    || inv.source().hasPermission("nullbrand.reload");
        }
        @Override
        public List<String> suggest(Invocation inv) {
            return NullBrandCommand.suggest(inv.arguments());
        }
    }
    // -----------------------------------------------------------------------
    // BungeeCord
    // -----------------------------------------------------------------------
    public static final class Bungee extends net.md_5.bungee.api.plugin.Command
            implements net.md_5.bungee.api.plugin.TabExecutor {
        private final BrandEngine engine;
        private final Path dataDir;
        private final ClassLoader cl;
        public Bungee(BrandEngine engine, Path dataDir, ClassLoader cl) {
            super("nullbrand", "nullbrand.admin", "nb");
            this.engine = engine;
            this.dataDir = dataDir;
            this.cl = cl;
        }
        @Override
        public void execute(net.md_5.bungee.api.CommandSender sender, String[] args) {
            Sender s = new Sender() {
                public void send(String msg) {
                    sender.sendMessage(new net.md_5.bungee.api.chat.TextComponent(ColorUtil.translate(msg)));
                }
                public boolean hasPermission(String perm) { return sender.hasPermission(perm); }
            };
            dispatch(s, args, engine, dataDir, cl);
        }
        @Override
        public Iterable<String> onTabComplete(net.md_5.bungee.api.CommandSender sender, String[] args) {
            return suggest(args);
        }
    }
    // -----------------------------------------------------------------------
    // Spigot / Paper
    // -----------------------------------------------------------------------
    public static final class Spigot implements org.bukkit.command.CommandExecutor,
            org.bukkit.command.TabCompleter {
        private final BrandEngine engine;
        private final Path dataDir;
        private final ClassLoader cl;
        public Spigot(BrandEngine engine, Path dataDir, ClassLoader cl) {
            this.engine = engine;
            this.dataDir = dataDir;
            this.cl = cl;
        }
        @Override
        public boolean onCommand(org.bukkit.command.CommandSender sender,
                                 org.bukkit.command.Command cmd,
                                 String label, String[] args) {
            Sender s = new Sender() {
                public void send(String msg) { sender.sendMessage(ColorUtil.translate(msg)); }
                public boolean hasPermission(String perm) { return sender.hasPermission(perm); }
            };
            dispatch(s, args, engine, dataDir, cl);
            return true;
        }
        @Override
        public List<String> onTabComplete(org.bukkit.command.CommandSender sender,
                                          org.bukkit.command.Command cmd,
                                          String alias, String[] args) {
            return suggest(args);
        }
    }
}
