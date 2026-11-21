package darks;

/**
 * Deterministic-mode hooks (B5).
 *
 * The VM enables deterministic execution only when the launcher is given
 * {@code -Ddarks.deterministic=<seed>} (and optionally
 * {@code -Ddarks.record=<file>} / {@code -Ddarks.replay=<file>}).  Outside
 * that mode every method here is a cheap no-op and {@link #enabled()} is
 * false, so this class is safe to call from runtime code such as
 * java.util.Random.
 */
public final class Deterministic
{
    private Deterministic()
    {
    }

    /** True when the current VM runs in deterministic mode. */
    public static native boolean enabled();

    /** FNV-1a hash of the decision log produced so far. */
    public static native long eventHash();

    /** Number of decision-log events produced so far. */
    public static native int eventCount();

    /** Virtual clock in nanoseconds (real clock when not deterministic). */
    public static native long nanoTime();

    /** Virtual clock in milliseconds since the epoch. */
    public static native long currentTimeMillis();

    /**
     * Records (or, while replaying, returns the recorded value of) one
     * java.util.Random draw.  {@code bits} is the argument of
     * {@code Random.next(bits)}, {@code value} the generated value.
     */
    public static native int nextRandom(int bits, int value);

    /** Marks a boundary where execution leaves deterministic territory. */
    public static native void boundary(String what);
}
