package java.util;

public interface NavigableMap<K, V> extends SortedMap<K, V>
{
    default Map.Entry<K, V> lowerEntry(K key)
    {
        throw new UnsupportedOperationException();
    }

    default K lowerKey(K key)
    {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> floorEntry(K key)
    {
        throw new UnsupportedOperationException();
    }

    default K floorKey(K key)
    {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> ceilingEntry(K key)
    {
        throw new UnsupportedOperationException();
    }

    default K ceilingKey(K key)
    {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> higherEntry(K key)
    {
        throw new UnsupportedOperationException();
    }

    default K higherKey(K key)
    {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> firstEntry()
    {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> lastEntry()
    {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> pollFirstEntry()
    {
        throw new UnsupportedOperationException();
    }

    default Map.Entry<K, V> pollLastEntry()
    {
        throw new UnsupportedOperationException();
    }

    default NavigableMap<K, V> descendingMap()
    {
        throw new UnsupportedOperationException();
    }

    default NavigableSet<K> navigableKeySet()
    {
        throw new UnsupportedOperationException();
    }

    default NavigableSet<K> descendingKeySet()
    {
        throw new UnsupportedOperationException();
    }

    default NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
        K toKey, boolean toInclusive)
    {
        throw new UnsupportedOperationException();
    }

    default NavigableMap<K, V> headMap(K toKey, boolean inclusive)
    {
        throw new UnsupportedOperationException();
    }

    default NavigableMap<K, V> tailMap(K fromKey, boolean inclusive)
    {
        throw new UnsupportedOperationException();
    }
}
