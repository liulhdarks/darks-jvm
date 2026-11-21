package java.util.concurrent.atomic;

/**
 * A long value that may be updated atomically.
 */
public class AtomicLong extends Number
{
    private long value;

    public AtomicLong()
    {
    }

    public AtomicLong(long initialValue)
    {
        value = initialValue;
    }

    public final long get()
    {
        synchronized (this)
        {
            return value;
        }
    }

    public final void set(long newValue)
    {
        synchronized (this)
        {
            value = newValue;
        }
    }

    public final void lazySet(long newValue)
    {
        set(newValue);
    }

    public final long getAndSet(long newValue)
    {
        synchronized (this)
        {
            long old = value;
            value = newValue;
            return old;
        }
    }

    public final boolean compareAndSet(long expect, long update)
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

    public final boolean weakCompareAndSet(long expect, long update)
    {
        return compareAndSet(expect, update);
    }

    public final long getAndIncrement()
    {
        synchronized (this)
        {
            return value++;
        }
    }

    public final long getAndDecrement()
    {
        synchronized (this)
        {
            return value--;
        }
    }

    public final long getAndAdd(long delta)
    {
        synchronized (this)
        {
            long old = value;
            value += delta;
            return old;
        }
    }

    public final long incrementAndGet()
    {
        synchronized (this)
        {
            return ++value;
        }
    }

    public final long decrementAndGet()
    {
        synchronized (this)
        {
            return --value;
        }
    }

    public final long addAndGet(long delta)
    {
        synchronized (this)
        {
            value += delta;
            return value;
        }
    }

    public int intValue()
    {
        return (int)get();
    }

    public long longValue()
    {
        return get();
    }

    public float floatValue()
    {
        return get();
    }

    public double doubleValue()
    {
        return get();
    }

    public String toString()
    {
        return Long.toString(get());
    }
}
