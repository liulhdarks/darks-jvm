package java.lang.reflect;

import java.security.BasicPermission;

/**
 * Permission for reflective operations.
 */
public final class ReflectPermission extends BasicPermission
{
    private static final long serialVersionUID = 7412737110241507485L;

    public ReflectPermission(String name)
    {
        super(name);
    }

    public ReflectPermission(String name, String actions)
    {
        super(name, actions);
    }
}
