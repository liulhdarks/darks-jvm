package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * A reentrant mutual exclusion lock.
 */
public interface Lock
{
    void lock();

    void lockInterruptibly() throws InterruptedException;

    boolean tryLock();

    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    void unlock();

    Condition newCondition();
}
