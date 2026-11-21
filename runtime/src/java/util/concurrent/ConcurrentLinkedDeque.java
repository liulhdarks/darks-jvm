package java.util.concurrent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

public class ConcurrentLinkedDeque<E> implements Deque<E>
{
    private final ArrayDeque<E> delegate = new ArrayDeque<E>();

    public ConcurrentLinkedDeque()
    {
    }

    public ConcurrentLinkedDeque(Collection<? extends E> values)
    {
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            add(it.next());
        }
    }

    public synchronized int size()
    {
        return delegate.size();
    }

    public synchronized boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public synchronized void addFirst(E value)
    {
        delegate.addFirst(value);
    }

    public synchronized void addLast(E value)
    {
        delegate.addLast(value);
    }

    public synchronized boolean add(E value)
    {
        return delegate.add(value);
    }

    public synchronized boolean offer(E value)
    {
        return delegate.offer(value);
    }

    public synchronized boolean offerFirst(E value)
    {
        return delegate.offerFirst(value);
    }

    public synchronized boolean offerLast(E value)
    {
        return delegate.offerLast(value);
    }

    public synchronized E removeFirst()
    {
        return delegate.removeFirst();
    }

    public synchronized E removeLast()
    {
        return delegate.removeLast();
    }

    public synchronized E pollFirst()
    {
        return delegate.pollFirst();
    }

    public synchronized E pollLast()
    {
        return delegate.pollLast();
    }

    public synchronized E poll()
    {
        return delegate.poll();
    }

    public synchronized E getFirst()
    {
        return delegate.getFirst();
    }

    public synchronized E getLast()
    {
        return delegate.getLast();
    }

    public synchronized E peekFirst()
    {
        return delegate.peekFirst();
    }

    public synchronized E peekLast()
    {
        return delegate.peekLast();
    }

    public synchronized E peek()
    {
        return delegate.peek();
    }

    public synchronized E element()
    {
        return delegate.element();
    }

    public synchronized E remove()
    {
        return delegate.remove();
    }

    public synchronized void push(E value)
    {
        delegate.push(value);
    }

    public synchronized E pop()
    {
        return delegate.pop();
    }

    public synchronized boolean contains(Object value)
    {
        return delegate.contains(value);
    }

    public synchronized boolean remove(Object value)
    {
        return delegate.remove(value);
    }

    public synchronized boolean removeFirstOccurrence(Object value)
    {
        return delegate.removeFirstOccurrence(value);
    }

    public synchronized boolean removeLastOccurrence(Object value)
    {
        return delegate.removeLastOccurrence(value);
    }

    public synchronized Object[] toArray()
    {
        return delegate.toArray();
    }

    public synchronized Iterator<E> iterator()
    {
        return delegate.iterator();
    }

    public synchronized void clear()
    {
        delegate.clear();
    }

    public synchronized String toString()
    {
        return delegate.toString();
    }
}
