package java.util;

/** Bidirectional iterator over a list. */
public interface ListIterator<E> extends Iterator<E>
{
    boolean hasNext();

    E next();

    boolean hasPrevious();

    E previous();

    int nextIndex();

    int previousIndex();

    void remove();

    void set(E value);

    void add(E value);
}
