package si.terona.nullbrand;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import si.terona.nullbrand.command.ReloadCommand;
import si.terona.nullbrand.listener.BrandingListener;
import si.terona.nullbrand.listener.MotdListener;
import si.terona.nullbrand.util.ConfigUtil;

@Plugin(
        id = "nullbrand",
        name = "NullBrand",
        version = "1.0.0",
        description = "Ultra-lightweight proxy branding and MOTD plugin",
        authors = {"Terona Studios"}
)
public final class NullBrand {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final ChannelIdentifier brandModern = MinecraftChannelIdentifier.create("minecraft", "brand");
    private final ChannelIdentifier brandLegacy = LegacyChannelIdentifier.from("MC|Brand");
    private ConfigUtil config;

    @Inject
    public NullBrand(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.config = new ConfigUtil(dataDirectory);
        reload();
        proxy.getChannelRegistrar().register(brandModern, brandLegacy);
        proxy.getEventManager().register(this, new MotdListener.Velocity(config));
        proxy.getEventManager().register(this, new BrandingListener.Velocity(config, brandModern, brandLegacy));
        proxy.getCommandManager().register("nullbrand", new ReloadCommand.Velocity(this), "nb");
    }

    public void reload() {
        try {
            config.load(getClass().getClassLoader());
        } catch (IOException e) {
            logger.error("Failed to load config.yml", e);
        }
    }

    public ConfigUtil config() {
        return config;
    }

    public static final class Bungee extends net.md_5.bungee.api.plugin.Plugin {

        private ConfigUtil config;

        @Override
        public void onEnable() {
            config = new ConfigUtil(getDataFolder().toPath());
            reload();
            getProxy().getPluginManager().registerListener(this, new MotdListener.Bungee(config));
            getProxy().getPluginManager().registerListener(this, new BrandingListener.Bungee(config));
            getProxy().getPluginManager().registerCommand(this, new ReloadCommand.Bungee(this));
        }

        public void reload() {
            try {
                config.load(getClass().getClassLoader());
            } catch (IOException e) {
                getLogger().severe("Failed to load config.yml: " + e.getMessage());
            }
        }

        public ConfigUtil config() {
            return config;
        }
    }
}
