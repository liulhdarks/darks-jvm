package java.lang;

public final class Float implements Comparable<Float>
{
    public static final Class TYPE = Class.getPrimitiveClass("float");

    public static final float MIN_VALUE = 1.4E-45f;
    public static final float MAX_VALUE = 3.4028235E38f;

    private final float value;

    public Float(float value)
    {
        this.value = value;
    }

    public static Float valueOf(float value)
    {
        return new Float(value);
    }

    public float floatValue()
    {
        return value;
    }

    public double doubleValue()
    {
        return (double)value;
    }

    public int intValue()
    {
        return (int)value;
    }

    public boolean equals(Object other)
    {
        return (other instanceof Float) && ((Float)other).value == value;
    }

    public int hashCode()
    {
        long bits = (long)value;
        return (int)(bits ^ (bits >>> 32));
    }

    public int compareTo(Float other)
    {
        if (value < other.value)
        {
            return -1;
        }
        return value == other.value ? 0 : 1;
    }

    public String toString()
    {
        return Double.toString((double)value);
    }
}
