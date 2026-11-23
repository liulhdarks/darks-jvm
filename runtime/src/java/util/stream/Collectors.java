package java.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Collectors
{
    private Collectors()
    {
    }

    public static <T> Collector<T, ?, List<T>> toList()
    {
        return new CollectorImpl<T, List<T>, List<T>>(
            new Supplier<List<T>>()
            {
                public List<T> get()
                {
                    return new ArrayList<T>();
                }
            },
            new BiConsumer<List<T>, T>()
            {
                public void accept(List<T> list, T value)
                {
                    list.add(value);
                }
            },
            new BinaryOperator<List<T>>()
            {
                public List<T> apply(List<T> first, List<T> second)
                {
                    first.addAll(second);
                    return first;
                }
            },
            new Function<List<T>, List<T>>()
            {
                public List<T> apply(List<T> list)
                {
                    return list;
                }
            });
    }

    public static <T, C extends Collection<T>> Collector<T, ?, C> toCollection(
        final Supplier<C> supplier)
    {
        return new CollectorImpl<T, C, C>(
            supplier,
            new BiConsumer<C, T>()
            {
                public void accept(C collection, T value)
                {
                    collection.add(value);
                }
            },
            new BinaryOperator<C>()
            {
                public C apply(C first, C second)
                {
                    first.addAll(second);
                    return first;
                }
            },
            new Function<C, C>()
            {
                public C apply(C collection)
                {
                    return collection;
                }
            });
    }

    public static <T> Collector<T, ?, Set<T>> toSet()
    {
        return new CollectorImpl<T, Set<T>, Set<T>>(
            new Supplier<Set<T>>()
            {
                public Set<T> get()
                {
                    return new HashSet<T>();
                }
            },
            new BiConsumer<Set<T>, T>()
            {
                public void accept(Set<T> set, T value)
                {
                    set.add(value);
                }
            },
            new BinaryOperator<Set<T>>()
            {
                public Set<T> apply(Set<T> first, Set<T> second)
                {
                    Iterator<T> it = second.iterator();
                    while (it.hasNext())
                    {
                        first.add(it.next());
                    }
                    return first;
                }
            },
            new Function<Set<T>, Set<T>>()
            {
                public Set<T> apply(Set<T> set)
                {
                    return set;
                }
            });
    }

    public static Collector<CharSequence, ?, String> joining()
    {
        return joining("", "", "");
    }

    public static Collector<CharSequence, ?, String> joining(final CharSequence delimiter)
    {
        return joining(delimiter, "", "");
    }

    public static Collector<CharSequence, ?, String> joining(
        final CharSequence delimiter,
        final CharSequence prefix,
        final CharSequence suffix)
    {
        final String separator = delimiter == null ? "" : delimiter.toString();
        final String head = prefix == null ? "" : prefix.toString();
        final String tail = suffix == null ? "" : suffix.toString();
        return new CollectorImpl<CharSequence, List<CharSequence>, String>(
            new Supplier<List<CharSequence>>()
            {
                public List<CharSequence> get()
                {
                    return new ArrayList<CharSequence>();
                }
            },
            new BiConsumer<List<CharSequence>, CharSequence>()
            {
                public void accept(List<CharSequence> parts, CharSequence value)
                {
                    parts.add(value);
                }
            },
            new BinaryOperator<List<CharSequence>>()
            {
                public List<CharSequence> apply(List<CharSequence> first, List<CharSequence> second)
                {
                    first.addAll(second);
                    return first;
                }
            },
            new Function<List<CharSequence>, String>()
            {
                public String apply(List<CharSequence> parts)
                {
                    StringBuilder sb = new StringBuilder(head);
                    for (int i = 0; i < parts.size(); i++)
                    {
                        if (i > 0)
                        {
                            sb.append(separator);
                        }
                        sb.append(parts.get(i) == null ? "null" : parts.get(i).toString());
                    }
                    sb.append(tail);
                    return sb.toString();
                }
            });
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
        final Function<? super T, ? extends K> keyMapper,
        final Function<? super T, ? extends U> valueMapper)
    {
        return new CollectorImpl<T, Map<K, U>, Map<K, U>>(
            new Supplier<Map<K, U>>()
            {
                public Map<K, U> get()
                {
                    return new HashMap<K, U>();
                }
            },
            new BiConsumer<Map<K, U>, T>()
            {
                public void accept(Map<K, U> map, T value)
                {
                    K key = keyMapper.apply(value);
                    if (map.containsKey(key))
                    {
                        throw new IllegalStateException("duplicate key: " + key);
                    }
                    map.put(key, valueMapper.apply(value));
                }
            },
            new BinaryOperator<Map<K, U>>()
            {
                public Map<K, U> apply(Map<K, U> first, Map<K, U> second)
                {
                    first.putAll(second);
                    return first;
                }
            },
            new Function<Map<K, U>, Map<K, U>>()
            {
                public Map<K, U> apply(Map<K, U> map)
                {
                    return map;
                }
            });
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
        final Function<? super T, ? extends K> keyMapper,
        final Function<? super T, ? extends U> valueMapper,
        final BinaryOperator<U> mergeFunction)
    {
        return toMap(keyMapper, valueMapper, mergeFunction,
            new Supplier<Map<K, U>>()
            {
                public Map<K, U> get()
                {
                    return new HashMap<K, U>();
                }
            });
    }

    public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
        final Function<? super T, ? extends K> keyMapper,
        final Function<? super T, ? extends U> valueMapper,
        final BinaryOperator<U> mergeFunction,
        final Supplier<M> mapFactory)
    {
        return new CollectorImpl<T, M, M>(
            mapFactory,
            new BiConsumer<M, T>()
            {
                public void accept(M map, T value)
                {
                    K key = keyMapper.apply(value);
                    U mapped = valueMapper.apply(value);
                    U previous = map.get(key);
                    if (previous == null)
                    {
                        map.put(key, mapped);
                    }
                    else
                    {
                        map.put(key, mergeFunction.apply(previous, mapped));
                    }
                }
            },
            new BinaryOperator<M>()
            {
                public M apply(M first, M second)
                {
                    for (Map.Entry<K, U> entry : second.entrySet())
                    {
                        K key = entry.getKey();
                        U value = entry.getValue();
                        U previous = first.get(key);
                        if (previous == null)
                        {
                            first.put(key, value);
                        }
                        else
                        {
                            first.put(key, mergeFunction.apply(previous, value));
                        }
                    }
                    return first;
                }
            },
            new Function<M, M>()
            {
                public M apply(M map)
                {
                    return map;
                }
            });
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingBy(
        final Function<? super T, ? extends K> classifier)
    {
        return groupingBy(classifier, toList());
    }

    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingBy(
        final Function<? super T, ? extends K> classifier,
        final Collector<? super T, A, D> downstream)
    {
        return groupingBy(classifier,
            new Supplier<Map<K, D>>()
            {
                public Map<K, D> get()
                {
                    return new HashMap<K, D>();
                }
            },
            downstream);
    }

    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingBy(
        final Function<? super T, ? extends K> classifier,
        final Supplier<M> mapFactory,
        final Collector<? super T, A, D> downstream)
    {
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        final Function<A, D> downstreamFinisher = downstream.finisher();
        return new CollectorImpl<T, Map<K, A>, M>(
            new Supplier<Map<K, A>>()
            {
                public Map<K, A> get()
                {
                    return new HashMap<K, A>();
                }
            },
            new BiConsumer<Map<K, A>, T>()
            {
                public void accept(Map<K, A> map, T value)
                {
                    K key = classifier.apply(value);
                    A bucket = map.get(key);
                    if (bucket == null)
                    {
                        bucket = downstreamSupplier.get();
                        map.put(key, bucket);
                    }
                    downstreamAccumulator.accept(bucket, value);
                }
            },
            new BinaryOperator<Map<K, A>>()
            {
                public Map<K, A> apply(Map<K, A> first, Map<K, A> second)
                {
                    for (Map.Entry<K, A> entry : second.entrySet())
                    {
                        K key = entry.getKey();
                        A bucket = first.get(key);
                        if (bucket == null)
                        {
                            first.put(key, entry.getValue());
                        }
                        else
                        {
                            first.put(key, downstream.combiner().apply(bucket, entry.getValue()));
                        }
                    }
                    return first;
                }
            },
            new Function<Map<K, A>, M>()
            {
                public M apply(Map<K, A> intermediate)
                {
                    M result = mapFactory.get();
                    for (Map.Entry<K, A> entry : intermediate.entrySet())
                    {
                        result.put(entry.getKey(), downstreamFinisher.apply(entry.getValue()));
                    }
                    return result;
                }
            });
    }

    public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(
        final Collector<T, A, R> downstream,
        final Function<R, RR> finisher)
    {
        return new CollectorImpl<T, A, RR>(
            downstream.supplier(),
            downstream.accumulator(),
            downstream.combiner(),
            new Function<A, RR>()
            {
                public RR apply(A value)
                {
                    return finisher.apply(downstream.finisher().apply(value));
                }
            });
    }
}

final class CollectorImpl<T, A, R> implements Collector<T, A, R>
{
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Characteristics> flags;

    CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                  BinaryOperator<A> combiner, Function<A, R> finisher)
    {
        this(supplier, accumulator, combiner, finisher, null);
    }

    CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                  BinaryOperator<A> combiner, Function<A, R> finisher,
                  Set<Characteristics> flags)
    {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.finisher = finisher;
        this.flags = flags;
    }

    public Supplier<A> supplier()
    {
        return supplier;
    }

    public BiConsumer<A, T> accumulator()
    {
        return accumulator;
    }

    public BinaryOperator<A> combiner()
    {
        return combiner;
    }

    public Function<A, R> finisher()
    {
        return finisher;
    }

    public Set<Characteristics> characteristics()
    {
        return flags == null ? Collections.emptySet() : flags;
    }
}
