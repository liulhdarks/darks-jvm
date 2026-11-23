package java.util.concurrent;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An unbounded blocking queue ordered by a binary heap.
 */
public class PriorityBlockingQueue<E> implements BlockingQueue<E>
{
    private final Object lock = new Object();
    private final Comparator<? super E> comparator;
    private Object[] heap;
    private int count;

    public PriorityBlockingQueue()
    {
        this(11, null);
    }

    public PriorityBlockingQueue(int initialCapacity)
    {
        this(initialCapacity, null);
    }

    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator)
    {
        if (initialCapacity < 1)
        {
            throw new IllegalArgumentException("initialCapacity < 1");
        }
        this.heap = new Object[initialCapacity];
        this.comparator = comparator;
    }

    public PriorityBlockingQueue(Comparator<? super E> comparator)
    {
        this(11, comparator);
    }

    public PriorityBlockingQueue(Collection<? extends E> initial)
    {
        this(initial, null);
    }

    public PriorityBlockingQueue(Collection<? extends E> initial, Comparator<? super E> comparator)
    {
        this(Math.max(initial.size(), 11), comparator);
        Iterator<? extends E> it = initial.iterator();
        while (it.hasNext())
        {
            add(it.next());
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(E a, E b)
    {
        if (comparator != null)
        {
            return comparator.compare(a, b);
        }
        return ((Comparable<? super E>)a).compareTo(b);
    }

    private void grow()
    {
        if (count == heap.length)
        {
            Object[] bigger = new Object[heap.length * 2];
            System.arraycopy(heap, 0, bigger, 0, count);
            heap = bigger;
        }
    }

    private void siftUp(int index)
    {
        E value = (E)heap[index];
        while (index > 0)
        {
            int parent = (index - 1) >>> 1;
            E up = (E)heap[parent];
            if (compare(value, up) >= 0)
            {
                break;
            }
            heap[index] = up;
            index = parent;
        }
        heap[index] = value;
    }

    private void siftDown(int index)
    {
        E value = (E)heap[index];
        int half = count >>> 1;
        while (index < half)
        {
            int child = index * 2 + 1;
            E best = (E)heap[child];
            int right = child + 1;
            if (right < count && compare((E)heap[right], best) < 0)
            {
                best = (E)heap[right];
                child = right;
            }
            if (compare(value, best) <= 0)
            {
                break;
            }
            heap[index] = best;
            index = child;
        }
        heap[index] = value;
    }

    public boolean add(E value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
        {
            grow();
            heap[count] = value;
            siftUp(count);
            count++;
            lock.notifyAll();
            return true;
        }
    }

    public boolean offer(E value)
    {
        return add(value);
    }

    public void put(E value) throws InterruptedException
    {
        add(value);
    }

    public boolean offer(E value, long timeout, TimeUnit unit) throws InterruptedException
    {
        return add(value);
    }

    public E poll()
    {
        synchronized (lock)
        {
            if (count == 0)
            {
                return null;
            }
            E head = (E)heap[0];
            count--;
            heap[0] = heap[count];
            heap[count] = null;
            if (count > 0)
            {
                siftDown(0);
            }
            lock.notifyAll();
            return head;
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
            E head = (E)heap[0];
            count--;
            heap[0] = heap[count];
            heap[count] = null;
            if (count > 0)
            {
                siftDown(0);
            }
            lock.notifyAll();
            return head;
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
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
            E head = (E)heap[0];
            count--;
            heap[0] = heap[count];
            heap[count] = null;
            if (count > 0)
            {
                siftDown(0);
            }
            lock.notifyAll();
            return head;
        }
    }

    public E peek()
    {
        synchronized (lock)
        {
            return count == 0 ? null : (E)heap[0];
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
        return Integer.MAX_VALUE;
    }

    public Comparator<? super E> comparator()
    {
        return comparator;
    }

    public boolean contains(Object value)
    {
        synchronized (lock)
        {
            for (int i = 0; i < count; i++)
            {
                if (value == null ? heap[i] == null : value.equals(heap[i]))
                {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean remove(Object value)
    {
        synchronized (lock)
        {
            for (int i = 0; i < count; i++)
            {
                if (value == null ? heap[i] == null : value.equals(heap[i]))
                {
                    int last = --count;
                    heap[i] = heap[last];
                    heap[last] = null;
                    if (i < count)
                    {
                        siftDown(i);
                        siftUp(i);
                    }
                    lock.notifyAll();
                    return true;
                }
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
                target.add(poll());
                moved++;
            }
            return moved;
        }
    }

    public void clear()
    {
        synchronized (lock)
        {
            for (int i = 0; i < count; i++)
            {
                heap[i] = null;
            }
            count = 0;
            lock.notifyAll();
        }
    }

    public Object[] toArray()
    {
        synchronized (lock)
        {
            Object[] copy = new Object[count];
            System.arraycopy(heap, 0, copy, 0, count);
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
