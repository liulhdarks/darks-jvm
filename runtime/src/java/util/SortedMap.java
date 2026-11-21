package java.util;

// Minimal SortedMap: Spring's condition evaluation report stores its outcome
// map in a SortedMap field backed by a TreeMap.  The ranged views are not
// needed by that code path and are left unsupported.
public interface SortedMap<K, V> extends Map<K, V>
{
    Comparator<? super K> comparator();

    K firstKey();

    K lastKey();

    default SortedMap<K, V> subMap(K fromKey, K toKey)
    {
        throw new UnsupportedOperationException();
    }

    default SortedMap<K, V> headMap(K toKey)
    {
        throw new UnsupportedOperationException();
    }

    default SortedMap<K, V> tailMap(K fromKey)
    {
        throw new UnsupportedOperationException();
    }
}
