package java.util;

import java.util.function.Function;

public interface Comparator<T>
{
    int compare(T a, T b);

    default Comparator<T> reversed()
    {
        final Comparator<T> self = this;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                return self.compare(b, a);
            }
        };
    }

    default Comparator<T> thenComparing(Comparator<? super T> other)
    {
        final Comparator<T> self = this;
        final Comparator<? super T> second = other;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                int result = self.compare(a, b);
                return result != 0 ? result : second.compare(a, b);
            }
        };
    }

    default <U extends Comparable<? super U>> Comparator<T> thenComparing(
        Function<? super T, ? extends U> keyExtractor)
    {
        return thenComparing(Comparator.<T, U>comparing(keyExtractor));
    }

    static <T, U extends Comparable<? super U>> Comparator<T> comparing(
        Function<? super T, ? extends U> keyExtractor)
    {
        final Function<? super T, ? extends U> key = keyExtractor;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                U first = key.apply(a);
                U second = key.apply(b);
                if (first == null)
                {
                    throw new NullPointerException("null key");
                }
                return ((Comparable<U>)first).compareTo(second);
            }
        };
    }

    static <T, U> Comparator<T> comparing(
        Function<? super T, ? extends U> keyExtractor,
        Comparator<? super U> keyComparator)
    {
        final Function<? super T, ? extends U> key = keyExtractor;
        final Comparator<? super U> comparator = keyComparator;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                return comparator.compare(key.apply(a), key.apply(b));
            }
        };
    }

    static <T extends Comparable<? super T>> Comparator<T> naturalOrder()
    {
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                return a.compareTo(b);
            }
        };
    }

    static <T extends Comparable<? super T>> Comparator<T> reverseOrder()
    {
        return Comparator.<T>naturalOrder().reversed();
    }
}
