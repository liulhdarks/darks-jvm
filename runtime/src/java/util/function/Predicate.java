package java.util.function;

public interface Predicate<T>
{
    boolean test(T value);

    default Predicate<T> and(final Predicate<? super T> other)
    {
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return Predicate.this.test(value) && other.test(value);
            }
        };
    }

    default Predicate<T> or(final Predicate<? super T> other)
    {
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return Predicate.this.test(value) || other.test(value);
            }
        };
    }

    default Predicate<T> negate()
    {
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return !Predicate.this.test(value);
            }
        };
    }

    static <T> Predicate<T> isEqual(final Object targetRef)
    {
        if (targetRef == null)
        {
            return new Predicate<T>()
            {
                public boolean test(T value)
                {
                    return value == null;
                }
            };
        }
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return targetRef.equals(value);
            }
        };
    }

    static <T> Predicate<T> not(final Predicate<? super T> target)
    {
        if (target == null)
        {
            throw new NullPointerException();
        }
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return !target.test(value);
            }
        };
    }
}
