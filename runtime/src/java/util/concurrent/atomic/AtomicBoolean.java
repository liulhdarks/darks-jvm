package java.util.concurrent.atomic;

/**
 * A boolean value that may be updated atomically.
 */
public class AtomicBoolean
{
    private boolean value;

    public AtomicBoolean()
    {
    }

    public AtomicBoolean(boolean initialValue)
    {
        value = initialValue;
    }

    public final boolean get()
    {
        synchronized (this)
        {
            return value;
        }
    }

    public final void set(boolean newValue)
    {
        synchronized (this)
        {
            value = newValue;
        }
    }

    public final void lazySet(boolean newValue)
    {
        set(newValue);
    }

    public final boolean getAndSet(boolean newValue)
    {
        synchronized (this)
        {
            boolean old = value;
            value = newValue;
            return old;
        }
    }

    public final boolean compareAndSet(boolean expect, boolean update)
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

    public final boolean weakCompareAndSet(boolean expect, boolean update)
    {
        return compareAndSet(expect, update);
    }

    public String toString()
    {
        return Boolean.toString(get());
    }
}
