package java.security;

/**
 * Protection domain attached to a class.  The runtime does not implement an
 * access-control policy, so {@link #implies} always returns true and there is
 * no code source unless one is supplied explicitly.
 */
public class ProtectionDomain
{
    private final CodeSource codeSource;
    private final PermissionCollection permissions;

    public ProtectionDomain(CodeSource codeSource, PermissionCollection permissions)
    {
        this.codeSource = codeSource;
        this.permissions = permissions;
    }

    public final CodeSource getCodeSource()
    {
        return codeSource;
    }

    public final PermissionCollection getPermissions()
    {
        return permissions;
    }

    public boolean implies(Permission permission)
    {
        return true;
    }

    public String toString()
    {
        return "ProtectionDomain " + codeSource;
    }
}
