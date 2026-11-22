import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Covers the concurrency primitives added for the Tomcat runtime: the
// LinkedBlockingDeque used by the async log buffer, the timer service used by
// Tomcat's background threads, Timer/TimerTask and the AQS-backed locks.
public class M3ConcurrencyTest
{
    private static int checks;
    private static int failures;

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    private static class Mutex extends AbstractQueuedSynchronizer
    {
        protected boolean tryAcquire(int arg)
        {
            return compareAndSetState(0, 1);
        }

        protected boolean tryRelease(int arg)
        {
            setState(0);
            return true;
        }

        void lock()
        {
            acquire(1);
        }

        void unlock()
        {
            release(1);
        }

        int state()
        {
            return getState();
        }
    }

    public static void main(String[] args) throws Exception
    {
        blockingDeque();
        timerService();
        scheduledExecutor();
        aqsMutex();
        readWriteLock();
        System.out.println("m3 concurrency checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("m3 concurrency: ok");
    }

    private static void blockingDeque() throws Exception
    {
        LinkedBlockingDeque<String> deque = new LinkedBlockingDeque<String>(2);
        deque.put("a");
        deque.put("b");
        check("deque full", !deque.offer("c"));
        check("deque take", "a".equals(deque.take()));
        check("deque offerFirst", deque.offerFirst("z"));
        check("deque size", deque.size() == 2);

        Iterator<String> iterator = deque.iterator();
        check("deque iterator first", "z".equals(iterator.next()));
        iterator.remove();
        check("deque iterator remove", !deque.contains("z") && deque.size() == 1);
        check("deque poll", "b".equals(deque.poll()));
        check("deque empty poll", deque.poll() == null);
        check("deque poll timeout", deque.poll(20L, TimeUnit.MILLISECONDS) == null);

        final LinkedBlockingDeque<String> shared = new LinkedBlockingDeque<String>(1);
        Thread producer = new Thread(new Runnable() {
            public void run()
            {
                try
                {
                    Thread.sleep(50L);
                    shared.put("late");
                }
                catch (InterruptedException interrupted)
                {
                }
            }
        });
        producer.start();
        long started = System.currentTimeMillis();
        String taken = shared.take();
        long waited = System.currentTimeMillis() - started;
        check("blocking take value", "late".equals(taken));
        check("blocking take waited", waited >= 30L);
        producer.join(3000L);

        LinkedBlockingDeque<String> stack = new LinkedBlockingDeque<String>();
        stack.push("one");
        stack.push("two");
        check("deque push/pop", "two".equals(stack.pop()) && "one".equals(stack.pop()));
        check("deque drain", stack.size() == 0);
    }

    private static void timerService() throws Exception
    {
        final AtomicInteger fired = new AtomicInteger();
        Timer timer = new Timer("m3-timer");
        timer.schedule(new TimerTask() {
            public void run()
            {
                fired.incrementAndGet();
            }
        }, 30L);
        waitFor(fired, 1, 3000L);
        check("timer one-shot", fired.get() == 1);

        final AtomicInteger ticks = new AtomicInteger();
        TimerTask periodic = new TimerTask() {
            public void run()
            {
                ticks.incrementAndGet();
            }
        };
        timer.scheduleAtFixedRate(periodic, 0L, 20L);
        waitFor(ticks, 2, 3000L);
        check("timer fixed rate", ticks.get() >= 2);
        check("timer cancel task", periodic.cancel());
        check("timer cancel twice", !periodic.cancel());
        timer.cancel();
    }

    private static void scheduledExecutor() throws Exception
    {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        ScheduledFuture<Integer> future = executor.schedule(new Callable<Integer>() {
            public Integer call()
            {
                return Integer.valueOf(7);
            }
        }, 40L, TimeUnit.MILLISECONDS);
        check("scheduled callable", future.get(5L, TimeUnit.SECONDS).intValue() == 7);

        final AtomicInteger ticks = new AtomicInteger();
        ScheduledFuture<?> rate = executor.scheduleAtFixedRate(new Runnable() {
            public void run()
            {
                ticks.incrementAndGet();
            }
        }, 0L, 20L, TimeUnit.MILLISECONDS);
        waitFor(ticks, 3, 3000L);
        check("scheduled fixed rate", ticks.get() >= 3);
        check("scheduled cancel", rate.cancel(false) && rate.isCancelled());
        int settled = ticks.get();
        Thread.sleep(80L);
        check("scheduled stopped", ticks.get() <= settled + 1);

        final AtomicInteger delayed = new AtomicInteger();
        executor.schedule(new Runnable() {
            public void run()
            {
                delayed.incrementAndGet();
            }
        }, 30L, TimeUnit.MILLISECONDS);
        waitFor(delayed, 1, 3000L);
        check("scheduled runnable", delayed.get() == 1);
        executor.shutdown();
    }

    private static void aqsMutex() throws Exception
    {
        final Mutex mutex = new Mutex();
        final AtomicInteger counter = new AtomicInteger();
        Runnable worker = new Runnable() {
            public void run()
            {
                int i = 0;
                while (i < 100)
                {
                    mutex.lock();
                    try
                    {
                        counter.incrementAndGet();
                    }
                    finally
                    {
                        mutex.unlock();
                    }
                    i = i + 1;
                }
            }
        };
        Thread first = new Thread(worker);
        Thread second = new Thread(worker);
        first.start();
        second.start();
        first.join(10000L);
        second.join(10000L);
        check("aqs mutex count", counter.get() == 200);

        Mutex tryLock = new Mutex();
        check("aqs tryAcquire", tryLock.tryAcquire(1));
        check("aqs tryAcquire held", !tryLock.tryAcquire(1));
        tryLock.unlock();
        check("aqs release", tryLock.tryAcquire(1));
        check("aqs state", tryLock.state() == 1);
    }

    private static void readWriteLock()
    {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        check("write locked", lock.isWriteLocked());
        check("no readers", lock.getReadLockCount() == 0);
        lock.writeLock().unlock();
        check("write unlocked", !lock.isWriteLocked());

        lock.readLock().lock();
        lock.readLock().lock();
        check("read lock count", lock.getReadLockCount() == 2);
        lock.readLock().unlock();
        check("read lock release", lock.getReadLockCount() == 1);
        lock.readLock().unlock();
        check("read lock released", lock.getReadLockCount() == 0);
    }

    private static void waitFor(AtomicInteger value, int target, long timeout)
        throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + timeout;
        while (value.get() < target && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(10L);
        }
    }
}
