package java.util.stream;

import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

public interface LongStream
{
    LongStream filter(LongPredicate predicate);

    LongStream map(LongUnaryOperator mapper);

    long count();

    void forEach(LongConsumer action);

    long sum();

    OptionalLong min();

    OptionalLong max();

    OptionalLong findFirst();

    boolean anyMatch(LongPredicate predicate);

    boolean allMatch(LongPredicate predicate);

    boolean noneMatch(LongPredicate predicate);

    long[] toArray();

    LongStream distinct();

    LongStream sorted();

    LongStream limit(long maxSize);

    LongStream skip(long count);

    Stream<Long> boxed();

    OptionalDouble average();

    LongStream sequential();

    LongStream parallel();

    static LongStream empty()
    {
        return new ArrayLongStream(new long[0]);
    }

    static LongStream of(long... values)
    {
        return new ArrayLongStream(values);
    }
}

final class ArrayLongStream implements LongStream
{
    private final long[] values;

    ArrayLongStream(long[] values)
    {
        this.values = values;
    }

    public LongStream filter(LongPredicate predicate)
    {
        long[] result = new long[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (predicate.test(values[i]))
            {
                result[count++] = values[i];
            }
        }
        return new ArrayLongStream(copy(result, count));
    }

    public LongStream map(LongUnaryOperator mapper)
    {
        long[] result = new long[values.length];
        for (int i = 0; i < values.length; i++)
        {
            result[i] = mapper.applyAsLong(values[i]);
        }
        return new ArrayLongStream(result);
    }

    public long count()
    {
        return values.length;
    }

    public void forEach(LongConsumer action)
    {
        for (int i = 0; i < values.length; i++)
        {
            action.accept(values[i]);
        }
    }

    public long sum()
    {
        long result = 0;
        for (int i = 0; i < values.length; i++)
        {
            result += values[i];
        }
        return result;
    }

    public OptionalLong min()
    {
        if (values.length == 0)
        {
            return OptionalLong.empty();
        }
        long result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            if (values[i] < result)
            {
                result = values[i];
            }
        }
        return OptionalLong.of(result);
    }

    public OptionalLong max()
    {
        if (values.length == 0)
        {
            return OptionalLong.empty();
        }
        long result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            if (values[i] > result)
            {
                result = values[i];
            }
        }
        return OptionalLong.of(result);
    }

    public OptionalLong findFirst()
    {
        return values.length == 0 ? OptionalLong.empty() : OptionalLong.of(values[0]);
    }

    public boolean anyMatch(LongPredicate predicate)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (predicate.test(values[i]))
            {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(LongPredicate predicate)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (!predicate.test(values[i]))
            {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(LongPredicate predicate)
    {
        return !anyMatch(predicate);
    }

    public long[] toArray()
    {
        return copy(values, values.length);
    }

    public LongStream distinct()
    {
        long[] result = new long[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            boolean seen = false;
            for (int j = 0; j < count; j++)
            {
                if (result[j] == values[i])
                {
                    seen = true;
                    break;
                }
            }
            if (!seen)
            {
                result[count++] = values[i];
            }
        }
        return new ArrayLongStream(copy(result, count));
    }

    public LongStream sorted()
    {
        long[] result = copy(values, values.length);
        java.util.Arrays.sort(result);
        return new ArrayLongStream(result);
    }

    public LongStream limit(long maxSize)
    {
        int size = maxSize < 0 ? 0 : (int)Math.min(maxSize, values.length);
        return new ArrayLongStream(copy(values, size));
    }

    public LongStream skip(long count)
    {
        int start = count < 0 ? 0 : (int)Math.min(count, values.length);
        return new ArrayLongStream(slice(values, start));
    }

    public Stream<Long> boxed()
    {
        java.util.ArrayList<Long> items = new java.util.ArrayList<Long>();
        for (int i = 0; i < values.length; i++)
        {
            items.add(Long.valueOf(values[i]));
        }
        return Stream.from(items);
    }

    public OptionalDouble average()
    {
        if (values.length == 0)
        {
            return OptionalDouble.empty();
        }
        long sum = 0;
        for (int i = 0; i < values.length; i++)
        {
            sum += values[i];
        }
        return OptionalDouble.of((double)sum / values.length);
    }

    public LongStream sequential()
    {
        return this;
    }

    public LongStream parallel()
    {
        return this;
    }

    private static long[] copy(long[] source, int length)
    {
        long[] result = new long[length];
        for (int i = 0; i < length; i++)
        {
            result[i] = source[i];
        }
        return result;
    }

    private static long[] slice(long[] source, int start)
    {
        long[] result = new long[source.length - start];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = source[start + i];
        }
        return result;
    }
}
