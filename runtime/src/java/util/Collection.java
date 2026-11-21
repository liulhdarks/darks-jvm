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

    default <T> T[] toArray(T[] array)
    {
        Object[] elements = toArray();
        if (array.length < elements.length)
        {
            array = (T[]) java.lang.reflect.Array.newInstance(
                array.getClass().getComponentType(), elements.length);
        }
        System.arraycopy(elements, 0, array, 0, elements.length);
        if (array.length > elements.length)
        {
            array[elements.length] = null;
        }
        return array;
    }

    default boolean containsAll(Collection<?> values)
    {
        for (Object value : values)
        {
            if (!contains(value))
            {
                return false;
            }
        }
        return true;
    }

    default boolean addAll(Collection<? extends E> values)
    {
        boolean changed = false;
        for (E value : values)
        {
            if (add(value))
            {
                changed = true;
            }
        }
        return changed;
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

    default boolean removeIf(java.util.function.Predicate<? super E> filter)
    {
        boolean removed = false;
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            if (filter.test(it.next()))
            {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }
}
