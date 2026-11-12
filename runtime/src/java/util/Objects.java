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

    public static boolean isNull(Object o)
    {
        return o == null;
    }

    public static boolean nonNull(Object o)
    {
        return o != null;
    }
}
