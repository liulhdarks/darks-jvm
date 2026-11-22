package java.lang;

/**
 * Per-thread variables.  Storage lives in a lazily grown Object[] on the
 * Thread object, so a suspended coroutine keeps its own values and the GC sees
 * them as ordinary heap references.
 */
public class ThreadLocal<T>
{
    private static int nextKey;

    // InheritableThreadLocal instances indexed by key, so a new Thread can copy
    // the values its parent had at creation time (JLS 17.12).
    private static InheritableThreadLocal<?>[] inheritable =
        new InheritableThreadLocal<?>[8];

    private final int key;

    public ThreadLocal()
    {
        key = allocateKey();
        if (this instanceof InheritableThreadLocal)
        {
            registerInheritable((InheritableThreadLocal<?>) this);
        }
    }

    private static synchronized int allocateKey()
    {
        int k = nextKey;
        nextKey = k + 1;
        return k;
    }

    private static synchronized void registerInheritable(InheritableThreadLocal<?> local)
    {
        int k = ((ThreadLocal<?>) local).key;
        if (k >= inheritable.length)
        {
            int n = inheritable.length;
            while (k >= n)
            {
                n = n * 2;
            }
            InheritableThreadLocal<?>[] grown = new InheritableThreadLocal<?>[n];
            for (int i = 0; i < inheritable.length; i++)
            {
                grown[i] = inheritable[i];
            }
            inheritable = grown;
        }
        inheritable[k] = local;
    }

    // Builds the child thread's storage from the parent table, applying
    // childValue() to every inheritable slot.  Called from Thread.init().
    @SuppressWarnings("unchecked")
    static synchronized Object[] inherit(Object[] parent)
    {
        if (parent == null)
        {
            return null;
        }
        Object[] child = null;
        int limit = parent.length < inheritable.length
            ? parent.length : inheritable.length;
        for (int k = 0; k < limit; k++)
        {
            if (inheritable[k] == null || parent[k] == null)
            {
                continue;
            }
            if (child == null)
            {
                child = new Object[parent.length];
            }
            InheritableThreadLocal<Object> local =
                (InheritableThreadLocal<Object>) inheritable[k];
            child[k] = local.childValue(parent[k]);
        }
        return child;
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

    public static <S> ThreadLocal<S> withInitial(
        java.util.function.Supplier<? extends S> supplier)
    {
        if (supplier == null)
        {
            throw new NullPointerException("supplier");
        }
        return new ThreadLocal<S>()
        {
            protected S initialValue()
            {
                return supplier.get();
            }
        };
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
