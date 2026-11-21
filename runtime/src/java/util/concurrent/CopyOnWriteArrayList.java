package java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A list where every mutation replaces the backing array, so iterators see a
 * stable snapshot and never throw ConcurrentModificationException.
 *
 * Reads take the same monitor as writes: the VM does not provide volatile
 * array-element semantics, so an unsynchronized read could observe a stale
 * array.  Writes still copy the whole array, as in the JDK implementation.
 */
public class CopyOnWriteArrayList<E> implements List<E>
{
    private final Object lock = new Object();
    private Object[] array;

    public CopyOnWriteArrayList()
    {
        array = new Object[0];
    }

    public CopyOnWriteArrayList(Collection<? extends E> initial)
    {
        Object[] values = initial.toArray();
        array = new Object[values.length];
        System.arraycopy(values, 0, array, 0, values.length);
    }

    public int size()
    {
        synchronized (lock)
        {
            return array.length;
        }
    }

    public boolean isEmpty()
    {
        synchronized (lock)
        {
            return array.length == 0;
        }
    }

    public boolean contains(Object value)
    {
        return indexOf(value) >= 0;
    }

    public int indexOf(Object value)
    {
        synchronized (lock)
        {
            for (int i = 0; i < array.length; i++)
            {
                if (value == null ? array[i] == null : value.equals(array[i]))
                {
                    return i;
                }
            }
            return -1;
        }
    }

    public int lastIndexOf(Object value)
    {
        synchronized (lock)
        {
            for (int i = array.length - 1; i >= 0; i--)
            {
                if (value == null ? array[i] == null : value.equals(array[i]))
                {
                    return i;
                }
            }
            return -1;
        }
    }

    public E get(int index)
    {
        synchronized (lock)
        {
            if (index < 0 || index >= array.length)
            {
                throw new IndexOutOfBoundsException("index " + index);
            }
            return (E)array[index];
        }
    }

    public E set(int index, E value)
    {
        synchronized (lock)
        {
            if (index < 0 || index >= array.length)
            {
                throw new IndexOutOfBoundsException("index " + index);
            }
            Object[] copy = new Object[array.length];
            System.arraycopy(array, 0, copy, 0, array.length);
            E old = (E)copy[index];
            copy[index] = value;
            array = copy;
            return old;
        }
    }

    public boolean add(E value)
    {
        synchronized (lock)
        {
            Object[] copy = new Object[array.length + 1];
            System.arraycopy(array, 0, copy, 0, array.length);
            copy[array.length] = value;
            array = copy;
            return true;
        }
    }

    public void add(int index, E value)
    {
        synchronized (lock)
        {
            if (index < 0 || index > array.length)
            {
                throw new IndexOutOfBoundsException("index " + index);
            }
            Object[] copy = new Object[array.length + 1];
            System.arraycopy(array, 0, copy, 0, index);
            copy[index] = value;
            System.arraycopy(array, index, copy, index + 1, array.length - index);
            array = copy;
        }
    }

    public boolean addIfAbsent(E value)
    {
        synchronized (lock)
        {
            for (int i = 0; i < array.length; i++)
            {
                if (value == null ? array[i] == null : value.equals(array[i]))
                {
                    return false;
                }
            }
            Object[] copy = new Object[array.length + 1];
            System.arraycopy(array, 0, copy, 0, array.length);
            copy[array.length] = value;
            array = copy;
            return true;
        }
    }

    public E remove(int index)
    {
        synchronized (lock)
        {
            if (index < 0 || index >= array.length)
            {
                throw new IndexOutOfBoundsException("index " + index);
            }
            E old = (E)array[index];
            Object[] copy = new Object[array.length - 1];
            System.arraycopy(array, 0, copy, 0, index);
            System.arraycopy(array, index + 1, copy, index, array.length - index - 1);
            array = copy;
            return old;
        }
    }

    public boolean removeAll(Collection<?> values)
    {
        if (values == null)
        {
            throw new NullPointerException("values");
        }
        synchronized (lock)
        {
            Object[] kept = new Object[array.length];
            int count = 0;
            for (int i = 0; i < array.length; i++)
            {
                if (!values.contains(array[i]))
                {
                    kept[count++] = array[i];
                }
            }
            if (count == array.length)
            {
                return false;
            }
            Object[] copy = new Object[count];
            System.arraycopy(kept, 0, copy, 0, count);
            array = copy;
            return true;
        }
    }

    public boolean retainAll(Collection<?> values)
    {
        if (values == null)
        {
            throw new NullPointerException("values");
        }
        synchronized (lock)
        {
            Object[] kept = new Object[array.length];
            int count = 0;
            for (int i = 0; i < array.length; i++)
            {
                if (values.contains(array[i]))
                {
                    kept[count++] = array[i];
                }
            }
            if (count == array.length)
            {
                return false;
            }
            Object[] copy = new Object[count];
            System.arraycopy(kept, 0, copy, 0, count);
            array = copy;
            return true;
        }
    }

    public boolean removeIf(java.util.function.Predicate<? super E> filter)
    {
        if (filter == null)
        {
            throw new NullPointerException("filter");
        }
        synchronized (lock)
        {
            Object[] kept = new Object[array.length];
            int count = 0;
            for (int i = 0; i < array.length; i++)
            {
                if (!filter.test((E)array[i]))
                {
                    kept[count++] = array[i];
                }
            }
            if (count == array.length)
            {
                return false;
            }
            Object[] copy = new Object[count];
            System.arraycopy(kept, 0, copy, 0, count);
            array = copy;
            return true;
        }
    }

    public boolean remove(Object value)
    {
        synchronized (lock)
        {
            int index = -1;
            for (int i = 0; i < array.length; i++)
            {
                if (value == null ? array[i] == null : value.equals(array[i]))
                {
                    index = i;
                    break;
                }
            }
            if (index < 0)
            {
                return false;
            }
            Object[] copy = new Object[array.length - 1];
            System.arraycopy(array, 0, copy, 0, index);
            System.arraycopy(array, index + 1, copy, index, array.length - index - 1);
            array = copy;
            return true;
        }
    }

    public boolean addAll(Collection<? extends E> values)
    {
        return addAll(arrayLength(), values);
    }

    public boolean addAll(int index, Collection<? extends E> values)
    {
        Object[] added = values.toArray();
        if (added.length == 0)
        {
            return false;
        }
        synchronized (lock)
        {
            if (index < 0 || index > array.length)
            {
                throw new IndexOutOfBoundsException("index " + index);
            }
            Object[] copy = new Object[array.length + added.length];
            System.arraycopy(array, 0, copy, 0, index);
            System.arraycopy(added, 0, copy, index, added.length);
            System.arraycopy(array, index, copy, index + added.length, array.length - index);
            array = copy;
            return true;
        }
    }

    private int arrayLength()
    {
        synchronized (lock)
        {
            return array.length;
        }
    }

    public void clear()
    {
        synchronized (lock)
        {
            array = new Object[0];
        }
    }

    public Object[] toArray()
    {
        synchronized (lock)
        {
            Object[] copy = new Object[array.length];
            System.arraycopy(array, 0, copy, 0, array.length);
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
