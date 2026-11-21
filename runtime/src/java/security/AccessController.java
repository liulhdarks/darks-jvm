package java.security;

public final class AccessController
{
    private AccessController()
    {
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action)
    {
        if (action == null)
        {
            throw new NullPointerException("action");
        }
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action,
        AccessControlContext context)
    {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action)
        throws PrivilegedActionException
    {
        if (action == null)
        {
            throw new NullPointerException("action");
        }
        try
        {
            return action.run();
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action,
        AccessControlContext context) throws PrivilegedActionException
    {
        return doPrivileged(action);
    }

    public static void checkPermission(Permission permission)
    {
    }

    public static AccessControlContext getContext()
    {
        return new AccessControlContext(new ProtectionDomain[0]);
    }
}
