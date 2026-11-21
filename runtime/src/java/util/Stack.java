package java.util;

public class Stack<E> extends ArrayList<E>
{
    public Stack()
    {
    }

    public E push(E item)
    {
        add(item);
        return item;
    }

    public synchronized E pop()
    {
        if (isEmpty())
        {
            throw new EmptyStackException();
        }
        return remove(size() - 1);
    }

    public synchronized E peek()
    {
        if (isEmpty())
        {
            throw new EmptyStackException();
        }
        return get(size() - 1);
    }

    public boolean empty()
    {
        return isEmpty();
    }

    public synchronized int search(Object value)
    {
        for (int i = size() - 1; i >= 0; i--)
        {
            E item = get(i);
            if (value == null ? item == null : value.equals(item))
            {
                return size() - i;
            }
        }
        return -1;
    }
}
