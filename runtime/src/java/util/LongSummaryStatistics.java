package java.util;

public class LongSummaryStatistics implements java.io.Serializable
{
    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;

    public LongSummaryStatistics()
    {
    }

    public void accept(int value)
    {
        accept((long)value);
    }

    public void accept(long value)
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

    public void combine(LongSummaryStatistics other)
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

    public final long getMin()
    {
        return min;
    }

    public final long getMax()
    {
        return max;
    }

    public final double getAverage()
    {
        return count > 0 ? (double)sum / count : 0.0;
    }

    public String toString()
    {
        return "LongSummaryStatistics{count=" + count + ", sum=" + sum
            + ", min=" + min + ", average=" + getAverage() + ", max=" + max + "}";
    }
}
