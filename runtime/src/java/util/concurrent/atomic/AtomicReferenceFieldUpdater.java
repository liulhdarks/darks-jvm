package java.util.concurrent.atomic;

import java.lang.reflect.Field;

public abstract class AtomicReferenceFieldUpdater<T, V>
{
    protected AtomicReferenceFieldUpdater()
    {
    }

    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(
        Class<U> tclass, Class<W> vclass, String fieldName)
    {
        return new FieldUpdater<U, W>(tclass, vclass, fieldName);
    }

    public abstract boolean compareAndSet(T obj, V expect, V update);

    public boolean weakCompareAndSet(T obj, V expect, V update)
    {
        return compareAndSet(obj, expect, update);
    }

    public abstract void set(T obj, V newValue);

    public abstract void lazySet(T obj, V newValue);

    public abstract V get(T obj);

    public V getAndSet(T obj, V newValue)
    {
        V old;
        synchronized (obj)
        {
            old = get(obj);
            set(obj, newValue);
        }
        return old;
    }

    private static final class FieldUpdater<U, W>
        extends AtomicReferenceFieldUpdater<U, W>
    {
        private final Field field;

        FieldUpdater(Class<U> tclass, Class<W> vclass, String fieldName)
        {
            try
            {
                field = tclass.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException e)
            {
                throw new IllegalArgumentException("field " + fieldName
                    + " not found in " + tclass);
            }
            if (vclass != null && !field.getType().equals(vclass))
            {
                throw new IllegalArgumentException("field " + fieldName
                    + " has wrong type");
            }
            field.setAccessible(true);
        }

        public boolean compareAndSet(U obj, W expect, W update)
        {
            synchronized (obj)
            {
                if (get(obj) == expect)
                {
                    set(obj, update);
                    return true;
                }
                return false;
            }
        }

        public void set(U obj, W newValue)
        {
            try
            {
                field.set(obj, newValue);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        public void lazySet(U obj, W newValue)
        {
            set(obj, newValue);
        }

        public W get(U obj)
        {
            try
            {
                return (W)field.get(obj);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
