package java.util.stream;

import java.util.ArrayList;
import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

// Eager, single-shot stream of int values.  Intermediate operations
// materialise their result, matching the Stream implementation.
public interface IntStream
{
    IntStream filter(IntPredicate predicate);

    IntStream map(IntUnaryOperator mapper);

    <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);

    long count();

    void forEach(IntConsumer action);

    int sum();

    OptionalInt min();

    OptionalInt max();

    OptionalInt findFirst();

    boolean anyMatch(IntPredicate predicate);

    boolean allMatch(IntPredicate predicate);

    boolean noneMatch(IntPredicate predicate);

    int[] toArray();

    IntStream distinct();

    IntStream sorted();

    IntStream limit(long maxSize);

    IntStream skip(long count);

    Stream<Integer> boxed();

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
                result[count++] = values[i];
            }
        }
        return new ArrayIntStream(copy(result, count));
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

    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper)
    {
        ArrayList<U> items = new ArrayList<U>(values.length > 0 ? values.length : 1);
        for (int i = 0; i < values.length; i++)
        {
            items.add(mapper.apply(values[i]));
        }
        return Stream.from(items);
    }

    public long count()
    {
        return values.length;
    }

    public void forEach(IntConsumer action)
    {
        for (int i = 0; i < values.length; i++)
        {
            action.accept(values[i]);
        }
    }

    public int sum()
    {
        int result = 0;
        for (int i = 0; i < values.length; i++)
        {
            result += values[i];
        }
        return result;
    }

    public OptionalInt min()
    {
        if (values.length == 0)
        {
            return OptionalInt.empty();
        }
        int result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            if (values[i] < result)
            {
                result = values[i];
            }
        }
        return OptionalInt.of(result);
    }

    public OptionalInt max()
    {
        if (values.length == 0)
        {
            return OptionalInt.empty();
        }
        int result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            if (values[i] > result)
            {
                result = values[i];
            }
        }
        return OptionalInt.of(result);
    }

    public OptionalInt findFirst()
    {
        return values.length == 0 ? OptionalInt.empty() : OptionalInt.of(values[0]);
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

    public int[] toArray()
    {
        return copy(values, values.length);
    }

    public IntStream distinct()
    {
        int[] result = new int[values.length];
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
        return new ArrayIntStream(copy(result, count));
    }

    public IntStream sorted()
    {
        int[] result = copy(values, values.length);
        for (int i = 1; i < result.length; i++)
        {
            int value = result[i];
            int j = i - 1;
            while (j >= 0 && result[j] > value)
            {
                result[j + 1] = result[j];
                j--;
            }
            result[j + 1] = value;
        }
        return new ArrayIntStream(result);
    }

    public IntStream limit(long maxSize)
    {
        int size = maxSize < 0 ? 0 : (int)Math.min(maxSize, values.length);
        return new ArrayIntStream(copy(values, size));
    }

    public IntStream skip(long count)
    {
        int start = count < 0 ? 0 : (int)Math.min(count, values.length);
        int size = values.length - start;
        int[] result = new int[size];
        for (int i = 0; i < size; i++)
        {
            result[i] = values[start + i];
        }
        return new ArrayIntStream(result);
    }

    public Stream<Integer> boxed()
    {
        ArrayList<Integer> items = new ArrayList<Integer>(values.length > 0 ? values.length : 1);
        for (int i = 0; i < values.length; i++)
        {
            items.add(Integer.valueOf(values[i]));
        }
        return Stream.from(items);
    }

    private static int[] copy(int[] source, int length)
    {
        int[] result = new int[length];
        for (int i = 0; i < length; i++)
        {
            result[i] = source[i];
        }
        return result;
    }
}
