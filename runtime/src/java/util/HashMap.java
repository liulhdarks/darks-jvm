package java.util;

public class HashMap<K, V> implements Map<K, V>
{
    private Object[] keys;
    private Object[] values;
    private boolean[] used;
    private int count;

    public HashMap()
    {
        this(16);
    }

    public HashMap(int capacity)
    {
        int size = 1;
        while (size < capacity)
        {
            size = size * 2;
        }
        keys = new Object[size];
        values = new Object[size];
        used = new boolean[size];
    }

    private static int spread(Object key)
    {
        if (key == null)
        {
            return 0;
        }
        int h = key.hashCode();
        h = h ^ (h >>> 16);
        return h & 0x7fffffff;
    }

    private int findIndex(Object key)
    {
        int mask = keys.length - 1;
        int index = spread(key) & mask;
        for (int step = 0; step < keys.length; step++)
        {
            if (!used[index])
            {
                return -1;
            }
            Object k = keys[index];
            if (k == key || (k != null && k.equals(key)))
            {
                return index;
            }
            index = (index + 1) & mask;
        }
        return -1;
    }

    private int emptyIndex(Object key)
    {
        int mask = keys.length - 1;
        int index = spread(key) & mask;
        for (int step = 0; step < keys.length; step++)
        {
            if (!used[index])
            {
                return index;
            }
            index = (index + 1) & mask;
        }
        return -1;
    }

    private void grow()
    {
        Object[] oldKeys = keys;
        Object[] oldValues = values;
        boolean[] oldUsed = used;
        int size = oldKeys.length * 2;
        keys = new Object[size];
        values = new Object[size];
        used = new boolean[size];
        count = 0;
        for (int i = 0; i < oldKeys.length; i++)
        {
            if (oldUsed[i])
            {
                int index = emptyIndex(oldKeys[i]);
                keys[index] = oldKeys[i];
                values[index] = oldValues[i];
                used[index] = true;
                count++;
            }
        }
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
        int existing = findIndex(key);
        if (existing >= 0)
        {
            V old = (V)values[existing];
            values[existing] = value;
            return old;
        }
        if ((count + 1) * 2 > keys.length)
        {
            grow();
        }
        int index = emptyIndex(key);
        keys[index] = key;
        values[index] = value;
        used[index] = true;
        count++;
        return null;
    }

    public V get(Object key)
    {
        int index = findIndex(key);
        return index < 0 ? null : (V)values[index];
    }

    public boolean containsKey(Object key)
    {
        return findIndex(key) >= 0;
    }

    public V remove(Object key)
    {
        int index = findIndex(key);
        if (index < 0)
        {
            return null;
        }
        V old = (V)values[index];
        keys[index] = null;
        values[index] = null;
        used[index] = false;
        count--;
        rebuild(keys.length);
        return old;
    }

    private void rebuild(int size)
    {
        Object[] oldKeys = keys;
        Object[] oldValues = values;
        boolean[] oldUsed = used;
        keys = new Object[size];
        values = new Object[size];
        used = new boolean[size];
        count = 0;
        for (int i = 0; i < oldKeys.length; i++)
        {
            if (oldUsed[i])
            {
                int index = emptyIndex(oldKeys[i]);
                keys[index] = oldKeys[i];
                values[index] = oldValues[i];
                used[index] = true;
                count++;
            }
        }
    }

    public void clear()
    {
        for (int i = 0; i < keys.length; i++)
        {
            keys[i] = null;
            values[i] = null;
            used[i] = false;
        }
        count = 0;
    }

    public boolean containsValue(Object value)
    {
        for (int i = 0; i < keys.length; i++)
        {
            if (!used[i])
            {
                continue;
            }
            Object v = values[i];
            if (value == null)
            {
                if (v == null)
                {
                    return true;
                }
            }
            else if (value.equals(v))
            {
                return true;
            }
        }
        return false;
    }

    public Set<K> keySet()
    {
        HashSet<K> result = new HashSet<K>();
        for (int i = 0; i < keys.length; i++)
        {
            if (used[i])
            {
                result.add((K)keys[i]);
            }
        }
        return result;
    }

    public Collection<V> values()
    {
        ArrayList<V> result = new ArrayList<V>(count > 0 ? count : 1);
        for (int i = 0; i < keys.length; i++)
        {
            if (used[i])
            {
                result.add((V)values[i]);
            }
        }
        return result;
    }

    public Set<Map.Entry<K, V>> entrySet()
    {
        HashSet<Map.Entry<K, V>> result = new HashSet<Map.Entry<K, V>>(count * 2 + 1);
        for (int i = 0; i < keys.length; i++)
        {
            if (used[i])
            {
                result.add(new EntryImpl((K)keys[i]));
            }
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
            return HashMap.this.get(key);
        }

        public V setValue(V value)
        {
            return HashMap.this.put(key, value);
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry entry = (Map.Entry)other;
            Object otherKey = entry.getKey();
            Object otherValue = entry.getValue();
            if (!(key == null ? otherKey == null : key.equals(otherKey)))
            {
                return false;
            }
            V mine = getValue();
            return mine == null ? otherValue == null : mine.equals(otherValue);
        }

        public int hashCode()
        {
            V value = getValue();
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < keys.length; i++)
        {
            if (used[i])
            {
                if (!first)
                {
                    sb.append(", ");
                }
                sb.append(String.valueOf(keys[i])).append("=").append(String.valueOf(values[i]));
                first = false;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
