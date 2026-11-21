package java.util;

public interface Queue<E> extends Collection<E>
{
    boolean offer(E value);

    E poll();

    E peek();

    E element();

    E remove();
}
