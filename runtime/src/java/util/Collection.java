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
}
