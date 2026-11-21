import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

// ThreadPoolExecutor, Executors factories and Future behaviour.
public class ExecutorPoolTest {
    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new RuntimeException("executor-pool failed: " + what);
        }
    }

    public static void main(String[] args) throws Exception {
        ExecutorService fixed = Executors.newFixedThreadPool(4);
        Future[] futures = new Future[200];
        for (int i = 0; i < futures.length; i++) {
            final int value = i;
            futures[i] = fixed.submit(new Callable<Object>() {
                public Object call() {
                    return Integer.valueOf(value * 2);
                }
            });
        }
        long sum = 0;
        for (int i = 0; i < futures.length; i++) {
            sum += ((Integer) futures[i].get()).intValue();
        }
        check(sum == 39800, "fixed pool sum " + sum);

        Future slow = fixed.submit(new Callable<Object>() {
            public Object call() throws Exception {
                Thread.sleep(200);
                return Integer.valueOf(7);
            }
        });
        boolean timedOut = false;
        try {
            slow.get(10, TimeUnit.MILLISECONDS);
        } catch (TimeoutException expected) {
            timedOut = true;
        }
        check(timedOut, "future timeout");
        check(((Integer) slow.get(5, TimeUnit.SECONDS)).intValue() == 7, "slow value");

        Future callableResult = fixed.submit(new Runnable() {
            public void run() {
            }
        }, "done");
        check("done".equals(callableResult.get(5, TimeUnit.SECONDS)), "submit runnable result");

        fixed.shutdown();
        check(fixed.isShutdown(), "fixed shutdown flag");
        check(fixed.awaitTermination(10, TimeUnit.SECONDS), "fixed termination");
        check(fixed.isTerminated(), "fixed isTerminated");
        boolean rejected = false;
        try {
            fixed.execute(new Runnable() {
                public void run() {
                }
            });
        } catch (RejectedExecutionException expected) {
            rejected = true;
        }
        check(rejected, "execute after shutdown rejected");

        final AtomicInteger cachedRan = new AtomicInteger();
        final CountDownLatch cachedRelease = new CountDownLatch(1);
        ExecutorService cached = Executors.newCachedThreadPool();
        Future[] cachedFutures = new Future[20];
        for (int i = 0; i < cachedFutures.length; i++) {
            cachedFutures[i] = cached.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    cachedRelease.await();
                    cachedRan.incrementAndGet();
                    return null;
                }
            });
        }
        cachedRelease.countDown();
        for (int i = 0; i < cachedFutures.length; i++) {
            cachedFutures[i].get(10, TimeUnit.SECONDS);
        }
        check(cachedRan.get() == 20, "cached ran " + cachedRan.get());
        cached.shutdown();
        check(cached.awaitTermination(10, TimeUnit.SECONDS), "cached termination");

        final CountDownLatch hold = new CountDownLatch(1);
        ThreadPoolExecutor bounded = new ThreadPoolExecutor(
                1, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(2));
        Runnable blocker = new Runnable() {
            public void run() {
                try {
                    hold.await();
                } catch (InterruptedException ignored) {
                }
            }
        };
        bounded.execute(blocker);
        bounded.execute(blocker);
        bounded.execute(blocker);
        bounded.execute(blocker);
        boolean boundedRejected = false;
        try {
            bounded.execute(blocker);
        } catch (RejectedExecutionException expected) {
            boundedRejected = true;
        }
        check(boundedRejected, "bounded pool rejection");
        check(bounded.getPoolSize() <= 2, "bounded pool size");
        hold.countDown();
        bounded.shutdown();
        check(bounded.awaitTermination(10, TimeUnit.SECONDS), "bounded termination");

        ThreadPoolExecutor prestarted = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        check(prestarted.prestartAllCoreThreads() == 2, "prestart core threads");
        check(prestarted.getPoolSize() == 2, "pool size after prestart");
        prestarted.shutdown();
        check(prestarted.awaitTermination(10, TimeUnit.SECONDS), "prestarted termination");

        final CountDownLatch stuck = new CountDownLatch(1);
        ThreadPoolExecutor draining = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        draining.execute(new Runnable() {
            public void run() {
                try {
                    stuck.await();
                } catch (InterruptedException ignored) {
                }
            }
        });
        draining.execute(new Runnable() {
            public void run() {
            }
        });
        draining.execute(new Runnable() {
            public void run() {
            }
        });
        List<Runnable> dropped = draining.shutdownNow();
        check(dropped.size() >= 1, "shutdownNow dropped " + dropped.size());
        stuck.countDown();
        check(draining.awaitTermination(10, TimeUnit.SECONDS), "draining termination");

        System.out.println("executor-pool ok checks=" + checks
                + " fixed=39800 rejected=true cached=20");
    }
}
