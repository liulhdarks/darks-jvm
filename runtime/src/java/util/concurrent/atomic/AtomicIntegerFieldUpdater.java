package java.util.concurrent.atomic;

import java.lang.reflect.Field;

public abstract class AtomicIntegerFieldUpdater<T>
{
    protected AtomicIntegerFieldUpdater()
    {
    }

    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass,
                                                              String fieldName)
    {
        return new FieldUpdater<U>(tclass, fieldName);
    }

    public abstract boolean compareAndSet(T obj, int expect, int update);

    public boolean weakCompareAndSet(T obj, int expect, int update)
    {
        return compareAndSet(obj, expect, update);
    }

    public abstract void set(T obj, int newValue);

    public abstract void lazySet(T obj, int newValue);

    public abstract int get(T obj);

    public int getAndSet(T obj, int newValue)
    {
        int old;
        synchronized (obj)
        {
            old = get(obj);
            set(obj, newValue);
        }
        return old;
    }

    public int getAndIncrement(T obj)
    {
        return getAndAdd(obj, 1);
    }

    public int getAndDecrement(T obj)
    {
        return getAndAdd(obj, -1);
    }

    public int incrementAndGet(T obj)
    {
        return getAndAdd(obj, 1) + 1;
    }

    public int decrementAndGet(T obj)
    {
        return getAndAdd(obj, -1) - 1;
    }

    public int addAndGet(T obj, int delta)
    {
        return getAndAdd(obj, delta) + delta;
    }

    public int getAndAdd(T obj, int delta)
    {
        int old;
        synchronized (obj)
        {
            old = get(obj);
            set(obj, old + delta);
        }
        return old;
    }

    private static final class FieldUpdater<U> extends AtomicIntegerFieldUpdater<U>
    {
        private final Field field;

        FieldUpdater(Class<U> tclass, String fieldName)
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
            if (field.getType() != Integer.TYPE)
            {
                throw new IllegalArgumentException("field " + fieldName
                    + " is not an int");
            }
            field.setAccessible(true);
        }

        public boolean compareAndSet(U obj, int expect, int update)
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

        public void set(U obj, int newValue)
        {
            try
            {
                field.set(obj, Integer.valueOf(newValue));
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        public void lazySet(U obj, int newValue)
        {
            set(obj, newValue);
        }

        public int get(U obj)
        {
            try
            {
                Object value = field.get(obj);
                return value == null ? 0 : ((Integer)value).intValue();
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
