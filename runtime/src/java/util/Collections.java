package java.util;

public class Collections
{
    private Collections()
    {
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list)
    {
        sort(list, null);
    }

    public static <T> void sort(List<T> list, Comparator<? super T> comparator)
    {
        int size = list.size();
        for (int i = 1; i < size; i++)
        {
            T value = list.get(i);
            int j = i - 1;
            while (j >= 0)
            {
                T other = list.get(j);
                int result;
                if (comparator == null)
                {
                    result = ((Comparable)other).compareTo(value);
                }
                else
                {
                    result = comparator.compare(other, value);
                }
                if (result <= 0)
                {
                    break;
                }
                list.set(j + 1, other);
                j--;
            }
            list.set(j + 1, value);
        }
    }

    public static void reverse(List<?> list)
    {
        int size = list.size();
        for (int i = 0, j = size - 1; i < j; i++, j--)
        {
            swap(list, i, j);
        }
    }

    public static void swap(List<?> list, int i, int j)
    {
        List<Object> target = (List<Object>)list;
        Object value = target.get(i);
        target.set(i, target.get(j));
        target.set(j, value);
    }

    public static <T> boolean addAll(Collection<? super T> values, T... elements)
    {
        boolean changed = false;
        for (int i = 0; i < elements.length; i++)
        {
            changed |= values.add(elements[i]);
        }
        return changed;
    }

    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> values)
    {
        Iterator<? extends T> it = values.iterator();
        T best = it.next();
        while (it.hasNext())
        {
            T value = it.next();
            if (((Comparable)value).compareTo(best) > 0)
            {
                best = value;
            }
        }
        return best;
    }

    public static <T> T max(Collection<? extends T> values, Comparator<? super T> comparator)
    {
        Iterator<? extends T> it = values.iterator();
        T best = it.next();
        while (it.hasNext())
        {
            T value = it.next();
            if (comparator.compare(value, best) > 0)
            {
                best = value;
            }
        }
        return best;
    }

    public static <T> List<T> unmodifiableList(List<? extends T> list)
    {
        return new UnmodifiableList<T>(list);
    }

    private static final ReverseComparator REVERSE_ORDER = new ReverseComparator();

    public static <T> Comparator<T> reverseOrder()
    {
        return (Comparator<T>)REVERSE_ORDER;
    }

    public static <T> Comparator<T> reverseOrder(Comparator<T> comparator)
    {
        if (comparator == null)
        {
            return reverseOrder();
        }
        return new ReverseComparator2<T>(comparator);
    }

    private static class ReverseComparator implements Comparator<Comparable<Object>>
    {
        public int compare(Comparable<Object> first, Comparable<Object> second)
        {
            return second.compareTo(first);
        }
    }

    private static class ReverseComparator2<T> implements Comparator<T>
    {
        private final Comparator<T> comparator;

        ReverseComparator2(Comparator<T> comparator)
        {
            this.comparator = comparator;
        }

        public int compare(T first, T second)
        {
            return comparator.compare(second, first);
        }
    }

    public static <T> Set<T> unmodifiableSet(Set<? extends T> set)
    {
        return new UnmodifiableSet<T>(set);
    }

    public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> values)
    {
        return new UnmodifiableCollection<T>(values);
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> map)
    {
        return new UnmodifiableMap<K, V>(map);
    }

    public static <T> Collection<T> synchronizedCollection(Collection<T> values)
    {
        return new SynchronizedCollection<T>(values);
    }

    public static <T> Set<T> synchronizedSet(Set<T> set)
    {
        return new SynchronizedSet<T>(set);
    }

    public static final List EMPTY_LIST = new EmptyList<Object>();

    public static final Set EMPTY_SET = new EmptySet<Object>();

    public static final Map EMPTY_MAP = new EmptyMap<Object, Object>();

    public static <T> List<T> emptyList()
    {
        return (List<T>)EMPTY_LIST;
    }

    public static <T> Set<T> emptySet()
    {
        return (Set<T>)EMPTY_SET;
    }

    public static <K, V> Map<K, V> emptyMap()
    {
        return (Map<K, V>)EMPTY_MAP;
    }

    public static <T> Iterator<T> emptyIterator()
    {
        return new EmptyIterator<T>();
    }

    public static <T> List<T> singletonList(T value)
    {
        return new SingletonList<T>(value);
    }

    public static <T> Set<T> singleton(T value)
    {
        return new SingletonSet<T>(value);
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value)
    {
        return new SingletonMap<K, V>(key, value);
    }

    private static class SynchronizedCollection<E> implements Collection<E>
    {
        final Collection<E> values;

        SynchronizedCollection(Collection<E> values)
        {
            if (values == null)
            {
                throw new NullPointerException("collection");
            }
            this.values = values;
        }

        public synchronized int size()
        {
            return values.size();
        }

        public synchronized boolean isEmpty()
        {
            return values.isEmpty();
        }

        public synchronized boolean contains(Object value)
        {
            return values.contains(value);
        }

        public synchronized boolean add(E value)
        {
            return values.add(value);
        }

        public synchronized boolean remove(Object value)
        {
            return values.remove(value);
        }

        public synchronized void clear()
        {
            values.clear();
        }

        public synchronized Object[] toArray()
        {
            return values.toArray();
        }

        public synchronized <T> T[] toArray(T[] target)
        {
            return values.toArray(target);
        }

        public synchronized boolean addAll(Collection<? extends E> other)
        {
            return values.addAll(other);
        }

        public synchronized boolean containsAll(Collection<?> other)
        {
            return values.containsAll(other);
        }

        public synchronized boolean removeAll(Collection<?> other)
        {
            return values.removeAll(other);
        }

        public synchronized boolean retainAll(Collection<?> other)
        {
            return values.retainAll(other);
        }

        public synchronized Iterator<E> iterator()
        {
            return values.iterator();
        }

        public synchronized String toString()
        {
            return values.toString();
        }

        public synchronized int hashCode()
        {
            return values.hashCode();
        }

        public synchronized boolean equals(Object other)
        {
            return values.equals(other);
        }
    }

    private static final class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E>
    {
        SynchronizedSet(Set<E> set)
        {
            super(set);
        }
    }

    private static UnsupportedOperationException unsupportedCollection()
    {
        return new UnsupportedOperationException("unmodifiable collection");
    }

    private static final class EmptyIterator<E> implements Iterator<E>
    {
        public boolean hasNext()
        {
            return false;
        }

        public E next()
        {
            throw new NoSuchElementException();
        }

        public void remove()
        {
            throw new IllegalStateException("no current element");
        }
    }

    private static class UnmodifiableCollection<E> implements Collection<E>
    {
        private final Collection<? extends E> values;

        UnmodifiableCollection(Collection<? extends E> values)
        {
            if (values == null)
            {
                throw new NullPointerException("collection");
            }
            this.values = values;
        }

        public int size()
        {
            return values.size();
        }

        public boolean isEmpty()
        {
            return values.isEmpty();
        }

        public boolean contains(Object value)
        {
            return values.contains(value);
        }

        public boolean add(E value)
        {
            throw unsupportedCollection();
        }

        public boolean remove(Object value)
        {
            throw unsupportedCollection();
        }

        public void clear()
        {
            throw unsupportedCollection();
        }

        public Object[] toArray()
        {
            return values.toArray();
        }

        public Iterator<E> iterator()
        {
            final Iterator<? extends E> it = values.iterator();
            return new Iterator<E>()
            {
                public boolean hasNext()
                {
                    return it.hasNext();
                }

                public E next()
                {
                    return it.next();
                }

                public void remove()
                {
                    throw unsupportedCollection();
                }
            };
        }

        public String toString()
        {
            return values.toString();
        }
    }

    private static final class UnmodifiableSet<E> extends UnmodifiableCollection<E> implements Set<E>
    {
        UnmodifiableSet(Set<? extends E> set)
        {
            super(set);
        }
    }

    private static final class UnmodifiableMap<K, V> implements Map<K, V>
    {
        private final Map<? extends K, ? extends V> map;

        UnmodifiableMap(Map<? extends K, ? extends V> map)
        {
            if (map == null)
            {
                throw new NullPointerException("map");
            }
            this.map = map;
        }

        public int size()
        {
            return map.size();
        }

        public boolean isEmpty()
        {
            return map.isEmpty();
        }

        public V put(K key, V value)
        {
            throw unsupportedCollection();
        }

        public V get(Object key)
        {
            return map.get(key);
        }

        public boolean containsKey(Object key)
        {
            return map.containsKey(key);
        }

        public boolean containsValue(Object value)
        {
            return map.containsValue(value);
        }

        public V remove(Object key)
        {
            throw unsupportedCollection();
        }

        public void clear()
        {
            throw unsupportedCollection();
        }

        public void putAll(Map<? extends K, ? extends V> other)
        {
            throw unsupportedCollection();
        }

        public V getOrDefault(Object key, V defaultValue)
        {
            return (V)((Map)map).getOrDefault(key, defaultValue);
        }

        public Set<K> keySet()
        {
            return (Set<K>)(Set)unmodifiableSet(map.keySet());
        }

        public Collection<V> values()
        {
            return (Collection<V>)unmodifiableCollection(map.values());
        }

        public Set<Map.Entry<K, V>> entrySet()
        {
            return (Set<Map.Entry<K, V>>)(Set)unmodifiableSet(map.entrySet());
        }

        public String toString()
        {
            return map.toString();
        }
    }

    private static class EmptyList<E> implements List<E>
    {
        public int size()
        {
            return 0;
        }

        public boolean isEmpty()
        {
            return true;
        }

        public E get(int index)
        {
            throw new IndexOutOfBoundsException("index " + index);
        }

        public E set(int index, E value)
        {
            throw unsupportedCollection();
        }

        public void add(int index, E value)
        {
            throw unsupportedCollection();
        }

        public E remove(int index)
        {
            throw unsupportedCollection();
        }

        public boolean add(E value)
        {
            throw unsupportedCollection();
        }

        public boolean addAll(Collection<? extends E> values)
        {
            throw unsupportedCollection();
        }

        public boolean addAll(int index, Collection<? extends E> values)
        {
            throw unsupportedCollection();
        }

        public boolean remove(Object value)
        {
            throw unsupportedCollection();
        }

        public boolean contains(Object value)
        {
            return false;
        }

        public int indexOf(Object value)
        {
            return -1;
        }

        public int lastIndexOf(Object value)
        {
            return -1;
        }

        public void clear()
        {
            throw unsupportedCollection();
        }

        public Object[] toArray()
        {
            return new Object[0];
        }

        public Iterator<E> iterator()
        {
            return new EmptyIterator<E>();
        }

        public String toString()
        {
            return "[]";
        }
    }

    private static final class EmptySet<E> extends UnmodifiableCollection<E> implements Set<E>
    {
        EmptySet()
        {
            super(new ArrayList<E>());
        }

        public Object[] toArray()
        {
            return new Object[0];
        }

        public String toString()
        {
            return "[]";
        }
    }

    private static final class EmptyMap<K, V> implements Map<K, V>
    {
        public int size()
        {
            return 0;
        }

        public boolean isEmpty()
        {
            return true;
        }

        public V put(K key, V value)
        {
            throw unsupportedCollection();
        }

        public V get(Object key)
        {
            return null;
        }

        public boolean containsKey(Object key)
        {
            return false;
        }

        public boolean containsValue(Object value)
        {
            return false;
        }

        public V remove(Object key)
        {
            throw unsupportedCollection();
        }

        public void clear()
        {
            throw unsupportedCollection();
        }

        public void putAll(Map<? extends K, ? extends V> other)
        {
            throw unsupportedCollection();
        }

        public V getOrDefault(Object key, V defaultValue)
        {
            return defaultValue;
        }

        public Set<K> keySet()
        {
            return (Set<K>)EMPTY_SET;
        }

        public Collection<V> values()
        {
            return (Collection<V>)EMPTY_LIST;
        }

        public Set<Map.Entry<K, V>> entrySet()
        {
            return (Set<Map.Entry<K, V>>)EMPTY_SET;
        }

        public String toString()
        {
            return "{}";
        }
    }

    private static final class SingletonList<E> extends EmptyList<E>
    {
        private final E value;

        SingletonList(E value)
        {
            this.value = value;
        }

        public int size()
        {
            return 1;
        }

        public boolean isEmpty()
        {
            return false;
        }

        public E get(int index)
        {
            if (index != 0)
            {
                throw new IndexOutOfBoundsException("index " + index);
            }
            return value;
        }

        public boolean contains(Object other)
        {
            return other == null ? value == null : other.equals(value);
        }

        public int indexOf(Object other)
        {
            return contains(other) ? 0 : -1;
        }

        public int lastIndexOf(Object other)
        {
            return indexOf(other);
        }

        public Object[] toArray()
        {
            Object[] result = new Object[1];
            result[0] = value;
            return result;
        }

        public Iterator<E> iterator()
        {
            return new Iterator<E>()
            {
                private int index;

                public boolean hasNext()
                {
                    return index < 1;
                }

                public E next()
                {
                    if (index >= 1)
                    {
                        throw new NoSuchElementException();
                    }
                    index++;
                    return value;
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("unmodifiable list");
                }
            };
        }

        public String toString()
        {
            return "[" + String.valueOf(value) + "]";
        }
    }

    private static final class SingletonSet<E> extends UnmodifiableCollection<E> implements Set<E>
    {
        private final E value;

        SingletonSet(E value)
        {
            super(new ArrayList<E>(1));
            this.value = value;
        }

        public int size()
        {
            return 1;
        }

        public boolean isEmpty()
        {
            return false;
        }

        public boolean contains(Object other)
        {
            return other == null ? value == null : other.equals(value);
        }

        public Object[] toArray()
        {
            Object[] result = new Object[1];
            result[0] = value;
            return result;
        }

        public String toString()
        {
            return "[" + String.valueOf(value) + "]";
        }
    }

    private static final class SingletonMap<K, V> implements Map<K, V>
    {
        private final K key;
        private final V value;

        SingletonMap(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public int size()
        {
            return 1;
        }

        public boolean isEmpty()
        {
            return false;
        }

        public V put(K otherKey, V otherValue)
        {
            throw unsupportedCollection();
        }

        public V get(Object otherKey)
        {
            return containsKey(otherKey) ? value : null;
        }

        public boolean containsKey(Object otherKey)
        {
            return otherKey == null ? key == null : otherKey.equals(key);
        }

        public boolean containsValue(Object otherValue)
        {
            return otherValue == null ? value == null : otherValue.equals(value);
        }

        public V remove(Object otherKey)
        {
            throw unsupportedCollection();
        }

        public void clear()
        {
            throw unsupportedCollection();
        }

        public void putAll(Map<? extends K, ? extends V> other)
        {
            throw unsupportedCollection();
        }

        public V getOrDefault(Object otherKey, V defaultValue)
        {
            return containsKey(otherKey) ? value : defaultValue;
        }

        public Set<K> keySet()
        {
            return Collections.singleton(key);
        }

        public Collection<V> values()
        {
            return Collections.singletonList(value);
        }

        public Set<Map.Entry<K, V>> entrySet()
        {
            return Collections.singleton((Map.Entry<K, V>)new SingletonEntry<K, V>(key, value));
        }

        public String toString()
        {
            return "{" + String.valueOf(key) + "=" + String.valueOf(value) + "}";
        }
    }

    private static final class SingletonEntry<K, V> implements Map.Entry<K, V>
    {
        private final K key;
        private final V value;

        SingletonEntry(K key, V value)
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

        public V setValue(V other)
        {
            throw unsupportedCollection();
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
            return (key == null ? otherKey == null : key.equals(otherKey))
                && (value == null ? otherValue == null : value.equals(otherValue));
        }

        public int hashCode()
        {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }

    public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> values)
    {
        Iterator<? extends T> it = values.iterator();
        T best = it.next();
        while (it.hasNext())
        {
            T value = it.next();
            if (((Comparable)value).compareTo(best) < 0)
            {
                best = value;
            }
        }
        return best;
    }

    public static <T> Enumeration<T> enumeration(Collection<T> values)
    {
        return new ArrayEnumeration<T>(values);
    }

    public static <T> ArrayList<T> list(Enumeration<T> values)
    {
        ArrayList<T> result = new ArrayList<T>();
        while (values.hasMoreElements())
        {
            result.add(values.nextElement());
        }
        return result;
    }

    private static final class UnmodifiableList<E> implements List<E>
    {
        private final List<? extends E> list;

        UnmodifiableList(List<? extends E> list)
        {
            if (list == null)
            {
                throw new NullPointerException("list");
            }
            this.list = list;
        }

        private static UnsupportedOperationException unsupported()
        {
            return new UnsupportedOperationException("unmodifiable list");
        }

        public int size()
        {
            return list.size();
        }

        public boolean isEmpty()
        {
            return list.isEmpty();
        }

        public E get(int index)
        {
            return list.get(index);
        }

        public E set(int index, E value)
        {
            throw unsupported();
        }

        public void add(int index, E value)
        {
            throw unsupported();
        }

        public boolean add(E value)
        {
            throw unsupported();
        }

        public boolean addAll(Collection<? extends E> values)
        {
            throw unsupported();
        }

        public boolean addAll(int index, Collection<? extends E> values)
        {
            throw unsupported();
        }

        public E remove(int index)
        {
            throw unsupported();
        }

        public boolean remove(Object value)
        {
            throw unsupported();
        }

        public boolean contains(Object value)
        {
            return list.contains(value);
        }

        public int indexOf(Object value)
        {
            return list.indexOf(value);
        }

        public int lastIndexOf(Object value)
        {
            return list.lastIndexOf(value);
        }

        public void clear()
        {
            throw unsupported();
        }

        public Object[] toArray()
        {
            return list.toArray();
        }

        public Iterator<E> iterator()
        {
            return new Itr(list.iterator());
        }

        public String toString()
        {
            return list.toString();
        }

        private static final class Itr<E> implements Iterator<E>
        {
            private final Iterator<? extends E> iterator;

            Itr(Iterator<? extends E> iterator)
            {
                this.iterator = iterator;
            }

            public boolean hasNext()
            {
                return iterator.hasNext();
            }

            public E next()
            {
                return iterator.next();
            }

            public void remove()
            {
                throw new UnsupportedOperationException("unmodifiable list");
            }
        }
    }

    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> map)
    {
        return new SynchronizedMap<K, V>(map);
    }

    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map)
    {
        if (map == null)
        {
            throw new NullPointerException("map");
        }
        if (!map.isEmpty())
        {
            throw new IllegalArgumentException("map is not empty");
        }
        return new SetFromMap<E>(map);
    }

    private static final class SetFromMap<E> implements Set<E>
    {
        private final Map<E, Boolean> map;

        SetFromMap(Map<E, Boolean> map)
        {
            this.map = map;
        }

        public int size()
        {
            return map.size();
        }

        public boolean isEmpty()
        {
            return map.isEmpty();
        }

        public boolean add(E value)
        {
            return map.put(value, Boolean.TRUE) == null;
        }

        public boolean contains(Object value)
        {
            return map.containsKey(value);
        }

        public boolean remove(Object value)
        {
            return map.remove(value) != null;
        }

        public void clear()
        {
            map.clear();
        }

        public Object[] toArray()
        {
            return map.keySet().toArray();
        }

        public Iterator<E> iterator()
        {
            final Iterator<E> keys = map.keySet().iterator();
            return new Iterator<E>()
            {
                private E last;

                public boolean hasNext()
                {
                    return keys.hasNext();
                }

                public E next()
                {
                    last = keys.next();
                    return last;
                }

                public void remove()
                {
                    if (last == null)
                    {
                        throw new IllegalStateException("no current element");
                    }
                    SetFromMap.this.remove(last);
                    last = null;
                }
            };
        }

        public String toString()
        {
            return map.keySet().toString();
        }
    }

    private static final class SynchronizedMap<K, V> implements Map<K, V>
    {
        private final Map<K, V> delegate;

        SynchronizedMap(Map<K, V> delegate)
        {
            this.delegate = delegate;
        }

        public synchronized int size()
        {
            return delegate.size();
        }

        public synchronized boolean isEmpty()
        {
            return delegate.isEmpty();
        }

        public synchronized V put(K key, V value)
        {
            return delegate.put(key, value);
        }

        public synchronized V get(Object key)
        {
            return delegate.get(key);
        }

        public synchronized boolean containsKey(Object key)
        {
            return delegate.containsKey(key);
        }

        public synchronized boolean containsValue(Object value)
        {
            return delegate.containsValue(value);
        }

        public synchronized V remove(Object key)
        {
            return delegate.remove(key);
        }

        public synchronized void clear()
        {
            delegate.clear();
        }

        public synchronized void putAll(Map<? extends K, ? extends V> other)
        {
            delegate.putAll(other);
        }

        public synchronized V getOrDefault(Object key, V defaultValue)
        {
            return delegate.getOrDefault(key, defaultValue);
        }

        public synchronized Set<K> keySet()
        {
            return delegate.keySet();
        }

        public synchronized Collection<V> values()
        {
            return delegate.values();
        }

        public synchronized Set<Map.Entry<K, V>> entrySet()
        {
            return delegate.entrySet();
        }
    }
}
