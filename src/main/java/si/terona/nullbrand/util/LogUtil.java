package si.terona.nullbrand.util;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Thin logging wrapper providing optional debug output and structured messages.
 * Thread-safe by delegating to the underlying logger.
 */
public final class LogUtil {
    private static final String PREFIX = "[NullBrand] ";
    private final Logger logger;
    private volatile boolean debug;
    public LogUtil(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public void info(String message) {
        logger.info(PREFIX + message);
    }
    public void warn(String message) {
        logger.warning(PREFIX + message);
    }
    public void error(String message) {
        logger.severe(PREFIX + message);
    }
    public void error(String message, Throwable t) {
        logger.log(Level.SEVERE, PREFIX + message, t);
    }
    public void debug(String message) {
        if (debug) {
            logger.info(PREFIX + "[DEBUG] " + message);
        }
    }
    public boolean isDebug() {
        return debug;
    }
    /**
     * Creates a SLF4J-backed LogUtil for Velocity compatibility.
     */
    public static LogUtil ofSlf4j(org.slf4j.Logger slf4j, boolean debug) {
        Logger jul = Logger.getLogger("NullBrand");
        jul.setUseParentHandlers(false);
        jul.addHandler(new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord r) {
                String msg = r.getMessage();
                if (r.getLevel() == Level.SEVERE) slf4j.error(msg);
                else if (r.getLevel() == Level.WARNING) slf4j.warn(msg);
                else slf4j.info(msg);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return new LogUtil(jul, debug);
    }
}
