package java.util;

public interface Map<K, V>
{
    int size();

    boolean isEmpty();

    V put(K key, V value);

    V get(Object key);

    boolean containsKey(Object key);

    V remove(Object key);

    void clear();

    Set<K> keySet();
}
