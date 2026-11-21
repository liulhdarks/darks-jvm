package java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class CopyOnWriteArraySet<E> implements Set<E>
{
    private final CopyOnWriteArrayList<E> list;

    public CopyOnWriteArraySet()
    {
        list = new CopyOnWriteArrayList<E>();
    }

    public CopyOnWriteArraySet(Collection<? extends E> values)
    {
        list = new CopyOnWriteArrayList<E>(values);
    }

    public int size()
    {
        return list.size();
    }

    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    public boolean contains(Object value)
    {
        return list.contains(value);
    }

    public boolean add(E value)
    {
        return list.addIfAbsent(value);
    }

    public boolean remove(Object value)
    {
        return list.remove(value);
    }

    public void clear()
    {
        list.clear();
    }

    public Object[] toArray()
    {
        return list.toArray();
    }

    public Iterator<E> iterator()
    {
        return list.iterator();
    }

    public String toString()
    {
        return list.toString();
    }
}
