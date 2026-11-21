package java.security;

/**
 * Computation performed with privilege checks bypassed.  This runtime has no
 * security manager, so a privileged action is simply run.
 */
public interface PrivilegedAction<T>
{
    T run();
}
