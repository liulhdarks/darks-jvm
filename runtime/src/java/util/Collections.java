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
}
