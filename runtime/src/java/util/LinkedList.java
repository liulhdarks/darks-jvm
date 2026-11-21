package java.util;

// Doubly linked list semantics; array-backed for simplicity.  List methods
// accept null elements, while the Deque operations reject them (as the
// Deque contract requires).
public class LinkedList<E> implements List<E>, Deque<E>
{
    private Object[] elements;
    private int count;

    public LinkedList()
    {
        elements = new Object[10];
    }

    public LinkedList(Collection<? extends E> values)
    {
        this();
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

    private static void checkValue(Object value)
    {
        if (value == null)
        {
            throw new NullPointerException("null element");
        }
    }

    private void checkIndex(int index)
    {
        if (index < 0 || index >= count)
        {
            throw new IndexOutOfBoundsException("index " + index + ", size " + count);
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

    public boolean addAll(int index, Collection<? extends E> values)
    {
        if (index < 0 || index > count)
        {
            throw new IndexOutOfBoundsException("index " + index);
        }
        int position = index;
        boolean changed = false;
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            add(position, it.next());
            position++;
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

    public int lastIndexOf(Object value)
    {
        for (int i = count - 1; i >= 0; i--)
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

    // Deque operations

    public void addFirst(E value)
    {
        checkValue(value);
        add(0, value);
    }

    public void addLast(E value)
    {
        checkValue(value);
        add(value);
    }

    public boolean offer(E value)
    {
        addLast(value);
        return true;
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
        return remove(0);
    }

    public E removeLast()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        return remove(count - 1);
    }

    public E poll()
    {
        return count == 0 ? null : remove(0);
    }

    public E pollFirst()
    {
        return poll();
    }

    public E pollLast()
    {
        return count == 0 ? null : remove(count - 1);
    }

    public E element()
    {
        return getFirst();
    }

    public E getFirst()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        return (E)elements[0];
    }

    public E getLast()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        return (E)elements[count - 1];
    }

    public E peek()
    {
        return count == 0 ? null : (E)elements[0];
    }

    public E peekFirst()
    {
        return peek();
    }

    public E peekLast()
    {
        return count == 0 ? null : (E)elements[count - 1];
    }

    public void push(E value)
    {
        addFirst(value);
    }

    public E pop()
    {
        return removeFirst();
    }

    public E remove()
    {
        return removeFirst();
    }

    public boolean removeFirstOccurrence(Object value)
    {
        return remove(value);
    }

    public boolean removeLastOccurrence(Object value)
    {
        int index = lastIndexOf(value);
        if (index < 0)
        {
            return false;
        }
        remove(index);
        return true;
    }

    private class Itr implements Iterator<E>
    {
        private int cursor;
        private int last = -1;

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
            last = cursor;
            return (E)elements[cursor++];
        }

        public void remove()
        {
            if (last < 0)
            {
                throw new IllegalStateException();
            }
            LinkedList.this.remove(last);
            cursor = last;
            last = -1;
        }
    }
}
