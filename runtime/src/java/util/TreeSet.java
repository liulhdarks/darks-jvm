package java.util;

/**
 * Sorted set implemented over a sorted array list.  Ordering uses the supplied
 * comparator or the elements' natural order.  Sub-set/head-set/tail-set return
 * copies rather than live views, which is enough for the container use cases
 * in this runtime.
 */
public class TreeSet<E> implements NavigableSet<E>, Cloneable
{
    private final ArrayList<E> values;
    private final Comparator<? super E> comparator;

    public TreeSet()
    {
        this((Comparator<? super E>)null);
    }

    public TreeSet(Comparator<? super E> comparator)
    {
        this.values = new ArrayList<E>();
        this.comparator = comparator;
    }

    public TreeSet(Collection<? extends E> source)
    {
        this((Comparator<? super E>)null);
        Iterator<? extends E> it = source.iterator();
        while (it.hasNext())
        {
            add(it.next());
        }
    }

    private int compareValues(E first, E second)
    {
        if (comparator != null)
        {
            return comparator.compare(first, second);
        }
        return ((Comparable)first).compareTo(second);
    }

    private int find(E value)
    {
        int low = 0;
        int high = values.size() - 1;
        while (low <= high)
        {
            int mid = (low + high) >>> 1;
            int cmp = compareValues(values.get(mid), value);
            if (cmp < 0)
            {
                low = mid + 1;
            }
            else if (cmp > 0)
            {
                high = mid - 1;
            }
            else
            {
                return mid;
            }
        }
        return -(low + 1);
    }

    public int size()
    {
        return values.size();
    }

    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    public boolean add(E value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        int index = find(value);
        if (index >= 0)
        {
            return false;
        }
        values.add(-(index + 1), value);
        return true;
    }

    public boolean remove(Object value)
    {
        int index = find((E)value);
        if (index < 0)
        {
            return false;
        }
        values.remove(index);
        return true;
    }

    public boolean contains(Object value)
    {
        return find((E)value) >= 0;
    }

    public void clear()
    {
        values.clear();
    }

    public Iterator<E> iterator()
    {
        return values.iterator();
    }

    public Object[] toArray()
    {
        return values.toArray();
    }

    public Comparator<? super E> comparator()
    {
        return comparator;
    }

    public E first()
    {
        if (values.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return values.get(0);
    }

    public E last()
    {
        if (values.isEmpty())
        {
            throw new NoSuchElementException();
        }
        return values.get(values.size() - 1);
    }

    public E lower(E value)
    {
        for (int i = values.size() - 1; i >= 0; i--)
        {
            if (compareValues(values.get(i), value) < 0)
            {
                return values.get(i);
            }
        }
        return null;
    }

    public E floor(E value)
    {
        for (int i = values.size() - 1; i >= 0; i--)
        {
            if (compareValues(values.get(i), value) <= 0)
            {
                return values.get(i);
            }
        }
        return null;
    }

    public E ceiling(E value)
    {
        for (int i = 0; i < values.size(); i++)
        {
            if (compareValues(values.get(i), value) >= 0)
            {
                return values.get(i);
            }
        }
        return null;
    }

    public E higher(E value)
    {
        for (int i = 0; i < values.size(); i++)
        {
            if (compareValues(values.get(i), value) > 0)
            {
                return values.get(i);
            }
        }
        return null;
    }

    public E pollFirst()
    {
        return values.isEmpty() ? null : values.remove(0);
    }

    public E pollLast()
    {
        return values.isEmpty() ? null : values.remove(values.size() - 1);
    }

    public NavigableSet<E> descendingSet()
    {
        TreeSet<E> result = new TreeSet<E>(comparator);
        for (int i = values.size() - 1; i >= 0; i--)
        {
            result.values.add(values.get(i));
        }
        return result;
    }

    public Iterator<E> descendingIterator()
    {
        ArrayList<E> copy = new ArrayList<E>(values.size() > 0 ? values.size() : 1);
        for (int i = values.size() - 1; i >= 0; i--)
        {
            copy.add(values.get(i));
        }
        return copy.iterator();
    }

    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                  E toElement, boolean toInclusive)
    {
        TreeSet<E> result = new TreeSet<E>(comparator);
        for (int i = 0; i < values.size(); i++)
        {
            E value = values.get(i);
            int low = compareValues(value, fromElement);
            int high = compareValues(value, toElement);
            boolean afterStart = fromInclusive ? low >= 0 : low > 0;
            boolean beforeEnd = toInclusive ? high <= 0 : high < 0;
            if (afterStart && beforeEnd)
            {
                result.values.add(value);
            }
        }
        return result;
    }

    public NavigableSet<E> headSet(E toElement, boolean inclusive)
    {
        return subSet(firstOr(toElement), true, toElement, inclusive);
    }

    public NavigableSet<E> tailSet(E fromElement, boolean inclusive)
    {
        return subSet(fromElement, inclusive, lastOr(fromElement), true);
    }

    private E firstOr(E fallback)
    {
        return values.isEmpty() ? fallback : values.get(0);
    }

    private E lastOr(E fallback)
    {
        return values.isEmpty() ? fallback : values.get(values.size() - 1);
    }

    public SortedSet<E> subSet(E fromElement, E toElement)
    {
        return subSet(fromElement, true, toElement, false);
    }

    public SortedSet<E> headSet(E toElement)
    {
        return headSet(toElement, false);
    }

    public SortedSet<E> tailSet(E fromElement)
    {
        return tailSet(fromElement, true);
    }

    public Object clone()
    {
        TreeSet<E> result = new TreeSet<E>(comparator);
        for (int i = 0; i < values.size(); i++)
        {
            result.values.add(values.get(i));
        }
        return result;
    }
}
