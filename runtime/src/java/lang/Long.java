package java.lang;

public final class Long implements Comparable<Long>
{
    public static final Class TYPE = Class.getPrimitiveClass("long");

    public static final long MIN_VALUE = -9223372036854775808L;
    public static final long MAX_VALUE = 9223372036854775807L;

    private final long value;

    public Long(long value)
    {
        this.value = value;
    }

    public static Long valueOf(long value)
    {
        return new Long(value);
    }

    public long longValue()
    {
        return value;
    }

    public int intValue()
    {
        return (int)value;
    }

    public double doubleValue()
    {
        return (double)value;
    }

    public float floatValue()
    {
        return (float)value;
    }

    public byte byteValue()
    {
        return (byte)value;
    }

    public short shortValue()
    {
        return (short)value;
    }

    public static long parseLong(String s)
    {
        return Integer.parseLong(s);
    }

    public static String toString(long v)
    {
        if (v == 0)
        {
            return "0";
        }
        boolean negative = false;
        if (v < 0)
        {
            negative = true;
            if (v == MIN_VALUE)
            {
                return "-9223372036854775808";
            }
            v = -v;
        }
        char[] digits = new char[20];
        int pos = digits.length;
        while (v > 0)
        {
            pos = pos - 1;
            digits[pos] = (char)('0' + (int)(v % 10));
            v = v / 10;
        }
        if (negative)
        {
            pos = pos - 1;
            digits[pos] = '-';
        }
        return new String(digits, pos, digits.length - pos);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Long))
        {
            return false;
        }
        return ((Long)other).value == value;
    }

    public int hashCode()
    {
        return (int)(value ^ (value >>> 32));
    }

    public static int hashCode(long value)
    {
        return (int)(value ^ (value >>> 32));
    }

    public int compareTo(Long other)
    {
        return compare(value, other.value);
    }

    public static int compare(long a, long b)
    {
        if (a < b)
        {
            return -1;
        }
        return a == b ? 0 : 1;
    }

    public String toString()
    {
        return toString(value);
    }
}
