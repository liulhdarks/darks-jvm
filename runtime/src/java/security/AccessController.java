package java.security;

/**
 * Access-control entry points.  There is no SecurityManager or access-control
 * context in this runtime, so {@code doPrivileged} just runs the action and
 * {@code checkPermission} returns normally.
 */
public final class AccessController
{
    private AccessController()
    {
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action)
    {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action)
        throws PrivilegedActionException
    {
        try
        {
            return action.run();
        }
        catch (java.lang.RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action,
        AccessControlContext context)
    {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action,
        AccessControlContext context) throws PrivilegedActionException
    {
        return doPrivileged(action);
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action)
    {
        return action.run();
    }

    public static void checkPermission(Permission perm)
    {
    }

    public static AccessControlContext getContext()
    {
        return new AccessControlContext();
    }
}
