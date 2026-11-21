package java.lang.ref;

public class SoftReference<T> extends Reference<T>
{
    public SoftReference(T referent)
    {
        super(referent);
    }

    public SoftReference(T referent, ReferenceQueue<? super T> queue)
    {
        super(referent, queue);
    }
}
