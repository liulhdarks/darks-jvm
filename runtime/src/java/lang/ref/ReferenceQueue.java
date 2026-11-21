package java.lang.ref;

public class ReferenceQueue<T>
{
    public ReferenceQueue()
    {
    }

    public Reference<? extends T> poll()
    {
        return null;
    }

    public Reference<? extends T> remove() throws InterruptedException
    {
        throw new InterruptedException("no reference is enqueued");
    }

    public Reference<? extends T> remove(long timeout) throws InterruptedException
    {
        throw new InterruptedException("no reference is enqueued");
    }
}
