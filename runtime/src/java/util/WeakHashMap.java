package java.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class WeakHashMap<K, V> implements Map<K, V>
{
    private static final Object NULL_KEY = new Object();

    private Entry<K, V>[] table;
    private int count;
    private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

    public WeakHashMap()
    {
        this(16);
    }

    public WeakHashMap(int capacity)
    {
        int size = 4;
        while (size < capacity)
        {
            size = size * 2;
        }
        table = (Entry<K, V>[])new Entry[size];
    }

    public WeakHashMap(Map<? extends K, ? extends V> other)
    {
        this(other.size() * 2);
        putAll(other);
    }

    private static Object maskNull(Object key)
    {
        return key == null ? NULL_KEY : key;
    }

    private static int spread(Object key)
    {
        int h = key.hashCode();
        h = h ^ (h >>> 16);
        return h & 0x7fffffff;
    }

    private static boolean keyEquals(Object stored, Object requested)
    {
        Object mine = stored == NULL_KEY ? null : stored;
        return mine == requested || (mine != null && mine.equals(requested));
    }

    private void expunge()
    {
        java.lang.ref.Reference<?> reference;
        while ((reference = queue.poll()) != null)
        {
            Entry<K, V> entry = (Entry<K, V>)reference;
            int index = entry.hash & (table.length - 1);
            Entry<K, V> previous = null;
            Entry<K, V> current = table[index];
            while (current != null)
            {
                if (current == entry)
                {
                    if (previous == null)
                    {
                        table[index] = current.chainNext;
                    }
                    else
                    {
                        previous.chainNext = current.chainNext;
                    }
                    count--;
                    break;
                }
                previous = current;
                current = current.chainNext;
            }
        }
    }

    private Entry<K, V> findEntry(Object key)
    {
        Object actual = maskNull(key);
        int index = spread(actual) & (table.length - 1);
        Entry<K, V> entry = table[index];
        while (entry != null)
        {
            Object stored = entry.get();
            if (stored == null)
            {
                entry = entry.chainNext;
                continue;
            }
            if (keyEquals(stored, key))
            {
                return entry;
            }
            entry = entry.chainNext;
        }
        return null;
    }

    public int size()
    {
        expunge();
        return count;
    }

    public boolean isEmpty()
    {
        expunge();
        return count == 0;
    }

    public V put(K key, V value)
    {
        expunge();
        Object actual = maskNull(key);
        Entry<K, V> existing = findEntry(key);
        if (existing != null)
        {
            V old = existing.value;
            existing.value = value;
            return old;
        }
        int hash = spread(actual);
        int index = hash & (table.length - 1);
        Entry<K, V> entry = new Entry<K, V>(actual, value, queue, hash);
        entry.chainNext = table[index];
        table[index] = entry;
        count++;
        return null;
    }

    public V get(Object key)
    {
        expunge();
        Entry<K, V> entry = findEntry(key);
        return entry == null ? null : entry.value;
    }

    public boolean containsKey(Object key)
    {
        expunge();
        return findEntry(key) != null;
    }

    public boolean containsValue(Object value)
    {
        expunge();
        for (int i = 0; i < table.length; i++)
        {
            Entry<K, V> entry = table[i];
            while (entry != null)
            {
                if (entry.get() != null)
                {
                    Object candidate = entry.value;
                    if (candidate == value ||
                        (candidate != null && candidate.equals(value)))
                    {
                        return true;
                    }
                }
                entry = entry.chainNext;
            }
        }
        return false;
    }

    public V remove(Object key)
    {
        expunge();
        Object actual = maskNull(key);
        int index = spread(actual) & (table.length - 1);
        Entry<K, V> previous = null;
        Entry<K, V> entry = table[index];
        while (entry != null)
        {
            Object stored = entry.get();
            if (stored != null && keyEquals(stored, key))
            {
                if (previous == null)
                {
                    table[index] = entry.chainNext;
                }
                else
                {
                    previous.chainNext = entry.chainNext;
                }
                V old = entry.value;
                entry.clear();
                count--;
                return old;
            }
            previous = entry;
            entry = entry.chainNext;
        }
        return null;
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
        V value = get(key);
        return value == null ? defaultValue : value;
    }

    public void clear()
    {
        for (int i = 0; i < table.length; i++)
        {
            Entry<K, V> entry = table[i];
            while (entry != null)
            {
                Entry<K, V> next = entry.chainNext;
                entry.clear();
                entry.chainNext = null;
                entry = next;
            }
            table[i] = null;
        }
        count = 0;
        while (queue.poll() != null)
        {
        }
    }

    private Entry<K, V>[] snapshot()
    {
        expunge();
        Entry<K, V>[] result = (Entry<K, V>[])new Entry[count];
        int n = 0;
        for (int i = 0; i < table.length; i++)
        {
            Entry<K, V> entry = table[i];
            while (entry != null)
            {
                if (entry.get() != null)
                {
                    result[n++] = entry;
                }
                entry = entry.chainNext;
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

    static class Entry<K, V> extends WeakReference<Object> implements Map.Entry<K, V>
    {
        V value;
        final int hash;
        Entry<K, V> chainNext;

        Entry(Object key, V value, ReferenceQueue<Object> queue, int hash)
        {
            super(key, queue);
            this.value = value;
            this.hash = hash;
        }

        public K getKey()
        {
            Object key = get();
            return (K)(key == NULL_KEY ? null : key);
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
            Object mine = getKey();
            Object theirKey = entry.getKey();
            if (!(mine == theirKey || (mine != null && mine.equals(theirKey))))
            {
                return false;
            }
            V myValue = getValue();
            Object theirValue = entry.getValue();
            return myValue == theirValue ||
                (myValue != null && myValue.equals(theirValue));
        }

        public int hashCode()
        {
            Object key = getKey();
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }

    private final class KeySet implements Set<K>
    {
        public int size()
        {
            return WeakHashMap.this.size();
        }

        public boolean isEmpty()
        {
            return WeakHashMap.this.isEmpty();
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
            WeakHashMap.this.remove(value);
            return true;
        }

        public void clear()
        {
            WeakHashMap.this.clear();
        }

        public Object[] toArray()
        {
            Entry<K, V>[] entries = snapshot();
            Object[] result = new Object[entries.length];
            for (int i = 0; i < entries.length; i++)
            {
                result[i] = entries[i].getKey();
            }
            return result;
        }

        public Iterator<K> iterator()
        {
            final Entry<K, V>[] entries = snapshot();
            return new Iterator<K>()
            {
                private int position;

                public boolean hasNext()
                {
                    return position < entries.length;
                }

                public K next()
                {
                    if (position >= entries.length)
                    {
                        throw new NoSuchElementException();
                    }
                    return entries[position++].getKey();
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
            return WeakHashMap.this.size();
        }

        public boolean isEmpty()
        {
            return WeakHashMap.this.isEmpty();
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
            Entry<K, V>[] entries = snapshot();
            for (int i = 0; i < entries.length; i++)
            {
                Object candidate = entries[i].value;
                if (candidate == value ||
                    (candidate != null && candidate.equals(value)))
                {
                    WeakHashMap.this.remove(entries[i].getKey());
                    return true;
                }
            }
            return false;
        }

        public void clear()
        {
            WeakHashMap.this.clear();
        }

        public Object[] toArray()
        {
            Entry<K, V>[] entries = snapshot();
            Object[] result = new Object[entries.length];
            for (int i = 0; i < entries.length; i++)
            {
                result[i] = entries[i].value;
            }
            return result;
        }

        public Iterator<V> iterator()
        {
            final Entry<K, V>[] entries = snapshot();
            return new Iterator<V>()
            {
                private int position;

                public boolean hasNext()
                {
                    return position < entries.length;
                }

                public V next()
                {
                    if (position >= entries.length)
                    {
                        throw new NoSuchElementException();
                    }
                    return entries[position++].value;
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
            return WeakHashMap.this.size();
        }

        public boolean isEmpty()
        {
            return WeakHashMap.this.isEmpty();
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
            Entry<K, V> mine = findEntry(entry.getKey());
            if (mine == null)
            {
                return false;
            }
            Object other = entry.getValue();
            return mine.value == other ||
                (mine.value != null && mine.value.equals(other));
        }

        public boolean remove(Object value)
        {
            if (!(value instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry entry = (Map.Entry)value;
            if (!contains(entry))
            {
                return false;
            }
            WeakHashMap.this.remove(entry.getKey());
            return true;
        }

        public void clear()
        {
            WeakHashMap.this.clear();
        }

        public Object[] toArray()
        {
            Entry<K, V>[] entries = snapshot();
            Object[] result = new Object[entries.length];
            for (int i = 0; i < entries.length; i++)
            {
                result[i] = entries[i];
            }
            return result;
        }

        public Iterator<Map.Entry<K, V>> iterator()
        {
            final Entry<K, V>[] entries = snapshot();
            return new Iterator<Map.Entry<K, V>>()
            {
                private int position;

                public boolean hasNext()
                {
                    return position < entries.length;
                }

                public Map.Entry<K, V> next()
                {
                    if (position >= entries.length)
                    {
                        throw new NoSuchElementException();
                    }
                    return entries[position++];
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        Entry<K, V>[] entries = snapshot();
        for (int i = 0; i < entries.length; i++)
        {
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(entries[i].getKey())).append("=")
              .append(String.valueOf(entries[i].value));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
