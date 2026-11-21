package java.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Optional<T>
{
    private static final Optional<?> EMPTY = new Optional<Object>();

    private final T value;

    private Optional()
    {
        this.value = null;
    }

    private Optional(T value)
    {
        this.value = Objects.requireNonNull(value);
    }

    public static <T> Optional<T> empty()
    {
        return (Optional<T>)EMPTY;
    }

    public static <T> Optional<T> of(T value)
    {
        return new Optional<T>(value);
    }

    public static <T> Optional<T> ofNullable(T value)
    {
        return value == null ? (Optional<T>)EMPTY : new Optional<T>(value);
    }

    public T get()
    {
        if (value == null)
        {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent()
    {
        return value != null;
    }

    public boolean isEmpty()
    {
        return value == null;
    }

    public void ifPresent(Consumer<? super T> action)
    {
        if (value != null)
        {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction)
    {
        if (value != null)
        {
            action.accept(value);
        }
        else
        {
            emptyAction.run();
        }
    }

    public Optional<T> filter(Predicate<? super T> predicate)
    {
        Objects.requireNonNull(predicate);
        if (value == null || predicate.test(value))
        {
            return this;
        }
        return empty();
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper)
    {
        Objects.requireNonNull(mapper);
        if (value == null)
        {
            return empty();
        }
        return Optional.ofNullable(mapper.apply(value));
    }

    public <U> Optional<U> flatMap(Function<? super T, ? extends Optional<? extends U>> mapper)
    {
        Objects.requireNonNull(mapper);
        if (value == null)
        {
            return empty();
        }
        Optional<U> result = (Optional<U>)mapper.apply(value);
        return Objects.requireNonNull(result);
    }

    public T orElse(T other)
    {
        return value != null ? value : other;
    }

    public T orElseGet(Supplier<? extends T> supplier)
    {
        return value != null ? value : supplier.get();
    }

    public T orElseThrow()
    {
        if (value == null)
        {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X
    {
        if (value != null)
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
        if (!(other instanceof Optional))
        {
            return false;
        }
        Optional<?> that = (Optional<?>)other;
        return Objects.equals(value, that.value);
    }

    public int hashCode()
    {
        return Objects.hashCode(value);
    }

    public String toString()
    {
        return value != null ? "Optional[" + value + "]" : "Optional.empty";
    }
}
