package java.util.concurrent.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A reentrant mutual exclusion lock implemented on top of the VM's
 * object monitors.
 *
 * The lock state (owner thread and hold count) is guarded by the lock
 * object's monitor.  Condition waiters park on a separate monitor and use a
 * generation counter so a signal racing with a waiter's release is never
 * lost.  Fairness is not implemented; the fair constructor argument is
 * accepted and ignored.
 */
public class ReentrantLock implements Lock
{
    private Thread owner;
    private int holds;

    public ReentrantLock()
    {
    }

    public ReentrantLock(boolean fair)
    {
    }

    public void lock()
    {
        boolean interrupted = false;
        synchronized (this)
        {
            while (owner != null && owner != Thread.currentThread())
            {
                try
                {
                    wait();
                }
                catch (InterruptedException interruptedWait)
                {
                    interrupted = true;
                }
            }
            owner = Thread.currentThread();
            holds++;
        }
        if (interrupted)
        {
            Thread.currentThread().interrupt();
        }
    }

    public void lockInterruptibly() throws InterruptedException
    {
        synchronized (this)
        {
            while (owner != null && owner != Thread.currentThread())
            {
                wait();
            }
            owner = Thread.currentThread();
            holds++;
        }
    }

    public boolean tryLock()
    {
        synchronized (this)
        {
            Thread self = Thread.currentThread();
            if (owner == null || owner == self)
            {
                owner = self;
                holds++;
                return true;
            }
            return false;
        }
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + unit.toMillis(time);
        synchronized (this)
        {
            Thread self = Thread.currentThread();
            while (owner != null && owner != self)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                wait(remaining);
            }
            owner = self;
            holds++;
            return true;
        }
    }

    public void unlock()
    {
        synchronized (this)
        {
            if (owner != Thread.currentThread())
            {
                throw new IllegalMonitorStateException("caller does not hold the lock");
            }
            holds--;
            if (holds == 0)
            {
                owner = null;
                notifyAll();
            }
        }
    }

    public Condition newCondition()
    {
        return new ConditionObject();
    }

    public boolean isLocked()
    {
        synchronized (this)
        {
            return owner != null;
        }
    }

    public boolean isHeldByCurrentThread()
    {
        synchronized (this)
        {
            return owner == Thread.currentThread();
        }
    }

    public int getHoldCount()
    {
        synchronized (this)
        {
            return owner == Thread.currentThread() ? holds : 0;
        }
    }

    public boolean isFair()
    {
        return false;
    }

    public boolean hasQueuedThreads()
    {
        return false;
    }

    public int getQueueLength()
    {
        return 0;
    }

    private final class ConditionObject implements Condition
    {
        private long generation;

        private void checkOwner()
        {
            if (owner != Thread.currentThread())
            {
                throw new IllegalMonitorStateException("caller does not hold the lock");
            }
        }

        private long releaseLockLocked()
        {
            checkOwner();
            holds = 0;
            owner = null;
            ReentrantLock.this.notifyAll();
            synchronized (this)
            {
                return generation;
            }
        }

        private void reacquireLocked(int savedHolds, boolean restoreInterrupt)
        {
            boolean interrupted = false;
            synchronized (ReentrantLock.this)
            {
                while (owner != null && owner != Thread.currentThread())
                {
                    try
                    {
                        ReentrantLock.this.wait();
                    }
                    catch (InterruptedException interruptedWait)
                    {
                        interrupted = true;
                    }
                }
                owner = Thread.currentThread();
                holds = savedHolds;
            }
            if (interrupted && restoreInterrupt)
            {
                Thread.currentThread().interrupt();
            }
        }

        public void await() throws InterruptedException
        {
            boolean interrupted = false;
            int saved;
            long captured;
            synchronized (ReentrantLock.this)
            {
                saved = holds;
                captured = releaseLockLocked();
            }
            synchronized (this)
            {
                while (generation == captured && !interrupted)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException interruptedWait)
                    {
                        interrupted = true;
                    }
                }
            }
            reacquireLocked(saved, false);
            if (interrupted)
            {
                throw new InterruptedException();
            }
        }

        public void awaitUninterruptibly()
        {
            int saved;
            long captured;
            synchronized (ReentrantLock.this)
            {
                saved = holds;
                captured = releaseLockLocked();
            }
            synchronized (this)
            {
                while (generation == captured)
                {
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
            reacquireLocked(saved, true);
        }

        public long awaitNanos(long nanosTimeout) throws InterruptedException
        {
            long deadline = System.nanoTime() + nanosTimeout;
            boolean interrupted = false;
            int saved;
            long captured;
            synchronized (ReentrantLock.this)
            {
                saved = holds;
                captured = releaseLockLocked();
            }
            synchronized (this)
            {
                while (generation == captured)
                {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0)
                    {
                        break;
                    }
                    try
                    {
                        wait(remaining / 1000000L + 1);
                    }
                    catch (InterruptedException interruptedWait)
                    {
                        interrupted = true;
                        break;
                    }
                }
            }
            reacquireLocked(saved, false);
            if (interrupted)
            {
                throw new InterruptedException();
            }
            return deadline - System.nanoTime();
        }

        public boolean await(long time, TimeUnit unit) throws InterruptedException
        {
            long deadline = System.currentTimeMillis() + unit.toMillis(time);
            boolean interrupted = false;
            boolean signaled = true;
            int saved;
            long captured;
            synchronized (ReentrantLock.this)
            {
                saved = holds;
                captured = releaseLockLocked();
            }
            synchronized (this)
            {
                while (generation == captured)
                {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0)
                    {
                        signaled = false;
                        break;
                    }
                    try
                    {
                        wait(remaining);
                    }
                    catch (InterruptedException interruptedWait)
                    {
                        interrupted = true;
                        break;
                    }
                }
            }
            reacquireLocked(saved, false);
            if (interrupted)
            {
                throw new InterruptedException();
            }
            return signaled;
        }

        public boolean awaitUntil(Date deadline) throws InterruptedException
        {
            long remaining = deadline.getTime() - System.currentTimeMillis();
            if (remaining <= 0)
            {
                return false;
            }
            return await(remaining, TimeUnit.MILLISECONDS);
        }

        public void signal()
        {
            synchronized (ReentrantLock.this)
            {
                checkOwner();
                synchronized (this)
                {
                    generation++;
                    notify();
                }
            }
        }

        public void signalAll()
        {
            synchronized (ReentrantLock.this)
            {
                checkOwner();
                synchronized (this)
                {
                    generation++;
                    notifyAll();
                }
            }
        }
    }

    public String toString()
    {
        synchronized (this)
        {
            return "ReentrantLock[owner=" + owner + ", holds=" + holds + "]";
        }
    }
}
