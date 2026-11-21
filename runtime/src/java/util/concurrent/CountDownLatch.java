package java.util.concurrent;

/**
 * A synchronization aid that lets threads wait until a count reaches zero.
 */
public class CountDownLatch
{
    private int count;

    public CountDownLatch(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = count;
    }

    public void await() throws InterruptedException
    {
        synchronized (this)
        {
            while (count > 0)
            {
                wait();
            }
        }
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        synchronized (this)
        {
            while (count > 0)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                wait(remaining);
            }
            return true;
        }
    }

    public void countDown()
    {
        synchronized (this)
        {
            if (count > 0)
            {
                count--;
                if (count == 0)
                {
                    notifyAll();
                }
            }
        }
    }

    public long getCount()
    {
        synchronized (this)
        {
            return count;
        }
    }

    public String toString()
    {
        return "CountDownLatch[count=" + getCount() + "]";
    }
}
