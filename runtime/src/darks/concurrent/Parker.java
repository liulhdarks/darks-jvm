package darks.concurrent;

/**
 * Single-waiter parking spot.
 *
 * The generation counter makes the handshake safe even when {@link #unpark()}
 * runs before {@link #park(long)}: the waiter captures the generation before
 * it drops the channel lock, and park returns immediately when the counter has
 * moved on.  Only the designated waiter is ever notified, so a wakeup never
 * disturbs a different thread.
 */
final class Parker
{
    private long generation;

    synchronized long generation()
    {
        return generation;
    }

    synchronized void park(long expected) throws InterruptedException
    {
        while (generation == expected)
        {
            wait();
        }
    }

    synchronized void unpark()
    {
        generation++;
        notifyAll();
    }
}
