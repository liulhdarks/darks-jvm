package java.util.concurrent.atomic;

import java.util.function.LongBinaryOperator;

/**
 * Monitor-protected accumulator matching the JDK API. The VM lacks CAS, so the
 * single cell is guarded instead of striped.
 */
public class LongAccumulator extends Number
{
    private final LongBinaryOperator function;
    private final long identity;
    private long value;

    public LongAccumulator(LongBinaryOperator accumulatorFunction, long identity)
    {
        if (accumulatorFunction == null)
        {
            throw new NullPointerException("accumulatorFunction");
        }
        this.function = accumulatorFunction;
        this.identity = identity;
        this.value = identity;
    }

    public void accumulate(long x)
    {
        synchronized (this)
        {
            value = function.applyAsLong(value, x);
        }
    }

    public long get()
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
            value = identity;
        }
    }

    public long getThenReset()
    {
        synchronized (this)
        {
            long old = value;
            value = identity;
            return old;
        }
    }

    public long longValue()
    {
        return get();
    }

    public int intValue()
    {
        return (int)get();
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
