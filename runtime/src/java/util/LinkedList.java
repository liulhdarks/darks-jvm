package java.util;

// List + Deque implementation backed by an array list.  Indexed and deque
// operations share the same storage so the reporting order matches the
// insertion order a real LinkedList would produce.
public class LinkedList<E> implements List<E>, Deque<E>, Cloneable
{
    private final ArrayList<E> values;

    public LinkedList()
    {
        values = new ArrayList<E>();
    }

    public LinkedList(Collection<? extends E> source)
    {
        this();
        Iterator<? extends E> it = source.iterator();
        while (it.hasNext())
        {
            values.add(it.next());
        }
    }

    public int size()
    {
        return values.size();
    }

    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    public boolean add(E value)
    {
        return values.add(value);
    }

    public void add(int index, E value)
    {
        values.add(index, value);
    }

    public boolean addAll(Collection<? extends E> source)
    {
        return values.addAll(source);
    }

    public boolean addAll(int index, Collection<? extends E> source)
    {
        return values.addAll(index, source);
    }

    public E get(int index)
    {
        return values.get(index);
    }

    public E set(int index, E value)
    {
        return values.set(index, value);
    }

    public E remove(int index)
    {
        return values.remove(index);
    }

    public boolean remove(Object value)
    {
        return values.remove(value);
    }

    public boolean contains(Object value)
    {
        return values.contains(value);
    }

    public int indexOf(Object value)
    {
        return values.indexOf(value);
    }

    public int lastIndexOf(Object value)
    {
        return values.lastIndexOf(value);
    }

    public void clear()
    {
        values.clear();
    }

    public Object[] toArray()
    {
        return values.toArray();
    }

    public Iterator<E> iterator()
    {
        return values.iterator();
    }

    public void addFirst(E value)
    {
        values.add(0, value);
    }

    public void addLast(E value)
    {
        values.add(value);
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
        if (values.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return values.remove(0);
    }

    public E removeLast()
    {
        if (values.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return values.remove(values.size() - 1);
    }

    public E pollFirst()
    {
        return values.isEmpty() ? null : values.remove(0);
    }

    public E pollLast()
    {
        return values.isEmpty() ? null : values.remove(values.size() - 1);
    }

    public E getFirst()
    {
        if (values.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return values.get(0);
    }

    public E getLast()
    {
        if (values.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return values.get(values.size() - 1);
    }

    public E peekFirst()
    {
        return values.isEmpty() ? null : values.get(0);
    }

    public E peekLast()
    {
        return values.isEmpty() ? null : values.get(values.size() - 1);
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
        int index = values.indexOf(value);
        if (index < 0)
        {
            return false;
        }
        values.remove(index);
        return true;
    }

    public boolean removeLastOccurrence(Object value)
    {
        int index = values.lastIndexOf(value);
        if (index < 0)
        {
            return false;
        }
        values.remove(index);
        return true;
    }

    public boolean offer(E value)
    {
        return offerLast(value);
    }

    public E poll()
    {
        return pollFirst();
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

    public Iterator<E> descendingIterator()
    {
        ArrayList<E> copy = new ArrayList<E>(values.size() > 0 ? values.size() : 1);
        for (int i = values.size() - 1; i >= 0; i--)
        {
            copy.add(values.get(i));
        }
        return copy.iterator();
    }

    public Object clone()
    {
        return new LinkedList<E>(values);
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof List))
        {
            return false;
        }
        List<?> that = (List<?>)other;
        if (that.size() != values.size())
        {
            return false;
        }
        for (int i = 0; i < values.size(); i++)
        {
            E mine = values.get(i);
            Object theirs = that.get(i);
            if (mine == null ? theirs != null : !mine.equals(theirs))
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        int result = 1;
        for (int i = 0; i < values.size(); i++)
        {
            E value = values.get(i);
            result = 31 * result + (value == null ? 0 : value.hashCode());
        }
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values.get(i) == null ? "null" : values.get(i).toString());
        }
        return sb.append("]").toString();
    }
}
