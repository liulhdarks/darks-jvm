package java.util;

// Insertion-ordered HashMap.  Iteration follows the sequence in which keys
// were first inserted; re-putting an existing key keeps its position, while
// remove + put moves it to the end.  Access-order mode is not provided.
public class LinkedHashMap<K, V> extends HashMap<K, V>
{
    public LinkedHashMap()
    {
        super();
    }

    public LinkedHashMap(int capacity)
    {
        super(capacity);
    }

    public LinkedHashMap(int capacity, float loadFactor)
    {
        super(capacity);
    }

    public LinkedHashMap(int capacity, float loadFactor, boolean accessOrder)
    {
        super(capacity);
    }

    public LinkedHashMap(Map<? extends K, ? extends V> values)
    {
        super();
        putAll(values);
    }

    protected int[] visitOrder()
    {
        return insertionOrder();
    }

    protected Set<K> newKeySet()
    {
        return new LinkedHashSet<K>();
    }

    protected Set<Map.Entry<K, V>> newEntrySet()
    {
        return new LinkedHashSet<Map.Entry<K, V>>();
    }
}
