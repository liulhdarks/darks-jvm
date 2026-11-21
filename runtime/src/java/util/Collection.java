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

    default java.util.stream.Stream<E> stream()
    {
        return java.util.stream.Stream.from(this);
    }
}
