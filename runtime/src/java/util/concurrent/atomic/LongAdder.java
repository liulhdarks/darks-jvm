package java.util.concurrent.atomic;

/**
 * One or more variables that together maintain an initially zero sum.
 *
 * A single monitor-protected cell replaces the JDK's striped cells: the VM
 * lacks CAS, so striping would not remove contention anyway.  The API is the
 * commonly used subset.
 */
public class LongAdder extends Number
{
    private long value;

    public LongAdder()
    {
    }

    public void add(long delta)
    {
        synchronized (this)
        {
            value += delta;
        }
    }

    public void increment()
    {
        add(1L);
    }

    public void decrement()
    {
        add(-1L);
    }

    public long sum()
    {
        synchronized (this)
        {
            return value;
        }
    }

    public void reset()
    {
        synchronized (this)
        {
            value = 0L;
        }
    }

    public long sumThenReset()
    {
        synchronized (this)
        {
            long old = value;
            value = 0L;
            return old;
        }
    }

    public long longValue()
    {
        return sum();
    }

    public int intValue()
    {
        return (int)sum();
    }

    public float floatValue()
    {
        return sum();
    }

    public double doubleValue()
    {
        return sum();
    }

    public String toString()
    {
        return Long.toString(sum());
    }
}
