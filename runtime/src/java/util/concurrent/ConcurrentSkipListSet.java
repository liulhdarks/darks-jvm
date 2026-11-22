package java.util.concurrent;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class ConcurrentSkipListSet<E> extends TreeSet<E>
{
    public ConcurrentSkipListSet()
    {
        super();
    }

    public ConcurrentSkipListSet(Comparator<? super E> comparator)
    {
        super(comparator);
    }

    public ConcurrentSkipListSet(Collection<? extends E> source)
    {
        super(source);
    }

    public ConcurrentSkipListSet(java.util.SortedSet<E> source)
    {
        super(source);
    }

    public synchronized boolean add(E value)
    {
        return super.add(value);
    }

    public synchronized boolean remove(Object value)
    {
        return super.remove(value);
    }

    public synchronized boolean contains(Object value)
    {
        return super.contains(value);
    }

    public synchronized int size()
    {
        return super.size();
    }

    public synchronized java.util.Iterator<E> iterator()
    {
        return super.iterator();
    }

    public ConcurrentSkipListSet<E> clone()
    {
        return new ConcurrentSkipListSet<E>(this);
    }
}
