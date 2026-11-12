package java.util;

public class HashSet<E> implements Set<E>
{
    private Object[] elements;
    private boolean[] used;
    private int count;

    public HashSet()
    {
        this(16);
    }

    public HashSet(int capacity)
    {
        int size = 1;
        while (size < capacity)
        {
            size = size * 2;
        }
        elements = new Object[size];
        used = new boolean[size];
    }

    public HashSet(Collection<? extends E> values)
    {
        this(values.size() * 2 + 1);
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            add(it.next());
        }
    }

    private static int spread(Object key)
    {
        if (key == null)
        {
            return 0;
        }
        int h = key.hashCode();
        h = h ^ (h >>> 16);
        return h & 0x7fffffff;
    }

    private int findIndex(Object key)
    {
        int mask = elements.length - 1;
        int index = spread(key) & mask;
        for (int step = 0; step < elements.length; step++)
        {
            if (!used[index])
            {
                return -1;
            }
            Object value = elements[index];
            if (value == key || (value != null && value.equals(key)))
            {
                return index;
            }
            index = (index + 1) & mask;
        }
        return -1;
    }

    private int emptyIndex(Object key)
    {
        int mask = elements.length - 1;
        int index = spread(key) & mask;
        for (int step = 0; step < elements.length; step++)
        {
            if (!used[index])
            {
                return index;
            }
            index = (index + 1) & mask;
        }
        return -1;
    }

    private void rebuild(int size)
    {
        Object[] oldElements = elements;
        boolean[] oldUsed = used;
        elements = new Object[size];
        used = new boolean[size];
        count = 0;
        for (int i = 0; i < oldElements.length; i++)
        {
            if (oldUsed[i])
            {
                int index = emptyIndex(oldElements[i]);
                elements[index] = oldElements[i];
                used[index] = true;
                count++;
            }
        }
    }

    public int size()
    {
        return count;
    }

    public boolean isEmpty()
    {
        return count == 0;
    }

    public boolean add(E value)
    {
        if (findIndex(value) >= 0)
        {
            return false;
        }
        if ((count + 1) * 2 > elements.length)
        {
            rebuild(elements.length * 2);
        }
        int index = emptyIndex(value);
        elements[index] = value;
        used[index] = true;
        count++;
        return true;
    }

    public boolean contains(Object value)
    {
        return findIndex(value) >= 0;
    }

    public boolean remove(Object value)
    {
        int index = findIndex(value);
        if (index < 0)
        {
            return false;
        }
        elements[index] = null;
        used[index] = false;
        count--;
        rebuild(elements.length);
        return true;
    }

    public void clear()
    {
        for (int i = 0; i < elements.length; i++)
        {
            elements[i] = null;
            used[i] = false;
        }
        count = 0;
    }

    public Object[] toArray()
    {
        Object[] result = new Object[count];
        int position = 0;
        for (int i = 0; i < elements.length; i++)
        {
            if (used[i])
            {
                result[position++] = elements[i];
            }
        }
        return result;
    }

    public Iterator<E> iterator()
    {
        return new Itr(this);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (int i = 0; i < elements.length; i++)
        {
            if (used[i])
            {
                if (!first)
                {
                    sb.append(", ");
                }
                sb.append(String.valueOf(elements[i]));
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static class Itr<E> implements Iterator<E>
    {
        private final HashSet<E> owner;
        private int cursor;
        private int last = -1;

        Itr(HashSet<E> owner)
        {
            this.owner = owner;
        }

        public boolean hasNext()
        {
            for (int i = cursor; i < owner.elements.length; i++)
            {
                if (owner.used[i])
                {
                    return true;
                }
            }
            return false;
        }

        public E next()
        {
            for (int i = cursor; i < owner.elements.length; i++)
            {
                if (owner.used[i])
                {
                    cursor = i + 1;
                    last = i;
                    return (E)owner.elements[i];
                }
            }
            throw new NoSuchElementException();
        }

        public void remove()
        {
            if (last < 0)
            {
                throw new IllegalStateException();
            }
            owner.remove(owner.elements[last]);
            last = -1;
            cursor = 0;
        }
    }
}
