package java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A zero-capacity queue where each insert must rendezvous with a take.
 *
 * Only a single handoff slot is modelled; producers wait until a consumer
 * takes the value.  {@link #offer(Object)} succeeds only when a consumer is
 * already waiting, which is exactly what ThreadPoolExecutor's cached-pool
 * handoff needs.
 */
public class SynchronousQueue<E> implements BlockingQueue<E>
{
    private final Object lock = new Object();
    private E item;
    private boolean filled;
    private int waitingConsumers;

    public SynchronousQueue()
    {
    }

    public SynchronousQueue(boolean fair)
    {
    }

    public void put(E value) throws InterruptedException
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
        {
            while (filled)
            {
                lock.wait();
            }
            item = value;
            filled = true;
            lock.notifyAll();
            while (filled)
            {
                lock.wait();
            }
        }
    }

    public boolean offer(E value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
        {
            if (filled || waitingConsumers == 0)
            {
                return false;
            }
            item = value;
            filled = true;
            lock.notifyAll();
            return true;
        }
    }

    public boolean offer(E value, long timeout, TimeUnit unit) throws InterruptedException
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        synchronized (lock)
        {
            while (filled)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                lock.wait(remaining);
            }
            item = value;
            filled = true;
            lock.notifyAll();
            while (filled)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    if (filled && item == value)
                    {
                        item = null;
                        filled = false;
                        lock.notifyAll();
                    }
                    return false;
                }
                lock.wait(remaining);
            }
            return true;
        }
    }

    public E take() throws InterruptedException
    {
        synchronized (lock)
        {
            waitingConsumers++;
            try
            {
                while (!filled)
                {
                    lock.wait();
                }
                E value = item;
                item = null;
                filled = false;
                lock.notifyAll();
                return value;
            }
            finally
            {
                waitingConsumers--;
            }
        }
    }

    public E poll()
    {
        synchronized (lock)
        {
            if (!filled)
            {
                return null;
            }
            E value = item;
            item = null;
            filled = false;
            lock.notifyAll();
            return value;
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        synchronized (lock)
        {
            waitingConsumers++;
            try
            {
                while (!filled)
                {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0)
                    {
                        return null;
                    }
                    lock.wait(remaining);
                }
                E value = item;
                item = null;
                filled = false;
                lock.notifyAll();
                return value;
            }
            finally
            {
                waitingConsumers--;
            }
        }
    }

    public E peek()
    {
        synchronized (lock)
        {
            return filled ? item : null;
        }
    }

    public boolean add(E value)
    {
        if (offer(value))
        {
            return true;
        }
        throw new IllegalStateException("no waiting consumer");
    }

    public E remove()
    {
        E value = poll();
        if (value == null)
        {
            throw new NoSuchElementException();
        }
        return value;
    }

    public E element()
    {
        E value = peek();
        if (value == null)
        {
            throw new NoSuchElementException();
        }
        return value;
    }

    public int size()
    {
        synchronized (lock)
        {
            return filled ? 1 : 0;
        }
    }

    public boolean isEmpty()
    {
        synchronized (lock)
        {
            return !filled;
        }
    }

    public int remainingCapacity()
    {
        synchronized (lock)
        {
            return filled ? 0 : 1;
        }
    }

    public boolean contains(Object value)
    {
        synchronized (lock)
        {
            return filled && (value == null ? item == null : value.equals(item));
        }
    }

    public boolean remove(Object value)
    {
        synchronized (lock)
        {
            if (filled && (value == null ? item == null : value.equals(item)))
            {
                item = null;
                filled = false;
                lock.notifyAll();
                return true;
            }
            return false;
        }
    }

    public int drainTo(Collection<? super E> target)
    {
        return drainTo(target, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> target, int maxElements)
    {
        int moved = 0;
        while (moved < maxElements)
        {
            E value = poll();
            if (value == null)
            {
                break;
            }
            target.add(value);
            moved++;
        }
        return moved;
    }

    public void clear()
    {
        synchronized (lock)
        {
            item = null;
            filled = false;
            lock.notifyAll();
        }
    }

    public Object[] toArray()
    {
        synchronized (lock)
        {
            if (!filled)
            {
                return new Object[0];
            }
            Object[] copy = new Object[1];
            copy[0] = item;
            return copy;
        }
    }

    public Iterator<E> iterator()
    {
        final Object[] snapshot = toArray();
        return new Iterator<E>()
        {
            private int index;

            public boolean hasNext()
            {
                return index < snapshot.length;
            }

            public E next()
            {
                if (index >= snapshot.length)
                {
                    throw new NoSuchElementException();
                }
                return (E)snapshot[index++];
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    public String toString()
    {
        return "SynchronousQueue";
    }
}
