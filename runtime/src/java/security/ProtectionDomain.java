package java.security;

public class ProtectionDomain
{
    private final CodeSource codeSource;
    private final Permission[] permissions;

    public ProtectionDomain(CodeSource codeSource, Permission[] permissions)
    {
        this.codeSource = codeSource;
        this.permissions = permissions;
    }

    public ProtectionDomain(CodeSource codeSource, PermissionCollection permissions)
    {
        this.codeSource = codeSource;
        this.permissions = permissions == null ? null : permissions.toArray();
    }

    public final CodeSource getCodeSource()
    {
        return codeSource;
    }

    public final PermissionCollection getPermissions()
    {
        return new PermissionCollection(permissions);
    }

    public boolean implies(Permission permission)
    {
        if (permissions == null)
        {
            return false;
        }
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i] != null && permissions[i].implies(permission))
            {
                return true;
            }
        }
        return false;
    }

    public String toString()
    {
        return "ProtectionDomain " + (codeSource == null ? "<null>" : codeSource.toString());
    }
}
