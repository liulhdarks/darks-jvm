package java.util;

public class IdentityHashMap<K, V> implements Map<K, V>
{
    private Object[] keys;
    private Object[] values;
    private boolean[] used;
    private int count;

    public IdentityHashMap()
    {
        this(32);
    }

    public IdentityHashMap(int expectedSize)
    {
        int size = 4;
        while (size < expectedSize * 2)
        {
            size = size * 2;
        }
        keys = new Object[size];
        values = new Object[size];
        used = new boolean[size];
    }

    public IdentityHashMap(Map<? extends K, ? extends V> other)
    {
        this(other.size());
        putAll(other);
    }

    private static int spread(Object key)
    {
        return key == null ? 0 : (System.identityHashCode(key) & 0x7fffffff);
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
            if (keys[index] == key)
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

    private void resize()
    {
        Object[] oldKeys = keys;
        Object[] oldValues = values;
        boolean[] oldUsed = used;
        keys = new Object[oldKeys.length * 2];
        values = new Object[oldKeys.length * 2];
        used = new boolean[oldKeys.length * 2];
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
            resize();
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

    public boolean containsValue(Object value)
    {
        for (int i = 0; i < keys.length; i++)
        {
            if (used[i])
            {
                Object candidate = values[i];
                if (candidate == value ||
                    (candidate != null && candidate.equals(value)))
                {
                    return true;
                }
            }
        }
        return false;
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
        int index = findIndex(key);
        return index < 0 ? defaultValue : (V)values[index];
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

    private int[] occupied()
    {
        int[] result = new int[count];
        int n = 0;
        for (int i = 0; i < keys.length; i++)
        {
            if (used[i])
            {
                result[n++] = i;
            }
        }
        return result;
    }

    public Set<K> keySet()
    {
        return new KeySet();
    }

    public Collection<V> values()
    {
        return new Values();
    }

    public Set<Map.Entry<K, V>> entrySet()
    {
        return new EntrySet();
    }

    private final class KeySet implements Set<K>
    {
        public int size()
        {
            return count;
        }

        public boolean isEmpty()
        {
            return count == 0;
        }

        public boolean add(K value)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean contains(Object value)
        {
            return containsKey(value);
        }

        public boolean remove(Object value)
        {
            if (!containsKey(value))
            {
                return false;
            }
            IdentityHashMap.this.remove(value);
            return true;
        }

        public void clear()
        {
            IdentityHashMap.this.clear();
        }

        public Object[] toArray()
        {
            Object[] result = new Object[count];
            int n = 0;
            for (int i = 0; i < keys.length; i++)
            {
                if (used[i])
                {
                    result[n++] = keys[i];
                }
            }
            return result;
        }

        public Iterator<K> iterator()
        {
            final int[] indices = occupied();
            return new Iterator<K>()
            {
                private int position;

                public boolean hasNext()
                {
                    return position < indices.length;
                }

                public K next()
                {
                    if (position >= indices.length)
                    {
                        throw new NoSuchElementException();
                    }
                    return (K)keys[indices[position++]];
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }

    private final class Values implements Collection<V>
    {
        public int size()
        {
            return count;
        }

        public boolean isEmpty()
        {
            return count == 0;
        }

        public boolean add(V value)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean contains(Object value)
        {
            return containsValue(value);
        }

        public boolean remove(Object value)
        {
            int[] indices = occupied();
            for (int i = 0; i < indices.length; i++)
            {
                if (values[indices[i]] == value ||
                    (values[indices[i]] != null && values[indices[i]].equals(value)))
                {
                    IdentityHashMap.this.remove(keys[indices[i]]);
                    return true;
                }
            }
            return false;
        }

        public void clear()
        {
            IdentityHashMap.this.clear();
        }

        public Object[] toArray()
        {
            Object[] result = new Object[count];
            int n = 0;
            for (int i = 0; i < keys.length; i++)
            {
                if (used[i])
                {
                    result[n++] = values[i];
                }
            }
            return result;
        }

        public Iterator<V> iterator()
        {
            final int[] indices = occupied();
            return new Iterator<V>()
            {
                private int position;

                public boolean hasNext()
                {
                    return position < indices.length;
                }

                public V next()
                {
                    if (position >= indices.length)
                    {
                        throw new NoSuchElementException();
                    }
                    return (V)values[indices[position++]];
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }

    private final class EntrySet implements Set<Map.Entry<K, V>>
    {
        public int size()
        {
            return count;
        }

        public boolean isEmpty()
        {
            return count == 0;
        }

        public boolean add(Map.Entry<K, V> value)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean contains(Object value)
        {
            if (!(value instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry entry = (Map.Entry)value;
            int index = findIndex(entry.getKey());
            if (index < 0)
            {
                return false;
            }
            Object mine = values[index];
            Object other = entry.getValue();
            return mine == other || (mine != null && mine.equals(other));
        }

        public boolean remove(Object value)
        {
            if (!(value instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry entry = (Map.Entry)value;
            int index = findIndex(entry.getKey());
            if (index < 0)
            {
                return false;
            }
            IdentityHashMap.this.remove(entry.getKey());
            return true;
        }

        public void clear()
        {
            IdentityHashMap.this.clear();
        }

        public Object[] toArray()
        {
            Object[] result = new Object[count];
            int n = 0;
            for (int i = 0; i < keys.length; i++)
            {
                if (used[i])
                {
                    result[n++] = new EntryImpl(keys[i]);
                }
            }
            return result;
        }

        public Iterator<Map.Entry<K, V>> iterator()
        {
            final int[] indices = occupied();
            return new Iterator<Map.Entry<K, V>>()
            {
                private int position;

                public boolean hasNext()
                {
                    return position < indices.length;
                }

                public Map.Entry<K, V> next()
                {
                    if (position >= indices.length)
                    {
                        throw new NoSuchElementException();
                    }
                    return new EntryImpl(keys[indices[position++]]);
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }

    private final class EntryImpl implements Map.Entry<K, V>
    {
        private final Object key;

        EntryImpl(Object key)
        {
            this.key = key;
        }

        public K getKey()
        {
            return (K)key;
        }

        public V getValue()
        {
            return IdentityHashMap.this.get(key);
        }

        public V setValue(V value)
        {
            return IdentityHashMap.this.put((K)key, value);
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry entry = (Map.Entry)other;
            if (entry.getKey() != key)
            {
                return false;
            }
            V mine = getValue();
            Object theirs = entry.getValue();
            return mine == theirs || (mine != null && mine.equals(theirs));
        }

        public int hashCode()
        {
            return System.identityHashCode(key) ^ Objects.hashCode(getValue());
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < keys.length; i++)
        {
            if (!used[i])
            {
                continue;
            }
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(keys[i])).append("=").append(String.valueOf(values[i]));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
