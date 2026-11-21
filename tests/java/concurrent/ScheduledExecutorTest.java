import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

// ScheduledThreadPoolExecutor: delayed, periodic and cancellable tasks.
public class ScheduledExecutorTest {
    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new RuntimeException("scheduled-executor failed: " + what);
        }
    }

    public static void main(String[] args) throws Exception {
        ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(2);

        ScheduledFuture<Integer> callable = sched.schedule(new Callable<Integer>() {
            public Integer call() {
                return Integer.valueOf(7);
            }
        }, 50, TimeUnit.MILLISECONDS);
        check(callable.get(5, TimeUnit.SECONDS).intValue() == 7, "scheduled callable");

        final CopyOnWriteArrayList<String> order = new CopyOnWriteArrayList<String>();
        sched.schedule(new Runnable() {
            public void run() {
                order.add("b");
            }
        }, 150, TimeUnit.MILLISECONDS);
        sched.schedule(new Runnable() {
            public void run() {
                order.add("a");
            }
        }, 30, TimeUnit.MILLISECONDS);
        Thread.sleep(350);
        check(order.size() == 2, "order count " + order.size());
        check("a".equals(order.get(0)) && "b".equals(order.get(1)), "delay order " + order);

        final AtomicInteger ticks = new AtomicInteger();
        ScheduledFuture<?> rate = sched.scheduleAtFixedRate(new Runnable() {
            public void run() {
                ticks.incrementAndGet();
            }
        }, 0L, 25L, TimeUnit.MILLISECONDS);
        Thread.sleep(180);
        rate.cancel(false);
        check(rate.isCancelled(), "fixed rate cancelled");
        int rateTicks = ticks.get();
        check(rateTicks >= 3 && rateTicks <= 20, "fixed rate ticks " + rateTicks);

        final AtomicInteger delayed = new AtomicInteger();
        ScheduledFuture<?> fixedDelay = sched.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                delayed.incrementAndGet();
            }
        }, 0L, 20L, TimeUnit.MILLISECONDS);
        Thread.sleep(150);
        fixedDelay.cancel(false);
        int delayTicks = delayed.get();
        check(delayTicks >= 2 && delayTicks <= 20, "fixed delay ticks " + delayTicks);

        ScheduledFuture<?> pending = sched.schedule(new Runnable() {
            public void run() {
            }
        }, 1000L, TimeUnit.MILLISECONDS);
        check(pending.cancel(false), "cancel pending");
        check(pending.isCancelled() && pending.isDone(), "cancel flags");
        boolean cancelledGet = false;
        try {
            pending.get();
        } catch (CancellationException expected) {
            cancelledGet = true;
        }
        check(cancelledGet, "get on cancelled");

        ScheduledFuture<Integer> late = sched.schedule(new Callable<Integer>() {
            public Integer call() {
                return Integer.valueOf(9);
            }
        }, 300L, TimeUnit.MILLISECONDS);
        boolean getTimedOut = false;
        try {
            late.get(20L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException expected) {
            getTimedOut = true;
        }
        check(getTimedOut, "scheduled get timeout");
        check(late.get(5, TimeUnit.SECONDS).intValue() == 9, "scheduled late value");

        sched.shutdown();
        check(sched.awaitTermination(10, TimeUnit.SECONDS), "scheduled termination");
        boolean rejected = false;
        try {
            sched.execute(new Runnable() {
                public void run() {
                }
            });
        } catch (RejectedExecutionException expected) {
            rejected = true;
        }
        check(rejected, "scheduled execute after shutdown");

        ScheduledExecutorService factoryPool = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> oneShot = factoryPool.schedule(new Runnable() {
            public void run() {
            }
        }, 10L, TimeUnit.MILLISECONDS);
        oneShot.get(5, TimeUnit.SECONDS);
        factoryPool.shutdown();
        check(factoryPool.awaitTermination(10, TimeUnit.SECONDS), "factory scheduled termination");

        System.out.println("scheduled-executor ok checks=" + checks
                + " order=ab cancel=true rate=" + rateTicks + " delay=" + delayTicks);
    }
}
