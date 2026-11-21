package java.util.function;

public interface Function<T, R>
{
    R apply(T value);

    default <V> Function<V, R> compose(final Function<? super V, ? extends T> before)
    {
        return new Function<V, R>()
        {
            public R apply(V value)
            {
                return Function.this.apply(before.apply(value));
            }
        };
    }

    default <V> Function<T, V> andThen(final Function<? super R, ? extends V> after)
    {
        return new Function<T, V>()
        {
            public V apply(T value)
            {
                return after.apply(Function.this.apply(value));
            }
        };
    }

    static <T> Function<T, T> identity()
    {
        return new Function<T, T>()
        {
            public T apply(T value)
            {
                return value;
            }
        };
    }
}
