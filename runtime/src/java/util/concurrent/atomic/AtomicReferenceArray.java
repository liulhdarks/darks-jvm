package java.util.concurrent.atomic;

/**
 * An Object array whose elements may be updated atomically.
 */
public class AtomicReferenceArray<E>
{
    private final Object[] array;

    public AtomicReferenceArray(int length)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("length < 0");
        }
        array = new Object[length];
    }

    public AtomicReferenceArray(E[] source)
    {
        array = new Object[source.length];
        System.arraycopy(source, 0, array, 0, source.length);
    }

    public final int length()
    {
        return array.length;
    }

    @SuppressWarnings("unchecked")
    public final E get(int index)
    {
        synchronized (this)
        {
            return (E)array[index];
        }
    }

    public final void set(int index, E newValue)
    {
        synchronized (this)
        {
            array[index] = newValue;
        }
    }

    public final void lazySet(int index, E newValue)
    {
        set(index, newValue);
    }

    @SuppressWarnings("unchecked")
    public final E getAndSet(int index, E newValue)
    {
        synchronized (this)
        {
            E old = (E)array[index];
            array[index] = newValue;
            return old;
        }
    }

    public final boolean compareAndSet(int index, E expect, E update)
    {
        synchronized (this)
        {
            if (array[index] == expect)
            {
                array[index] = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(int index, E expect, E update)
    {
        return compareAndSet(index, expect, update);
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
