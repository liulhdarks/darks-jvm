package java.lang;

public final class Byte implements Comparable<Byte>
{
    public static final Class TYPE = Class.getPrimitiveClass("byte");

    public static final byte MIN_VALUE = -128;
    public static final byte MAX_VALUE = 127;

    private final byte value;

    public Byte(byte value)
    {
        this.value = value;
    }

    public static Byte valueOf(byte value)
    {
        return new Byte(value);
    }

    public byte byteValue()
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
        return (other instanceof Byte) && ((Byte)other).value == value;
    }

    public int hashCode()
    {
        return value;
    }

    public int compareTo(Byte other)
    {
        return value - other.value;
    }

    public String toString()
    {
        return Integer.toString(value);
    }
}
