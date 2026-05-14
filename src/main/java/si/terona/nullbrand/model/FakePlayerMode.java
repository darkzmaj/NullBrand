package si.terona.nullbrand.model;
/** Controls how fake players are added to the displayed online count. */
public enum FakePlayerMode {
    /** Add a fixed static amount to the real online count. */
    STATIC,
    /** Add a random value between min and max each cache interval. */
    RANDOM,
    /** Multiply the real online count by a configurable factor. */
    SMART,
    /** Vary the count based on time of day and day of week. */
    TIME_BASED
}
