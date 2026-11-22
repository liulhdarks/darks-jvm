package java.util;

import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

public final class OptionalDouble
{
    private static final OptionalDouble EMPTY = new OptionalDouble();

    private final boolean present;
    private final double value;

    private OptionalDouble()
    {
        this.present = false;
        this.value = 0;
    }

    private OptionalDouble(double value)
    {
        this.present = true;
        this.value = value;
    }

    public static OptionalDouble empty()
    {
        return EMPTY;
    }

    public static OptionalDouble of(double value)
    {
        return new OptionalDouble(value);
    }

    public double getAsDouble()
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

    public void ifPresent(DoubleConsumer action)
    {
        if (present)
        {
            action.accept(value);
        }
    }

    public OptionalDouble map(DoubleUnaryOperator mapper)
    {
        if (!present)
        {
            return this;
        }
        return of(mapper.applyAsDouble(value));
    }

    public OptionalDouble filter(DoublePredicate predicate)
    {
        if (!present || predicate.test(value))
        {
            return this;
        }
        return empty();
    }

    public double orElse(double other)
    {
        return present ? value : other;
    }

    public double orElseGet(DoubleSupplier supplier)
    {
        return present ? value : supplier.getAsDouble();
    }

    public double orElseThrow()
    {
        if (!present)
        {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public <X extends Throwable> double orElseThrow(
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
        if (!(other instanceof OptionalDouble))
        {
            return false;
        }
        OptionalDouble that = (OptionalDouble)other;
        return present == that.present &&
            (!present || Double.doubleToLongBits(value) ==
                Double.doubleToLongBits(that.value));
    }

    public int hashCode()
    {
        if (!present)
        {
            return 0;
        }
        long bits = Double.doubleToLongBits(value);
        return (int)(bits ^ (bits >>> 32));
    }

    public String toString()
    {
        return present ? "OptionalDouble[" + value + "]" : "OptionalDouble.empty";
    }
}
