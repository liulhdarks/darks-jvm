package java.security;

public final class AccessControlContext
{
    public AccessControlContext()
    {
    }

    public AccessControlContext(ProtectionDomain[] context)
    {
    }

    public void checkPermission(Permission permission)
    {
    }

    public ProtectionDomain[] getProtectionDomains()
    {
        return new ProtectionDomain[0];
    }
}
