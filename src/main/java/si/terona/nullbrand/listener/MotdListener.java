package si.terona.nullbrand.listener;
import si.terona.nullbrand.engine.BrandEngine;
import si.terona.nullbrand.engine.BrandEngine.EngineState;
import si.terona.nullbrand.favicon.FaviconStore;
import si.terona.nullbrand.model.*;
import si.terona.nullbrand.placeholder.PlaceholderEngine;
import si.terona.nullbrand.render.MotdRenderer;
import java.nio.charset.StandardCharsets;
import java.util.*;
/**
 * Platform-specific MOTD listeners.
 * All shared logic delegates to the {@link BrandEngine}.
 */
public final class MotdListener {
    private MotdListener() {}
    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------
    /** Build a PlaceholderEngine.PingContext from available ping data. */
    private static PlaceholderEngine.PingContext buildContext(EngineState state,
                                                               MotdProfile profile,
                                                               int realOnline, int realMax) {
        FakePlayerSettings fake = profile.getFakePlayerSettings();
        int displayOnline = state.fakePlayersFor(profile).computeDisplayed(realOnline);
        int fakeBonus = displayOnline - realOnline;
        MaxPlayersSettings maxSettings = profile.getMaxPlayersSettings();
        int displayMax = maxSettings.isEnabled() ? maxSettings.getAmount() : realMax;
        boolean maintenance = state.maintenanceManager().isActive();
        String fakeInfo = fake.isEnabled() ? fake.getMode() + " +" + fakeBonus : "off";
        return new PlaceholderEngine.PingContext(
                realOnline, realMax, fakeBonus, displayOnline, displayMax,
                profile.getName(), maintenance, fakeInfo);
    }
    /**
     * Resolve the current MOTD text for a profile, respecting animation.
     */
    private static String resolveMotd(EngineState state, MotdProfile profile,
                                       PlaceholderEngine.PingContext ctx) {
        String raw;
        AnimationSettings anim = profile.getMotdAnimation();
        if (anim.isEnabled() && anim.hasFrames()) {
            String frame = state.animationEngine().currentFrame("motd:" + profile.getName(), anim.getFrames());
            raw = frame != null ? frame : profile.getJoined();
        } else {
            raw = profile.getJoined();
        }
        return state.placeholderEngine().resolve(raw, ctx);
    }
    /**
     * Build the hover sample players list.
     * If hoverRandom is true, a random line from the list is displayed repeatedly.
     */
    private static List<String> resolveHoverLines(EngineState state, MotdProfile profile,
                                                   PlaceholderEngine.PingContext ctx) {
        List<String> lines = profile.getHoverLines();
        if (lines.isEmpty()) return Collections.emptyList();
        List<String> resolved = new ArrayList<>(lines.size());
        if (profile.isHoverRandom()) {
            // Use a single random line
            String randomLine = lines.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(lines.size()));
            resolved.add(state.placeholderEngine().resolve(randomLine, ctx));
        } else {
            for (String line : lines) {
                resolved.add(state.placeholderEngine().resolve(line, ctx));
            }
        }
        return resolved;
    }
    /**
     * Resolve the playercount display text (the animated hover "sample" list).
     */
    private static List<String> resolvePlayerCountText(EngineState state, MotdProfile profile,
                                                         PlaceholderEngine.PingContext ctx) {
        PlayerCountSettings pcs = profile.getPlayerCountSettings();
        if (!pcs.isEnabled()) return null;
        if (pcs.getAnimation().isEnabled() && pcs.getAnimation().hasFrames()) {
            String frame = state.animationEngine().currentFrame("pc:" + profile.getName(),
                    pcs.getAnimation().getFrames());
            if (frame != null) {
                return List.of(state.placeholderEngine().resolve(frame, ctx));
            }
        }
        if (pcs.isTextEnabled() && !pcs.getTextValue().isEmpty()) {
            return List.of(state.placeholderEngine().resolve(pcs.getTextValue(), ctx));
        }
        return null;
    }
    // -----------------------------------------------------------------------
    // Velocity
    // -----------------------------------------------------------------------
    public static final class Velocity {
        private final BrandEngine engine;
        public Velocity(BrandEngine engine) {
            this.engine = engine;
        }
        @com.velocitypowered.api.event.Subscribe
        public void onPing(com.velocitypowered.api.event.proxy.ProxyPingEvent event) {
            EngineState state = engine.state();
            if (state == null || !state.config().isMotdEnabled()) return;
            String ip = null;
            try { ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress(); }
            catch (Throwable ignored) {}
            if (state.rateLimiter().isBlocked(ip)) return;
            MotdProfile profile;
            try { profile = state.resolveProfile(); } catch (Throwable t) { return; }
            int realOnline = event.getPing().getPlayers().map(
                    com.velocitypowered.api.proxy.server.ServerPing.Players::getOnline).orElse(0);
            int realMax = event.getPing().getPlayers().map(
                    com.velocitypowered.api.proxy.server.ServerPing.Players::getMax).orElse(0);
            PlaceholderEngine.PingContext ctx = buildContext(state, profile, realOnline, realMax);
            String resolvedMotd = resolveMotd(state, profile, ctx);
            net.kyori.adventure.text.Component motdComponent = MotdRenderer.toMotdComponent(resolvedMotd);
            com.velocitypowered.api.proxy.server.ServerPing.Builder builder =
                    event.getPing().asBuilder().description(motdComponent);
            // Player counts
            builder.onlinePlayers(ctx.displayOnline());
            builder.maximumPlayers(ctx.displayMax());
            // Favicon
            FaviconStore fav = state.faviconFor(profile.getName());
            if (fav.hasFavicon() && fav.getVelocity() instanceof com.velocitypowered.api.util.Favicon) {
                builder.favicon((com.velocitypowered.api.util.Favicon) fav.getVelocity());
            }
            // Hover / sample players
            List<String> pcText = resolvePlayerCountText(state, profile, ctx);
            if (pcText != null) {
                // Override hover with playercount text
                List<com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer> sample = new ArrayList<>();
                int idx = 0;
                for (String line : pcText) {
                    UUID id = UUID.nameUUIDFromBytes(("nb-pc-" + idx).getBytes(StandardCharsets.UTF_8));
                    sample.add(new com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer(
                            MotdRenderer.toLegacy(line), id));
                    idx++;
                }
                builder.samplePlayers(sample.toArray(new com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer[0]));
            } else if (profile.isHoverEnabled()) {
                List<String> hoverLines = resolveHoverLines(state, profile, ctx);
                List<com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer> sample = new ArrayList<>();
                int idx = 0;
                for (String line : hoverLines) {
                    UUID id = UUID.nameUUIDFromBytes(("nb-hover-" + idx).getBytes(StandardCharsets.UTF_8));
                    sample.add(new com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer(
                            MotdRenderer.toLegacy(line), id));
                    idx++;
                }
                builder.samplePlayers(sample.toArray(new com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer[0]));
            }
            try { event.setPing(builder.build()); } catch (Throwable ignored) {}
        }
    }
    // -----------------------------------------------------------------------
    // BungeeCord / Waterfall
    // -----------------------------------------------------------------------
    public static final class Bungee implements net.md_5.bungee.api.plugin.Listener {
        private final BrandEngine engine;
        public Bungee(BrandEngine engine) {
            this.engine = engine;
        }
        @net.md_5.bungee.event.EventHandler
        public void onPing(net.md_5.bungee.api.event.ProxyPingEvent event) {
            EngineState state = engine.state();
            if (state == null || !state.config().isMotdEnabled()) return;
            if (event.getResponse() == null) return;
            String ip = null;
            try { ip = ((java.net.InetSocketAddress) event.getConnection().getSocketAddress()).getAddress().getHostAddress(); }
            catch (Throwable ignored) {}
            if (state.rateLimiter().isBlocked(ip)) return;
            MotdProfile profile;
            try { profile = state.resolveProfile(); } catch (Throwable t) { return; }
            net.md_5.bungee.api.ServerPing response = event.getResponse();
            int realOnline = response.getPlayers() != null ? response.getPlayers().getOnline() : 0;
            int realMax = response.getPlayers() != null ? response.getPlayers().getMax() : 0;
            PlaceholderEngine.PingContext ctx = buildContext(state, profile, realOnline, realMax);
            String resolvedMotd = resolveMotd(state, profile, ctx);
            response.setDescriptionComponent(net.md_5.bungee.api.chat.TextComponent.fromLegacy(si.terona.nullbrand.util.ColorUtil.translate(resolvedMotd)));
            // Player counts
            if (response.getPlayers() != null) {
                response.getPlayers().setOnline(ctx.displayOnline());
                response.getPlayers().setMax(ctx.displayMax());
            }
            // Favicon
            FaviconStore fav = state.faviconFor(profile.getName());
            if (fav.hasFavicon() && fav.getBungee() instanceof net.md_5.bungee.api.Favicon) {
                response.setFavicon((net.md_5.bungee.api.Favicon) fav.getBungee());
            }
            // Hover / sample players
            List<String> pcText = resolvePlayerCountText(state, profile, ctx);
            List<String> hoverLines = pcText != null ? pcText
                    : (profile.isHoverEnabled() ? resolveHoverLines(state, profile, ctx) : null);
            if (hoverLines != null && response.getPlayers() != null) {
                net.md_5.bungee.api.ServerPing.PlayerInfo[] sample =
                        new net.md_5.bungee.api.ServerPing.PlayerInfo[hoverLines.size()];
                for (int i = 0; i < hoverLines.size(); i++) {
                    UUID id = UUID.nameUUIDFromBytes(("nb-hover-" + i).getBytes(StandardCharsets.UTF_8));
                    sample[i] = new net.md_5.bungee.api.ServerPing.PlayerInfo(
                            si.terona.nullbrand.util.ColorUtil.translate(hoverLines.get(i)), id);
                }
                response.getPlayers().setSample(sample);
            }
        }
    }
    // -----------------------------------------------------------------------
    // Spigot / Paper
    // -----------------------------------------------------------------------
    public static final class Spigot implements org.bukkit.event.Listener {
        private final BrandEngine engine;
        public Spigot(BrandEngine engine) {
            this.engine = engine;
        }
        @org.bukkit.event.EventHandler
        public void onPing(org.bukkit.event.server.ServerListPingEvent event) {
            handleSpigot(event);
        }
        @SuppressWarnings("deprecation") // Plain Spigot exposes the legacy string MOTD API.
        private void handleSpigot(org.bukkit.event.server.ServerListPingEvent event) {
            EngineState state = engine.state();
            if (state == null || !state.config().isMotdEnabled()) return;
            String ip = null;
            try { ip = event.getAddress().getHostAddress(); } catch (Throwable ignored) {}
            if (state.rateLimiter().isBlocked(ip)) return;
            MotdProfile profile;
            try { profile = state.resolveProfile(); } catch (Throwable t) { return; }
            int realOnline = event.getNumPlayers();
            int realMax = event.getMaxPlayers();
            PlaceholderEngine.PingContext ctx = buildContext(state, profile, realOnline, realMax);
            String resolvedMotd = resolveMotd(state, profile, ctx);
            event.setMotd(si.terona.nullbrand.util.ColorUtil.translate(resolvedMotd));
            MaxPlayersSettings maxSettings = profile.getMaxPlayersSettings();
            if (maxSettings.isEnabled()) event.setMaxPlayers(maxSettings.getAmount());
            FaviconStore fav = state.faviconFor(profile.getName());
            if (fav.hasFavicon() && fav.getSpigot() instanceof org.bukkit.util.CachedServerIcon) {
                event.setServerIcon((org.bukkit.util.CachedServerIcon) fav.getSpigot());
            }
        }
    }
    /** Loaded only on Paper, so Spigot never resolves Paper event classes. */
    public static final class Paper implements org.bukkit.event.Listener {
        private final BrandEngine engine;
        public Paper(BrandEngine engine) { this.engine = engine; }
        @org.bukkit.event.EventHandler
        public void onPing(com.destroystokyo.paper.event.server.PaperServerListPingEvent event) {
            if (!event.isCancelled()) handlePaper(event);
        }
        private void handlePaper(com.destroystokyo.paper.event.server.PaperServerListPingEvent event) {
            EngineState state = engine.state();
            if (state == null || !state.config().isMotdEnabled()) return;
            String ip = null;
            try { java.net.InetSocketAddress sa = event.getClient().getAddress(); ip = sa.getAddress() != null ? sa.getAddress().getHostAddress() : sa.getHostString(); } catch (Throwable ignored) {}
            if (state.rateLimiter().isBlocked(ip)) return;
            MotdProfile profile;
            try { profile = state.resolveProfile(); } catch (Throwable t) { return; }
            int realOnline = event.getNumPlayers();
            int realMax = event.getMaxPlayers();
            PlaceholderEngine.PingContext ctx = buildContext(state, profile, realOnline, realMax);
            String resolvedMotd = resolveMotd(state, profile, ctx);
            event.motd(MotdRenderer.toMotdComponent(resolvedMotd));
            MaxPlayersSettings maxSettings = profile.getMaxPlayersSettings();
            if (maxSettings.isEnabled()) event.setMaxPlayers(maxSettings.getAmount());
            FaviconStore fav = state.faviconFor(profile.getName());
            if (fav.hasFavicon() && fav.getSpigot() instanceof org.bukkit.util.CachedServerIcon) {
                event.setServerIcon((org.bukkit.util.CachedServerIcon) fav.getSpigot());
            }
            event.setNumPlayers(ctx.displayOnline());
            List<String> pcText = resolvePlayerCountText(state, profile, ctx);
            if (pcText != null || profile.isHoverEnabled()) {
                List<String> hoverLines = pcText != null ? pcText : resolveHoverLines(state, profile, ctx);
                setPaperSample(event, hoverLines);
            }
        }
        @SuppressWarnings({"deprecation", "removal"})
        private static void setPaperSample(com.destroystokyo.paper.event.server.PaperServerListPingEvent event,
                                           List<String> lines) {
            try {
                java.util.List<com.destroystokyo.paper.event.server.PaperServerListPingEvent.ListedPlayerInfo> sample = event.getListedPlayers();
                sample.clear();
                for (int i = 0; i < lines.size(); i++) {
                    sample.add(new com.destroystokyo.paper.event.server.PaperServerListPingEvent.ListedPlayerInfo(
                        MotdRenderer.toLegacy(lines.get(i)), UUID.nameUUIDFromBytes(("nb-hover-" + i).getBytes(StandardCharsets.UTF_8))));
                }
            } catch (NoSuchMethodError | NoClassDefFoundError legacyPaper) {
                // Older Paper releases expose only the profile sample API.
                event.getPlayerSample().clear();
                for (int i = 0; i < lines.size(); i++) {
                    event.getPlayerSample().add(org.bukkit.Bukkit.createProfile(
                        UUID.nameUUIDFromBytes(("nb-hover-" + i).getBytes(StandardCharsets.UTF_8)), MotdRenderer.toLegacy(lines.get(i))));
                }
            }
        }

    }
}
