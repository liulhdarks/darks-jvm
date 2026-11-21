package java.time.temporal;

public final class ValueRange
{
    private final long minimum;
    private final long maximum;

    private ValueRange(long minimum, long maximum)
    {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static ValueRange of(long minimum, long maximum)
    {
        if (minimum > maximum)
        {
            throw new IllegalArgumentException("minimum > maximum");
        }
        return new ValueRange(minimum, maximum);
    }

    public static ValueRange of(long minimum, long largestMaximum, long maximum)
    {
        return of(minimum, maximum);
    }

    public long getMinimum()
    {
        return minimum;
    }

    public long getLargestMinimum()
    {
        return minimum;
    }

    public long getSmallestMaximum()
    {
        return maximum;
    }

    public long getMaximum()
    {
        return maximum;
    }

    public boolean isFixed()
    {
        return true;
    }

    public boolean isIntValue()
    {
        return minimum >= Integer.MIN_VALUE && maximum <= Integer.MAX_VALUE;
    }

    public boolean isValidValue(long value)
    {
        return value >= minimum && value <= maximum;
    }

    public boolean isValidIntValue(long value)
    {
        return isIntValue() && isValidValue(value);
    }

    public long checkValidValue(long value)
    {
        if (!isValidValue(value))
        {
            throw new java.time.DateTimeException("Invalid value: " + value);
        }
        return value;
    }

    public int checkValidIntValue(long value)
    {
        return (int)checkValidValue(value);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ValueRange))
        {
            return false;
        }
        ValueRange range = (ValueRange)other;
        return minimum == range.minimum && maximum == range.maximum;
    }

    public int hashCode()
    {
        long bits = minimum * 31 + maximum;
        return (int)(bits ^ (bits >>> 32));
    }

    public String toString()
    {
        return minimum + " - " + maximum;
    }
}
