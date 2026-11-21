package java.util;

import java.util.function.Consumer;

public interface Spliterator<T>
{
    int ORDERED = 0x00000010;
    int DISTINCT = 0x00000001;
    int SORTED = 0x00000004;
    int SIZED = 0x00000040;
    int NONNULL = 0x00000100;
    int IMMUTABLE = 0x00000400;
    int CONCURRENT = 0x00001000;
    int SUBSIZED = 0x00004000;

    boolean tryAdvance(Consumer<? super T> action);

    default void forEachRemaining(Consumer<? super T> action)
    {
        while (tryAdvance(action))
        {
        }
    }

    Spliterator<T> trySplit();

    long estimateSize();

    default long getExactSizeIfKnown()
    {
        return (characteristics() & SIZED) == 0 ? -1L : estimateSize();
    }

    int characteristics();

    default boolean hasCharacteristics(int characteristics)
    {
        return (characteristics() & characteristics) == characteristics;
    }

    default Comparator<? super T> getComparator()
    {
        throw new IllegalStateException();
    }
}
