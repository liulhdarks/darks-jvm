package java.security;

public abstract class Permission
{
    private String name;

    public Permission(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void checkGuard(Object object)
    {
    }

    public abstract boolean implies(Permission permission);

    public abstract boolean equals(Object object);

    public abstract int hashCode();

    public abstract String getActions();

    public String toString()
    {
        return "(" + getClass().getName() + " " + name + ")";
    }
}
