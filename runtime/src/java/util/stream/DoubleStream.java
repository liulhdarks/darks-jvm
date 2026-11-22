package java.util.stream;

import java.util.OptionalDouble;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

public interface DoubleStream
{
    DoubleStream filter(DoublePredicate predicate);

    DoubleStream map(DoubleUnaryOperator mapper);

    long count();

    void forEach(DoubleConsumer action);

    double sum();

    OptionalDouble min();

    OptionalDouble max();

    OptionalDouble findFirst();

    double average();

    boolean anyMatch(DoublePredicate predicate);

    boolean allMatch(DoublePredicate predicate);

    boolean noneMatch(DoublePredicate predicate);

    double[] toArray();

    DoubleStream distinct();

    DoubleStream sorted();

    DoubleStream limit(long maxSize);

    DoubleStream skip(long count);

    Stream<Double> boxed();

    DoubleStream sequential();

    DoubleStream parallel();

    static DoubleStream empty()
    {
        return new ArrayDoubleStream(new double[0]);
    }

    static DoubleStream of(double... values)
    {
        return new ArrayDoubleStream(values);
    }
}

final class ArrayDoubleStream implements DoubleStream
{
    private final double[] values;

    ArrayDoubleStream(double[] values)
    {
        this.values = values;
    }

    public DoubleStream filter(DoublePredicate predicate)
    {
        double[] result = new double[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (predicate.test(values[i]))
            {
                result[count++] = values[i];
            }
        }
        return new ArrayDoubleStream(copy(result, count));
    }

    public DoubleStream map(DoubleUnaryOperator mapper)
    {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++)
        {
            result[i] = mapper.applyAsDouble(values[i]);
        }
        return new ArrayDoubleStream(result);
    }

    public long count()
    {
        return values.length;
    }

    public void forEach(DoubleConsumer action)
    {
        for (int i = 0; i < values.length; i++)
        {
            action.accept(values[i]);
        }
    }

    public double sum()
    {
        double result = 0;
        for (int i = 0; i < values.length; i++)
        {
            result += values[i];
        }
        return result;
    }

    public OptionalDouble min()
    {
        if (values.length == 0)
        {
            return OptionalDouble.empty();
        }
        double result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            if (values[i] < result)
            {
                result = values[i];
            }
        }
        return OptionalDouble.of(result);
    }

    public OptionalDouble max()
    {
        if (values.length == 0)
        {
            return OptionalDouble.empty();
        }
        double result = values[0];
        for (int i = 1; i < values.length; i++)
        {
            if (values[i] > result)
            {
                result = values[i];
            }
        }
        return OptionalDouble.of(result);
    }

    public OptionalDouble findFirst()
    {
        return values.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(values[0]);
    }

    public double average()
    {
        if (values.length == 0)
        {
            return 0.0d;
        }
        return sum() / values.length;
    }

    public boolean anyMatch(DoublePredicate predicate)
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

    public boolean allMatch(DoublePredicate predicate)
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

    public boolean noneMatch(DoublePredicate predicate)
    {
        return !anyMatch(predicate);
    }

    public double[] toArray()
    {
        return copy(values, values.length);
    }

    public DoubleStream distinct()
    {
        double[] result = new double[values.length];
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
        return new ArrayDoubleStream(copy(result, count));
    }

    public DoubleStream sorted()
    {
        double[] result = copy(values, values.length);
        for (int i = 1; i < result.length; i++)
        {
            double value = result[i];
            int j = i - 1;
            while (j >= 0 && result[j] > value)
            {
                result[j + 1] = result[j];
                j--;
            }
            result[j + 1] = value;
        }
        return new ArrayDoubleStream(result);
    }

    public DoubleStream limit(long maxSize)
    {
        int size = maxSize < 0 ? 0 : (int)Math.min(maxSize, values.length);
        return new ArrayDoubleStream(copy(values, size));
    }

    public DoubleStream skip(long count)
    {
        int start = count < 0 ? 0 : (int)Math.min(count, values.length);
        double[] result = new double[values.length - start];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = values[start + i];
        }
        return new ArrayDoubleStream(result);
    }

    public Stream<Double> boxed()
    {
        java.util.ArrayList<Double> items = new java.util.ArrayList<Double>();
        for (int i = 0; i < values.length; i++)
        {
            items.add(Double.valueOf(values[i]));
        }
        return Stream.from(items);
    }

    public DoubleStream sequential()
    {
        return this;
    }

    public DoubleStream parallel()
    {
        return this;
    }

    private static double[] copy(double[] source, int length)
    {
        double[] result = new double[length];
        for (int i = 0; i < length; i++)
        {
            result[i] = source[i];
        }
        return result;
    }
}
