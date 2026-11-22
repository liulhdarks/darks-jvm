package java.lang.management;

public class ThreadInfo
{
    private final long threadId;
    private final String threadName;
    private final Thread.State state;

    public ThreadInfo(long threadId, String threadName, Thread.State state)
    {
        this.threadId = threadId;
        this.threadName = threadName;
        this.state = state;
    }

    public long getThreadId()
    {
        return threadId;
    }

    public String getThreadName()
    {
        return threadName;
    }

    public Thread.State getThreadState()
    {
        return state;
    }

    public String toString()
    {
        return "ThreadInfo[" + threadName + "," + state + "]";
    }
}
