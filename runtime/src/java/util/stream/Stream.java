package java.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

// Eager, single-shot stream over an in-memory list.  Each intermediate
// operation materialises its result, which keeps the implementation small and
// predictable; laziness and parallel execution are intentionally absent.
public interface Stream<T> extends BaseStream<T, Stream<T>>
{
    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    Stream<T> sorted();

    Stream<T> sorted(Comparator<? super T> comparator);

    Stream<T> distinct();

    Stream<T> limit(long maxSize);

    Stream<T> skip(long count);

    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    IntStream mapToInt(ToIntFunction<? super T> mapper);

    java.util.stream.DoubleStream mapToDouble(java.util.function.ToDoubleFunction<? super T> mapper);

    java.util.stream.LongStream mapToLong(java.util.function.ToLongFunction<? super T> mapper);

    IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    java.util.stream.LongStream flatMapToLong(
        Function<? super T, ? extends java.util.stream.LongStream> mapper);

    java.util.stream.DoubleStream flatMapToDouble(
        Function<? super T, ? extends java.util.stream.DoubleStream> mapper);

    Stream<T> peek(Consumer<? super T> action);

    Stream<T> takeWhile(Predicate<? super T> predicate);

    Stream<T> dropWhile(Predicate<? super T> predicate);

    void forEachOrdered(Consumer<? super T> action);

    Optional<T> findAny();

    Optional<T> min(Comparator<? super T> comparator);

    Optional<T> max(Comparator<? super T> comparator);

    T reduce(T identity, BinaryOperator<T> accumulator);

    <U> U reduce(U identity, java.util.function.BiFunction<U, ? super T, U> accumulator,
        BinaryOperator<U> combiner);

    <R> R collect(java.util.function.Supplier<R> supplier,
        java.util.function.BiConsumer<R, ? super T> accumulator,
        java.util.function.BiConsumer<R, R> combiner);

    Optional<T> findFirst();

    Optional<T> reduce(BinaryOperator<T> accumulator);

    Iterator<T> iterator();

    long count();

    void forEach(Consumer<? super T> action);

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    <R, A> R collect(Collector<? super T, A, R> collector);

    List<T> toList();

    Object[] toArray();

    <A> A[] toArray(IntFunction<A[]> generator);

    static <T> Stream<T> of(T... values)
    {
        ArrayList<T> list = new ArrayList<T>(values.length > 0 ? values.length : 1);
        for (int i = 0; i < values.length; i++)
        {
            list.add(values[i]);
        }
        return new ArrayListStream<T>(list);
    }

    static <T> Stream<T> of(T value)
    {
        ArrayList<T> list = new ArrayList<T>(1);
        list.add(value);
        return new ArrayListStream<T>(list);
    }

    static <T> Stream<T> ofNullable(T value)
    {
        if (value == null)
        {
            return empty();
        }
        return of(value);
    }

    static <T> Stream<T> empty()
    {
        return new ArrayListStream<T>(new ArrayList<T>());
    }

    static <T> Stream<T> concat(Stream<? extends T> first, Stream<? extends T> second)
    {
        ArrayList<T> list = new ArrayList<T>();
        Iterator<? extends T> it = first.iterator();
        while (it.hasNext())
        {
            list.add(it.next());
        }
        it = second.iterator();
        while (it.hasNext())
        {
            list.add(it.next());
        }
        return new ArrayListStream<T>(list);
    }

    static <T> Stream<T> from(Collection<? extends T> values)
    {
        ArrayList<T> list = new ArrayList<T>(values.size() > 0 ? values.size() : 1);
        Iterator<? extends T> it = values.iterator();
        while (it.hasNext())
        {
            list.add(it.next());
        }
        return new ArrayListStream<T>(list);
    }
}

final class ArrayListStream<T> implements Stream<T>
{
    private final ArrayList<T> elements;

    ArrayListStream(ArrayList<T> elements)
    {
        this.elements = elements;
    }

    private ArrayList<T> copy()
    {
        ArrayList<T> result = new ArrayList<T>(elements.size() > 0 ? elements.size() : 1);
        for (int i = 0; i < elements.size(); i++)
        {
            result.add(elements.get(i));
        }
        return result;
    }

    public Stream<T> filter(Predicate<? super T> predicate)
    {
        ArrayList<T> result = new ArrayList<T>();
        for (int i = 0; i < elements.size(); i++)
        {
            T value = elements.get(i);
            if (predicate.test(value))
            {
                result.add(value);
            }
        }
        return new ArrayListStream<T>(result);
    }

    public <R> Stream<R> map(Function<? super T, ? extends R> mapper)
    {
        ArrayList<R> result = new ArrayList<R>(elements.size() > 0 ? elements.size() : 1);
        for (int i = 0; i < elements.size(); i++)
        {
            result.add(mapper.apply(elements.get(i)));
        }
        return new ArrayListStream<R>(result);
    }

