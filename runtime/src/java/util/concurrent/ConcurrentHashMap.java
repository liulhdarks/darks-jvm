package java.util.concurrent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A thread-safe hash map.
 *
 * All operations are serialized on one monitor and the storage is the
 * regular {@link HashMap}.  This trades scalability for correctness on a VM
 * without CAS or volatile field semantics; per-bin locking would require both.
 * Bulk views ({@code keySet}, {@code values}, {@code entrySet}) are snapshots.
 */
public class ConcurrentHashMap<K, V> implements ConcurrentMap<K, V>
{
    private final HashMap<K, V> map;
    private final Object lock = new Object();

    public ConcurrentHashMap()
    {
        map = new HashMap<K, V>();
    }

    public ConcurrentHashMap(int initialCapacity)
    {
        map = new HashMap<K, V>(initialCapacity);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor)
    {
        map = new HashMap<K, V>(initialCapacity);
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> other)
    {
        map = new HashMap<K, V>();
        putAll(other);
    }

    private static void checkKey(Object key)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
    }

    private static void checkValue(Object value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
    }

    public int size()
    {
        synchronized (lock)
        {
            return map.size();
        }
    }

    public int mappingCount()
    {
        return size();
    }

    public boolean isEmpty()
    {
        synchronized (lock)
        {
            return map.isEmpty();
        }
    }

    public V put(K key, V value)
    {
        checkKey(key);
        checkValue(value);
        synchronized (lock)
        {
            return map.put(key, value);
        }
    }

    public V get(Object key)
    {
        checkKey(key);
        synchronized (lock)
        {
            return map.get(key);
        }
    }

    public boolean containsKey(Object key)
    {
        checkKey(key);
        synchronized (lock)
        {
            return map.containsKey(key);
        }
    }

    public boolean containsValue(Object value)
    {
        checkValue(value);
        synchronized (lock)
        {
            return map.containsValue(value);
        }
    }

    public V remove(Object key)
    {
        checkKey(key);
        synchronized (lock)
        {
            return map.remove(key);
        }
    }

    public void clear()
    {
        synchronized (lock)
        {
            map.clear();
        }
    }

    public void putAll(Map<? extends K, ? extends V> other)
    {
        if (other == null)
        {
            throw new NullPointerException("map");
        }
        synchronized (lock)
        {
            map.putAll(other);
        }
    }

    public V getOrDefault(Object key, V defaultValue)
    {
        checkKey(key);
        synchronized (lock)
        {
            V value = map.get(key);
            return value == null ? defaultValue : value;
        }
    }

    public V putIfAbsent(K key, V value)
    {
        checkKey(key);
        checkValue(value);
        synchronized (lock)
        {
            V existing = map.get(key);
            if (existing != null)
            {
                return existing;
            }
            map.put(key, value);
            return null;
        }
    }

    public boolean remove(Object key, Object value)
    {
        checkKey(key);
        checkValue(value);
        synchronized (lock)
        {
            V existing = map.get(key);
            if (existing != null && existing.equals(value))
            {
                map.remove(key);
                return true;
            }
            return false;
        }
    }

    public boolean replace(K key, V oldValue, V newValue)
    {
        checkKey(key);
        checkValue(oldValue);
        checkValue(newValue);
        synchronized (lock)
        {
            V existing = map.get(key);
            if (existing != null && existing.equals(oldValue))
            {
                map.put(key, newValue);
                return true;
            }
            return false;
        }
    }

    public V replace(K key, V value)
    {
        checkKey(key);
        checkValue(value);
        synchronized (lock)
        {
            if (map.containsKey(key))
            {
                return map.put(key, value);
            }
            return null;
        }
    }

    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mapping)
    {
        checkKey(key);
        if (mapping == null)
        {
            throw new NullPointerException("mapping");
        }
        synchronized (lock)
        {
            V existing = map.get(key);
            if (existing != null)
            {
                return existing;
            }
            V value = mapping.apply(key);
            checkValue(value);
            map.put(key, value);
            return value;
        }
    }

    public V computeIfPresent(K key,
                              java.util.function.BiFunction<? super K, ? super V, ? extends V> remapping)
    {
        checkKey(key);
        if (remapping == null)
        {
            throw new NullPointerException("remapping");
        }
        synchronized (lock)
        {
            V existing = map.get(key);
            if (existing == null)
            {
                return null;
            }
            V value = remapping.apply(key, existing);
            if (value == null)
            {
                map.remove(key);
            }
            else
            {
                map.put(key, value);
            }
            return value;
        }
    }

    public V compute(K key,
                     java.util.function.BiFunction<? super K, ? super V, ? extends V> remapping)
    {
        checkKey(key);
        if (remapping == null)
        {
            throw new NullPointerException("remapping");
        }
        synchronized (lock)
        {
            V existing = map.get(key);
            V value = remapping.apply(key, existing);
            if (value == null)
            {
                map.remove(key);
            }
            else
            {
                map.put(key, value);
            }
            return value;
        }
    }

    public V merge(K key, V value,
                   java.util.function.BiFunction<? super V, ? super V, ? extends V> remapping)
    {
        checkKey(key);
        checkValue(value);
        if (remapping == null)
        {
            throw new NullPointerException("remapping");
        }
        synchronized (lock)
        {
            V existing = map.get(key);
            V merged = existing == null ? value : remapping.apply(existing, value);
            if (merged == null)
            {
                map.remove(key);
            }
            else
            {
                map.put(key, merged);
            }
            return merged;
        }
    }

    public Set<K> keySet()
    {
        synchronized (lock)
        {
            return map.keySet();
        }
    }

    public Collection<V> values()
    {
        synchronized (lock)
        {
            return map.values();
        }
    }

    public Set<Map.Entry<K, V>> entrySet()
    {
        synchronized (lock)
        {
            HashSet<Map.Entry<K, V>> result = new HashSet<Map.Entry<K, V>>(map.size() * 2 + 1);
            Set<Map.Entry<K, V>> entries = map.entrySet();
            java.util.Iterator<Map.Entry<K, V>> it = entries.iterator();
            while (it.hasNext())
            {
                Map.Entry<K, V> entry = it.next();
                result.add(new Entry<K, V>(entry.getKey(), entry.getValue()));
            }
            return result;
        }
    }

    private static final class Entry<K, V> implements Map.Entry<K, V>
    {
        private final K key;
        private V value;

        Entry(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public K getKey()
        {
            return key;
        }

        public V getValue()
        {
            return value;
        }

        public V setValue(V newValue)
        {
            if (newValue == null)
            {
                throw new NullPointerException("value");
            }
            V old = value;
            value = newValue;
            return old;
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)other;
            return (key == null ? entry.getKey() == null : key.equals(entry.getKey()))
                && (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
        }

        public int hashCode()
        {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }
    }

    public String toString()
    {
        synchronized (lock)
        {
            return map.toString();
        }
    }
}
