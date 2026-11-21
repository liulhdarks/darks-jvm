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

    default V getOrDefault(Object key, V defaultValue)
    {
        V value = get(key);
        return value != null || containsKey(key) ? value : defaultValue;
    }

    default V putIfAbsent(K key, V value)
    {
        V current = get(key);
        if (current == null)
        {
            current = put(key, value);
        }
        return current;
    }

    default boolean remove(Object key, Object value)
    {
        Object current = get(key);
        if (current == value || (current != null && current.equals(value)))
        {
            remove(key);
            return true;
        }
        return false;
    }

    default boolean replace(K key, V oldValue, V newValue)
    {
        Object current = get(key);
        if (current == oldValue || (current != null && current.equals(oldValue)))
        {
            put(key, newValue);
            return true;
        }
        return false;
    }

    default V replace(K key, V value)
    {
        if (containsKey(key))
        {
            return put(key, value);
        }
        return null;
    }

    default V computeIfAbsent(K key,
        java.util.function.Function<? super K, ? extends V> mappingFunction)
    {
        V current = get(key);
        if (current == null)
        {
            V computed = mappingFunction.apply(key);
            if (computed != null)
            {
                put(key, computed);
                return computed;
            }
        }
        return current;
    }

    default V computeIfPresent(K key,
        java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        V current = get(key);
        if (current != null)
        {
            V computed = remappingFunction.apply(key, current);
            if (computed == null)
            {
                remove(key);
            }
            else
            {
                put(key, computed);
            }
            return computed;
        }
        return null;
    }

    default V compute(K key,
        java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        V current = get(key);
        V computed = remappingFunction.apply(key, current);
        if (computed == null)
        {
            if (current != null || containsKey(key))
            {
                remove(key);
            }
        }
        else
        {
            put(key, computed);
        }
        return computed;
    }

    default V merge(K key, V value,
        java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        V current = get(key);
        V computed = current == null ? value : remappingFunction.apply(current, value);
        if (computed == null)
        {
            remove(key);
        }
        else
        {
            put(key, computed);
        }
        return computed;
    }

    default void forEach(java.util.function.BiConsumer<? super K, ? super V> action)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    default void replaceAll(
        java.util.function.BiFunction<? super K, ? super V, ? extends V> function)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            entry.setValue(function.apply(entry.getKey(), entry.getValue()));
        }
    }

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
