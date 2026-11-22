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

    default Comparator<T> thenComparingInt(
        java.util.function.ToIntFunction<? super T> keyExtractor)
    {
        final Comparator<T> self = this;
        final java.util.function.ToIntFunction<? super T> key = keyExtractor;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                int result = self.compare(a, b);
                if (result != 0)
                {
                    return result;
                }
                int first = key.applyAsInt(a);
                int second = key.applyAsInt(b);
                return first < second ? -1 : (first == second ? 0 : 1);
            }
        };
    }

    default Comparator<T> thenComparingLong(
        java.util.function.ToLongFunction<? super T> keyExtractor)
    {
        final Comparator<T> self = this;
        final java.util.function.ToLongFunction<? super T> key = keyExtractor;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                int result = self.compare(a, b);
                if (result != 0)
                {
                    return result;
                }
                long first = key.applyAsLong(a);
                long second = key.applyAsLong(b);
                return first < second ? -1 : (first == second ? 0 : 1);
            }
        };
    }

    default Comparator<T> thenComparingDouble(
        java.util.function.ToDoubleFunction<? super T> keyExtractor)
    {
        final Comparator<T> self = this;
        final java.util.function.ToDoubleFunction<? super T> key = keyExtractor;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                int result = self.compare(a, b);
                if (result != 0)
                {
                    return result;
                }
                double first = key.applyAsDouble(a);
                double second = key.applyAsDouble(b);
                return first < second ? -1 : (first == second ? 0 : 1);
            }
        };
    }

    static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator)
    {
        final Comparator<? super T> inner = comparator;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                if (a == null)
                {
                    return b == null ? 0 : -1;
                }
                if (b == null)
                {
                    return 1;
                }
                return inner.compare(a, b);
            }
        };
    }

    static <T> Comparator<T> nullsLast(Comparator<? super T> comparator)
    {
        final Comparator<? super T> inner = comparator;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                if (a == null)
                {
                    return b == null ? 0 : 1;
                }
                if (b == null)
                {
                    return -1;
                }
                return inner.compare(a, b);
            }
        };
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

    static <T> Comparator<T> comparingLong(java.util.function.ToLongFunction<? super T> keyExtractor)
    {
        final java.util.function.ToLongFunction<? super T> key = keyExtractor;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                long first = key.applyAsLong(a);
                long second = key.applyAsLong(b);
                return first < second ? -1 : (first == second ? 0 : 1);
            }
        };
    }

    static <T> Comparator<T> comparingInt(java.util.function.ToIntFunction<? super T> keyExtractor)
    {
        final java.util.function.ToIntFunction<? super T> key = keyExtractor;
        return new Comparator<T>()
        {
            public int compare(T a, T b)
            {
                int first = key.applyAsInt(a);
                int second = key.applyAsInt(b);
                return first < second ? -1 : (first == second ? 0 : 1);
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
