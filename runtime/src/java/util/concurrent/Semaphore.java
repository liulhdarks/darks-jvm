package java.util.concurrent;

/**
 * A counting semaphore.  The fair flag is accepted and ignored.
 */
public class Semaphore
{
    private int permits;

    public Semaphore(int permits)
    {
        if (permits < 0)
        {
            throw new IllegalArgumentException("permits < 0");
        }
        this.permits = permits;
    }

    public Semaphore(int permits, boolean fair)
    {
        this(permits);
    }

    public void acquire() throws InterruptedException
    {
        acquire(1);
    }

    public void acquire(int count) throws InterruptedException
    {
        if (count <= 0)
        {
            throw new IllegalArgumentException("count <= 0");
        }
        synchronized (this)
        {
            while (permits < count)
            {
                wait();
            }
            permits -= count;
        }
    }

    public void acquireUninterruptibly()
    {
        acquireUninterruptibly(1);
    }

    public void acquireUninterruptibly(int count)
    {
        synchronized (this)
        {
            while (permits < count)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException ignored)
                {
                }
            }
            permits -= count;
        }
    }

    public boolean tryAcquire()
    {
        synchronized (this)
        {
            if (permits > 0)
            {
                permits--;
                return true;
            }
            return false;
        }
    }

    public boolean tryAcquire(int count)
    {
        synchronized (this)
        {
            if (permits >= count)
            {
                permits -= count;
                return true;
            }
            return false;
        }
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException
    {
        return tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int count, long timeout, TimeUnit unit)
        throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        synchronized (this)
        {
            while (permits < count)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                wait(remaining);
            }
            permits -= count;
            return true;
        }
    }

    public void release()
    {
        release(1);
    }

    public void release(int count)
    {
        if (count <= 0)
        {
            throw new IllegalArgumentException("count <= 0");
        }
        synchronized (this)
        {
            permits += count;
            notifyAll();
        }
    }

    public int availablePermits()
    {
        synchronized (this)
        {
            return permits;
        }
    }

    public int drainPermits()
    {
        synchronized (this)
        {
            int drained = permits;
            permits = 0;
            return drained;
        }
    }

    public boolean isFair()
    {
        return false;
    }

    public String toString()
    {
        return "Semaphore[permits=" + availablePermits() + "]";
    }
}
