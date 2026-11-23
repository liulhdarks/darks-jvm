package java.util.concurrent.atomic;

public class DoubleAdder
{
    private double value;

    public DoubleAdder()
    {
    }

    public void add(double delta)
    {
        synchronized (this)
        {
            value += delta;
        }
    }

    public double sum()
    {
        synchronized (this)
        {
            return value;
        }
    }

    public void reset()
    {
        synchronized (this)
        {
            value = 0.0;
        }
    }

    public double sumThenReset()
    {
        synchronized (this)
        {
            double result = value;
            value = 0.0;
            return result;
        }
    }

    public double doubleValue()
    {
        return sum();
    }

    public long longValue()
    {
        return (long)sum();
    }

    public int intValue()
    {
        return (int)sum();
    }

    public float floatValue()
    {
        return (float)sum();
    }

    public String toString()
    {
        return Double.toString(sum());
    }
}
