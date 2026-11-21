package java.util.concurrent.atomic;

/**
 * An object reference that may be updated atomically.
 */
public class AtomicReference<V>
{
    private V value;

    public AtomicReference()
    {
    }

    public AtomicReference(V initialValue)
    {
        value = initialValue;
    }

    public final V get()
    {
        synchronized (this)
        {
            return value;
        }
    }

    public final void set(V newValue)
    {
        synchronized (this)
        {
            value = newValue;
        }
    }

    public final void lazySet(V newValue)
    {
        set(newValue);
    }

    public final V getAndSet(V newValue)
    {
        synchronized (this)
        {
            V old = value;
            value = newValue;
            return old;
        }
    }

    public final boolean compareAndSet(V expect, V update)
    {
        synchronized (this)
        {
            if (value == expect)
            {
                value = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(V expect, V update)
    {
        return compareAndSet(expect, update);
    }

    public String toString()
    {
        return String.valueOf(get());
    }
}
