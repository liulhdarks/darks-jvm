package java.lang;

public final class Math
{
    public static final double PI = 3.141592653589793;
    public static final double E = 2.718281828459045;

    public static native double sqrt(double value);
    public static native double pow(double base, double exponent);
    public static native double floor(double value);
    public static native double ceil(double value);

    public static int abs(int value)
    {
        return value < 0 ? -value : value;
    }

    public static long abs(long value)
    {
        return value < 0 ? -value : value;
    }

    public static double abs(double value)
    {
        return value < 0 ? -value : value;
    }

    public static int max(int a, int b)
    {
        return a > b ? a : b;
    }

    public static long max(long a, long b)
    {
        return a > b ? a : b;
    }

    public static double max(double a, double b)
    {
        return a > b ? a : b;
    }

    public static int min(int a, int b)
    {
        return a < b ? a : b;
    }

    public static long min(long a, long b)
    {
        return a < b ? a : b;
    }

    public static double min(double a, double b)
    {
        return a < b ? a : b;
    }

    public static long round(double value)
    {
        if (value < 0)
        {
            return (long)(value - 0.5);
        }
        return (long)(value + 0.5);
    }

    public static int round(float value)
    {
        return (int)round((double)value);
    }

    public static int signum(int value)
    {
        return value < 0 ? -1 : (value == 0 ? 0 : 1);
    }

    public static long signum(long value)
    {
        return value < 0 ? -1L : (value == 0 ? 0L : 1L);
    }

    public static double signum(double value)
    {
        if (value < 0)
        {
            return -1.0;
        }
        if (value > 0)
        {
            return 1.0;
        }
        return value;
    }

    public static int toIntExact(long value)
    {
        if (value < -2147483648L || value > 2147483647L)
        {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }

    public static int floorDiv(int a, int b)
    {
        int q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0)))
        {
            q = q - 1;
        }
        return q;
    }

    public static long floorDiv(long a, long b)
    {
        long q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0)))
        {
            q = q - 1;
        }
        return q;
    }

    public static int floorMod(int a, int b)
    {
        return a - floorDiv(a, b) * b;
    }

    public static long floorMod(long a, long b)
    {
        return a - floorDiv(a, b) * b;
    }

    public static double hypot(double a, double b)
    {
        return sqrt(a * a + b * b);
    }

    public static double random()
    {
        long n = System.nanoTime() & 16777215L;
        return (double)n / 16777216.0;
    }
}
