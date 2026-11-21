package java.security;

/**
 * Base type for permissions.  Nothing in this runtime enforces them; the type
 * exists so code that names permissions in signatures keeps linking.
 */
public abstract class Permission
{
    private final String name;

    protected Permission(String name)
    {
        this.name = name;
    }

    public final String getName()
    {
        return name;
    }

    public boolean implies(Permission permission)
    {
        return equals(permission);
    }

    public abstract boolean equals(Object obj);

    public abstract int hashCode();

    public String toString()
    {
        return name;
    }
}
