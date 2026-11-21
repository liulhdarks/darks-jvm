package java.util;

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V>
    implements Cloneable, java.io.Serializable
{
    private final Class<K> keyType;
    private Object[] values;
    private Object[] keys;
    private boolean[] present;
    private int size;

    public EnumMap(Class<K> keyType)
    {
        if (keyType == null)
        {
            throw new NullPointerException("keyType");
        }
        this.keyType = keyType;
        this.values = new Object[8];
        this.keys = new Object[8];
        this.present = new boolean[8];
    }

    public EnumMap(EnumMap<K, ? extends V> map)
    {
        this(map.keyType);
        putAll(map);
    }

    public EnumMap(Map<K, ? extends V> map)
    {
        this(keyTypeOf(map));
        putAll(map);
    }

    private static <K extends Enum<K>> Class<K> keyTypeOf(Map<K, ?> map)
    {
        if (map instanceof EnumMap)
        {
            return ((EnumMap<K, ?>)map).keyType;
        }
        Iterator<K> it = map.keySet().iterator();
        if (!it.hasNext())
        {
            throw new IllegalArgumentException("empty map");
        }
        return (Class<K>)it.next().getClass();
    }

    private void ensureCapacity(int index)
    {
        if (index < values.length)
        {
            return;
        }
        int capacity = values.length;
        while (capacity <= index)
        {
            capacity = capacity * 2;
        }
        Object[] newValues = new Object[capacity];
        Object[] newKeys = new Object[capacity];
        boolean[] newPresent = new boolean[capacity];
        for (int i = 0; i < values.length; i++)
        {
            newValues[i] = values[i];
            newKeys[i] = keys[i];
            newPresent[i] = present[i];
        }
        values = newValues;
        keys = newKeys;
        present = newPresent;
    }

    public int size()
    {
        return size;
    }

    public boolean containsKey(Object key)
    {
        if (!isValidKey(key))
        {
            return false;
        }
        int index = ((Enum)key).ordinal();
        return index < present.length && present[index];
    }

    public boolean containsValue(Object value)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (present[i] && (value == values[i] ||
                (value != null && value.equals(values[i]))))
            {
                return true;
            }
        }
        return false;
    }

    public V get(Object key)
    {
        if (!isValidKey(key))
        {
            return null;
        }
        int index = ((Enum)key).ordinal();
        return index < present.length && present[index] ? (V)values[index] : null;
    }

    public V put(K key, V value)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        int index = key.ordinal();
        ensureCapacity(index);
        V previous = present[index] ? (V)values[index] : null;
        values[index] = value;
        keys[index] = key;
        if (!present[index])
        {
            present[index] = true;
            size = size + 1;
        }
        return previous;
    }

    public V remove(Object key)
    {
        if (!isValidKey(key))
        {
            return null;
        }
        int index = ((Enum)key).ordinal();
        if (index >= present.length || !present[index])
        {
            return null;
        }
        V previous = (V)values[index];
        values[index] = null;
        keys[index] = null;
        present[index] = false;
        size = size - 1;
        return previous;
    }

    public void clear()
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = null;
            keys[i] = null;
            present[i] = false;
        }
        size = 0;
    }

    private boolean isValidKey(Object key)
    {
        return key != null && key.getClass() == keyType;
    }

    public Set<Map.Entry<K, V>> entrySet()
    {
        return new EntrySet();
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>>
    {
        public Iterator<Map.Entry<K, V>> iterator()
        {
            return new EntryIterator();
        }

        public int size()
        {
            return size;
        }
    }

    private final class EntryIterator implements Iterator<Map.Entry<K, V>>
    {
        private int cursor = -1;
        private int nextIndex = -1;

        public boolean hasNext()
        {
            if (nextIndex < 0)
            {
                nextIndex = findNext(cursor + 1);
            }
            return nextIndex >= 0;
        }

        public Map.Entry<K, V> next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            cursor = nextIndex;
            nextIndex = -1;
            return new EntryImpl(cursor);
        }

        public void remove()
        {
            if (cursor < 0)
            {
                throw new IllegalStateException();
            }
            values[cursor] = null;
            keys[cursor] = null;
            present[cursor] = false;
            size = size - 1;
            cursor = -1;
        }
    }

    private int findNext(int from)
    {
        for (int i = from; i < present.length; i++)
        {
            if (present[i])
            {
                return i;
            }
        }
        return -1;
    }

    private final class EntryImpl implements Map.Entry<K, V>
    {
        private final int index;

        EntryImpl(int index)
        {
            this.index = index;
        }

        public K getKey()
        {
            return (K)keyAt(index);
        }

        public V getValue()
        {
            return (V)values[index];
        }

        public V setValue(V value)
        {
            V previous = (V)values[index];
            values[index] = value;
            return previous;
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry entry = (Map.Entry)other;
            Object key = entry.getKey();
            Object value = entry.getValue();
            return key == getKey() && (value == getValue() ||
                (value != null && value.equals(getValue())));
        }

        public int hashCode()
        {
            Object value = getValue();
            return getKey().hashCode() ^ (value == null ? 0 : value.hashCode());
        }
    }

    private Enum keyAt(int index)
    {
        return index < keys.length ? (Enum)keys[index] : null;
    }
}
