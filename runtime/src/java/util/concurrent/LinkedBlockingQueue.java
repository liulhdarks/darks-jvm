package java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An optionally bounded FIFO queue backed by linked nodes.
 */
public class LinkedBlockingQueue<E> implements BlockingQueue<E>
{
    private static final class Node<E>
    {
        E item;
        Node<E> next;

        Node(E item)
        {
            this.item = item;
        }
    }

    private final Object lock = new Object();
    private final int capacity;
    private Node<E> head;
    private Node<E> tail;
    private int count;

    public LinkedBlockingQueue()
    {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingQueue(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity <= 0");
        }
        this.capacity = capacity;
    }

    public LinkedBlockingQueue(Collection<? extends E> initial)
    {
        this(Integer.MAX_VALUE);
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
        Node<E> node = new Node<E>(value);
        if (tail == null)
        {
            head = node;
            tail = node;
        }
        else
        {
            tail.next = node;
            tail = node;
        }
        count++;
    }

    private E dequeue()
    {
        Node<E> node = head;
        E value = node.item;
        head = node.next;
        if (head == null)
        {
            tail = null;
        }
        node.next = null;
        node.item = null;
        count--;
        return value;
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
            if (count == capacity)
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
            while (count == capacity)
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
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        synchronized (lock)
        {
            while (count == capacity)
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
            E value = dequeue();
            lock.notifyAll();
            return value;
        }
    }

    public E peek()
    {
        synchronized (lock)
        {
            return count == 0 ? null : head.item;
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
            return capacity - count;
        }
    }

    public boolean contains(Object value)
    {
        synchronized (lock)
        {
            Node<E> node = head;
            while (node != null)
            {
                if (value == null ? node.item == null : value.equals(node.item))
                {
                    return true;
                }
                node = node.next;
            }
            return false;
        }
    }

    public boolean remove(Object value)
    {
        synchronized (lock)
        {
            Node<E> prev = null;
            Node<E> node = head;
            while (node != null)
            {
                if (value == null ? node.item == null : value.equals(node.item))
                {
                    if (prev == null)
                    {
                        head = node.next;
                    }
                    else
                    {
                        prev.next = node.next;
                    }
                    if (tail == node)
                    {
                        tail = prev;
                    }
                    node.next = null;
                    node.item = null;
                    count--;
                    lock.notifyAll();
                    return true;
                }
                prev = node;
                node = node.next;
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
            int index = 0;
            Node<E> node = head;
            while (node != null)
            {
                copy[index++] = node.item;
                node = node.next;
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
