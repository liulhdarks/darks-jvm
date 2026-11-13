package java.lang;

public final class Double implements Comparable<Double>
{
    public static final Class TYPE = Class.getPrimitiveClass("double");

    public static final double MAX_VALUE = 1.7976931348623157e308;
    public static final double MIN_VALUE = 4.9e-324;
    public static final double POSITIVE_INFINITY = 1.0 / 0.0;
    public static final double NEGATIVE_INFINITY = -1.0 / 0.0;
    public static final double NaN = 0.0 / 0.0;

    private final double value;

    public Double(double value)
    {
        this.value = value;
    }

    public static Double valueOf(double value)
    {
        return new Double(value);
    }

    public double doubleValue()
    {
        return value;
    }

    public int intValue()
    {
        return (int)value;
    }

    public static boolean isNaN(double v)
    {
        return v != v;
    }

    public static boolean isInfinite(double v)
    {
        return v > MAX_VALUE || v < -MAX_VALUE;
    }

    public static int hashCode(double v)
    {
        long bits = (long)v;
        return (int)(bits ^ (bits >>> 32));
    }

    public static double parseDouble(String s)
    {
        if (s == null)
        {
            throw new NumberFormatException("null");
        }
        int i = 0;
        boolean negative = false;
        if (s.length() > 0 && s.charAt(0) == '-')
        {
            negative = true;
            i = 1;
        }
        else if (s.length() > 0 && s.charAt(0) == '+')
        {
            i = 1;
        }
        long intPart = 0;
        boolean any = false;
        while (i < s.length())
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                break;
            }
            intPart = intPart * 10 + (long)(c - '0');
            any = true;
            i = i + 1;
        }
        double result = (double)intPart;
        if (i < s.length() && s.charAt(i) == '.')
        {
            i = i + 1;
            double scale = 0.1;
            while (i < s.length())
            {
                char c = s.charAt(i);
                if (c < '0' || c > '9')
                {
                    throw new NumberFormatException(s);
                }
                result = result + (double)(c - '0') * scale;
                scale = scale * 0.1;
                any = true;
                i = i + 1;
            }
        }
        if (!any || i != s.length())
        {
            throw new NumberFormatException(s);
        }
        return negative ? -result : result;
    }

    public static String toString(double d)
    {
        if (d != d)
        {
            return "NaN";
        }
        if (d > 1.0e308)
        {
            return "Infinity";
        }
        if (d < -1.0e308)
        {
            return "-Infinity";
        }
        boolean negative = false;
        if (d < 0)
        {
            negative = true;
            d = -d;
        }
        long intPart = (long)d;
        double fraction = d - (double)intPart;
        long frac = (long)(fraction * 1000000.0 + 0.5);
        if (frac >= 1000000L)
        {
            intPart = intPart + 1;
            frac = frac - 1000000L;
        }
        String result = Long.toString(intPart);
        if (frac > 0)
        {
            String f = Long.toString(frac);
            while (f.length() < 6)
            {
                f = "0" + f;
            }
            while (f.length() > 1 && f.charAt(f.length() - 1) == '0')
            {
                f = f.substring(0, f.length() - 1);
            }
            result = result + "." + f;
        }
        if (negative)
        {
            result = "-" + result;
        }
        return result;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Double))
        {
            return false;
        }
        return ((Double)other).value == value;
    }

    public int hashCode()
    {
        long bits = (long)value;
        return (int)(bits ^ (bits >>> 32));
    }

    public int compareTo(Double other)
    {
        return compare(value, other.value);
    }

    public static int compare(double a, double b)
    {
        if (a < b)
        {
            return -1;
        }
        if (a > b)
        {
            return 1;
        }
        return 0;
    }

    public String toString()
    {
        return toString(value);
    }
}
