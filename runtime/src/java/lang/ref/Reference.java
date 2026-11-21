package java.lang.ref;

// Reference types share one referent slot.  darks-jvm has no weak-reference
// support in its collector yet, so a Reference keeps its referent strongly
// reachable; get()/clear()/enqueue() semantics match the JDK otherwise.
public abstract class Reference<T>
{
    private T referent;
    private final ReferenceQueue<? super T> queue;

    Reference(T referent)
    {
        this(referent, null);
    }

    Reference(T referent, ReferenceQueue<? super T> queue)
    {
        this.referent = referent;
        this.queue = queue;
    }

    public T get()
    {
        return referent;
    }

    public void clear()
    {
        referent = null;
    }

    public boolean isEnqueued()
    {
        return false;
    }

    public boolean enqueue()
    {
        return false;
    }

    ReferenceQueue<? super T> getQueue()
    {
        return queue;
    }
}
