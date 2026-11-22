package java.util;

public interface List<E> extends Collection<E>
{
    E get(int index);

    E set(int index, E value);

    void add(int index, E value);

    E remove(int index);

    int indexOf(Object value);

    boolean addAll(Collection<? extends E> values);

    boolean addAll(int index, Collection<? extends E> values);

    int lastIndexOf(Object value);

    ListIterator<E> listIterator();

    ListIterator<E> listIterator(int index);

    default List<E> subList(int fromIndex, int toIndex)
    {
        if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
        {
            throw new IndexOutOfBoundsException();
        }
        ArrayList<E> copy = new ArrayList<E>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++)
        {
            copy.add(get(i));
        }
        return copy;
    }

    default void sort(Comparator<? super E> comparator)
    {
        Object[] values = toArray();
        Arrays.sort(values, (Comparator)comparator);
        for (int i = 0; i < values.length; i++)
        {
            set(i, (E)values[i]);
        }
    }
}
