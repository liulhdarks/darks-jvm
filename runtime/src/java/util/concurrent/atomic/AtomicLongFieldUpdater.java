package java.util.concurrent.atomic;

import java.lang.reflect.Field;

public abstract class AtomicLongFieldUpdater<T>
{
    protected AtomicLongFieldUpdater()
    {
    }

    public static <U> AtomicLongFieldUpdater<U> newUpdater(Class<U> tclass,
                                                           String fieldName)
    {
        return new FieldUpdater<U>(tclass, fieldName);
    }

    public abstract boolean compareAndSet(T obj, long expect, long update);

    public boolean weakCompareAndSet(T obj, long expect, long update)
    {
        return compareAndSet(obj, expect, update);
    }

    public abstract void set(T obj, long newValue);

    public abstract void lazySet(T obj, long newValue);

    public abstract long get(T obj);

    public long getAndSet(T obj, long newValue)
    {
        long old;
        synchronized (obj)
        {
            old = get(obj);
            set(obj, newValue);
        }
        return old;
    }

    public long getAndIncrement(T obj)
    {
        return getAndAdd(obj, 1L);
    }

    public long getAndDecrement(T obj)
    {
        return getAndAdd(obj, -1L);
    }

    public long incrementAndGet(T obj)
    {
        return getAndAdd(obj, 1L) + 1L;
    }

    public long decrementAndGet(T obj)
    {
        return getAndAdd(obj, -1L) - 1L;
    }

    public long addAndGet(T obj, long delta)
    {
        return getAndAdd(obj, delta) + delta;
    }

    public long getAndAdd(T obj, long delta)
    {
        long old;
        synchronized (obj)
        {
            old = get(obj);
            set(obj, old + delta);
        }
        return old;
    }

    private static final class FieldUpdater<U> extends AtomicLongFieldUpdater<U>
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
            if (field.getType() != Long.TYPE)
            {
                throw new IllegalArgumentException("field " + fieldName
                    + " is not a long");
            }
            field.setAccessible(true);
        }

        public boolean compareAndSet(U obj, long expect, long update)
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

        public void set(U obj, long newValue)
        {
            try
            {
                field.set(obj, Long.valueOf(newValue));
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        public void lazySet(U obj, long newValue)
        {
            set(obj, newValue);
        }

        public long get(U obj)
        {
            try
            {
                Object value = field.get(obj);
                return value == null ? 0L : ((Long)value).longValue();
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
