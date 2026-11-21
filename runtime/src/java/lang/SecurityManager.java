package java.lang;

/**
 * Minimal security manager.  The runtime never installs one, so the check
 * methods are no-ops and {@link System#getSecurityManager()} returns null
 * unless code explicitly installs a manager.
 */
public class SecurityManager
{
    public SecurityManager()
    {
    }

    public boolean checkTopLevelWindow(Object window)
    {
        return true;
    }

    public void checkExit(int status)
    {
    }

    public void checkAccess(Thread thread)
    {
    }

    public void checkAccess(ThreadGroup group)
    {
    }

    public void checkRead(String file)
    {
    }

    public void checkRead(String file, Object context)
    {
    }

    public void checkWrite(String file)
    {
    }

    public void checkDelete(String file)
    {
    }

    public ThreadGroup getThreadGroup()
    {
        return Thread.currentThread().getThreadGroup();
    }
}
