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

    public boolean containsValue(Object value)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            V current = it.next().getValue();
            if (value == null ? current == null : value.equals(current))
            {
                return true;
            }
        }
        return false;
    }

    public boolean containsKey(Object key)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            K current = it.next().getKey();
            if (key == null ? current == null : key.equals(current))
            {
                return true;
            }
        }
        return false;
    }

    public V get(Object key)
    {
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            K current = entry.getKey();
            if (key == null ? current == null : key.equals(current))
            {
                return entry.getValue();
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
            K current = entry.getKey();
            if (key == null ? current == null : key.equals(current))
            {
                V old = entry.getValue();
                it.remove();
                return old;
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

    public Set<K> keySet()
    {
        return new AbstractSet<K>()
        {
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

            public int size()
            {
                return AbstractMap.this.size();
            }

            public boolean contains(Object key)
            {
                return containsKey(key);
            }

            public boolean remove(Object key)
            {
                boolean present = containsKey(key);
                if (present)
                {
                    AbstractMap.this.remove(key);
                }
                return present;
            }
        };
    }

    public Collection<V> values()
    {
        return new AbstractCollection<V>()
        {
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

            public int size()
            {
                return AbstractMap.this.size();
            }

            public boolean contains(Object value)
            {
                return containsValue(value);
            }
        };
    }

    public V getOrDefault(Object key, V defaultValue)
    {
        V value = get(key);
        return value != null || containsKey(key) ? value : defaultValue;
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
        Map map = (Map)other;
        if (map.size() != size())
        {
            return false;
        }
        try
        {
            Iterator<Map.Entry<K, V>> it = entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<K, V> entry = it.next();
                K key = entry.getKey();
                V value = entry.getValue();
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
        }
        catch (ClassCastException e)
        {
            return false;
        }
        catch (NullPointerException e)
        {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        int result = 0;
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext())
        {
            result += it.next().hashCode();
        }
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<K, V>> it = entrySet().iterator();
        boolean first = true;
        while (it.hasNext())
        {
            Map.Entry<K, V> entry = it.next();
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(entry.getKey())).append("=").append(String.valueOf(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
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
            return (key == null ? entry.getKey() == null : key.equals(entry.getKey()))
                && (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
        }

        public int hashCode()
        {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
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
            throw new UnsupportedOperationException("setValue");
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

        public String toString()
        {
            return String.valueOf(key) + "=" + String.valueOf(value);
        }
    }
}
