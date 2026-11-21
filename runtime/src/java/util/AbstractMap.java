package java.util;

public abstract class AbstractMap<K, V> implements Map<K, V>
{
    protected AbstractMap()
    {
    }

    public abstract Set<Map.Entry<K, V>> entrySet();

    public int size()
    {
        return entrySet().size();
    }

    public boolean isEmpty()
    {
        return size() == 0;
    }

    public boolean containsKey(Object key)
    {
        return get(key) != null || entryForKey(key) != null;
    }

    public boolean containsValue(Object value)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Object candidate = it.next().getValue();
            if (candidate == value || (candidate != null && candidate.equals(value)))
            {
                return true;
            }
        }
        return false;
    }

    public V get(Object key)
    {
        Map.Entry<K, V> entry = entryForKey(key);
        return entry == null ? null : entry.getValue();
    }

    private Map.Entry<K, V> entryForKey(Object key)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            Object candidate = entry.getKey();
            if (candidate == key || (candidate != null && candidate.equals(key)))
            {
                return entry;
            }
        }
        return null;
    }

    public V put(K key, V value)
    {
        throw new UnsupportedOperationException("put");
    }

    public V remove(Object key)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            Object candidate = entry.getKey();
            if (candidate == key || (candidate != null && candidate.equals(key)))
            {
                V value = entry.getValue();
                it.remove();
                return value;
            }
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

    public void clear()
    {
        entrySet().clear();
    }

    public V getOrDefault(Object key, V defaultValue)
    {
        V value = get(key);
        return value == null && !containsKey(key) ? defaultValue : value;
    }

    public Set<K> keySet()
    {
        return new KeySetView();
    }

    public Collection<V> values()
    {
        return new ValuesView();
    }

    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof Map))
        {
            return false;
        }
        Map<?, ?> map = (Map<?, ?>)other;
        if (map.size() != size())
        {
            return false;
        }
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value == null)
            {
                if (!(map.get(key) == null && map.containsKey(key)))
                {
                    return false;
                }
            }
            else if (!value.equals(map.get(key)))
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        int result = 0;
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            result += Objects.hashCode(entry.getKey()) ^ Objects.hashCode(entry.getValue());
        }
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(entry.getKey())).append("=")
              .append(String.valueOf(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private final class KeySetView extends AbstractSet<K>
    {
        public int size()
        {
            return AbstractMap.this.size();
        }

        public Iterator<K> iterator()
        {
            final Iterator<Map.Entry<K, V>> entries = entrySet().iterator();
            return new Iterator<K>()
            {
                public boolean hasNext()
                {
                    return entries.hasNext();
                }

                public K next()
                {
                    return entries.next().getKey();
                }

                public void remove()
                {
                    entries.remove();
                }
            };
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
            AbstractMap.this.remove(value);
            return true;
        }

        public void clear()
        {
            AbstractMap.this.clear();
        }
    }

    private final class ValuesView extends AbstractCollection<V>
    {
        public int size()
        {
            return AbstractMap.this.size();
        }

        public Iterator<V> iterator()
        {
            final Iterator<Map.Entry<K, V>> entries = entrySet().iterator();
            return new Iterator<V>()
            {
                public boolean hasNext()
                {
                    return entries.hasNext();
                }

                public V next()
                {
                    return entries.next().getValue();
                }

                public void remove()
                {
                    entries.remove();
                }
            };
        }

        public boolean contains(Object value)
        {
            return containsValue(value);
        }

        public void clear()
        {
            AbstractMap.this.clear();
        }
    }

    public static class SimpleEntry<K, V> implements Map.Entry<K, V>
    {
        private final K key;
        private V value;

        public SimpleEntry(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public SimpleEntry(Map.Entry<? extends K, ? extends V> entry)
        {
            this.key = entry.getKey();
            this.value = entry.getValue();
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
            Object otherKey = entry.getKey();
            Object otherValue = entry.getValue();
            return (key == otherKey || (key != null && key.equals(otherKey))) &&
                (value == otherValue || (value != null && value.equals(otherValue)));
        }

        public int hashCode()
        {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public String toString()
        {
            return String.valueOf(key) + "=" + String.valueOf(value);
        }
    }

    public static class SimpleImmutableEntry<K, V> implements Map.Entry<K, V>
    {
        private final K key;
        private final V value;

        public SimpleImmutableEntry(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> entry)
        {
            this.key = entry.getKey();
            this.value = entry.getValue();
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
            throw new UnsupportedOperationException("immutable entry");
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
            return (key == otherKey || (key != null && key.equals(otherKey))) &&
                (value == otherValue || (value != null && value.equals(otherValue)));
        }

        public int hashCode()
        {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public String toString()
        {
            return String.valueOf(key) + "=" + String.valueOf(value);
        }
    }
}
