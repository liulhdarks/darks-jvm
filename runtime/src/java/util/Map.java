package java.util;

public interface Map<K, V>
{
    int size();

    boolean isEmpty();

    V put(K key, V value);

    V get(Object key);

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V remove(Object key);

    void clear();

    void putAll(Map<? extends K, ? extends V> other);

    V getOrDefault(Object key, V defaultValue);

    Set<K> keySet();

    Collection<V> values();

    Set<Map.Entry<K, V>> entrySet();

    public interface Entry<K, V>
    {
        K getKey();

        V getValue();

        V setValue(V value);

        boolean equals(Object other);

        int hashCode();
    }
}
