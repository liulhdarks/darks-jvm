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
}
