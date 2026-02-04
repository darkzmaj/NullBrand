package si.terona.nullbrand.listener;

import si.terona.nullbrand.util.ConfigUtil;

public final class MotdListener {

    private MotdListener() {
    }

    public static final class Velocity {

        private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY =
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                        .character('\u00A7')
                        .hexColors()
                        .build();

        private final ConfigUtil config;

        public Velocity(ConfigUtil config) {
            this.config = config;
        }

        @com.velocitypowered.api.event.Subscribe
        public void onProxyPing(com.velocitypowered.api.event.proxy.ProxyPingEvent event) {
            if (!config.isMotdEnabled()) {
                return;
            }
            String motd = config.getMotdJoinedColored();
            net.kyori.adventure.text.Component component = LEGACY.deserialize(motd);
            event.setPing(event.getPing().asBuilder().description(component).build());
        }
    }

    public static final class Bungee implements net.md_5.bungee.api.plugin.Listener {

        private final ConfigUtil config;

        public Bungee(ConfigUtil config) {
            this.config = config;
        }

        @net.md_5.bungee.event.EventHandler
        public void onProxyPing(net.md_5.bungee.api.event.ProxyPingEvent event) {
            if (!config.isMotdEnabled()) {
                return;
            }
            if (event.getResponse() == null) {
                return;
            }
            event.getResponse().setDescription(config.getMotdJoinedColored());
        }
    }
}
