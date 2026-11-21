package java.util;

public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E>
{
    protected AbstractSet()
    {
    }

    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof Set))
        {
            return false;
        }
        Set<?> set = (Set<?>)other;
        if (set.size() != size())
        {
            return false;
        }
        return containsAll(set);
    }

    public int hashCode()
    {
        int result = 0;
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            E element = it.next();
            if (element != null)
            {
                result += element.hashCode();
            }
        }
        return result;
    }
}
