package java.util.concurrent;

/**
 * A {@link Future} that can be delayed and cancelled.
 */
public interface ScheduledFuture<V> extends Future<V>, Delayed
{
}
