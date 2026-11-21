package java.lang;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public interface Iterable<T>
{
    Iterator<T> iterator();

    default Spliterator<T> spliterator()
    {
        final Iterator<T> it = iterator();
        return new Spliterator<T>()
        {
            public boolean tryAdvance(Consumer<? super T> action)
            {
                if (!it.hasNext())
                {
                    return false;
                }
                action.accept(it.next());
                return true;
            }

            public Spliterator<T> trySplit()
            {
                return null;
            }

            public long estimateSize()
            {
                return Long.MAX_VALUE;
            }

            public int characteristics()
            {
                return 0;
            }
        };
    }

    default void forEach(Consumer<? super T> action)
    {
        Iterator<T> it = iterator();
        while (it.hasNext())
        {
            action.accept(it.next());
        }
    }
}
