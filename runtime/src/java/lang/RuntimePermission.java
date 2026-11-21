package java.lang;

public final class RuntimePermission extends java.security.BasicPermission
{
    public RuntimePermission(String name)
    {
        super(name);
    }

    public RuntimePermission(String name, String actions)
    {
        super(name, actions);
    }
}
