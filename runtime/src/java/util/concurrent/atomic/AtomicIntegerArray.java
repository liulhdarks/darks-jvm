package java.util.concurrent.atomic;

/**
 * An int array whose elements may be updated atomically.
 */
public class AtomicIntegerArray
{
    private final int[] array;

    public AtomicIntegerArray(int length)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("length < 0");
        }
        array = new int[length];
    }

    public AtomicIntegerArray(int[] source)
    {
        array = new int[source.length];
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

    public final int get(int index)
    {
        synchronized (this)
        {
            check(index);
            return array[index];
        }
    }

    public final void set(int index, int value)
    {
        synchronized (this)
        {
            check(index);
            array[index] = value;
        }
    }

    public final void lazySet(int index, int value)
    {
        set(index, value);
    }

    public final int getAndSet(int index, int value)
    {
        synchronized (this)
        {
            check(index);
            int old = array[index];
            array[index] = value;
            return old;
        }
    }

    public final boolean compareAndSet(int index, int expect, int update)
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

    public final boolean weakCompareAndSet(int index, int expect, int update)
    {
        return compareAndSet(index, expect, update);
    }

    public final int getAndIncrement(int index)
    {
        synchronized (this)
        {
            check(index);
            return array[index]++;
        }
    }

    public final int getAndDecrement(int index)
    {
        synchronized (this)
        {
            check(index);
            return array[index]--;
        }
    }

    public final int getAndAdd(int index, int delta)
    {
        synchronized (this)
        {
            check(index);
            int old = array[index];
            array[index] += delta;
            return old;
        }
    }

    public final int incrementAndGet(int index)
    {
        synchronized (this)
        {
            check(index);
            return ++array[index];
        }
    }

    public final int decrementAndGet(int index)
    {
        synchronized (this)
        {
            check(index);
            return --array[index];
        }
    }

    public final int addAndGet(int index, int delta)
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
