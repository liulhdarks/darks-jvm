package java.lang;

public class ThreadGroup
{
    private final String name;
    private final ThreadGroup parent;

    public ThreadGroup(String name)
    {
        this(null, name);
    }

    public ThreadGroup(ThreadGroup parent, String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        this.name = name;
        this.parent = parent;
    }

    public final String getName()
    {
        return name;
    }

    public final ThreadGroup getParent()
    {
        return parent;
    }

    public final boolean parentOf(ThreadGroup group)
    {
        ThreadGroup current = group;
        while (current != null)
        {
            if (current == this)
            {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    public int activeCount()
    {
        return Thread.currentThread().getThreadGroup() == this ? 1 : 0;
    }

    public int activeGroupCount()
    {
        return 0;
    }

    public boolean isDaemon()
    {
        return false;
    }

    public void setDaemon(boolean daemon)
    {
    }

    public int getMaxPriority()
    {
        return Thread.MAX_PRIORITY;
    }

    public String toString()
    {
        return "java.lang.ThreadGroup[name=" + name + ",maxpri=" + getMaxPriority() + "]";
    }
}
