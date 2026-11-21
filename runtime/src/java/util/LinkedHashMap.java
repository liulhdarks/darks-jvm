package java.util;

// Insertion-ordered HashMap.  Iteration follows the sequence in which keys
// were first inserted; re-putting an existing key keeps its position, while
// remove + put moves it to the end.  With accessOrder enabled, get() also
// moves the key to the end, matching java.util.LinkedHashMap.
public class LinkedHashMap<K, V> extends HashMap<K, V>
{
    private final boolean accessOrder;

    public LinkedHashMap()
    {
        super();
        this.accessOrder = false;
    }

    public LinkedHashMap(int capacity)
    {
        super(capacity);
        this.accessOrder = false;
    }

    public LinkedHashMap(int capacity, float loadFactor)
    {
        super(capacity);
        this.accessOrder = false;
    }

    public LinkedHashMap(int capacity, float loadFactor, boolean accessOrder)
    {
        super(capacity);
        this.accessOrder = accessOrder;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> values)
    {
        super();
        this.accessOrder = false;
        putAll(values);
    }

    public V put(K key, V value)
    {
        V old = super.put(key, value);
        if (accessOrder)
        {
            touchKey(key);
        }
        Object eldest = eldestKey();
        if (eldest != null && removeEldestEntry(new Entry<K, V>((K)eldest, super.get(eldest))))
        {
            remove(eldest);
        }
        return old;
    }

    public V get(Object key)
    {
        V value = super.get(key);
        if (accessOrder && value != null)
        {
            touchKey(key);
        }
        return value;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
    {
        return false;
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
            Map.Entry entry = (Map.Entry)other;
            return (key == null ? entry.getKey() == null : key.equals(entry.getKey()))
                && (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
        }

        public int hashCode()
        {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }
    }
}
