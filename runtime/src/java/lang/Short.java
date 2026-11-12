package java.lang;

public final class Short implements Comparable<Short>
{
    public static final Class TYPE = Class.getPrimitiveClass("short");

    public static final short MIN_VALUE = -32768;
    public static final short MAX_VALUE = 32767;

    private final short value;

    public Short(short value)
    {
        this.value = value;
    }

    public static Short valueOf(short value)
    {
        return new Short(value);
    }

    public short shortValue()
    {
        return value;
    }

    public int intValue()
    {
        return value;
    }

    public long longValue()
    {
        return value;
    }

    public double doubleValue()
    {
        return value;
    }

    public boolean equals(Object other)
    {
        return (other instanceof Short) && ((Short)other).value == value;
    }

    public int hashCode()
    {
        return value;
    }

    public int compareTo(Short other)
    {
        return value - other.value;
    }

    public String toString()
    {
        return Integer.toString(value);
    }
}
