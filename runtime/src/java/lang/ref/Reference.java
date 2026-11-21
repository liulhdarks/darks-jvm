package java.lang.ref;

public abstract class Reference<T>
{
    // The collector treats this field as a weak edge: it is not traced as a
    // strong root, and is set to null once the referent is unreachable.
    private T referent;

    volatile ReferenceQueue<? super T> queue;

    // Intrusive singly linked list used by ReferenceQueue.
    Reference next;

    volatile boolean enqueued;

    Reference(T referent)
    {
        this.referent = referent;
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
        return enqueued;
    }

    public boolean enqueue()
    {
        ReferenceQueue<? super T> q = queue;
        if (q == null || enqueued)
        {
            return false;
        }
        return q.enqueue(this);
    }

    public static void reachabilityFence(Object ref)
    {
    }
}
