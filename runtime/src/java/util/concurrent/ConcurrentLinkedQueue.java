package java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * An unbounded thread-safe FIFO queue.
 *
 * The queue is CAS-free: all mutations are serialized on one monitor.  Reads
 * that need a stable answer (size, toArray, iteration) also take the monitor,
 * so no volatile field support is required from the VM.
 */
public class ConcurrentLinkedQueue<E> implements Queue<E>
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
    private Node<E> head;
    private Node<E> tail;
    private int count;

    public ConcurrentLinkedQueue()
    {
    }

    public ConcurrentLinkedQueue(Collection<? extends E> initial)
    {
        Iterator<? extends E> it = initial.iterator();
        while (it.hasNext())
        {
            offer(it.next());
        }
    }

    public boolean add(E value)
    {
        return offer(value);
    }

    public boolean offer(E value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
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
            return true;
        }
    }

    public E poll()
    {
        synchronized (lock)
        {
            if (head == null)
            {
                return null;
            }
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

    public E peek()
    {
        synchronized (lock)
        {
            return head == null ? null : head.item;
        }
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
                    return true;
                }
                prev = node;
                node = node.next;
            }
            return false;
        }
    }

    public void clear()
    {
        synchronized (lock)
        {
            while (head != null)
            {
                Node<E> node = head;
                head = node.next;
                node.next = null;
                node.item = null;
            }
            tail = null;
            count = 0;
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
