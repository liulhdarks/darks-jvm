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
            if (value == null ? element == null : value.equals(element))
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
            if (value == null ? element == null : value.equals(element))
            {
                it.remove();
                return true;
            }
        }
        return false;
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
        Iterator<E> it = iterator();
        int index = 0;
        while (it.hasNext())
        {
            result[index++] = it.next();
        }
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        Iterator<E> it = iterator();
        boolean first = true;
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
