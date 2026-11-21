package java.lang.ref;

public class ReferenceQueue<T>
{
    private Reference<? extends T> head;
    private Reference<? extends T> tail;

    public ReferenceQueue()
    {
    }

    boolean enqueue(Reference<? extends T> reference)
    {
        if (reference == null || reference.enqueued)
        {
            return false;
        }
        reference.next = null;
        if (tail == null)
        {
            head = (Reference<? extends T>)reference;
        }
        else
        {
            tail.next = reference;
        }
        tail = (Reference<? extends T>)reference;
        reference.enqueued = true;
        return true;
    }

    public Reference<? extends T> poll()
    {
        Reference<? extends T> reference = head;
        if (reference == null)
        {
            return null;
        }
        head = (Reference<? extends T>)reference.next;
        if (head == null)
        {
            tail = null;
        }
        reference.next = null;
        reference.enqueued = false;
        return reference;
    }

    public Reference<? extends T> remove() throws InterruptedException
    {
        return remove(0);
    }

    public Reference<? extends T> remove(long timeout) throws InterruptedException
    {
        if (timeout < 0)
        {
            throw new IllegalArgumentException("Negative timeout value");
        }
        long deadline = timeout == 0 ? 0 : System.currentTimeMillis() + timeout;
        while (true)
        {
            Reference<? extends T> reference = poll();
            if (reference != null)
            {
                return reference;
            }
            if (timeout > 0 && System.currentTimeMillis() >= deadline)
            {
                return null;
            }
            Thread.sleep(1);
        }
    }
}
