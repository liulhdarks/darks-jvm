import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// ReentrantLock, Condition, CountDownLatch and Semaphore.
public class LocksTest {
    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new RuntimeException("locks failed: " + what);
        }
    }

    static void bumpMaximum(AtomicInteger maximum, int candidate) {
        while (true) {
            int current = maximum.get();
            if (candidate <= current || maximum.compareAndSet(current, candidate)) {
                return;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        final int[] counter = new int[1];
        Thread[] workers = new Thread[4];
        for (int t = 0; t < workers.length; t++) {
            workers[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < 500; i++) {
                        lock.lock();
                        try {
                            counter[0]++;
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            });
            workers[t].start();
        }
        for (int t = 0; t < workers.length; t++) {
            workers[t].join();
        }
        check(counter[0] == 2000, "lock counter " + counter[0]);

        lock.lock();
        check(lock.isLocked() && lock.isHeldByCurrentThread(), "lock held");
        check(lock.getHoldCount() == 1, "hold count 1");
        lock.lock();
        check(lock.getHoldCount() == 2, "hold count 2");
        lock.unlock();
        check(lock.getHoldCount() == 1, "hold count back to 1");
        lock.unlock();
        check(!lock.isLocked(), "lock released");

        lock.lock();
        final AtomicInteger tryLockResult = new AtomicInteger();
        Thread contender = new Thread(new Runnable() {
            public void run() {
                if (lock.tryLock()) {
                    tryLockResult.set(1);
                    lock.unlock();
                } else {
                    tryLockResult.set(2);
                }
            }
        });
        contender.start();
        contender.join(5000);
        check(tryLockResult.get() == 2, "tryLock fails while held");
        lock.unlock();

        lock.lock();
        final AtomicInteger timedTry = new AtomicInteger();
        final long[] timedElapsed = new long[1];
        Thread timedContender = new Thread(new Runnable() {
            public void run() {
                long start = System.currentTimeMillis();
                try {
                    if (lock.tryLock(80, TimeUnit.MILLISECONDS)) {
                        timedTry.set(1);
                        lock.unlock();
                    } else {
                        timedTry.set(2);
                    }
                } catch (InterruptedException interrupted) {
                    timedTry.set(3);
                }
                timedElapsed[0] = System.currentTimeMillis() - start;
            }
        });
        timedContender.start();
        timedContender.join(5000);
        check(timedTry.get() == 2, "timed tryLock fails");
        check(timedElapsed[0] >= 60, "timed tryLock waited " + timedElapsed[0]);
        lock.unlock();

        lock.lock();
        final AtomicInteger interruptible = new AtomicInteger();
        Thread blockedLock = new Thread(new Runnable() {
            public void run() {
                try {
                    lock.lockInterruptibly();
                    interruptible.set(1);
                    lock.unlock();
                } catch (InterruptedException expected) {
                    interruptible.set(2);
                }
            }
        });
        blockedLock.start();
        Thread.sleep(40);
        blockedLock.interrupt();
        blockedLock.join(5000);
        check(interruptible.get() == 2, "lockInterruptibly interrupted");
        lock.unlock();

        final ReentrantLock conditionLock = new ReentrantLock();
        final Condition ready = conditionLock.newCondition();
        final boolean[] flag = new boolean[1];
        final AtomicInteger awakened = new AtomicInteger();
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                conditionLock.lock();
                try {
                    while (!flag[0]) {
                        ready.await();
                    }
                    awakened.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    awakened.set(-1);
                } finally {
                    conditionLock.unlock();
                }
            }
        });
        waiter.start();
        Thread.sleep(30);
        conditionLock.lock();
        try {
            flag[0] = true;
            ready.signal();
        } finally {
            conditionLock.unlock();
        }
        waiter.join(5000);
        check(awakened.get() == 1, "condition signaled");

        conditionLock.lock();
        boolean timedOut;
        try {
            timedOut = !ready.await(30, TimeUnit.MILLISECONDS);
        } finally {
            conditionLock.unlock();
        }
        check(timedOut, "condition timed await");

        final ReentrantLock allLock = new ReentrantLock();
        final Condition allReady = allLock.newCondition();
        final int[] allFlag = new int[1];
        final AtomicInteger allAwake = new AtomicInteger();
        Thread[] allWaiters = new Thread[3];
        for (int w = 0; w < allWaiters.length; w++) {
            allWaiters[w] = new Thread(new Runnable() {
                public void run() {
                    allLock.lock();
                    try {
                        while (allFlag[0] == 0) {
                            allReady.await();
                        }
                        allAwake.incrementAndGet();
                    } catch (InterruptedException interrupted) {
                        allAwake.set(-1);
                    } finally {
                        allLock.unlock();
                    }
                }
            });
            allWaiters[w].start();
        }
        Thread.sleep(30);
        allLock.lock();
        try {
            allFlag[0] = 1;
            allReady.signalAll();
        } finally {
            allLock.unlock();
        }
        for (int w = 0; w < allWaiters.length; w++) {
            allWaiters[w].join(5000);
        }
        check(allAwake.get() == 3, "signalAll woke " + allAwake.get());

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch doneGate = new CountDownLatch(4);
        final AtomicInteger latched = new AtomicInteger();
        Thread[] latchedWorkers = new Thread[4];
        for (int t = 0; t < latchedWorkers.length; t++) {
            latchedWorkers[t] = new Thread(new Runnable() {
                public void run() {
                    try {
                        startGate.await();
                        latched.incrementAndGet();
                        doneGate.countDown();
                    } catch (InterruptedException interrupted) {
                        latched.set(-1);
                    }
                }
            });
            latchedWorkers[t].start();
        }
        startGate.countDown();
        check(doneGate.await(5, TimeUnit.SECONDS), "latch done");
        check(latched.get() == 4, "latch workers " + latched.get());
        check(doneGate.await(1, TimeUnit.MILLISECONDS), "latch already open");

        final Semaphore semaphore = new Semaphore(2);
        final AtomicInteger inside = new AtomicInteger();
        final AtomicInteger maxInside = new AtomicInteger();
        final AtomicInteger acquired = new AtomicInteger();
        Thread[] semaphoreWorkers = new Thread[4];
        for (int t = 0; t < semaphoreWorkers.length; t++) {
            semaphoreWorkers[t] = new Thread(new Runnable() {
                public void run() {
                    try {
                        semaphore.acquire();
                        int current = inside.incrementAndGet();
                        bumpMaximum(maxInside, current);
                        Thread.sleep(20);
                        inside.decrementAndGet();
                        acquired.incrementAndGet();
                        semaphore.release();
                    } catch (InterruptedException interrupted) {
                        acquired.set(-1);
                    }
                }
            });
            semaphoreWorkers[t].start();
        }
        for (int t = 0; t < semaphoreWorkers.length; t++) {
            semaphoreWorkers[t].join(5000);
        }
        check(acquired.get() == 4, "semaphore acquired " + acquired.get());
        check(maxInside.get() <= 2, "semaphore limit " + maxInside.get());
        check(semaphore.availablePermits() == 2, "semaphore permits restored");

        final Semaphore emptySemaphore = new Semaphore(0);
        long tryStart = System.currentTimeMillis();
        check(!emptySemaphore.tryAcquire(50, TimeUnit.MILLISECONDS), "semaphore tryAcquire timeout");
        check(System.currentTimeMillis() - tryStart >= 40, "semaphore tryAcquire waited");

        final Semaphore interruptSemaphore = new Semaphore(0);
        final AtomicInteger acquireInterrupted = new AtomicInteger();
        Thread blockedAcquire = new Thread(new Runnable() {
            public void run() {
                try {
                    interruptSemaphore.acquire();
                } catch (InterruptedException expected) {
                    acquireInterrupted.incrementAndGet();
                }
            }
        });
        blockedAcquire.start();
        Thread.sleep(40);
        blockedAcquire.interrupt();
        blockedAcquire.join(5000);
        check(acquireInterrupted.get() == 1, "semaphore acquire interrupted");

        System.out.println("locks ok checks=" + checks
                + " lock=2000 cond=true latch=true sem=2");
    }
}
