import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// ExecutorService / Future smoke test on top of virtual threads.
public class ExecutorServiceTest {
    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 10000;
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        Future[] futures = new Future[count];
        for (int i = 0; i < count; i++) {
            final int value = i;
            futures[i] = pool.submit(new Callable() {
                public Object call() {
                    return Integer.valueOf(value * 2);
                }
            });
        }
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += ((Integer) futures[i].get()).intValue();
        }
        long expected = (long) count * (count - 1);
        if (sum != expected) {
            throw new RuntimeException("sum=" + sum + " expected=" + expected);
        }

        Future slow = pool.submit(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(300);
                return Integer.valueOf(7);
            }
        });
        boolean timedOut = false;
        try {
            slow.get(10, TimeUnit.MILLISECONDS);
        } catch (TimeoutException expectedTimeout) {
            timedOut = true;
        }
        if (!timedOut) {
            throw new RuntimeException("get(timeout) did not time out");
        }
        int slowValue = ((Integer) slow.get(5, TimeUnit.SECONDS)).intValue();
        if (slowValue != 7) {
            throw new RuntimeException("slow value=" + slowValue);
        }

        Future cancellable = pool.submit(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(5000);
                return Integer.valueOf(1);
            }
        });
        boolean cancelled = cancellable.cancel(true);
        if (!cancelled || !cancellable.isCancelled()) {
            throw new RuntimeException("cancel failed");
        }
        boolean cancelledGet = false;
        try {
            cancellable.get();
        } catch (java.util.concurrent.CancellationException expectedCancel) {
            cancelledGet = true;
        }
        if (!cancelledGet) {
            throw new RuntimeException("get() on cancelled task did not throw");
        }

        pool.shutdown();
        boolean terminated = pool.awaitTermination(10, TimeUnit.SECONDS);
        if (!terminated || !pool.isShutdown() || !pool.isTerminated()) {
            throw new RuntimeException("executor did not terminate");
        }
        boolean rejected = false;
        try {
            pool.execute(new Runnable() {
                public void run() {
                }
            });
        } catch (RejectedExecutionException expectedReject) {
            rejected = true;
        }
        if (!rejected) {
            throw new RuntimeException("execute() after shutdown was accepted");
        }

        System.out.println("executor-service ok n=" + count + " sum=" + sum
                + " timeout=" + timedOut + " cancel=" + cancelled
                + " terminated=" + terminated + " rejected=" + rejected);
    }
}
