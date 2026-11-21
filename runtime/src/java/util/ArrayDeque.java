package java.util;

// Resizable circular-buffer deque.  Null elements are rejected, matching
// java.util.ArrayDeque.
public class ArrayDeque<E> implements Deque<E>
{
    private Object[] elements;
    private int head;
    private int count;

    public ArrayDeque()
    {
        elements = new Object[16];
    }

    public ArrayDeque(int capacity)
    {
        elements = new Object[capacity > 0 ? capacity : 1];
    }

    public ArrayDeque(Collection<? extends E> values)
    {
        this(values.size() > 0 ? values.size() : 1);
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            addLast(it.next());
        }
    }

    private int slot(int index)
    {
        return (head + index) % elements.length;
    }

    private void ensure(int needed)
    {
        if (needed <= elements.length)
        {
            return;
        }
        int size = elements.length * 2;
        while (size < needed)
        {
            size = size * 2;
        }
        Object[] grown = new Object[size];
        for (int i = 0; i < count; i++)
        {
            grown[i] = elements[slot(i)];
        }
        elements = grown;
        head = 0;
    }

    private static void checkValue(Object value)
    {
        if (value == null)
        {
            throw new NullPointerException("null element");
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

    public void addFirst(E value)
    {
        checkValue(value);
        ensure(count + 1);
        head = (head - 1 + elements.length) % elements.length;
        elements[head] = value;
        count++;
    }

    public void addLast(E value)
    {
        checkValue(value);
        ensure(count + 1);
        elements[slot(count)] = value;
        count++;
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

    public boolean offerFirst(E value)
    {
        addFirst(value);
        return true;
    }

    public boolean offerLast(E value)
    {
        addLast(value);
        return true;
    }

    public E removeFirst()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        E value = (E)elements[head];
        elements[head] = null;
        head = (head + 1) % elements.length;
        count--;
        return value;
    }

    public E removeLast()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        int index = slot(count - 1);
        E value = (E)elements[index];
        elements[index] = null;
        count--;
        return value;
    }

    public E pollFirst()
    {
        return count == 0 ? null : removeFirst();
    }

    public E pollLast()
    {
        return count == 0 ? null : removeLast();
    }

    public E poll()
    {
        return pollFirst();
    }

    public E getFirst()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        return (E)elements[head];
    }

    public E getLast()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        return (E)elements[slot(count - 1)];
    }

    public E peekFirst()
    {
        return count == 0 ? null : (E)elements[head];
    }

    public E peekLast()
    {
        return count == 0 ? null : (E)elements[slot(count - 1)];
    }

    public E peek()
    {
        return peekFirst();
    }

    public E element()
    {
        return getFirst();
    }

    public E remove()
    {
        return removeFirst();
    }

    public void push(E value)
    {
        addFirst(value);
    }

    public E pop()
    {
        return removeFirst();
    }

    public boolean contains(Object value)
    {
        return indexOf(value) >= 0;
    }

    private int indexOf(Object value)
    {
        for (int i = 0; i < count; i++)
        {
            Object current = elements[slot(i)];
            if (value == null ? current == null : value.equals(current))
            {
                return i;
            }
        }
        return -1;
    }

    public boolean remove(Object value)
    {
        return removeFirstOccurrence(value);
    }

    public boolean removeFirstOccurrence(Object value)
    {
        int index = indexOf(value);
        if (index < 0)
        {
            return false;
        }
        removeAt(index);
        return true;
    }

    public boolean removeLastOccurrence(Object value)
    {
        for (int i = count - 1; i >= 0; i--)
        {
            Object current = elements[slot(i)];
            if (value == null ? current == null : value.equals(current))
            {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    private void removeAt(int index)
    {
        if (index == 0)
        {
            removeFirst();
            return;
        }
        if (index == count - 1)
        {
            removeLast();
            return;
        }
        for (int i = index; i < count - 1; i++)
        {
            elements[slot(i)] = elements[slot(i + 1)];
        }
        elements[slot(count - 1)] = null;
        count--;
    }

    public void clear()
    {
        for (int i = 0; i < count; i++)
        {
            elements[slot(i)] = null;
        }
        head = 0;
        count = 0;
    }

    public Object[] toArray()
    {
        Object[] result = new Object[count];
        for (int i = 0; i < count; i++)
        {
            result[i] = elements[slot(i)];
        }
        return result;
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
            sb.append(String.valueOf(elements[slot(i)]));
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
            return (E)elements[slot(cursor++)];
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
