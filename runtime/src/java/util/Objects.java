package java.util;

public final class Objects
{
    private Objects()
    {
    }

    public static <T> T requireNonNull(T obj)
    {
        if (obj == null)
        {
            throw new NullPointerException();
        }
        return obj;
    }

    public static <T> T requireNonNull(T obj, String message)
    {
        if (obj == null)
        {
            throw new NullPointerException(message);
        }
        return obj;
    }

    public static boolean equals(Object a, Object b)
    {
        return a == b || (a != null && a.equals(b));
    }

    public static int hashCode(Object o)
    {
        return o == null ? 0 : o.hashCode();
    }

    public static String toString(Object o)
    {
        return o == null ? "null" : o.toString();
    }

    public static String toString(Object o, String nullDefault)
    {
        return o == null ? nullDefault : o.toString();
    }

    public static int hash(Object... values)
    {
        int result = 1;
        int i = 0;
        while (i < values.length)
        {
            result = 31 * result + hashCode(values[i]);
            i = i + 1;
        }
        return result;
    }

    public static <T> int compare(T a, T b, Comparator<? super T> c)
    {
        return a == b ? 0 : c.compare(a, b);
    }

    public static boolean isNull(Object o)
    {
        return o == null;
    }

    public static boolean nonNull(Object o)
    {
        return o != null;
    }
}
