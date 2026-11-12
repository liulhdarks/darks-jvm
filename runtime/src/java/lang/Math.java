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

    public static double random()
    {
        long n = System.nanoTime() & 16777215L;
        return (double)n / 16777216.0;
    }
}
