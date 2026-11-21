package java.util;

public interface Deque<E> extends Queue<E>
{
    void addFirst(E value);

    void addLast(E value);

    boolean offerFirst(E value);

    boolean offerLast(E value);

    E removeFirst();

    E removeLast();

    E pollFirst();

    E pollLast();

    E getFirst();

    E getLast();

    E peekFirst();

    E peekLast();

    void push(E value);

    E pop();

    boolean removeFirstOccurrence(Object value);

    boolean removeLastOccurrence(Object value);
}
