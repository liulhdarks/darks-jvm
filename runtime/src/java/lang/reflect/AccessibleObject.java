package java.lang.reflect;

/**
 * Base class for reflective members (Field, Method, Constructor).
 *
 * The VM does not walk the Java call stack for access checks: a non-public
 * member can only be used after setAccessible(true) has been called on it.
 * Public members are always accessible.
 */
public class AccessibleObject
{
    private boolean accessible;

    protected AccessibleObject()
    {
    }

    public void setAccessible(boolean flag)
    {
        accessible = flag;
    }

    public boolean isAccessible()
    {
        return accessible;
    }

    public static void setAccessible(AccessibleObject[] array, boolean flag)
    {
        if (array == null)
        {
            throw new NullPointerException();
        }
        for (int i = 0; i < array.length; i++)
        {
            if (array[i] != null)
            {
                array[i].setAccessible(flag);
            }
        }
    }
}
