package java.util.function;

public interface Predicate<T>
{
    boolean test(T value);

    default Predicate<T> and(final Predicate<? super T> other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return Predicate.this.test(value) && other.test(value);
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

    default Predicate<T> or(final Predicate<? super T> other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return Predicate.this.test(value) || other.test(value);
            }
        };
    }

    static <T> Predicate<T> isEqual(final Object targetRef)
    {
        return new Predicate<T>()
        {
            public boolean test(T value)
            {
                return targetRef == null ? value == null : targetRef.equals(value);
            }
        };
    }

    static <T> Predicate<T> not(final Predicate<? super T> target)
    {
        if (target == null)
        {
            throw new NullPointerException("target");
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
