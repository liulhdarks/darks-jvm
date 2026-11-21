package java.util;

public class EnumMap<K extends Enum<K>, V> implements Map<K, V>
{
    private final Class<K> keyType;
    private final HashMap<K, V> delegate;

    public EnumMap(Class<K> keyType)
    {
        if (keyType == null)
        {
            throw new NullPointerException("keyType");
        }
        this.keyType = keyType;
        this.delegate = new HashMap<K, V>();
    }

    public EnumMap(EnumMap<K, ? extends V> other)
    {
        this(other.keyType);
        putAll(other);
    }

    public EnumMap(Map<K, ? extends V> other)
    {
        if (other instanceof EnumMap)
        {
            this.keyType = ((EnumMap<K, ?>)other).keyType;
        }
        else
        {
            if (other.isEmpty())
            {
                throw new IllegalArgumentException("Specified map is empty");
            }
            this.keyType = other.keySet().iterator().next().getDeclaringClass();
        }
        this.delegate = new HashMap<K, V>();
        putAll(other);
    }

    public int size()
    {
        return delegate.size();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return delegate.containsValue(value);
    }

    public V get(Object key)
    {
        return delegate.get(key);
    }

    public V put(K key, V value)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        return delegate.put(key, value);
    }

    public V remove(Object key)
    {
        return delegate.remove(key);
    }

    public void putAll(Map<? extends K, ? extends V> other)
    {
        delegate.putAll(other);
    }

    public void clear()
    {
        delegate.clear();
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
