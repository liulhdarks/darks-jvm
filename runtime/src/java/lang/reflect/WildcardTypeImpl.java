package java.lang.reflect;

final class WildcardTypeImpl implements WildcardType, Type
{
    private final Type[] upper;
    private final Type[] lower;

    WildcardTypeImpl(Type[] upper, Type[] lower)
    {
        this.upper = upper == null || upper.length == 0
            ? new Type[] { Object.class } : upper;
        this.lower = lower == null ? new Type[0] : lower;
    }

    public Type[] getUpperBounds()
    {
        return upper;
    }

    public Type[] getLowerBounds()
    {
        return lower;
    }

    public String toString()
    {
        if (lower.length > 0)
        {
            return "? super " + String.valueOf(lower[0]);
        }
        if (upper.length == 1 && upper[0] == Object.class)
        {
            return "?";
        }
        return "? extends " + String.valueOf(upper[0]);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof WildcardType))
        {
            return false;
        }
        WildcardType type = (WildcardType)other;
        return same(upper, type.getUpperBounds()) && same(lower, type.getLowerBounds());
    }

    public int hashCode()
    {
        return 31 * hash(upper) + hash(lower);
    }

    private static boolean same(Type[] a, Type[] b)
    {
        if (a.length != b.length)
        {
            return false;
        }
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] == null ? b[i] != null : !a[i].equals(b[i]))
            {
                return false;
            }
        }
        return true;
    }

    private static int hash(Type[] values)
    {
        int h = 1;
        for (int i = 0; i < values.length; i++)
        {
            h = 31 * h + (values[i] == null ? 0 : values[i].hashCode());
        }
        return h;
    }
}
