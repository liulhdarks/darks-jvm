package java.util.concurrent;

import java.util.Map;

/**
 * A {@link Map} providing thread-safe atomic operations.
 *
 * Spring and other libraries use this interface as the declared type of
 * concurrent maps; keeping it as a plain extension of Map is enough for
 * darks-jvm, whose ConcurrentHashMap already exposes the atomic methods.
 */
public interface ConcurrentMap<K, V> extends Map<K, V>
{
    V putIfAbsent(K key, V value);

    boolean remove(Object key, Object value);

    boolean replace(K key, V oldValue, V newValue);

    V replace(K key, V value);
}
