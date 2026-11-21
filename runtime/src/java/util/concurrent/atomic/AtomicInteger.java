package java.util.concurrent.atomic;

/**
 * An int value that may be updated atomically.
 *
 * The VM has no CAS instruction exposed to Java, so the value is guarded by
 * the instance monitor.  The API and atomicity match the JDK class; only
 * scalability differs.
 */
public class AtomicInteger extends Number
{
    private int value;

    public AtomicInteger()
    {
    }

    public AtomicInteger(int initialValue)
    {
        value = initialValue;
    }

    public final int get()
    {
        synchronized (this)
        {
            return value;
        }
    }

    public final void set(int newValue)
    {
        synchronized (this)
        {
            value = newValue;
        }
    }

    public final void lazySet(int newValue)
    {
        set(newValue);
    }

    public final int getAndSet(int newValue)
    {
        synchronized (this)
        {
            int old = value;
            value = newValue;
            return old;
        }
    }

    public final boolean compareAndSet(int expect, int update)
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

    public final boolean weakCompareAndSet(int expect, int update)
    {
        return compareAndSet(expect, update);
    }

    public final int getAndIncrement()
    {
        synchronized (this)
        {
            return value++;
        }
    }

    public final int getAndDecrement()
    {
        synchronized (this)
        {
            return value--;
        }
    }

    public final int getAndAdd(int delta)
    {
        synchronized (this)
        {
            int old = value;
            value += delta;
            return old;
        }
    }

    public final int incrementAndGet()
    {
        synchronized (this)
        {
            return ++value;
        }
    }

    public final int decrementAndGet()
    {
        synchronized (this)
        {
            return --value;
        }
    }

    public final int addAndGet(int delta)
    {
        synchronized (this)
        {
            value += delta;
            return value;
        }
    }

    public int intValue()
    {
        return get();
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
        return Integer.toString(get());
    }
}
