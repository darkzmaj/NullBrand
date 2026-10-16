package si.terona.nullbrand.favicon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
/**
 * Loads and caches favicon images for all platforms.
 * Favicon objects are platform-specific but held as Object references to avoid hard compile-time deps.
 */
public final class FaviconStore {
    private final String base64;
    private final Object velocityFavicon;
    private final Object bungeeFavicon;
    private final Object spigotFavicon;
    private FaviconStore(String base64, Object velocity, Object bungee, Object spigot) {
        this.base64 = base64;
        this.velocityFavicon = velocity;
        this.bungeeFavicon = bungee;
        this.spigotFavicon = spigot;
    }
    public static final FaviconStore EMPTY = new FaviconStore(null, null, null, null);
    /**
     * Load a favicon from the favicons directory.
     *
     * @param faviconDir directory containing favicon files
     * @param name       file name (e.g. "logo.png")
     * @return loaded store, or EMPTY on failure
     */
    public static FaviconStore load(Path faviconDir, String name) {
        if (name == null || name.isEmpty()) return EMPTY;
        try {
            Path root = faviconDir.toRealPath();
            Path path = root.resolve(name).normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)
                    || !path.toRealPath().startsWith(root) || Files.size(path) > 1_048_576) return EMPTY;
            BufferedImage image;
            try (javax.imageio.stream.ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
                if (input == null) return EMPTY;
                java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) return EMPTY;
                javax.imageio.ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    if (!reader.getFormatName().equalsIgnoreCase("png")
                            || reader.getWidth(0) != 64 || reader.getHeight(0) != 64) return EMPTY;
                    image = reader.read(0);
                } finally { reader.dispose(); }
            }
            if (image == null) return EMPTY;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
            Object velocity = null;
            Object bungee = null;
            Object spigot = null;
            try { velocity = com.velocitypowered.api.util.Favicon.create(image); } catch (LinkageError | RuntimeException ignored) {}
            try { bungee = net.md_5.bungee.api.Favicon.create(image); } catch (LinkageError | RuntimeException ignored) {}
            try { spigot = org.bukkit.Bukkit.loadServerIcon(image); } catch (LinkageError | Exception ignored) {}
            return new FaviconStore(base64, velocity, bungee, spigot);
        } catch (java.io.IOException | RuntimeException e) {
            return EMPTY;
        }
    }
    public boolean hasFavicon() { return base64 != null; }
    public String getBase64() { return base64; }
    public Object getVelocity() { return velocityFavicon; }
    public Object getBungee() { return bungeeFavicon; }
    public Object getSpigot() { return spigotFavicon; }
}
