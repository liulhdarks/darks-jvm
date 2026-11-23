package java.util;

public class DoubleSummaryStatistics implements java.io.Serializable
{
    private long count;
    private double sum;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    public DoubleSummaryStatistics()
    {
    }

    public void accept(double value)
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

    public void combine(DoubleSummaryStatistics other)
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

    public final double getSum()
    {
        return sum;
    }

    public final double getMin()
    {
        return min;
    }

    public final double getMax()
    {
        return max;
    }

    public final double getAverage()
    {
        return count > 0 ? sum / count : 0.0;
    }

    public String toString()
    {
        return "DoubleSummaryStatistics{count=" + count + ", sum=" + sum
            + ", min=" + min + ", average=" + getAverage() + ", max=" + max + "}";
    }
}
