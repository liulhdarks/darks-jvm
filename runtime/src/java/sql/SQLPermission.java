package java.sql;

import java.security.Permission;

public class SQLPermission extends Permission
{
    public SQLPermission(String name)
    {
        super(name);
    }

    public SQLPermission(String name, String actions)
    {
        super(name);
    }

    public boolean implies(Permission permission)
    {
        return equals(permission);
    }

    public String getActions()
    {
        return "";
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof SQLPermission))
        {
            return false;
        }
        SQLPermission o = (SQLPermission)other;
        return getName().equals(o.getName()) && getActions().equals(o.getActions());
    }

    public int hashCode()
    {
        return getName().hashCode();
    }
}
