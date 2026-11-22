package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.HashMap;

/**
 * Fairness-free reentrant read/write lock. Blocking is implemented with short
 * sleeps so the coroutine scheduler keeps control of the thread.
 */
public class ReentrantReadWriteLock implements ReadWriteLock
{
    private static class State
    {
        int readers;
        Thread writer;
        int writeCount;
        int waitingWriters;
    }

    private final State state = new State();
    private final HashMap<Thread, Integer> readHolds = new HashMap<Thread, Integer>();
    private final ReadLock readLock = new ReadLock(this);
    private final WriteLock writeLock = new WriteLock(this);

    public ReentrantReadWriteLock()
    {
    }

    public ReentrantReadWriteLock(boolean fair)
    {
    }

    public ReadLock readLock()
    {
        return readLock;
    }

    public WriteLock writeLock()
    {
        return writeLock;
    }

    public final int getReadLockCount()
    {
        synchronized (state)
        {
            return state.readers;
        }
    }

    public final boolean isWriteLocked()
    {
        synchronized (state)
        {
            return state.writeCount > 0;
        }
    }

    public final Thread getOwner()
    {
        synchronized (state)
        {
            return state.writeCount > 0 ? state.writer : null;
        }
    }

    private boolean tryAcquireRead()
    {
        Thread current = Thread.currentThread();
        synchronized (state)
        {
            if (state.writer != null && state.writer != current)
            {
                return false;
            }
            if (state.waitingWriters > 0 && state.writer != current)
            {
                return false;
            }
            state.readers++;
            Integer held = readHolds.get(current);
            readHolds.put(current, Integer.valueOf(held == null ? 1 : held.intValue() + 1));
            return true;
        }
    }

    private boolean tryAcquireWrite()
    {
        Thread current = Thread.currentThread();
        synchronized (state)
        {
            if (state.writer == current)
            {
                state.writeCount++;
                return true;
            }
            if (state.writer != null || state.readers > 0)
            {
                return false;
            }
            state.writer = current;
            state.writeCount = 1;
            return true;
        }
    }

    private void releaseRead()
    {
        Thread current = Thread.currentThread();
        synchronized (state)
        {
            Integer held = readHolds.get(current);
            if (held == null)
            {
                throw new IllegalMonitorStateException("read lock not held");
            }
            if (held.intValue() <= 1)
            {
                readHolds.remove(current);
            }
            else
            {
                readHolds.put(current, Integer.valueOf(held.intValue() - 1));
            }
            state.readers--;
        }
    }

    private void releaseWrite()
    {
        Thread current = Thread.currentThread();
        synchronized (state)
        {
            if (state.writer != current)
            {
                throw new IllegalMonitorStateException("write lock not held");
            }
            state.writeCount--;
            if (state.writeCount == 0)
            {
                state.writer = null;
            }
        }
    }

    private static void sleepShort()
    {
        try
        {
            Thread.sleep(1);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public static class ReadLock implements Lock
    {
        private final ReentrantReadWriteLock lock;

        ReadLock(ReentrantReadWriteLock lock)
        {
            this.lock = lock;
        }

        public void lock()
        {
            while (!lock.tryAcquireRead())
            {
                sleepShort();
            }
        }

        public void lockInterruptibly() throws InterruptedException
        {
            lock();
        }

        public boolean tryLock()
        {
            return lock.tryAcquireRead();
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
        {
            long deadline = System.currentTimeMillis() + unit.toMillis(time);
            while (true)
            {
                if (lock.tryAcquireRead())
                {
                    return true;
                }
                if (System.currentTimeMillis() >= deadline)
                {
                    return false;
                }
                Thread.sleep(1);
            }
        }

        public void unlock()
        {
            lock.releaseRead();
        }

        public Condition newCondition()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class WriteLock implements Lock
    {
        private final ReentrantReadWriteLock lock;

        WriteLock(ReentrantReadWriteLock lock)
        {
            this.lock = lock;
        }

        public void lock()
        {
            synchronized (lock.state)
            {
                lock.state.waitingWriters++;
            }
            try
            {
                while (!lock.tryAcquireWrite())
                {
                    sleepShort();
                }
            }
            finally
            {
                synchronized (lock.state)
                {
                    lock.state.waitingWriters--;
                }
            }
        }

        public void lockInterruptibly() throws InterruptedException
        {
            lock();
        }

        public boolean tryLock()
        {
            return lock.tryAcquireWrite();
        }

        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
        {
            long deadline = System.currentTimeMillis() + unit.toMillis(time);
            while (true)
            {
                if (lock.tryAcquireWrite())
                {
                    return true;
                }
                if (System.currentTimeMillis() >= deadline)
                {
                    return false;
                }
                Thread.sleep(1);
            }
        }

        public void unlock()
        {
            lock.releaseWrite();
        }

        public Condition newCondition()
        {
            throw new UnsupportedOperationException();
        }
    }
}
