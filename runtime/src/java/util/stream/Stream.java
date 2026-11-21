package java.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

// Eager, single-shot stream over an in-memory list.  Each intermediate
// operation materialises its result, which keeps the implementation small and
// predictable; laziness and parallel execution are intentionally absent.
public interface Stream<T>
{
    Stream<T> filter(Predicate<? super T> predicate);

    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    Stream<T> sorted();

    Stream<T> sorted(Comparator<? super T> comparator);

    Stream<T> distinct();

    Stream<T> limit(long maxSize);

    Stream<T> skip(long count);

    long count();

    void forEach(Consumer<? super T> action);

    boolean anyMatch(Predicate<? super T> predicate);

    boolean allMatch(Predicate<? super T> predicate);

    boolean noneMatch(Predicate<? super T> predicate);

    <R, A> R collect(Collector<? super T, A, R> collector);

    List<T> toList();

    Object[] toArray();

    static <T> Stream<T> of(T... values)
    {
        ArrayList<T> list = new ArrayList<T>(values.length > 0 ? values.length : 1);
        for (int i = 0; i < values.length; i++)
        {
            list.add(values[i]);
        }
        return new ArrayListStream<T>(list);
    }

    static <T> Stream<T> empty()
    {
        return new ArrayListStream<T>(new ArrayList<T>());
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
}
