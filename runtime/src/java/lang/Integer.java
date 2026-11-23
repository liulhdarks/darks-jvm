package java.lang;

public final class Integer extends Number implements Comparable<Integer>
{
    public static final Class TYPE = Class.getPrimitiveClass("int");

    public static final int MIN_VALUE = -2147483648;
    public static final int MAX_VALUE = 2147483647;

    private final int value;

    public Integer(int value)
    {
        this.value = value;
    }

    public static Integer valueOf(int value)
    {
        return new Integer(value);
    }

    public static Integer valueOf(String s)
    {
        return new Integer(parseInt(s));
    }

    public static Integer getInteger(String name)
    {
        return getInteger(name, (Integer)null);
    }

    public static Integer getInteger(String name, int defaultValue)
    {
        Integer result = getInteger(name, (Integer)null);
        return result == null ? valueOf(defaultValue) : result;
    }

    public static Integer getInteger(String name, Integer defaultValue)
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
            return valueOf(parseInt(value));
        }
        catch (NumberFormatException ignored)
        {
            return defaultValue;
        }
    }

    public int intValue()
    {
        return value;
    }

    public long longValue()
    {
        return (long)value;
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

    public static int highestOneBit(int i)
    {
        i |= (i >> 1);
        i |= (i >> 2);
        i |= (i >> 4);
        i |= (i >> 8);
        i |= (i >> 16);
        return i - (i >>> 1);
    }

    public static int lowestOneBit(int i)
    {
        return i & -i;
    }

    public static int parseInt(String s)
    {
        return (int)parseLong(s);
    }

    public static int parseInt(String s, int radix)
    {
        return (int)parseLong(s, radix);
    }

    public static long parseLong(String s, int radix)
    {
        if (s == null || s.length() == 0)
        {
            throw new NumberFormatException("empty string");
        }
        if (radix < 2 || radix > 36)
        {
            throw new NumberFormatException("radix " + radix);
        }
        int i = 0;
        boolean negative = false;
        if (s.charAt(0) == '-')
        {
            negative = true;
            i = 1;
        }
        else if (s.charAt(0) == '+')
        {
            i = 1;
        }
        if (i >= s.length())
        {
            throw new NumberFormatException(s);
        }
        long result = 0;
        while (i < s.length())
        {
            char c = s.charAt(i);
            int digit;
            if (c >= '0' && c <= '9')
            {
                digit = c - '0';
            }
            else if (c >= 'a' && c <= 'z')
            {
                digit = c - 'a' + 10;
            }
            else if (c >= 'A' && c <= 'Z')
            {
                digit = c - 'A' + 10;
            }
            else
            {
                throw new NumberFormatException(s);
            }
            if (digit >= radix)
            {
                throw new NumberFormatException(s);
            }
            result = result * radix + digit;
            i = i + 1;
        }
        return negative ? -result : result;
    }

    public static long parseLong(String s)
    {
        if (s == null || s.length() == 0)
        {
            throw new NumberFormatException("empty string");
        }
        int i = 0;
        boolean negative = false;
        if (s.charAt(0) == '-')
        {
            negative = true;
            i = 1;
        }
        else if (s.charAt(0) == '+')
        {
            i = 1;
        }
        if (i >= s.length())
        {
            throw new NumberFormatException(s);
        }
        long result = 0;
        while (i < s.length())
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                throw new NumberFormatException(s);
            }
            result = result * 10 + (long)(c - '0');
            i = i + 1;
        }
        return negative ? -result : result;
    }

    public static String toString(int v)
    {
        if (v == 0)
        {
            return "0";
        }
        long n = (long)v;
        boolean negative = false;
        if (n < 0)
        {
            negative = true;
            n = -n;
        }
        char[] digits = new char[12];
        int pos = digits.length;
        while (n > 0)
        {
            pos = pos - 1;
            digits[pos] = (char)('0' + (int)(n % 10));
            n = n / 10;
        }
        if (negative)
        {
            pos = pos - 1;
            digits[pos] = '-';
        }
        return new String(digits, pos, digits.length - pos);
    }

    public static String toHexString(int v)
    {
        if (v == 0)
        {
            return "0";
        }
        char[] digits = new char[8];
        int pos = digits.length;
        long n = (long)v & 0xFFFFFFFFL;
        while (n > 0)
        {
            pos = pos - 1;
            int d = (int)(n & 15);
            digits[pos] = (char)(d < 10 ? ('0' + d) : ('a' + (d - 10)));
            n = n >>> 4;
        }
        return new String(digits, pos, digits.length - pos);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Integer))
        {
            return false;
        }
        return ((Integer)other).value == value;
    }

    public int hashCode()
    {
        return value;
    }

    public static int hashCode(int value)
    {
        return value;
    }

    public int compareTo(Integer other)
    {
        return compare(value, other.value);
    }

    public static int compare(int a, int b)
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

    public static Integer valueOf(String s, int radix)
    {
        return valueOf(parseInt(s, radix));
    }

    public static int bitCount(int i)
    {
        i = i - ((i >>> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
        i = (i + (i >>> 4)) & 0x0f0f0f0f;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        return i & 0x3f;
    }

    public static int numberOfLeadingZeros(int i)
    {
        if (i == 0)
        {
            return 32;
        }
        int n = 1;
        if ((i >>> 16) == 0)
        {
            n += 16;
            i <<= 16;
        }
        if ((i >>> 24) == 0)
        {
            n += 8;
            i <<= 8;
        }
        if ((i >>> 28) == 0)
        {
            n += 4;
            i <<= 4;
        }
        if ((i >>> 30) == 0)
        {
            n += 2;
            i <<= 2;
        }
        n -= i >>> 31;
        return n;
    }

    public static int numberOfTrailingZeros(int i)
    {
        if (i == 0)
        {
            return 32;
        }
        int n = 0;
        while ((i & 1) == 0)
        {
            n++;
            i >>>= 1;
        }
        return n;
    }

    public static int signum(int i)
    {
        return (i >> 31) | (-i >>> 31);
    }

    public static int reverse(int i)
    {
        i = ((i & 0x55555555) << 1) | ((i >>> 1) & 0x55555555);
        i = ((i & 0x33333333) << 2) | ((i >>> 2) & 0x33333333);
        i = ((i & 0x0f0f0f0f) << 4) | ((i >>> 4) & 0x0f0f0f0f);
        i = ((i & 0x00ff00ff) << 8) | ((i >>> 8) & 0x00ff00ff);
        return (i << 16) | (i >>> 16);
    }

    public static int rotateLeft(int i, int distance)
    {
        return (i << distance) | (i >>> -distance);
    }

    public static int rotateRight(int i, int distance)
    {
        return (i >>> distance) | (i << -distance);
    }

    public static int max(int a, int b)
    {
        return a >= b ? a : b;
    }

    public static int min(int a, int b)
    {
        return a <= b ? a : b;
    }

    public static int sum(int a, int b)
    {
        return a + b;
    }

    public static int compareUnsigned(int a, int b)
    {
        return compare(a + MIN_VALUE, b + MIN_VALUE);
    }

    public static Integer decode(String s)
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
        return valueOf(sign * parseInt(rest, radix));
    }

    public static String toString(int v, int radix)
    {
        if (radix < 2 || radix > 36)
        {
            throw new NumberFormatException("radix " + radix);
        }
        if (radix == 10)
        {
            return toString(v);
        }
        long n = (long)v & 0xffffffffL;
        if (n == 0)
        {
            return "0";
        }
        char[] digits = new char[33];
        int pos = digits.length;
        while (n > 0)
        {
            int d = (int)(n % radix);
            pos = pos - 1;
            digits[pos] = (char)(d < 10 ? ('0' + d) : ('a' + (d - 10)));
            n = n / radix;
        }
        return new String(digits, pos, digits.length - pos);
    }

    public static String toBinaryString(int v)
    {
        return toString(v, 2);
    }

    public static String toOctalString(int v)
    {
        return toString(v, 8);
    }

    public static String toUnsignedString(int v)
    {
        return toUnsignedString(v, 10);
    }

    public static String toUnsignedString(int v, int radix)
    {
        if (radix == 10)
        {
            return Long.toString((long)v & 0xffffffffL);
        }
        return toString(v, radix);
    }

    public static long toUnsignedLong(int v)
    {
        return (long)v & 0xffffffffL;
    }

    public static int parseUnsignedInt(String s)
    {
        return parseUnsignedInt(s, 10);
    }

    public static int parseUnsignedInt(String s, int radix)
    {
        long value = parseLong(s, radix);
        if (value < 0L || value > 0xffffffffL)
        {
            throw new NumberFormatException(s);
        }
        return (int)value;
    }
}
