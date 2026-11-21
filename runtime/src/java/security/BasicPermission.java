package java.security;

public abstract class BasicPermission extends Permission
{
    public BasicPermission(String name)
    {
        super(name);
    }

    public BasicPermission(String name, String actions)
    {
        super(name);
    }

    public boolean implies(Permission permission)
    {
        return permission != null && getName().equals(permission.getName());
    }

    public boolean equals(Object object)
    {
        return object != null && object.getClass() == getClass()
            && getName().equals(((Permission)object).getName());
    }

    public int hashCode()
    {
        return getName().hashCode();
    }

    public String getActions()
    {
        return "";
    }
}
