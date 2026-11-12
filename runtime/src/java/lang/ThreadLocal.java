package java.lang;

/**
 * Per-thread variables.  Storage lives in a lazily grown Object[] on the
 * Thread object, so a suspended coroutine keeps its own values and the GC sees
 * them as ordinary heap references.
 */
public class ThreadLocal<T>
{
    private static int nextKey;

    private final int key;

    public ThreadLocal()
    {
        key = allocateKey();
    }

    private static synchronized int allocateKey()
    {
        int k = nextKey;
        nextKey = k + 1;
        return k;
    }

    private Object[] table(boolean create)
    {
        Thread t = Thread.currentThread();
        if (t == null)
        {
            return null;
        }
        Object[] tab = t.tls;
        if (tab == null)
        {
            if (!create)
            {
                return null;
            }
            tab = new Object[8];
            t.tls = tab;
        }
        if (create && key >= tab.length)
        {
            int n = tab.length;
            while (key >= n)
            {
                n = n * 2;
            }
            Object[] grown = new Object[n];
            for (int i = 0; i < tab.length; i++)
            {
                grown[i] = tab[i];
            }
            t.tls = grown;
            tab = grown;
        }
        return tab;
    }

    protected T initialValue()
    {
        return null;
    }

    public T get()
    {
        Object[] tab = table(false);
        if (tab != null && key < tab.length)
        {
            Object v = tab[key];
            if (v != null)
            {
                return (T) v;
            }
        }
        T value = initialValue();
        set(value);
        return value;
    }

    public void set(T value)
    {
        table(true)[key] = value;
    }

    public void remove()
    {
        Object[] tab = table(false);
        if (tab != null && key < tab.length)
        {
            tab[key] = null;
        }
    }
}
