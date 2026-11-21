package java.security;

/**
 * Privileged computation that may throw a checked exception.  This runtime
 * has no security manager, so the action is simply run.
 */
public interface PrivilegedExceptionAction<T>
{
    T run() throws Exception;
}
