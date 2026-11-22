package java.util.concurrent.locks;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractQueuedSynchronizer
{
    private volatile int state;
    private int waiters;
    private Thread exclusiveOwnerThread;

    protected AbstractQueuedSynchronizer()
    {
    }

    protected final void setExclusiveOwnerThread(Thread thread)
    {
        exclusiveOwnerThread = thread;
    }

    protected final Thread getExclusiveOwnerThread()
    {
        return exclusiveOwnerThread;
    }

    protected final int getState()
    {
        return state;
    }

    protected final void setState(int newState)
    {
        state = newState;
    }

    protected final boolean compareAndSetState(int expect, int update)
    {
        synchronized (this)
        {
            if (state != expect)
            {
                return false;
            }
            state = update;
            return true;
        }
    }

    protected boolean tryAcquire(int arg)
    {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int arg)
    {
        throw new UnsupportedOperationException();
    }

    protected int tryAcquireShared(int arg)
    {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int arg)
    {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively()
    {
        throw new UnsupportedOperationException();
    }

    private void await(long nanos)
    {
        synchronized (this)
        {
            waiters = waiters + 1;
        }
        try
        {
            if (nanos < 0)
            {
                Thread.sleep(1);
            }
            else if (nanos > 0)
            {
                long millis = nanos / 1000000L;
                Thread.sleep(millis > 0 ? millis : 1);
            }
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            synchronized (this)
            {
                waiters = waiters - 1;
            }
        }
    }

    public final void acquire(int arg)
    {
        while (!tryAcquire(arg))
        {
            await(-1);
        }
    }

    public final void acquireInterruptibly(int arg) throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        while (!tryAcquire(arg))
        {
            await(-1);
            if (Thread.interrupted())
            {
                throw new InterruptedException();
            }
        }
    }

    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
        throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        if (tryAcquire(arg))
        {
            return true;
        }
        long deadline = System.currentTimeMillis() + nanosTimeout / 1000000L;
        while (System.currentTimeMillis() < deadline)
        {
            Thread.sleep(1);
            if (tryAcquire(arg))
            {
                return true;
            }
            if (Thread.interrupted())
            {
                throw new InterruptedException();
            }
        }
        return false;
    }

    public final boolean release(int arg)
    {
        if (!tryRelease(arg))
        {
            return false;
        }
        synchronized (this)
        {
            notifyAll();
        }
        return true;
    }

    public final void acquireShared(int arg)
    {
        while (tryAcquireShared(arg) < 0)
        {
            await(-1);
        }
    }

    public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        while (tryAcquireShared(arg) < 0)
        {
            await(-1);
            if (Thread.interrupted())
            {
                throw new InterruptedException();
            }
        }
    }

    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
        throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) >= 0)
        {
            return true;
        }
        long deadline = System.currentTimeMillis() + nanosTimeout / 1000000L;
        while (System.currentTimeMillis() < deadline)
        {
            Thread.sleep(1);
            if (tryAcquireShared(arg) >= 0)
            {
                return true;
            }
            if (Thread.interrupted())
            {
                throw new InterruptedException();
            }
        }
        return false;
    }

    public final boolean releaseShared(int arg)
    {
        if (!tryReleaseShared(arg))
        {
            return false;
        }
        synchronized (this)
        {
            notifyAll();
        }
        return true;
    }

    public final boolean hasQueuedThreads()
    {
        synchronized (this)
        {
            return waiters > 0;
        }
    }

    public final boolean hasContended()
    {
        return false;
    }

    public final Thread getFirstQueuedThread()
    {
        return null;
    }

    public final boolean isQueued(Thread thread)
    {
        return false;
    }

    public final Collection<Thread> getQueuedThreads()
    {
        synchronized (this)
        {
            return new ArrayList<Thread>();
        }
    }

    public final Collection<Thread> getExclusiveQueuedThreads()
    {
        return getQueuedThreads();
    }

    public final Collection<Thread> getSharedQueuedThreads()
    {
        return getQueuedThreads();
    }

    public final int getQueueLength()
    {
        synchronized (this)
        {
            return waiters;
        }
    }
}
