package java.util;

public interface Collection<E> extends Iterable<E>
{
    int size();

    boolean isEmpty();

    boolean add(E value);

    boolean contains(Object value);

    boolean remove(Object value);

    void clear();

    Object[] toArray();

    default <T> T[] toArray(T[] values)
    {
        int size = size();
        if (values.length < size)
        {
            Class<?> ctype = values.getClass().getComponentType();
            values = (T[])java.lang.reflect.Array.newInstance(ctype, size);
        }
        Iterator<E> it = iterator();
        int index = 0;
        while (it.hasNext())
        {
            values[index++] = (T)it.next();
        }
        if (values.length > size)
        {
            values[size] = null;
        }
        return values;
    }

    default boolean addAll(Collection<? extends E> values)
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

    default boolean containsAll(Collection<?> values)
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

    default boolean removeAll(Collection<?> values)
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

    default boolean removeIf(java.util.function.Predicate<? super E> filter)
    {
        if (filter == null)
        {
            throw new NullPointerException("filter");
        }
        boolean changed = false;
        Iterator<E> iterator = iterator();
        while (iterator.hasNext())
        {
            if (filter.test(iterator.next()))
            {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    default boolean retainAll(Collection<?> values)
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

    default java.util.stream.Stream<E> stream()
    {
        return java.util.stream.Stream.from(this);
    }
}
