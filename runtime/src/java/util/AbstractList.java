package java.util;

public abstract class AbstractList<E> implements List<E>
{
    protected transient int modCount;

    protected AbstractList()
    {
    }
}
