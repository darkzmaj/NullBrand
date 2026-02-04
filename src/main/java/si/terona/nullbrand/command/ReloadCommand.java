package si.terona.nullbrand.command;

import si.terona.nullbrand.NullBrand;
import si.terona.nullbrand.util.ColorUtil;

public final class ReloadCommand {

    private static final String NO_PERMISSION = "&cNo permission.";
    private static final String RELOADED = "&aNullBrand reloaded.";

    private ReloadCommand() {
    }

    public static final class Velocity implements com.velocitypowered.api.command.SimpleCommand {

        private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY =
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                        .character('\u00A7')
                        .hexColors()
                        .build();

        private final NullBrand plugin;

        public Velocity(NullBrand plugin) {
            this.plugin = plugin;
        }

        @Override
        public void execute(Invocation invocation) {
            if (!invocation.source().hasPermission("nullbrand.reload")) {
                invocation.source().sendMessage(LEGACY.deserialize(ColorUtil.translate(NO_PERMISSION)));
                return;
            }
            plugin.reload();
            invocation.source().sendMessage(LEGACY.deserialize(ColorUtil.translate(RELOADED)));
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("nullbrand.reload");
        }
    }

    public static final class Bungee extends net.md_5.bungee.api.plugin.Command {

        private final NullBrand.Bungee plugin;

        public Bungee(NullBrand.Bungee plugin) {
            super("nullbrand", "nullbrand.reload", "nb");
            this.plugin = plugin;
        }

        @Override
        public void execute(net.md_5.bungee.api.CommandSender sender, String[] args) {
            if (!sender.hasPermission("nullbrand.reload")) {
                sender.sendMessage(new net.md_5.bungee.api.chat.TextComponent(ColorUtil.translate(NO_PERMISSION)));
                return;
            }
            plugin.reload();
            sender.sendMessage(new net.md_5.bungee.api.chat.TextComponent(ColorUtil.translate(RELOADED)));
        }
    }
}
