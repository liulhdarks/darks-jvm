package java.util.stream;

import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

// Eager, single-shot primitive int stream over an in-memory array, mirroring
// the list-backed Stream implementation.
public interface IntStream
{
    IntStream filter(IntPredicate predicate);

    IntStream map(IntUnaryOperator mapper);

    void forEach(IntConsumer action);

    boolean anyMatch(IntPredicate predicate);

    boolean allMatch(IntPredicate predicate);

    boolean noneMatch(IntPredicate predicate);

    OptionalInt findFirst();

    int sum();

    long count();

    IntStream limit(long maxSize);

    IntStream skip(long count);

    int[] toArray();

    static IntStream empty()
    {
        return new ArrayIntStream(new int[0]);
    }

    static IntStream of(int... values)
    {
        return new ArrayIntStream(values);
    }

    static IntStream range(int startInclusive, int endExclusive)
    {
        if (startInclusive >= endExclusive)
        {
            return empty();
        }
        int[] values = new int[endExclusive - startInclusive];
        for (int i = 0; i < values.length; i++)
        {
            values[i] = startInclusive + i;
        }
        return new ArrayIntStream(values);
    }

    static IntStream rangeClosed(int startInclusive, int endInclusive)
    {
        if (startInclusive > endInclusive)
        {
            return empty();
        }
        return range(startInclusive, endInclusive + 1);
    }
}

final class ArrayIntStream implements IntStream
{
    private final int[] values;

    ArrayIntStream(int[] values)
    {
        this.values = values;
    }

    public IntStream filter(IntPredicate predicate)
    {
        int[] result = new int[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (predicate.test(values[i]))
            {
                result[count] = values[i];
                count = count + 1;
            }
        }
        int[] trimmed = new int[count];
        for (int i = 0; i < count; i++)
        {
            trimmed[i] = result[i];
        }
        return new ArrayIntStream(trimmed);
    }

    public IntStream map(IntUnaryOperator mapper)
    {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++)
        {
            result[i] = mapper.applyAsInt(values[i]);
        }
        return new ArrayIntStream(result);
    }

    public void forEach(IntConsumer action)
    {
        for (int i = 0; i < values.length; i++)
        {
            action.accept(values[i]);
        }
    }

    public boolean anyMatch(IntPredicate predicate)
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

    public boolean allMatch(IntPredicate predicate)
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

    public boolean noneMatch(IntPredicate predicate)
    {
        return !anyMatch(predicate);
    }

    public OptionalInt findFirst()
    {
        if (values.length == 0)
        {
            return OptionalInt.empty();
        }
        return OptionalInt.of(values[0]);
    }

    public int sum()
    {
        int total = 0;
        for (int i = 0; i < values.length; i++)
        {
            total = total + values[i];
        }
        return total;
    }

    public long count()
    {
        return values.length;
    }

    public IntStream limit(long maxSize)
    {
        if (maxSize < 0)
        {
            throw new IllegalArgumentException("negative limit: " + maxSize);
        }
        int limit = maxSize > values.length ? values.length : (int)maxSize;
        int[] result = new int[limit];
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return new ArrayIntStream(result);
    }

    public IntStream skip(long count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("negative skip: " + count);
        }
        int start = count > values.length ? values.length : (int)count;
        int[] result = new int[values.length - start];
        for (int i = start; i < values.length; i++)
        {
            result[i - start] = values[i];
        }
        return new ArrayIntStream(result);
    }

    public int[] toArray()
    {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++)
        {
            result[i] = values[i];
        }
        return result;
    }
}
