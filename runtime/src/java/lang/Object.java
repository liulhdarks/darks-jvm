package java.lang;

public class Object
{
    public Object()
    {
    }

    public boolean equals(Object other)
    {
        return this == other;
    }

    public native int hashCode();

    public String toString()
    {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public final native Class getClass();
}
