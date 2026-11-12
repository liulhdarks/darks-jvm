package java.lang;

public final class Boolean implements Comparable<Boolean>
{
    public static final Class TYPE = Class.getPrimitiveClass("boolean");

    public static final Boolean TRUE = new Boolean(true);
    public static final Boolean FALSE = new Boolean(false);

    private final boolean value;

    public Boolean(boolean value)
    {
        this.value = value;
    }

    public static Boolean valueOf(boolean value)
    {
        return value ? TRUE : FALSE;
    }

    public boolean booleanValue()
    {
        return value;
    }

    public static boolean parseBoolean(String s)
    {
        return s != null && s.equalsIgnoreCase("true");
    }

    public static String toString(boolean b)
    {
        return b ? "true" : "false";
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Boolean))
        {
            return false;
        }
        return ((Boolean)other).value == value;
    }

    public int hashCode()
    {
        return value ? 1231 : 1237;
    }

    public int compareTo(Boolean other)
    {
        if (value == other.value)
        {
            return 0;
        }
        return value ? 1 : -1;
    }

    public String toString()
    {
        return toString(value);
    }
}
