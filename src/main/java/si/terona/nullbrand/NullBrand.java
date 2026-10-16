package si.terona.nullbrand;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import si.terona.nullbrand.api.NullBrandAPI;
import si.terona.nullbrand.command.NullBrandCommand;
import si.terona.nullbrand.engine.BrandEngine;
import si.terona.nullbrand.listener.MotdListener;
import si.terona.nullbrand.util.LogUtil;
import java.nio.file.Path;
/**
 * NullBrand — Premium Server-List Branding Engine.
 * Version 2.0.1
 *
 * <p>Supports: Velocity · BungeeCord · Waterfall · Spigot · Paper
 */
@Plugin(
        id = "nullbrand",
        name = "NullBrand",
        version = "2.0.1",
        description = "Premium server-list branding engine — MOTD, favicon, fake players, animations & more.",
        authors = {"Terona Studios"}
)
public final class NullBrand {
    private final ProxyServer proxy;
    private final Logger slf4jLogger;
    private final Path dataDirectory;
    private BrandEngine engine;
    private LogUtil log;
    @Inject
    public NullBrand(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.slf4jLogger = logger;
        this.dataDirectory = dataDirectory;
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        log = LogUtil.ofSlf4j(slf4jLogger, false);
        engine = new BrandEngine(log, "Velocity");
        NullBrandAPI.init(engine);
        if (!engine.load(dataDirectory, getClass().getClassLoader())) {
            log.error("Failed to load configuration. Plugin may not function correctly.");
        }
        proxy.getEventManager().register(this, new MotdListener.Velocity(engine));
        proxy.getCommandManager().register("nullbrand",
                new NullBrandCommand.Velocity(engine, dataDirectory, getClass().getClassLoader()), "nb");
        log.info("NullBrand 2.0.1 online on Velocity.");
    }
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (engine != null) engine.shutdown();
            NullBrandAPI.init(null);
    }
    // -------------------------------------------------------------------------
    // BungeeCord / Waterfall entry point
    // -------------------------------------------------------------------------
    public static final class Bungee extends net.md_5.bungee.api.plugin.Plugin {
        private BrandEngine engine;
        private LogUtil log;
        @Override
        public void onEnable() {
            log = new LogUtil(getLogger(), false);
            engine = new BrandEngine(log, "BungeeCord");
            NullBrandAPI.init(engine);
            Path dataDir = getDataFolder().toPath();
            if (!engine.load(dataDir, getClass().getClassLoader())) {
                log.error("Failed to load configuration. Plugin may not function correctly.");
            }
            getProxy().getPluginManager().registerListener(this, new MotdListener.Bungee(engine));
            getProxy().getPluginManager().registerCommand(this,
                    new NullBrandCommand.Bungee(engine, dataDir, getClass().getClassLoader()));
            log.info("NullBrand 2.0.1 online on BungeeCord/Waterfall.");
        }
        @Override
        public void onDisable() {
            if (engine != null) engine.shutdown();
            NullBrandAPI.init(null);
        }
    }
    // -------------------------------------------------------------------------
    // Spigot / Paper entry point
    // -------------------------------------------------------------------------
    public static final class Spigot extends org.bukkit.plugin.java.JavaPlugin {
        private BrandEngine engine;
        private LogUtil log;
        @Override
        public void onEnable() {
            log = new LogUtil(getLogger(), false);
            engine = new BrandEngine(log, "Spigot");
            NullBrandAPI.init(engine);
            Path dataDir = getDataFolder().toPath();
            if (!engine.load(dataDir, getClass().getClassLoader())) {
                log.error("Failed to load configuration. Plugin may not function correctly.");
            }
            org.bukkit.event.Listener listener;
            try {
                Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
                listener = new MotdListener.Paper(engine);
            } catch (ClassNotFoundException legacySpigot) {
                listener = new MotdListener.Spigot(engine);
            }
            getServer().getPluginManager().registerEvents(listener, this);
            org.bukkit.command.PluginCommand cmd = getCommand("nullbrand");
            if (cmd != null) {
                NullBrandCommand.Spigot handler =
                        new NullBrandCommand.Spigot(engine, dataDir, getClass().getClassLoader());
                cmd.setExecutor(handler);
                cmd.setTabCompleter(handler);
            }
            log.info("NullBrand 2.0.1 online on Spigot/Paper.");
        }
        @Override
        public void onDisable() {
            if (engine != null) engine.shutdown();
            NullBrandAPI.init(null);
        }
    }
}
