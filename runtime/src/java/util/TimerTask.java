package java.util;

public abstract class TimerTask implements Runnable
{
    long nextTime;
    long period;
    boolean fixedRate;
    boolean cancelled;
    boolean scheduled;

    protected TimerTask()
    {
    }

    public abstract void run();

    public boolean cancel()
    {
        boolean wasScheduled = scheduled && !cancelled;
        cancelled = true;
        scheduled = false;
        return wasScheduled;
    }

    public long scheduledExecutionTime()
    {
        return nextTime;
    }
}
