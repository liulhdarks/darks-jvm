package java.security;

public final class AllPermission extends Permission
{
    public AllPermission()
    {
        super("<all permissions>");
    }

    public AllPermission(String name, String actions)
    {
        super(name == null ? "<all permissions>" : name);
    }

    public boolean implies(Permission permission)
    {
        return true;
    }

    public boolean equals(Object object)
    {
        return object instanceof AllPermission;
    }

    public int hashCode()
    {
        return 1;
    }

    public String getActions()
    {
        return "<all actions>";
    }
}
