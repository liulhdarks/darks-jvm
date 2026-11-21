package java.lang;

import java.util.HashMap;

public abstract class ClassValue<T>
{
    private static final Object NULL_VALUE = new Object();

    private final HashMap<Class<?>, Object> values = new HashMap<Class<?>, Object>();

    protected abstract T computeValue(Class<?> type);

    public T get(Class<?> type)
    {
        if (type == null)
        {
            throw new NullPointerException("type");
        }
        synchronized (this)
        {
            Object value = values.get(type);
            if (value == null)
            {
                T computed = computeValue(type);
                value = computed == null ? NULL_VALUE : computed;
                values.put(type, value);
            }
            return value == NULL_VALUE ? null : (T)value;
        }
    }

    public void remove(Class<?> type)
    {
        if (type == null)
        {
            throw new NullPointerException("type");
        }
        synchronized (this)
        {
            values.remove(type);
        }
    }
}
