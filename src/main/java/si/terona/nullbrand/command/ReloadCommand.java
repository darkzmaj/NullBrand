package si.terona.nullbrand.command;
/**
 * @deprecated Superseded by {@link NullBrandCommand}. Kept for binary compatibility.
 * This class is no longer registered and has no effect.
 */
@Deprecated
public final class ReloadCommand {
    private ReloadCommand() {}
    /** @deprecated Use {@link NullBrandCommand.Velocity} */
    @Deprecated
    public static final class Velocity {}
    /** @deprecated Use {@link NullBrandCommand.Bungee} */
    @Deprecated
    public static final class Bungee {}
    /** @deprecated Use {@link NullBrandCommand.Spigot} */
    @Deprecated
    public static final class Spigot {}
}