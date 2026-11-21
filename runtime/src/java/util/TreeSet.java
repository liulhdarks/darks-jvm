package java.util;

public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>
{
    private final TreeMap<E, Object> map;

    private static final Object PRESENT = new Object();

    public TreeSet()
    {
        map = new TreeMap<E, Object>();
    }

    public TreeSet(Comparator<? super E> comparator)
    {
        map = new TreeMap<E, Object>(comparator);
    }

    public TreeSet(Collection<? extends E> values)
    {
        this();
        addAll(values);
    }

    public Iterator<E> iterator()
    {
        return map.keySet().iterator();
    }

    public int size()
    {
        return map.size();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean contains(Object value)
    {
        return map.containsKey(value);
    }

    public boolean add(E value)
    {
        return map.put(value, PRESENT) == null;
    }

    public boolean remove(Object value)
    {
        return map.remove(value) != null;
    }

    public void clear()
    {
        map.clear();
    }

    public E first()
    {
        return map.firstKey();
    }

    public E last()
    {
        return map.lastKey();
    }

    public Comparator<? super E> comparator()
    {
        return map.comparator();
    }

    public String toString()
    {
        return map.keySet().toString();
    }
}
