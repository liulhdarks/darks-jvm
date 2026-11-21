package java.lang.ref;

public class PhantomReference<T> extends Reference<T>
{
    public PhantomReference(T referent, ReferenceQueue<? super T> queue)
    {
        super(referent, queue);
    }

    public T get()
    {
        return null;
    }
}
