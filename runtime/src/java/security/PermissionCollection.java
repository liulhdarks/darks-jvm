package java.security;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class PermissionCollection
{
    private Permission[] permissions;

    public PermissionCollection()
    {
        this.permissions = new Permission[0];
    }

    public PermissionCollection(Permission[] permissions)
    {
        this.permissions = permissions == null ? new Permission[0] : permissions;
    }

    public void add(Permission permission)
    {
        Permission[] next = new Permission[permissions.length + 1];
        for (int i = 0; i < permissions.length; i++)
        {
            next[i] = permissions[i];
        }
        next[permissions.length] = permission;
        permissions = next;
    }

    public boolean implies(Permission permission)
    {
        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i] != null && permissions[i].implies(permission))
            {
                return true;
            }
        }
        return false;
    }

    public Enumeration<Permission> elements()
    {
        return new Elements(permissions);
    }

    public Permission[] toArray()
    {
        Permission[] copy = new Permission[permissions.length];
        for (int i = 0; i < permissions.length; i++)
        {
            copy[i] = permissions[i];
        }
        return copy;
    }

    private static final class Elements implements Enumeration<Permission>
    {
        private final Permission[] values;
        private int index;

        Elements(Permission[] values)
        {
            this.values = values;
        }

        public boolean hasMoreElements()
        {
            return index < values.length;
        }

        public Permission nextElement()
        {
            if (index >= values.length)
            {
                throw new NoSuchElementException();
            }
            return values[index++];
        }
    }
}
