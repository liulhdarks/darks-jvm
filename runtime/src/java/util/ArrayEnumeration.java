package java.util;

final class ArrayEnumeration<E> implements Enumeration<E>
{
    private final Object[] values;
    private int index;

    ArrayEnumeration(Collection<E> source)
    {
        ArrayList<E> copy = new ArrayList<E>();
        Iterator<E> it = source.iterator();
        while (it.hasNext())
        {
            copy.add(it.next());
        }
        values = copy.toArray();
    }

    public boolean hasMoreElements()
    {
        return index < values.length;
    }

    public E nextElement()
    {
        if (index >= values.length)
        {
            throw new NoSuchElementException();
        }
        return (E)values[index++];
    }
}
