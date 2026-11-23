package java.util;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E>
{
    protected transient int modCount;

    protected AbstractList()
    {
    }

    public abstract E get(int index);

    public E set(int index, E value)
    {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E value)
    {
        throw new UnsupportedOperationException();
    }

    public E remove(int index)
    {
        throw new UnsupportedOperationException();
    }

    public boolean add(E value)
    {
        add(size(), value);
        return true;
    }

    public int indexOf(Object value)
    {
        ListIterator<E> it = listIterator();
        while (it.hasNext())
        {
            E element = it.next();
            if (value == null ? element == null : value.equals(element))
            {
                return it.previousIndex();
            }
        }
        return -1;
    }

    public int lastIndexOf(Object value)
    {
        ListIterator<E> it = listIterator(size());
        while (it.hasPrevious())
        {
            E element = it.previous();
            if (value == null ? element == null : value.equals(element))
            {
                return it.nextIndex();
            }
        }
        return -1;
    }

    public void clear()
    {
        removeRange(0, size());
    }

    public boolean addAll(int index, Collection<? extends E> values)
    {
        if (index < 0 || index > size())
        {
            throw new IndexOutOfBoundsException();
        }
        boolean changed = false;
        Iterator<? extends E> it = values.iterator();
        while (it.hasNext())
        {
            add(index++, it.next());
            changed = true;
        }
        return changed;
    }

    protected void removeRange(int fromIndex, int toIndex)
    {
        ListIterator<E> it = listIterator(fromIndex);
        for (int i = 0, n = toIndex - fromIndex; i < n; i++)
        {
            it.next();
            it.remove();
        }
    }

    public Iterator<E> iterator()
    {
        return listIterator();
    }

    public ListIterator<E> listIterator()
    {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(int index)
    {
        if (index < 0 || index > size())
        {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return new ListItr(index);
    }

    private class Itr implements Iterator<E>
    {
        int cursor;
        int lastRet = -1;
        int expectedModCount = modCount;

        Itr()
        {
        }

        public boolean hasNext()
        {
            return cursor != size();
        }

        public E next()
        {
            checkForComodification();
            int index = cursor;
            if (index >= size())
            {
                throw new NoSuchElementException();
            }
            cursor = index + 1;
            lastRet = index;
            return get(index);
        }

        public void remove()
        {
            if (lastRet < 0)
            {
                throw new IllegalStateException();
            }
            checkForComodification();
            AbstractList.this.remove(lastRet);
            if (lastRet < cursor)
            {
                cursor--;
            }
            lastRet = -1;
            expectedModCount = modCount;
        }

        final void checkForComodification()
        {
            if (modCount != expectedModCount)
            {
                throw new ConcurrentModificationException();
            }
        }
    }

    private class ListItr extends Itr implements ListIterator<E>
    {
        ListItr(int index)
        {
            cursor = index;
        }

        public boolean hasPrevious()
        {
            return cursor != 0;
        }

        public E previous()
        {
            checkForComodification();
            int index = cursor - 1;
            if (index < 0)
            {
                throw new NoSuchElementException();
            }
            cursor = index;
            lastRet = index;
            return get(index);
        }

        public int nextIndex()
        {
            return cursor;
        }

        public int previousIndex()
        {
            return cursor - 1;
        }

        public void set(E value)
        {
            if (lastRet < 0)
            {
                throw new IllegalStateException();
            }
            checkForComodification();
            AbstractList.this.set(lastRet, value);
            expectedModCount = modCount;
        }

        public void add(E value)
        {
            checkForComodification();
            try
            {
                int index = cursor;
                AbstractList.this.add(index, value);
                cursor = index + 1;
                lastRet = -1;
                expectedModCount = modCount;
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new ConcurrentModificationException();
            }
        }
    }

    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof List))
        {
            return false;
        }
        List<?> other_list = (List<?>)other;
        if (size() != other_list.size())
        {
            return false;
        }
        Iterator<E> mine = iterator();
        Iterator<?> theirs = other_list.iterator();
        while (mine.hasNext() && theirs.hasNext())
        {
            E a = mine.next();
            Object b = theirs.next();
            if (!(a == null ? b == null : a.equals(b)))
            {
                return false;
            }
        }
        return !(mine.hasNext() || theirs.hasNext());
    }

    public int hashCode()
    {
        int hash = 1;
        Iterator<E> it = iterator();
        while (it.hasNext())
        {
            E element = it.next();
            hash = 31 * hash + (element == null ? 0 : element.hashCode());
        }
        return hash;
    }
}
