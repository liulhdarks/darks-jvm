package java.security;

public final class SecurityPermission extends BasicPermission
{
    public SecurityPermission(String name)
    {
        super(name);
    }

    public SecurityPermission(String name, String actions)
    {
        super(name, actions);
    }
}
