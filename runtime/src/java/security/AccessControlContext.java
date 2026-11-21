package java.security;

/**
 * Placeholder access-control context.  With no SecurityManager installed,
 * {@link #checkPermission} always succeeds.
 */
public final class AccessControlContext
{
    public AccessControlContext()
    {
    }

    public void checkPermission(Permission perm)
    {
    }
}
