package java.security;

import java.util.Enumeration;

/**
 * Abstract container for permissions.  No permission checks are enforced by
 * this runtime.
 */
public abstract class PermissionCollection
{
    public abstract void add(Permission permission);

    public abstract boolean implies(Permission permission);

    public abstract Enumeration<Permission> elements();
}
