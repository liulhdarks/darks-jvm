package java.util.concurrent.atomic;

/**
 * A long array whose elements may be updated atomically.
 */
public class AtomicLongArray
{
    private final long[] array;

    public AtomicLongArray(int length)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("length < 0");
        }
        array = new long[length];
    }

    public AtomicLongArray(long[] source)
    {
        array = new long[source.length];
        System.arraycopy(source, 0, array, 0, source.length);
    }

    public final int length()
    {
        return array.length;
    }

    private void check(int index)
    {
        if (index < 0 || index >= array.length)
        {
            throw new IndexOutOfBoundsException("index " + index);
        }
    }

    public final long get(int index)
    {
        synchronized (this)
        {
            check(index);
            return array[index];
        }
    }

    public final void set(int index, long value)
    {
        synchronized (this)
        {
            check(index);
            array[index] = value;
        }
    }

    public final void lazySet(int index, long value)
    {
        set(index, value);
    }

    public final long getAndSet(int index, long value)
    {
        synchronized (this)
        {
            check(index);
            long old = array[index];
            array[index] = value;
            return old;
        }
    }

    public final boolean compareAndSet(int index, long expect, long update)
    {
        synchronized (this)
        {
            check(index);
            if (array[index] == expect)
            {
                array[index] = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(int index, long expect, long update)
    {
        return compareAndSet(index, expect, update);
    }

    public final long getAndIncrement(int index)
    {
        synchronized (this)
        {
            check(index);
            return array[index]++;
        }
    }

    public final long getAndDecrement(int index)
    {
        synchronized (this)
        {
            check(index);
            return array[index]--;
        }
    }

    public final long getAndAdd(int index, long delta)
    {
        synchronized (this)
        {
            check(index);
            long old = array[index];
            array[index] += delta;
            return old;
        }
    }

    public final long incrementAndGet(int index)
    {
        synchronized (this)
        {
            check(index);
            return ++array[index];
        }
    }

    public final long decrementAndGet(int index)
    {
        synchronized (this)
        {
            check(index);
            return --array[index];
        }
    }

    public final long addAndGet(int index, long delta)
    {
        synchronized (this)
        {
            check(index);
            array[index] += delta;
            return array[index];
        }
    }

    public String toString()
    {
        synchronized (this)
        {
            StringBuilder text = new StringBuilder("[");
            for (int i = 0; i < array.length; i++)
            {
                if (i > 0)
                {
                    text.append(", ");
                }
                text.append(array[i]);
            }
            return text.append("]").toString();
        }
    }
}
