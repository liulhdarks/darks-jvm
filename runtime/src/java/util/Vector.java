package java.util;

public class Vector<E> extends ArrayList<E>
{
    public Vector()
    {
    }

    public Vector(int capacity)
    {
        super(capacity);
    }

    public synchronized void addElement(E item)
    {
        add(item);
    }

    public synchronized boolean removeElement(Object item)
    {
        int index = indexOf(item);
        if (index < 0)
        {
            return false;
        }
        remove(index);
        return true;
    }

    public synchronized void removeAllElements()
    {
        clear();
    }

    public synchronized E elementAt(int index)
    {
        return get(index);
    }

    public synchronized E firstElement()
    {
        return get(0);
    }

    public synchronized E lastElement()
    {
        return get(size() - 1);
    }

    public synchronized void setElementAt(E item, int index)
    {
        set(index, item);
    }

    public synchronized void insertElementAt(E item, int index)
    {
        add(index, item);
    }

    public synchronized void removeElementAt(int index)
    {
        remove(index);
    }

    public synchronized Enumeration<E> elements()
    {
        return new ArrayEnumeration<E>(this);
    }
}
