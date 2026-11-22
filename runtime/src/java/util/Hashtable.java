package java.util;

public class Hashtable<K, V> extends HashMap<K, V>
{
    public Hashtable()
    {
    }

    public Hashtable(int capacity)
    {
        super(capacity);
    }

    public Hashtable(Map<? extends K, ? extends V> other)
    {
        super(other);
    }

    public synchronized int size()
    {
        return super.size();
    }

    public synchronized boolean isEmpty()
    {
        return super.isEmpty();
    }

    public synchronized V put(K key, V value)
    {
        return super.put(key, value);
    }

    public synchronized void putAll(Map<? extends K, ? extends V> other)
    {
        super.putAll(other);
    }

    public synchronized V get(Object key)
    {
        return super.get(key);
    }

    public synchronized boolean containsKey(Object key)
    {
        return super.containsKey(key);
    }

    public synchronized boolean containsValue(Object value)
    {
        return super.containsValue(value);
    }

    public synchronized V remove(Object key)
    {
        return super.remove(key);
    }

    public synchronized void clear()
    {
        super.clear();
    }

    public synchronized V computeIfAbsent(K key,
        java.util.function.Function<? super K, ? extends V> mappingFunction)
    {
        V value = super.get(key);
        if (value == null)
        {
            value = mappingFunction.apply(key);
            if (value != null)
            {
                super.put(key, value);
            }
        }
        return value;
    }

    public synchronized V getOrDefault(Object key, V defaultValue)
    {
        return super.getOrDefault(key, defaultValue);
    }

    public synchronized Enumeration<K> keys()
    {
        return new ArrayEnumeration<K>(super.keySet());
    }

    public synchronized Enumeration<V> elements()
    {
        return new ArrayEnumeration<V>(super.values());
    }

    public synchronized Set<K> keySet()
    {
        return super.keySet();
    }

    public synchronized Collection<V> values()
    {
        return super.values();
    }

    public synchronized Set<Map.Entry<K, V>> entrySet()
    {
        return super.entrySet();
    }

    public synchronized String toString()
    {
        return super.toString();
    }
}
