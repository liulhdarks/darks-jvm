package java.util;

import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

public final class OptionalLong
{
    private static final OptionalLong EMPTY = new OptionalLong();

    private final boolean present;
    private final long value;

    private OptionalLong()
    {
        this.present = false;
        this.value = 0;
    }

    private OptionalLong(long value)
    {
        this.present = true;
        this.value = value;
    }

    public static OptionalLong empty()
    {
        return EMPTY;
    }

    public static OptionalLong of(long value)
    {
        return new OptionalLong(value);
    }

    public long getAsLong()
    {
        if (!present)
        {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent()
    {
        return present;
    }

    public boolean isEmpty()
    {
        return !present;
    }

    public void ifPresent(LongConsumer action)
    {
        if (present)
        {
            action.accept(value);
        }
    }

    public OptionalLong map(LongUnaryOperator mapper)
    {
        if (!present)
        {
            return this;
        }
        return of(mapper.applyAsLong(value));
    }

    public OptionalLong filter(LongPredicate predicate)
    {
        if (!present || predicate.test(value))
        {
            return this;
        }
        return empty();
    }

    public long orElse(long other)
    {
        return present ? value : other;
    }

    public long orElseGet(LongSupplier supplier)
    {
        return present ? value : supplier.getAsLong();
    }

    public long orElseThrow()
    {
        if (!present)
        {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public <X extends Throwable> long orElseThrow(
        Supplier<? extends X> exceptionSupplier) throws X
    {
        if (present)
        {
            return value;
        }
        throw exceptionSupplier.get();
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof OptionalLong))
        {
            return false;
        }
        OptionalLong that = (OptionalLong)other;
        return present == that.present && (!present || value == that.value);
    }

    public int hashCode()
    {
        return present ? (int)(value ^ (value >>> 32)) : 0;
    }

    public String toString()
    {
        return present ? "OptionalLong[" + value + "]" : "OptionalLong.empty";
    }
}
