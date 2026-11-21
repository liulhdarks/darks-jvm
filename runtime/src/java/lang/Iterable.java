package java.lang;

import java.util.Iterator;

public interface Iterable<T>
{
    Iterator<T> iterator();

    default java.util.Spliterator<T> spliterator()
    {
        return java.util.Spliterators.spliteratorUnknownSize(iterator(), 0);
    }

    default void forEach(java.util.function.Consumer<? super T> action)
    {
        if (action == null)
        {
            throw new NullPointerException("action");
        }
        Iterator<T> it = iterator();
        while (it.hasNext())
        {
            action.accept(it.next());
        }
    }
}
