package java.util.concurrent;

// Single-threaded VM: the deque is backed by the regular resizable
// circular-buffer implementation.
public class ConcurrentLinkedDeque<E> extends java.util.ArrayDeque<E>
{
    public ConcurrentLinkedDeque()
    {
        super();
    }

    public ConcurrentLinkedDeque(java.util.Collection<? extends E> values)
    {
        super(values);
    }
}
