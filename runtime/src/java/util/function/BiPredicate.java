package java.util.function;

public interface BiPredicate<T, U>
{
    boolean test(T first, U second);

    default BiPredicate<T, U> and(final BiPredicate<? super T, ? super U> other)
    {
        return new BiPredicate<T, U>()
        {
            public boolean test(T first, U second)
            {
                return BiPredicate.this.test(first, second) && other.test(first, second);
            }
        };
    }

    default BiPredicate<T, U> or(final BiPredicate<? super T, ? super U> other)
    {
        return new BiPredicate<T, U>()
        {
            public boolean test(T first, U second)
            {
                return BiPredicate.this.test(first, second) || other.test(first, second);
            }
        };
    }

    default BiPredicate<T, U> negate()
    {
        return new BiPredicate<T, U>()
        {
            public boolean test(T first, U second)
            {
                return !BiPredicate.this.test(first, second);
            }
        };
    }
}
