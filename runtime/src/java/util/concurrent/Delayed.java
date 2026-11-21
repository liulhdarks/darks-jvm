package java.util.concurrent;

/**
 * An object that should be acted on after a given delay.
 */
public interface Delayed extends Comparable<Delayed>
{
    long getDelay(TimeUnit unit);
}
