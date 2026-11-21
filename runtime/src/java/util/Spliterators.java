package java.util;

import java.util.function.Consumer;

public final class Spliterators
{
    private Spliterators()
    {
    }

    public static <T> Spliterator<T> spliteratorUnknownSize(
        Iterator<? extends T> iterator, int characteristics)
    {
        return new IteratorSpliterator<T>(iterator, characteristics);
    }

    public static <T> Spliterator<T> spliterator(
        Collection<? extends T> collection, int characteristics)
    {
        return spliteratorUnknownSize(collection.iterator(),
            characteristics | Spliterator.SIZED | Spliterator.SUBSIZED);
    }

    private static final class IteratorSpliterator<T> implements Spliterator<T>
    {
        private final Iterator<? extends T> iterator;
        private final int characteristics;

        IteratorSpliterator(Iterator<? extends T> iterator, int characteristics)
        {
            this.iterator = iterator;
            this.characteristics = characteristics;
        }

        public boolean tryAdvance(Consumer<? super T> action)
        {
            if (action == null)
            {
                throw new NullPointerException();
            }
            if (!iterator.hasNext())
            {
                return false;
            }
            action.accept(iterator.next());
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
            return characteristics;
        }
    }
}
