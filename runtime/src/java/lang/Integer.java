package java.lang;

public final class Integer implements Comparable<Integer>
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

    public static int parseInt(String s)
    {
        return (int)parseLong(s);
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
}