    public Stream<T> sorted()
    {
        ArrayList<T> result = copy();
        Collections.sort((List)result);
        return new ArrayListStream<T>(result);
    }

    public Stream<T> sorted(Comparator<? super T> comparator)
    {
        ArrayList<T> result = copy();
        Collections.sort(result, comparator);
        return new ArrayListStream<T>(result);
    }

    public Stream<T> distinct()
    {
        ArrayList<T> result = new ArrayList<T>(elements.size() > 0 ? elements.size() : 1);
        HashSet<T> seen = new HashSet<T>();
        for (int i = 0; i < elements.size(); i++)
        {
            T value = elements.get(i);
            if (seen.add(value))
            {
                result.add(value);
            }
        }
        return new ArrayListStream<T>(result);
    }

    public Stream<T> limit(long maxSize)
    {
        if (maxSize < 0)
        {
            throw new IllegalArgumentException("negative limit: " + maxSize);
        }
        ArrayList<T> result = new ArrayList<T>();
        int limit = maxSize > elements.size() ? elements.size() : (int)maxSize;
        for (int i = 0; i < limit; i++)
        {
            result.add(elements.get(i));
        }
        return new ArrayListStream<T>(result);
    }

    public Stream<T> skip(long count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("negative skip: " + count);
        }
        ArrayList<T> result = new ArrayList<T>();
        int start = count > elements.size() ? elements.size() : (int)count;
        for (int i = start; i < elements.size(); i++)
        {
            result.add(elements.get(i));
        }
        return new ArrayListStream<T>(result);
    }

    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper)
    {
        ArrayList<R> result = new ArrayList<R>();
        for (int i = 0; i < elements.size(); i++)
        {
            Stream<? extends R> inner = mapper.apply(elements.get(i));
            if (inner == null)
            {
                continue;
            }
            java.util.Iterator<? extends R> it = inner.iterator();
            while (it.hasNext())
            {
                result.add(it.next());
            }
        }
        return new ArrayListStream<R>(result);
    }

    public IntStream mapToInt(ToIntFunction<? super T> mapper)
    {
        int[] result = new int[elements.size()];
        for (int i = 0; i < elements.size(); i++)
        {
            result[i] = mapper.applyAsInt(elements.get(i));
        }
        return IntStream.of(result);
    }

    public DoubleStream mapToDouble(java.util.function.ToDoubleFunction<? super T> mapper)
    {
        double[] result = new double[elements.size()];
        for (int i = 0; i < elements.size(); i++)
        {
            result[i] = mapper.applyAsDouble(elements.get(i));
        }
        return DoubleStream.of(result);
    }

    public LongStream mapToLong(java.util.function.ToLongFunction<? super T> mapper)
    {
        long[] result = new long[elements.size()];
        for (int i = 0; i < elements.size(); i++)
        {
            result[i] = mapper.applyAsLong(elements.get(i));
        }
        return LongStream.of(result);
    }

    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper)
    {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < elements.size(); i++)
        {
            IntStream inner = mapper.apply(elements.get(i));
            if (inner != null)
            {
                int[] values = inner.toArray();
                for (int k = 0; k < values.length; k++)
                {
                    result.add(values[k]);
                }
            }
        }
        int[] values = new int[result.size()];
        for (int i = 0; i < result.size(); i++)
        {
            values[i] = result.get(i);
        }
        return IntStream.of(values);
    }

    public LongStream flatMapToLong(
        Function<? super T, ? extends LongStream> mapper)
    {
        ArrayList<Long> result = new ArrayList<Long>();
        for (int i = 0; i < elements.size(); i++)
        {
            LongStream inner = mapper.apply(elements.get(i));
            if (inner != null)
            {
                long[] values = inner.toArray();
                for (int k = 0; k < values.length; k++)
                {
                    result.add(values[k]);
                }
            }
        }
        long[] values = new long[result.size()];
        for (int i = 0; i < result.size(); i++)
        {
            values[i] = result.get(i);
        }
        return LongStream.of(values);
    }

    public DoubleStream flatMapToDouble(
        Function<? super T, ? extends DoubleStream> mapper)
    {
        ArrayList<Double> result = new ArrayList<Double>();
        for (int i = 0; i < elements.size(); i++)
        {
            DoubleStream inner = mapper.apply(elements.get(i));
            if (inner != null)
            {
                double[] values = inner.toArray();
                for (int k = 0; k < values.length; k++)
                {
                    result.add(values[k]);
                }
            }
        }
        double[] values = new double[result.size()];
        for (int i = 0; i < result.size(); i++)
        {
            values[i] = result.get(i);
        }
        return DoubleStream.of(values);
    }

    public Stream<T> peek(Consumer<? super T> action)
    {
        for (int i = 0; i < elements.size(); i++)
        {
            action.accept(elements.get(i));
        }
        return new ArrayListStream<T>(copy());
    }

    public Stream<T> takeWhile(Predicate<? super T> predicate)
    {
        ArrayList<T> result = new ArrayList<T>();
        for (int i = 0; i < elements.size(); i++)
        {
            T value = elements.get(i);
            if (!predicate.test(value))
            {
                break;
            }
            result.add(value);
        }
        return new ArrayListStream<T>(result);
    }

    public Stream<T> dropWhile(Predicate<? super T> predicate)
    {
        ArrayList<T> result = new ArrayList<T>();
        boolean dropping = true;
        for (int i = 0; i < elements.size(); i++)
        {
            T value = elements.get(i);
            if (dropping && predicate.test(value))
            {
                continue;
            }
            dropping = false;
            result.add(value);
        }
        return new ArrayListStream<T>(result);
    }

    public void forEachOrdered(Consumer<? super T> action)
    {
        forEach(action);
    }

    public Optional<T> findAny()
    {
        return findFirst();
    }

    public Optional<T> min(Comparator<? super T> comparator)
    {
        if (elements.isEmpty())
        {
            return Optional.empty();
        }
        T best = elements.get(0);
        for (int i = 1; i < elements.size(); i++)
        {
            if (comparator.compare(elements.get(i), best) < 0)
            {
                best = elements.get(i);
            }
        }
        return Optional.of(best);
    }

    public Optional<T> max(Comparator<? super T> comparator)
    {
        if (elements.isEmpty())
        {
            return Optional.empty();
        }
        T best = elements.get(0);
        for (int i = 1; i < elements.size(); i++)
        {
            if (comparator.compare(elements.get(i), best) > 0)
            {
                best = elements.get(i);
            }
        }
        return Optional.of(best);
    }

    public T reduce(T identity, BinaryOperator<T> accumulator)
    {
        T result = identity;
        for (int i = 0; i < elements.size(); i++)
        {
            result = accumulator.apply(result, elements.get(i));
        }
        return result;
    }

    public <U> U reduce(U identity,
        java.util.function.BiFunction<U, ? super T, U> accumulator,
        BinaryOperator<U> combiner)
    {
        U result = identity;
        for (int i = 0; i < elements.size(); i++)
        {
            result = accumulator.apply(result, elements.get(i));
        }
        return result;
    }

    public <R> R collect(java.util.function.Supplier<R> supplier,
        java.util.function.BiConsumer<R, ? super T> accumulator,
        java.util.function.BiConsumer<R, R> combiner)
    {
        R container = supplier.get();
        for (int i = 0; i < elements.size(); i++)
        {
            accumulator.accept(container, elements.get(i));
        }
        return container;
    }

    public Optional<T> findFirst()
    {
        if (elements.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(elements.get(0));
    }

    public Optional<T> reduce(BinaryOperator<T> accumulator)
    {
        if (elements.isEmpty())
        {
            return Optional.empty();
        }
        T result = elements.get(0);
        for (int i = 1; i < elements.size(); i++)
        {
            result = accumulator.apply(result, elements.get(i));
        }
        return Optional.of(result);
    }

    public Iterator<T> iterator()
    {
        return copy().iterator();
    }

    public Spliterator<T> spliterator()
    {
        return Spliterators.spliterator(elements, Spliterator.ORDERED);
    }

    public boolean isParallel()
    {
        return false;
    }

    public Stream<T> sequential()
    {
        return this;
    }

    public Stream<T> parallel()
    {
        return this;
    }

    public Stream<T> unordered()
    {
        return this;
    }

    public Stream<T> onClose(Runnable closeHandler)
    {
        return this;
    }

    public void close()
    {
    }

    public long count()
    {
        return elements.size();
    }

    public void forEach(Consumer<? super T> action)
    {
        for (int i = 0; i < elements.size(); i++)
        {
            action.accept(elements.get(i));
        }
    }

    public boolean anyMatch(Predicate<? super T> predicate)
    {
        for (int i = 0; i < elements.size(); i++)
        {
            if (predicate.test(elements.get(i)))
            {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(Predicate<? super T> predicate)
    {
        for (int i = 0; i < elements.size(); i++)
        {
            if (!predicate.test(elements.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(Predicate<? super T> predicate)
    {
        return !anyMatch(predicate);
    }

    public <R, A> R collect(Collector<? super T, A, R> collector)
    {
        A container = collector.supplier().get();
        java.util.function.BiConsumer<A, ? super T> accumulator = collector.accumulator();
        for (int i = 0; i < elements.size(); i++)
        {
            accumulator.accept(container, elements.get(i));
        }
        return collector.finisher().apply(container);
    }

    public List<T> toList()
    {
        return copy();
    }

    public Object[] toArray()
    {
        Object[] result = new Object[elements.size()];
        for (int i = 0; i < elements.size(); i++)
        {
            result[i] = elements.get(i);
        }
        return result;
    }

    public <A> A[] toArray(IntFunction<A[]> generator)
    {
        A[] result = generator.apply(elements.size());
        for (int i = 0; i < elements.size(); i++)
        {
            result[i] = (A)elements.get(i);
        }
        return result;
    }
}
