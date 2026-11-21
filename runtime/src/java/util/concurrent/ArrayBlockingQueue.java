package java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A bounded FIFO queue backed by a circular array.
 *
 * The fair flag accepted by the constructors is ignored: waiters are woken
 * with notifyAll and contend for the monitor, so ordering is not guaranteed.
 */
public class ArrayBlockingQueue<E> implements BlockingQueue<E>
{
    private final Object lock = new Object();
    private final Object[] items;
    private int takeIndex;
    private int putIndex;
    private int count;

    public ArrayBlockingQueue(int capacity)
    {
        this(capacity, false);
    }

    public ArrayBlockingQueue(int capacity, boolean fair)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity <= 0");
        }
        items = new Object[capacity];
    }

    public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> initial)
    {
        this(capacity, fair);
        Iterator<? extends E> it = initial.iterator();
        while (it.hasNext())
        {
            if (!offer(it.next()))
            {
                throw new IllegalArgumentException("queue full");
            }
        }
    }

    private void enqueue(E value)
    {
        items[putIndex] = value;
        putIndex = (putIndex + 1) % items.length;
        count++;
    }

    private E dequeue()
    {
        Object value = items[takeIndex];
        items[takeIndex] = null;
        takeIndex = (takeIndex + 1) % items.length;
        count--;
        return (E)value;
    }

    private static long deadlineAfter(long timeout, TimeUnit unit)
    {
        return System.currentTimeMillis() + unit.toMillis(timeout);
    }

    public boolean add(E value)
    {
        if (offer(value))
        {
            return true;
        }
        throw new IllegalStateException("queue full");
    }

    public boolean offer(E value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
        {
            if (count == items.length)
            {
                return false;
            }
            enqueue(value);
            lock.notifyAll();
            return true;
        }
    }

    public void put(E value) throws InterruptedException
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
        {
            while (count == items.length)
            {
                lock.wait();
            }
            enqueue(value);
            lock.notifyAll();
        }
    }

    public boolean offer(E value, long timeout, TimeUnit unit) throws InterruptedException
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        long deadline = deadlineAfter(timeout, unit);
        synchronized (lock)
        {
            while (count == items.length)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                lock.wait(remaining);
            }
            enqueue(value);
            lock.notifyAll();
            return true;
        }
    }

    public E poll()
    {
        synchronized (lock)
        {
            if (count == 0)
            {
                return null;
            }
            E value = dequeue();
            lock.notifyAll();
            return value;
        }
    }

    public E take() throws InterruptedException
    {
        synchronized (lock)
        {
            while (count == 0)
            {
                lock.wait();
            }
            E value = dequeue();
            lock.notifyAll();
            return value;
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        long deadline = deadlineAfter(timeout, unit);
        synchronized (lock)
        {
            while (count == 0)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return null;
                }
                lock.wait(remaining);
            }
            E value = dequeue();
            lock.notifyAll();
            return value;
        }
    }

    public E peek()
    {
        synchronized (lock)
        {
            return count == 0 ? null : (E)items[takeIndex];
        }
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
            return count;
        }
    }

    public boolean isEmpty()
    {
        synchronized (lock)
        {
            return count == 0;
        }
    }

    public int remainingCapacity()
    {
        synchronized (lock)
        {
            return items.length - count;
        }
    }

    public boolean contains(Object value)
    {
        synchronized (lock)
        {
            int index = takeIndex;
            for (int i = 0; i < count; i++)
            {
                if (value == null ? items[index] == null : value.equals(items[index]))
                {
                    return true;
                }
                index = (index + 1) % items.length;
            }
            return false;
        }
    }

    public boolean remove(Object value)
    {
        synchronized (lock)
        {
            int index = takeIndex;
            for (int i = 0; i < count; i++)
            {
                if (value == null ? items[index] == null : value.equals(items[index]))
                {
                    removeAt(index);
                    lock.notifyAll();
                    return true;
                }
                index = (index + 1) % items.length;
            }
            return false;
        }
    }

    private void removeAt(int index)
    {
        while (index != putIndex)
        {
            int next = (index + 1) % items.length;
            items[index] = items[next];
            index = next;
        }
        putIndex = (putIndex + items.length - 1) % items.length;
        items[putIndex] = null;
        count--;
    }

    public int drainTo(Collection<? super E> target)
    {
        return drainTo(target, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> target, int maxElements)
    {
        if (target == null)
        {
            throw new NullPointerException("target");
        }
        if (target == this)
        {
            throw new IllegalArgumentException("drain to self");
        }
        synchronized (lock)
        {
            int moved = 0;
            while (moved < maxElements && count > 0)
            {
                target.add(dequeue());
                moved++;
            }
            if (moved > 0)
            {
                lock.notifyAll();
            }
            return moved;
        }
    }

    public void clear()
    {
        synchronized (lock)
        {
            while (count > 0)
            {
                dequeue();
            }
            lock.notifyAll();
        }
    }

    public Object[] toArray()
    {
        synchronized (lock)
        {
            Object[] copy = new Object[count];
            int index = takeIndex;
            for (int i = 0; i < count; i++)
            {
                copy[i] = items[index];
                index = (index + 1) % items.length;
            }
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
        StringBuilder text = new StringBuilder("[");
        Object[] snapshot = toArray();
        for (int i = 0; i < snapshot.length; i++)
        {
            if (i > 0)
            {
                text.append(", ");
            }
            text.append(snapshot[i]);
        }
        return text.append("]").toString();
    }
}
