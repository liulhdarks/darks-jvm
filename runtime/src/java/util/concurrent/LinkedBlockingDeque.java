package java.util.concurrent;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An optionally bounded double-ended queue backed by linked nodes.
 *
 * Blocking operations poll the deque with a short sleep instead of using
 * Object.wait(): a wait/notify handoff can re-queue a parked coroutine while
 * its carrier is still unwinding, which corrupts the ready queue.
 */
public class LinkedBlockingDeque<E> implements BlockingQueue<E>, Deque<E>
{
    private static final class Node<E>
    {
        E item;
        Node<E> prev;
        Node<E> next;

        Node(E item)
        {
            this.item = item;
        }
    }

    private final Object lock = new Object();
    private final int capacity;
    private Node<E> first;
    private Node<E> last;
    private int count;

    public LinkedBlockingDeque()
    {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingDeque(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity <= 0");
        }
        this.capacity = capacity;
    }

    public LinkedBlockingDeque(Collection<? extends E> values)
    {
        this(Integer.MAX_VALUE);
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            addLast(it.next());
        }
    }

    private void linkFirst(E value)
    {
        Node<E> node = new Node<E>(value);
        node.next = first;
        if (first != null)
        {
            first.prev = node;
        }
        first = node;
        if (last == null)
        {
            last = node;
        }
        count++;
    }

    private void linkLast(E value)
    {
        Node<E> node = new Node<E>(value);
        node.prev = last;
        if (last != null)
        {
            last.next = node;
        }
        last = node;
        if (first == null)
        {
            first = node;
        }
        count++;
    }

    private E unlinkFirst()
    {
        Node<E> node = first;
        E value = node.item;
        first = node.next;
        if (first == null)
        {
            last = null;
        }
        else
        {
            first.prev = null;
        }
        node.item = null;
        node.next = null;
        count--;
        return value;
    }

    private E unlinkLast()
    {
        Node<E> node = last;
        E value = node.item;
        last = node.prev;
        if (last == null)
        {
            first = null;
        }
        else
        {
            last.next = null;
        }
        node.item = null;
        node.prev = null;
        count--;
        return value;
    }

    private boolean unlink(Node<E> node)
    {
        if (node == null)
        {
            return false;
        }
        if (node.prev != null)
        {
            node.prev.next = node.next;
        }
        else
        {
            first = node.next;
        }
        if (node.next != null)
        {
            node.next.prev = node.prev;
        }
        else
        {
            last = node.prev;
        }
        node.item = null;
        node.prev = null;
        node.next = null;
        count--;
        return true;
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
        if (value == null)
        {
            return false;
        }
        synchronized (lock)
        {
            for (Node<E> node = first; node != null; node = node.next)
            {
                if (value.equals(node.item))
                {
                    return true;
                }
            }
            return false;
        }
    }

    public void clear()
    {
        synchronized (lock)
        {
            first = null;
            last = null;
            count = 0;
        }
    }

    public Object[] toArray()
    {
        synchronized (lock)
        {
            Object[] values = new Object[count];
            int index = 0;
            for (Node<E> node = first; node != null; node = node.next)
            {
                values[index++] = node.item;
            }
            return values;
        }
    }

    public Iterator<E> iterator()
    {
        synchronized (lock)
        {
            return new DequeIterator<E>(this, first);
        }
    }

    private static final class DequeIterator<E> implements Iterator<E>
    {
        private final LinkedBlockingDeque<E> owner;
        private Node<E> next;
        private Node<E> last;

        DequeIterator(LinkedBlockingDeque<E> owner, Node<E> first)
        {
            this.owner = owner;
            this.next = first;
        }

        public boolean hasNext()
        {
            return next != null;
        }

        public E next()
        {
            if (next == null)
            {
                throw new NoSuchElementException("no more elements");
            }
            last = next;
            next = next.next;
            return last.item;
        }

        public void remove()
        {
            if (last == null)
            {
                throw new IllegalStateException("next not called");
            }
            synchronized (owner.lock)
            {
                owner.unlink(last);
            }
            last = null;
        }
    }

    public boolean add(E value)
    {
        addLast(value);
        return true;
    }

    public boolean offer(E value)
    {
        return offerLast(value);
    }

    public void put(E value) throws InterruptedException
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        for (;;)
        {
            synchronized (lock)
            {
                if (count < capacity)
                {
                    linkLast(value);
                    return;
                }
            }
            Thread.sleep(1);
        }
    }

    public boolean offer(E value, long timeout, TimeUnit unit) throws InterruptedException
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        for (;;)
        {
            synchronized (lock)
            {
                if (count < capacity)
                {
                    linkLast(value);
                    return true;
                }
            }
            if (System.currentTimeMillis() >= deadline)
            {
                return false;
            }
            Thread.sleep(1);
        }
    }

    public E take() throws InterruptedException
    {
        for (;;)
        {
            synchronized (lock)
            {
                if (count > 0)
                {
                    return unlinkFirst();
                }
            }
            Thread.sleep(1);
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        for (;;)
        {
            synchronized (lock)
            {
                if (count > 0)
                {
                    return unlinkFirst();
                }
            }
            if (System.currentTimeMillis() >= deadline)
            {
                return null;
            }
            Thread.sleep(1);
        }
    }

    public E poll()
    {
        synchronized (lock)
        {
            return count > 0 ? unlinkFirst() : null;
        }
    }

    public E peek()
    {
        synchronized (lock)
        {
            return first == null ? null : first.item;
        }
    }

    public E element()
    {
        return getFirst();
    }

    public E remove()
    {
        return removeFirst();
    }

    public int remainingCapacity()
    {
        synchronized (lock)
        {
            return capacity - count;
        }
    }

    public int drainTo(Collection<? super E> target)
    {
        return drainTo(target, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> target, int maxElements)
    {
        if (target == this)
        {
            throw new IllegalArgumentException("cannot drain to itself");
        }
        synchronized (lock)
        {
            int moved = 0;
            while (moved < maxElements && count > 0)
            {
                target.add(unlinkFirst());
                moved++;
            }
            return moved;
        }
    }

    public void addFirst(E value)
    {
        if (!offerFirst(value))
        {
            throw new IllegalStateException("deque full");
        }
    }

    public void addLast(E value)
    {
        if (!offerLast(value))
        {
            throw new IllegalStateException("deque full");
        }
    }

    public boolean offerFirst(E value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
        {
            if (count >= capacity)
            {
                return false;
            }
            linkFirst(value);
            return true;
        }
    }

    public boolean offerLast(E value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        synchronized (lock)
        {
            if (count >= capacity)
            {
                return false;
            }
            linkLast(value);
            return true;
        }
    }

    public E removeFirst()
    {
        E value = pollFirst();
        if (value == null)
        {
            throw new NoSuchElementException("deque empty");
        }
        return value;
    }

    public E removeLast()
    {
        E value = pollLast();
        if (value == null)
        {
            throw new NoSuchElementException("deque empty");
        }
        return value;
    }

    public E pollFirst()
    {
        synchronized (lock)
        {
            return count > 0 ? unlinkFirst() : null;
        }
    }

    public E pollLast()
    {
        synchronized (lock)
        {
            return count > 0 ? unlinkLast() : null;
        }
    }

    public E getFirst()
    {
        synchronized (lock)
        {
            if (first == null)
            {
                throw new NoSuchElementException("deque empty");
            }
            return first.item;
        }
    }

    public E getLast()
    {
        synchronized (lock)
        {
            if (last == null)
            {
                throw new NoSuchElementException("deque empty");
            }
            return last.item;
        }
    }

    public E peekFirst()
    {
        synchronized (lock)
        {
            return first == null ? null : first.item;
        }
    }

    public E peekLast()
    {
        synchronized (lock)
        {
            return last == null ? null : last.item;
        }
    }

    public void push(E value)
    {
        addFirst(value);
    }

    public E pop()
    {
        return removeFirst();
    }

    public boolean removeFirstOccurrence(Object value)
    {
        if (value == null)
        {
            return false;
        }
        synchronized (lock)
        {
            for (Node<E> node = first; node != null; node = node.next)
            {
                if (value.equals(node.item))
                {
                    return unlink(node);
                }
            }
            return false;
        }
    }

    public boolean removeLastOccurrence(Object value)
    {
        if (value == null)
        {
            return false;
        }
        synchronized (lock)
        {
            for (Node<E> node = last; node != null; node = node.prev)
            {
                if (value.equals(node.item))
                {
                    return unlink(node);
                }
            }
            return false;
        }
    }

    public boolean remove(Object value)
    {
        return removeFirstOccurrence(value);
    }
}
