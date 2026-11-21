package java.util.concurrent;

import java.util.Collection;
import java.util.Queue;

/**
 * A {@link Queue} that additionally supports blocking operations.
 *
 * Implementations use Object.wait/notify on an internal monitor; the
 * scheduler turns a blocked wait into a coroutine suspension, so blocking
 * queue operations do not pin a carrier thread.
 */
public interface BlockingQueue<E> extends Queue<E>
{
    void put(E value) throws InterruptedException;

    boolean offer(E value, long timeout, TimeUnit unit) throws InterruptedException;

    E take() throws InterruptedException;

    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    int remainingCapacity();

    int drainTo(Collection<? super E> target);

    int drainTo(Collection<? super E> target, int maxElements);
}
