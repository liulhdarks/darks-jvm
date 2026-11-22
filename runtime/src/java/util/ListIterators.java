package java.util;

/**
 * ListIterator implementations shared by the runtime list classes.  The
 * cursor based implementation mirrors ArrayList's iterator; the read-only
 * variant is used by the empty, singleton and unmodifiable wrappers.
 */
final class ListIterators
{
    private ListIterators()
    {
    }

    static <E> ListIterator<E> create(final List<? extends E> list, int index,
                                       final boolean modifiable)
    {
        return new Cursor<E>(list, index, modifiable);
    }

    private static final class Cursor<E> implements ListIterator<E>
    {
        private final List<? extends E> list;
        private final boolean modifiable;
        private int cursor;
        private int last = -1;

        Cursor(List<? extends E> list, int index, boolean modifiable)
        {
            if (index < 0 || index > list.size())
            {
                throw new IndexOutOfBoundsException("Index: " + index);
            }
            this.list = list;
            this.cursor = index;
            this.modifiable = modifiable;
        }

        public boolean hasNext()
        {
            return cursor < list.size();
        }

        public E next()
        {
            if (cursor >= list.size())
            {
                throw new NoSuchElementException();
            }
            last = cursor;
            return list.get(cursor++);
        }

        public boolean hasPrevious()
        {
            return cursor > 0;
        }

        public E previous()
        {
            if (cursor <= 0)
            {
                throw new NoSuchElementException();
            }
            last = --cursor;
            return list.get(cursor);
        }

        public int nextIndex()
        {
            return cursor;
        }

        public int previousIndex()
        {
            return cursor - 1;
        }

        public void remove()
        {
            checkModifiable();
            if (last < 0)
            {
                throw new IllegalStateException();
            }
            list.remove(last);
            cursor = last;
            last = -1;
        }

        public void set(E value)
        {
            checkModifiable();
            if (last < 0)
            {
                throw new IllegalStateException();
            }
            ((List<E>)list).set(last, value);
        }

        public void add(E value)
        {
            checkModifiable();
            ((List<E>)list).add(cursor++, value);
            last = -1;
        }

        private void checkModifiable()
        {
            if (!modifiable)
            {
                throw new UnsupportedOperationException();
            }
        }
    }
}
