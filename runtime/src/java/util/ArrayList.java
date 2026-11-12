package java.util;

public class ArrayList<E> implements List<E>
{
    private Object[] elements;
    private int count;

    public ArrayList()
    {
        elements = new Object[10];
    }

    public ArrayList(int capacity)
    {
        elements = new Object[capacity > 0 ? capacity : 1];
    }

    public ArrayList(Collection<? extends E> values)
    {
        this(values.size() > 0 ? values.size() : 1);
        addAll(values);
    }

    private void ensure(int needed)
    {
        if (needed <= elements.length)
        {
            return;
        }
        int size = elements.length * 2;
        if (size < needed)
        {
            size = needed;
        }
        Object[] grown = new Object[size];
        for (int i = 0; i < count; i++)
        {
            grown[i] = elements[i];
        }
        elements = grown;
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
        ensure(count + 1);
        elements[count++] = value;
        return true;
    }

    public void add(int index, E value)
    {
        if (index < 0 || index > count)
        {
            throw new IndexOutOfBoundsException("index " + index);
        }
        ensure(count + 1);
        for (int i = count; i > index; i--)
        {
            elements[i] = elements[i - 1];
        }
        elements[index] = value;
        count++;
    }

    public boolean addAll(Collection<? extends E> values)
    {
        Iterator<? extends E> it = values.iterator();
        boolean changed = false;
        while (it.hasNext())
        {
            add(it.next());
            changed = true;
        }
        return changed;
    }

    public E get(int index)
    {
        checkIndex(index);
        return (E)elements[index];
    }

    public E set(int index, E value)
    {
        checkIndex(index);
        E old = (E)elements[index];
        elements[index] = value;
        return old;
    }

    public E remove(int index)
    {
        checkIndex(index);
        E old = (E)elements[index];
        for (int i = index; i < count - 1; i++)
        {
            elements[i] = elements[i + 1];
        }
        elements[--count] = null;
        return old;
    }

    public boolean remove(Object value)
    {
        int index = indexOf(value);
        if (index < 0)
        {
            return false;
        }
        remove(index);
        return true;
    }

    public boolean contains(Object value)
    {
        return indexOf(value) >= 0;
    }

    public int indexOf(Object value)
    {
        for (int i = 0; i < count; i++)
        {
            if (value == null ? elements[i] == null : value.equals(elements[i]))
            {
                return i;
            }
        }
        return -1;
    }

    public void clear()
    {
        for (int i = 0; i < count; i++)
        {
            elements[i] = null;
        }
        count = 0;
    }

    public Object[] toArray()
    {
        Object[] result = new Object[count];
        for (int i = 0; i < count; i++)
        {
            result[i] = elements[i];
        }
        return result;
    }

    private void checkIndex(int index)
    {
        if (index < 0 || index >= count)
        {
            throw new IndexOutOfBoundsException("index " + index + ", size " + count);
        }
    }

    public Iterator<E> iterator()
    {
        return new Itr();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(elements[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private class Itr implements Iterator<E>
    {
        private int cursor;

        public boolean hasNext()
        {
            return cursor < count;
        }

        public E next()
        {
            if (cursor >= count)
            {
                throw new NoSuchElementException();
            }
            return (E)elements[cursor++];
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
