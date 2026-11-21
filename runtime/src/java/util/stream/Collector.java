package java.util.stream;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Collector<T, A, R>
{
    enum Characteristics
    {
        CONCURRENT, UNORDERED, IDENTITY_FINISH
    }

    Supplier<A> supplier();

    BiConsumer<A, T> accumulator();

    BinaryOperator<A> combiner();

    Function<A, R> finisher();

    Set<Characteristics> characteristics();

    static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier,
        BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
        Function<A, R> finisher, Characteristics... characteristics)
    {
        Set<Characteristics> flags = new HashSet<Characteristics>();
        for (int i = 0; i < characteristics.length; i++)
        {
            flags.add(characteristics[i]);
        }
        return new CollectorImpl<T, A, R>(supplier, accumulator, combiner,
            finisher, flags);
    }

    static <T, A> Collector<T, A, A> of(Supplier<A> supplier,
        BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
        Characteristics... characteristics)
    {
        return of(supplier, accumulator, combiner, Function.<A>identity(),
            characteristics);
    }
}
