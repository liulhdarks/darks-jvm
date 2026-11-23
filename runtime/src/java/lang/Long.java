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

    public static Long valueOf(String s)
    {
        return new Long(parseLong(s));
    }

    public static Long getLong(String name)
    {
        return getLong(name, (Long)null);
    }

    public static Long getLong(String name, long defaultValue)
    {
        Long result = getLong(name, (Long)null);
        return result == null ? valueOf(defaultValue) : result;
    }

    public static Long getLong(String name, Long defaultValue)
    {
        if (name == null)
        {
            return defaultValue;
        }
        String value;
        try
        {
            value = System.getProperty(name);
        }
        catch (RuntimeException ignored)
        {
            return defaultValue;
        }
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return valueOf(parseLong(value));
        }
        catch (NumberFormatException ignored)
        {
            return defaultValue;
        }
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

    public static Long valueOf(String s, int radix)
    {
        return valueOf(parseLong(s, radix));
    }

    public static long parseLong(String s, int radix)
    {
        return Integer.parseLong(s, radix);
    }

    public static int compareUnsigned(long a, long b)
    {
        return compare(a + MIN_VALUE, b + MIN_VALUE);
    }

    public static long highestOneBit(long i)
    {
        i |= (i >> 1);
        i |= (i >> 2);
        i |= (i >> 4);
        i |= (i >> 8);
        i |= (i >> 16);
        i |= (i >> 32);
        return i - (i >>> 1);
    }

    public static long lowestOneBit(long i)
    {
        return i & -i;
    }

    public static int signum(long i)
    {
        return (int)((i >> 63) | (-i >>> 63));
    }

    public static long reverse(long i)
    {
        i = ((i & 0x5555555555555555L) << 1) | ((i >>> 1) & 0x5555555555555555L);
        i = ((i & 0x3333333333333333L) << 2) | ((i >>> 2) & 0x3333333333333333L);
        i = ((i & 0x0f0f0f0f0f0f0f0fL) << 4) | ((i >>> 4) & 0x0f0f0f0f0f0f0f0fL);
        i = ((i & 0x00ff00ff00ff00ffL) << 8) | ((i >>> 8) & 0x00ff00ff00ff00ffL);
        i = ((i & 0x0000ffff0000ffffL) << 16) | ((i >>> 16) & 0x0000ffff0000ffffL);
        return (i << 32) | (i >>> 32);
    }

    public static long rotateLeft(long i, int distance)
    {
        return (i << distance) | (i >>> -distance);
    }

    public static long rotateRight(long i, int distance)
    {
        return (i >>> distance) | (i << -distance);
    }

    public static long max(long a, long b)
    {
        return a >= b ? a : b;
    }

    public static long min(long a, long b)
    {
        return a <= b ? a : b;
    }

    public static long sum(long a, long b)
    {
        return a + b;
    }

    public static Long decode(String s)
    {
        if (s == null)
        {
            throw new NumberFormatException("null");
        }
        int sign = 1;
        int start = 0;
        if (s.length() > 0 && s.charAt(0) == '-')
        {
            sign = -1;
            start = 1;
        }
        else if (s.length() > 0 && s.charAt(0) == '+')
        {
            start = 1;
        }
        String rest = s.substring(start);
        int radix = 10;
        if (rest.startsWith("0x") || rest.startsWith("0X"))
        {
            radix = 16;
            rest = rest.substring(2);
        }
        else if (rest.startsWith("#"))
        {
            radix = 16;
            rest = rest.substring(1);
        }
        else if (rest.length() > 1 && rest.charAt(0) == '0')
        {
            radix = 8;
            rest = rest.substring(1);
        }
        return valueOf(sign * parseLong(rest, radix));
    }

    public static String toString(long v, int radix)
    {
        if (radix < 2 || radix > 36)
        {
            throw new NumberFormatException("radix " + radix);
        }
        if (radix == 10)
        {
            return toString(v);
        }
        if (v == 0)
        {
            return "0";
        }
        char[] digits = new char[65];
        int pos = digits.length;
        boolean negative = v < 0;
        while (v != 0)
        {
            int d = (int)(v % radix);
            if (d < 0)
            {
                d = -d;
            }
            pos = pos - 1;
            digits[pos] = (char)(d < 10 ? ('0' + d) : ('a' + (d - 10)));
            v = v / radix;
        }
        if (negative)
        {
            pos = pos - 1;
            digits[pos] = '-';
        }
        return new String(digits, pos, digits.length - pos);
    }

    public static String toBinaryString(long v)
    {
        return toUnsignedString(v, 2);
    }

    public static String toOctalString(long v)
    {
        return toUnsignedString(v, 8);
    }

    public static String toUnsignedString(long v)
    {
        return toUnsignedString(v, 10);
    }

    public static String toUnsignedString(long v, int radix)
    {
        if (radix == 10)
        {
            if (v >= 0)
            {
                return toString(v);
            }
            long quotient = (v >>> 1) / 5;
            long remainder = v - quotient * 10;
            return toString(quotient) + (char)('0' + remainder);
        }
        if (radix != 2 && radix != 4 && radix != 8 && radix != 16)
        {
            return toString(v, radix);
        }
        if (v == 0)
        {
            return "0";
        }
        int shift = radix == 2 ? 1 : radix == 4 ? 2 : radix == 8 ? 3 : 4;
        int mask = radix - 1;
        char[] digits = new char[65];
        int pos = digits.length;
        while (v != 0)
        {
            int d = (int)(v & mask);
            pos = pos - 1;
            digits[pos] = (char)(d < 10 ? ('0' + d) : ('a' + (d - 10)));
            v = v >>> shift;
        }
        return new String(digits, pos, digits.length - pos);
    }
}
