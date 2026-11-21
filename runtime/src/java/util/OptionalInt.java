package java.util;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public final class OptionalInt
{
    private static final OptionalInt EMPTY = new OptionalInt();

    private final boolean present;
    private final int value;

    private OptionalInt()
    {
        this.present = false;
        this.value = 0;
    }

    private OptionalInt(int value)
    {
        this.present = true;
        this.value = value;
    }

    public static OptionalInt empty()
    {
        return EMPTY;
    }

    public static OptionalInt of(int value)
    {
        return new OptionalInt(value);
    }

    public int getAsInt()
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

    public void ifPresent(IntConsumer action)
    {
        if (present)
        {
            action.accept(value);
        }
    }

    public OptionalInt map(IntUnaryOperator mapper)
    {
        if (!present)
        {
            return this;
        }
        return of(mapper.applyAsInt(value));
    }

    public OptionalInt filter(java.util.function.IntPredicate predicate)
    {
        if (!present || predicate.test(value))
        {
            return this;
        }
        return empty();
    }

    public int orElse(int other)
    {
        return present ? value : other;
    }

    public int orElseGet(IntSupplier supplier)
    {
        return present ? value : supplier.getAsInt();
    }

    public int orElseThrow()
    {
        if (!present)
        {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X
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
        if (!(other instanceof OptionalInt))
        {
            return false;
        }
        OptionalInt that = (OptionalInt)other;
        return present == that.present && (!present || value == that.value);
    }

    public int hashCode()
    {
        return present ? value : 0;
    }

    public String toString()
    {
        return present ? "OptionalInt[" + value + "]" : "OptionalInt.empty";
    }
}
