package java.util.concurrent;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CopyOnWriteArraySet<E> extends AbstractSet<E>
    implements java.io.Serializable
{
    private final List<E> elements = new ArrayList<E>();

    public CopyOnWriteArraySet()
    {
    }

    public CopyOnWriteArraySet(Collection<? extends E> values)
    {
        addAll(values);
    }

    public int size()
    {
        return elements.size();
    }

    public boolean isEmpty()
    {
        return elements.isEmpty();
    }

    public boolean contains(Object value)
    {
        return elements.contains(value);
    }

    public Iterator<E> iterator()
    {
        Object[] snapshot = elements.toArray();
        return new SnapshotIterator(snapshot);
    }

    public boolean add(E value)
    {
        if (elements.contains(value))
        {
            return false;
        }
        return elements.add(value);
    }

    public boolean remove(Object value)
    {
        return elements.remove(value);
    }

    public boolean addAll(Collection<? extends E> values)
    {
        boolean changed = false;
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            if (add(it.next()))
            {
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection<?> values)
    {
        boolean changed = false;
        Iterator<?> it = values.iterator();
        while (it.hasNext())
        {
            if (remove(it.next()))
            {
                changed = true;
            }
        }
        return changed;
    }

    public boolean retainAll(Collection<?> values)
    {
        boolean changed = false;
        Object[] snapshot = elements.toArray();
        for (int i = 0; i < snapshot.length; i++)
        {
            if (!values.contains(snapshot[i]) && elements.remove(snapshot[i]))
            {
                changed = true;
            }
        }
        return changed;
    }

    public void clear()
    {
        elements.clear();
    }

    public Object[] toArray()
    {
        return elements.toArray();
    }

    private class SnapshotIterator implements Iterator<E>
    {
        private final Object[] snapshot;
        private int cursor;

        SnapshotIterator(Object[] snapshot)
        {
            this.snapshot = snapshot;
        }

        public boolean hasNext()
        {
            return cursor < snapshot.length;
        }

        public E next()
        {
            if (cursor >= snapshot.length)
            {
                throw new java.util.NoSuchElementException();
            }
            return (E)snapshot[cursor++];
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
