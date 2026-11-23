package java.util;

public class IntSummaryStatistics implements java.io.Serializable
{
    private long count;
    private long sum;
    private int min = Integer.MAX_VALUE;
    private int max = Integer.MIN_VALUE;

    public IntSummaryStatistics()
    {
    }

    public void accept(int value)
    {
        count++;
        sum += value;
        if (value < min)
        {
            min = value;
        }
        if (value > max)
        {
            max = value;
        }
    }

    public void combine(IntSummaryStatistics other)
    {
        count += other.count;
        sum += other.sum;
        if (other.min < min)
        {
            min = other.min;
        }
        if (other.max > max)
        {
            max = other.max;
        }
    }

    public final long getCount()
    {
        return count;
    }

    public final long getSum()
    {
        return sum;
    }

    public final int getMin()
    {
        return min;
    }

    public final int getMax()
    {
        return max;
    }

    public final double getAverage()
    {
        return count > 0 ? (double)sum / count : 0.0;
    }

    public String toString()
    {
        return "IntSummaryStatistics{count=" + count + ", sum=" + sum
            + ", min=" + min + ", average=" + getAverage() + ", max=" + max + "}";
    }
}
