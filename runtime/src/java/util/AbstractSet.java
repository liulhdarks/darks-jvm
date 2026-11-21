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
        Set set = (Set)other;
        if (set.size() != size())
        {
            return false;
        }
        try
        {
            return containsAll(set);
        }
        catch (NullPointerException e)
        {
            return false;
        }
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

    public boolean containsAll(Collection<?> values)
    {
        Iterator<?> it = values.iterator();
        while (it.hasNext())
        {
            if (!contains(it.next()))
            {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(Collection<? extends E> values)
    {
        boolean changed = false;
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            if (add(it.next()))
            {
                changed = true;
            }
        }
        return changed;
    }
}
