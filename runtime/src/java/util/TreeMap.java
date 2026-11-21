package java.util;

// Sorted map backed by a sorted key/value pair of arrays.  Operations are
// O(n) on insertion/removal and O(log n) on lookup, which is adequate for the
// small configuration maps this runtime targets.
public class TreeMap<K, V> implements Map<K, V>
{
    private Object[] keys;
    private Object[] values;
    private int count;
    private final Comparator<? super K> comparator;

    public TreeMap()
    {
        this((Comparator<? super K>)null);
    }

    public TreeMap(Comparator<? super K> comparator)
    {
        this.comparator = comparator;
        this.keys = new Object[8];
        this.values = new Object[8];
    }

    public TreeMap(Map<? extends K, ? extends V> values)
    {
        this((Comparator<? super K>)null);
        putAll(values);
    }

    public Comparator<? super K> comparator()
    {
        return comparator;
    }

    private int compare(Object a, Object b)
    {
        if (comparator != null)
        {
            return comparator.compare((K)a, (K)b);
        }
        if (a == null)
        {
            throw new NullPointerException("null key");
        }
        return ((Comparable)a).compareTo(b);
    }

    private int find(Object key)
    {
        int lo = 0;
        int hi = count - 1;
        while (lo <= hi)
        {
            int mid = (lo + hi) >>> 1;
            int result = compare(keys[mid], key);
            if (result < 0)
            {
                lo = mid + 1;
            }
            else if (result > 0)
            {
                hi = mid - 1;
            }
            else
            {
                return mid;
            }
        }
        return -(lo + 1);
    }

    private void ensure(int needed)
    {
        if (needed <= keys.length)
        {
            return;
        }
        int size = keys.length * 2;
        while (size < needed)
        {
            size = size * 2;
        }
        Object[] grownKeys = new Object[size];
        Object[] grownValues = new Object[size];
        for (int i = 0; i < count; i++)
        {
            grownKeys[i] = keys[i];
            grownValues[i] = values[i];
        }
        keys = grownKeys;
        values = grownValues;
    }

    public int size()
    {
        return count;
    }

    public boolean isEmpty()
    {
        return count == 0;
    }

    public V put(K key, V value)
    {
        int at = find(key);
        if (at >= 0)
        {
            V old = (V)values[at];
            values[at] = value;
            return old;
        }
        int index = -at - 1;
        ensure(count + 1);
        for (int i = count; i > index; i--)
        {
            keys[i] = keys[i - 1];
            values[i] = values[i - 1];
        }
        keys[index] = key;
        values[index] = value;
        count++;
        return null;
    }

    public V get(Object key)
    {
        int at = find(key);
        return at < 0 ? null : (V)values[at];
    }

    public boolean containsKey(Object key)
    {
        return find(key) >= 0;
    }

    public boolean containsValue(Object value)
    {
        for (int i = 0; i < count; i++)
        {
            Object current = values[i];
            if (value == null ? current == null : value.equals(current))
            {
                return true;
            }
        }
        return false;
    }

    public V remove(Object key)
    {
        int at = find(key);
        if (at < 0)
        {
            return null;
        }
        V old = (V)values[at];
        for (int i = at; i < count - 1; i++)
        {
            keys[i] = keys[i + 1];
            values[i] = values[i + 1];
        }
        count--;
        keys[count] = null;
        values[count] = null;
        return old;
    }

    public void putAll(Map<? extends K, ? extends V> other)
    {
        Iterator<? extends K> it = other.keySet().iterator();
        while (it.hasNext())
        {
            K key = it.next();
            put(key, other.get(key));
        }
    }

    public V getOrDefault(Object key, V defaultValue)
    {
        int at = find(key);
        return at < 0 ? defaultValue : (V)values[at];
    }

    public void clear()
    {
        for (int i = 0; i < count; i++)
        {
            keys[i] = null;
            values[i] = null;
        }
        count = 0;
    }

    public K firstKey()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        return (K)keys[0];
    }

    public K lastKey()
    {
        if (count == 0)
        {
            throw new NoSuchElementException();
        }
        return (K)keys[count - 1];
    }

    public Set<K> keySet()
    {
        LinkedHashSet<K> result = new LinkedHashSet<K>();
        for (int i = 0; i < count; i++)
        {
            result.add((K)keys[i]);
        }
        return result;
    }

    public Collection<V> values()
    {
        ArrayList<V> result = new ArrayList<V>(count > 0 ? count : 1);
        for (int i = 0; i < count; i++)
        {
            result.add((V)values[i]);
        }
        return result;
    }

    public Set<Map.Entry<K, V>> entrySet()
    {
        LinkedHashSet<Map.Entry<K, V>> result = new LinkedHashSet<Map.Entry<K, V>>();
        for (int i = 0; i < count; i++)
        {
            result.add(new EntryImpl((K)keys[i]));
        }
        return result;
    }

    private final class EntryImpl implements Map.Entry<K, V>
    {
        private final K key;

        EntryImpl(K key)
        {
            this.key = key;
        }

        public K getKey()
        {
            return key;
        }

        public V getValue()
        {
            return TreeMap.this.get(key);
        }

        public V setValue(V value)
        {
            return TreeMap.this.put(key, value);
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry entry = (Map.Entry)other;
            Object otherKey = entry.getKey();
            if (!(key == null ? otherKey == null : key.equals(otherKey)))
            {
                return false;
            }
            V mine = getValue();
            Object otherValue = entry.getValue();
            return mine == null ? otherValue == null : mine.equals(otherValue);
        }

        public int hashCode()
        {
            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < count; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(keys[i])).append("=").append(String.valueOf(values[i]));
        }
        sb.append("}");
        return sb.toString();
    }
}
