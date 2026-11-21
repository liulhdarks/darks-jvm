package java.util;

public abstract class AbstractCollection<E> implements Collection<E>
{
    protected AbstractCollection()
    {
    }

    public abstract Iterator<E> iterator();

    public abstract int size();

    public boolean isEmpty()
    {
        return size() == 0;
    }

    public boolean contains(Object value)
    {
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            E element = it.next();
            if (value == element || (value != null && value.equals(element)))
            {
                return true;
            }
        }
        return false;
    }

    public boolean add(E value)
    {
        throw new UnsupportedOperationException("add");
    }

    public boolean remove(Object value)
    {
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            E element = it.next();
            if (value == element || (value != null && value.equals(element)))
            {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Collection<?> values)
    {
        Iterator<?> it = values.iterator();
        while (it.hasNext())
        {
            if (!contains(it.next()))
            {
                return false;
            }
        }
        return true;
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
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            if (values.contains(it.next()))
            {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    public boolean retainAll(Collection<?> values)
    {
        boolean changed = false;
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            if (!values.contains(it.next()))
            {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    public void clear()
    {
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            it.next();
            it.remove();
        }
    }

    public Object[] toArray()
    {
        Object[] result = new Object[size()];
        int n = 0;
        Iterator<E> it = iterator();
        while (it.hasNext() && n < result.length)
        {
            result[n++] = it.next();
        }
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(it.next()));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
