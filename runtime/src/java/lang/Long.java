package java.lang;

public final class Long extends Number implements Comparable<Long>
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

    public static String toHexString(long v)
    {
        if (v == 0)
        {
            return "0";
        }
        char[] digits = new char[16];
        int pos = digits.length;
        while (v != 0)
        {
            pos = pos - 1;
            int d = (int)(v & 15);
            digits[pos] = (char)(d < 10 ? ('0' + d) : ('a' + (d - 10)));
            v = v >>> 4;
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

    public static int bitCount(long i)
    {
        i = i - ((i >>> 1) & 0x5555555555555555L);
        i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
        i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        i = i + (i >>> 32);
        return (int)i & 0x7f;
    }

    public static int numberOfLeadingZeros(long i)
    {
        if (i == 0)
        {
            return 64;
        }
        int n = 1;
        int x = (int)(i >>> 32);
        if (x == 0)
        {
            n += 32;
            x = (int)i;
        }
        if (x >>> 16 == 0) { n += 16; x <<= 16; }
        if (x >>> 24 == 0) { n += 8; x <<= 8; }
        if (x >>> 28 == 0) { n += 4; x <<= 4; }
        if (x >>> 30 == 0) { n += 2; x <<= 2; }
        n -= x >>> 31;
        return n;
    }

    public static int numberOfTrailingZeros(long i)
    {
        if (i == 0)
        {
            return 64;
        }
        int n = 63;
        int x = (int)i;
        if (x != 0)
        {
            n -= 32;
        }
        else
        {
            x = (int)(i >>> 32);
        }
        if ((x << 16) != 0) { n -= 16; x <<= 16; }
        if ((x << 8) != 0) { n -= 8; x <<= 8; }
        if ((x << 4) != 0) { n -= 4; x <<= 4; }
        if ((x << 2) != 0) { n -= 2; x <<= 2; }
        return n - ((x << 1) >>> 31);
    }

    public String toString()
    {
        return toString(value);
    }
}
