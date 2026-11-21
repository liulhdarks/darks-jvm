package java.util;

// The VM does not observe the weak reference queue yet, so entries are held
// strongly: values disappear only when the map itself does or a key is
// removed.  Mapping and iteration semantics otherwise follow java.util.
public class WeakHashMap<K, V> implements Map<K, V>
{
    private final HashMap<K, V> delegate;

    public WeakHashMap()
    {
        delegate = new HashMap<K, V>();
    }

    public WeakHashMap(int initialCapacity)
    {
        delegate = new HashMap<K, V>(initialCapacity);
    }

    public WeakHashMap(Map<? extends K, ? extends V> values)
    {
        delegate = new HashMap<K, V>();
        putAll(values);
    }

    public int size()
    {
        return delegate.size();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public V put(K key, V value)
    {
        return delegate.put(key, value);
    }

    public V get(Object key)
    {
        return delegate.get(key);
    }

    public boolean containsKey(Object key)
    {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return delegate.containsValue(value);
    }

    public V remove(Object key)
    {
        return delegate.remove(key);
    }

    public void clear()
    {
        delegate.clear();
    }

    public void putAll(Map<? extends K, ? extends V> other)
    {
        delegate.putAll(other);
    }

    public V getOrDefault(Object key, V defaultValue)
    {
        return delegate.getOrDefault(key, defaultValue);
    }

    public Set<K> keySet()
    {
        return delegate.keySet();
    }

    public Collection<V> values()
    {
        return delegate.values();
    }

    public Set<Map.Entry<K, V>> entrySet()
    {
        return delegate.entrySet();
    }

    public String toString()
    {
        return delegate.toString();
    }
}
