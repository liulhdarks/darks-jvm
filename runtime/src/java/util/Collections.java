package java.util;

public class Collections
{
    private Collections()
    {
    }

    public static <T> Comparator<T> reverseOrder()
    {
        return new Comparator<T>()
        {
            public int compare(T first, T second)
            {
                return ((Comparable<T>)second).compareTo(first);
            }
        };
    }

    public static <T> Comparator<T> reverseOrder(final Comparator<T> comparator)
    {
        if (comparator == null)
        {
            return reverseOrder();
        }
        return new Comparator<T>()
        {
            public int compare(T first, T second)
            {
                return comparator.compare(second, first);
            }
        };
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

    public static <T> boolean addAll(Collection<? super T> collection, T... values)
    {
        boolean changed = false;
        if (values != null)
        {
            for (int i = 0; i < values.length; i++)
            {
                if (collection.add(values[i]))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }

    public static <T> List<T> emptyList()
    {
        return (List<T>)new EmptyList();
    }

    public static <T> Set<T> emptySet()
    {
        return (Set<T>)new EmptySet();
    }

    public static <K, V> Map<K, V> emptyMap()
    {
        return (Map<K, V>)new EmptyMap();
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

    public static <T> Set<T> unmodifiableSet(Set<? extends T> set)
    {
        if (set == null)
        {
            throw new NullPointerException("set");
        }
        return new UnmodifiableSet<T>(set);
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> map)
    {
        if (map == null)
        {
            throw new NullPointerException("map");
        }
        return new UnmodifiableMap<K, V>(map);
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

    public static <T> Set<T> synchronizedSet(Set<T> set)
    {
        if (set == null)
        {
            throw new NullPointerException("set");
        }
        return new SynchronizedSet<T>(set);
    }

    public static <T> List<T> synchronizedList(List<T> list)
    {
        if (list == null)
        {
            throw new NullPointerException("list");
        }
        return new SynchronizedList<T>(list);
    }

    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> map)
    {
        if (map == null)
        {
            throw new NullPointerException("map");
        }
        return new SynchronizedMap<K, V>(map);
    }

    private static final class SynchronizedSet<E> extends AbstractSet<E>
    {
        private final Set<E> delegate;

        SynchronizedSet(Set<E> delegate)
        {
            this.delegate = delegate;
        }

        public int size()
        {
            return delegate.size();
        }

        public boolean isEmpty()
        {
            return delegate.isEmpty();
        }

        public boolean contains(Object value)
        {
            return delegate.contains(value);
        }

        public Iterator<E> iterator()
        {
            return delegate.iterator();
        }

        public Object[] toArray()
        {
            return delegate.toArray();
        }

        public boolean add(E value)
        {
            return delegate.add(value);
        }

        public boolean remove(Object value)
        {
            return delegate.remove(value);
        }

        public boolean containsAll(Collection<?> values)
        {
            return delegate.containsAll(values);
        }

        public boolean addAll(Collection<? extends E> values)
        {
            return delegate.addAll(values);
        }

        public boolean removeAll(Collection<?> values)
        {
            return delegate.removeAll(values);
        }

        public boolean retainAll(Collection<?> values)
        {
            return delegate.retainAll(values);
        }

        public void clear()
        {
            delegate.clear();
        }

        public boolean equals(Object other)
        {
            return delegate.equals(other);
        }

        public int hashCode()
        {
            return delegate.hashCode();
        }

        public String toString()
        {
            return delegate.toString();
        }
    }

    private static final class SynchronizedList<E> implements List<E>
    {
        private final List<E> delegate;

        SynchronizedList(List<E> delegate)
        {
            this.delegate = delegate;
        }

        public ListIterator<E> listIterator()
        {
            return delegate.listIterator();
        }

        public ListIterator<E> listIterator(int index)
        {
            return delegate.listIterator(index);
        }

        public int size()
        {
            return delegate.size();
        }

        public boolean isEmpty()
        {
            return delegate.isEmpty();
        }

        public boolean contains(Object value)
        {
            return delegate.contains(value);
        }

        public Iterator<E> iterator()
        {
            return delegate.iterator();
        }

        public Object[] toArray()
        {
            return delegate.toArray();
        }

        public <T> T[] toArray(T[] array)
        {
            return delegate.toArray(array);
        }

        public boolean add(E value)
        {
            return delegate.add(value);
        }

        public boolean remove(Object value)
        {
            return delegate.remove(value);
        }

        public boolean containsAll(Collection<?> values)
        {
            return delegate.containsAll(values);
        }

        public boolean addAll(Collection<? extends E> values)
        {
            return delegate.addAll(values);
        }

        public boolean addAll(int index, Collection<? extends E> values)
        {
            return delegate.addAll(index, values);
        }

        public boolean removeAll(Collection<?> values)
        {
            return delegate.removeAll(values);
        }

        public boolean retainAll(Collection<?> values)
        {
            return delegate.retainAll(values);
        }

        public void clear()
        {
            delegate.clear();
        }

        public E get(int index)
        {
            return delegate.get(index);
        }

        public E set(int index, E value)
        {
            return delegate.set(index, value);
        }

        public void add(int index, E value)
        {
            delegate.add(index, value);
        }

        public E remove(int index)
        {
            return delegate.remove(index);
        }

        public int indexOf(Object value)
        {
            return delegate.indexOf(value);
        }

        public int lastIndexOf(Object value)
        {
            return delegate.lastIndexOf(value);
        }

        public boolean equals(Object other)
        {
            return delegate.equals(other);
        }

        public int hashCode()
        {
            return delegate.hashCode();
        }

        public String toString()
        {
            return delegate.toString();
        }
    }

    private static final class SynchronizedMap<K, V> extends AbstractMap<K, V>
    {
        private final Map<K, V> delegate;

        SynchronizedMap(Map<K, V> delegate)
        {
            this.delegate = delegate;
        }

        public int size()
        {
            return delegate.size();
        }

        public boolean isEmpty()
        {
            return delegate.isEmpty();
        }

        public boolean containsKey(Object key)
        {
            return delegate.containsKey(key);
        }

        public boolean containsValue(Object value)
        {
            return delegate.containsValue(value);
        }

        public V get(Object key)
        {
            return delegate.get(key);
        }

        public V put(K key, V value)
        {
            return delegate.put(key, value);
        }

        public V remove(Object key)
        {
            return delegate.remove(key);
        }

        public void putAll(Map<? extends K, ? extends V> values)
        {
            delegate.putAll(values);
        }

        public void clear()
        {
            delegate.clear();
        }

        public Set<K> keySet()
        {
            return delegate.keySet();
        }

        public Collection<V> values()
        {
            return delegate.values();
        }

        public Set<Map.Entry<K, V>> entrySet()
        {
            return delegate.entrySet();
        }

        public boolean equals(Object other)
        {
            return delegate.equals(other);
        }

        public int hashCode()
        {
            return delegate.hashCode();
        }

        public String toString()
        {
            return delegate.toString();
        }
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

        public ListIterator<E> listIterator()
        {
            return listIterator(0);
        }

        public ListIterator<E> listIterator(int index)
        {
            return ListIterators.create(this, index, false);
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
            throw new UnsupportedOperationException("remove");
        }
    }

    private static final class EmptyList<E> implements List<E>
    {
        public ListIterator<E> listIterator()
        {
            return listIterator(0);
        }

        public ListIterator<E> listIterator(int index)
        {
            return ListIterators.create(this, index, false);
        }

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
            throw new UnsupportedOperationException("set");
        }

        public void add(int index, E value)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean add(E value)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean addAll(Collection<? extends E> values)
        {
            throw new UnsupportedOperationException("addAll");
        }

        public boolean addAll(int index, Collection<? extends E> values)
        {
            throw new UnsupportedOperationException("addAll");
        }

        public E remove(int index)
        {
            throw new UnsupportedOperationException("remove");
        }

        public boolean remove(Object value)
        {
            throw new UnsupportedOperationException("remove");
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

    private static final class EmptySet<E> implements Set<E>
    {
        public int size()
        {
            return 0;
        }

        public boolean isEmpty()
        {
            return true;
        }

        public boolean add(E value)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean contains(Object value)
        {
            return false;
        }

        public boolean remove(Object value)
        {
            throw new UnsupportedOperationException("remove");
        }

        public void clear()
        {
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
            throw new UnsupportedOperationException("put");
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
            throw new UnsupportedOperationException("remove");
        }

        public void clear()
        {
        }

        public void putAll(Map<? extends K, ? extends V> other)
        {
            throw new UnsupportedOperationException("putAll");
        }

        public V getOrDefault(Object key, V defaultValue)
        {
            return defaultValue;
        }

        public Set<K> keySet()
        {
            return Collections.emptySet();
        }

        public Collection<V> values()
        {
            return Collections.emptyList();
        }

        public Set<Map.Entry<K, V>> entrySet()
        {
            return Collections.emptySet();
        }

        public String toString()
        {
            return "{}";
        }
    }

    private static final class SingletonList<E> implements List<E>
    {
        private final E value;

        SingletonList(E value)
        {
            this.value = value;
        }

        public ListIterator<E> listIterator()
        {
            return listIterator(0);
        }

        public ListIterator<E> listIterator(int index)
        {
            return ListIterators.create(this, index, false);
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

        public E set(int index, E newValue)
        {
            throw new UnsupportedOperationException("set");
        }

        public void add(int index, E newValue)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean add(E newValue)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean addAll(Collection<? extends E> values)
        {
            throw new UnsupportedOperationException("addAll");
        }

        public boolean addAll(int index, Collection<? extends E> values)
        {
            throw new UnsupportedOperationException("addAll");
        }

        public E remove(int index)
        {
            throw new UnsupportedOperationException("remove");
        }

        public boolean remove(Object oldValue)
        {
            throw new UnsupportedOperationException("remove");
        }

        public boolean contains(Object candidate)
        {
            return candidate == value || (candidate != null && candidate.equals(value));
        }

        public int indexOf(Object candidate)
        {
            return contains(candidate) ? 0 : -1;
        }

        public int lastIndexOf(Object candidate)
        {
            return contains(candidate) ? 0 : -1;
        }

        public void clear()
        {
            throw new UnsupportedOperationException("clear");
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
                private boolean done;

                public boolean hasNext()
                {
                    return !done;
                }

                public E next()
                {
                    if (done)
                    {
                        throw new NoSuchElementException();
                    }
                    done = true;
                    return value;
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }

    private static final class SingletonSet<E> implements Set<E>
    {
        private final E value;

        SingletonSet(E value)
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

        public boolean add(E newValue)
        {
            throw new UnsupportedOperationException("add");
        }

        public boolean contains(Object candidate)
        {
            return candidate == value || (candidate != null && candidate.equals(value));
        }

        public boolean remove(Object candidate)
        {
            throw new UnsupportedOperationException("remove");
        }

        public void clear()
        {
            throw new UnsupportedOperationException("clear");
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
                private boolean done;

                public boolean hasNext()
                {
                    return !done;
                }

                public E next()
                {
                    if (done)
                    {
                        throw new NoSuchElementException();
                    }
                    done = true;
                    return value;
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("remove");
                }
            };
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

        public V put(K newKey, V newValue)
        {
            throw new UnsupportedOperationException("put");
        }

        public V get(Object candidate)
        {
            return containsKey(candidate) ? value : null;
        }

        public boolean containsKey(Object candidate)
        {
            return candidate == key || (candidate != null && candidate.equals(key));
        }

        public boolean containsValue(Object candidate)
        {
            return candidate == value || (candidate != null && candidate.equals(value));
        }

        public V remove(Object candidate)
        {
            throw new UnsupportedOperationException("remove");
        }

        public void clear()
        {
            throw new UnsupportedOperationException("clear");
        }

        public void putAll(Map<? extends K, ? extends V> other)
        {
            throw new UnsupportedOperationException("putAll");
        }

        public V getOrDefault(Object candidate, V defaultValue)
        {
            return containsKey(candidate) ? value : defaultValue;
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
            return Collections.singleton((Map.Entry<K, V>)new SingletonEntry(key, value));
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
            Object otherKey = entry.getKey();
            Object otherValue = entry.getValue();
            if (!(key == otherKey || (key != null && key.equals(otherKey))))
            {
                return false;
            }
            return value == otherValue || (value != null && value.equals(otherValue));
        }

        public int hashCode()
        {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }

    private static final class UnmodifiableSet<E> implements Set<E>
    {
        private final Set<? extends E> set;

        UnmodifiableSet(Set<? extends E> set)
        {
            this.set = set;
        }

        public int size()
        {
            return set.size();
        }

        public boolean isEmpty()
        {
            return set.isEmpty();
        }

        public boolean add(E value)
        {
            throw new UnsupportedOperationException("unmodifiable set");
        }

        public boolean contains(Object value)
        {
            return set.contains(value);
        }

        public boolean remove(Object value)
        {
            throw new UnsupportedOperationException("unmodifiable set");
        }

        public void clear()
        {
            throw new UnsupportedOperationException("unmodifiable set");
        }

        public Object[] toArray()
        {
            return set.toArray();
        }

        public Iterator<E> iterator()
        {
            final Iterator<? extends E> iterator = set.iterator();
            return new Iterator<E>()
            {
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
                    throw new UnsupportedOperationException("unmodifiable set");
                }
            };
        }
    }

    private static final class UnmodifiableMap<K, V> implements Map<K, V>
    {
        private final Map<? extends K, ? extends V> map;

        UnmodifiableMap(Map<? extends K, ? extends V> map)
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

        public V put(K key, V value)
        {
            throw new UnsupportedOperationException("unmodifiable map");
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
            throw new UnsupportedOperationException("unmodifiable map");
        }

        public void clear()
        {
            throw new UnsupportedOperationException("unmodifiable map");
        }

        public void putAll(Map<? extends K, ? extends V> other)
        {
            throw new UnsupportedOperationException("unmodifiable map");
        }

        public V getOrDefault(Object key, V defaultValue)
        {
            return (V)((Map)map).getOrDefault(key, defaultValue);
        }

        public Set<K> keySet()
        {
            return (Set<K>)(Set<?>)map.keySet();
        }

        public Collection<V> values()
        {
            return (Collection<V>)(Collection<?>)map.values();
        }

        public Set<Map.Entry<K, V>> entrySet()
        {
            return (Set<Map.Entry<K, V>>)(Set<?>)map.entrySet();
        }
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
            return map.keySet().iterator();
        }
    }
}
