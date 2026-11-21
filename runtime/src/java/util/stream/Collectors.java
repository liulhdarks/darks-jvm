package java.util.stream;

import java.util.ArrayList;
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
}

final class CollectorImpl<T, A, R> implements Collector<T, A, R>
{
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;

    CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                  BinaryOperator<A> combiner, Function<A, R> finisher)
    {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.finisher = finisher;
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
}
