package java.util;

// Insertion-ordered HashSet.
public class LinkedHashSet<E> extends HashSet<E>
{
    public LinkedHashSet()
    {
        super();
    }

    public LinkedHashSet(int capacity)
    {
        super(capacity);
    }

    public LinkedHashSet(Collection<? extends E> values)
    {
        super(values);
    }

    protected int[] visitOrder()
    {
        return insertionOrder();
    }
}
