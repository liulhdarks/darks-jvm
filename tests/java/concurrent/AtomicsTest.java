import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

// Atomic scalars, reference, int array and LongAdder under contention.
public class AtomicsTest {
    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new RuntimeException("atomics failed: " + what);
        }
    }

    public static void main(String[] args) throws Exception {
        final AtomicInteger ai = new AtomicInteger();
        Thread[] intWorkers = new Thread[8];
        for (int t = 0; t < intWorkers.length; t++) {
            intWorkers[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < 2000; i++) {
                        ai.incrementAndGet();
                    }
                }
            });
            intWorkers[t].start();
        }
        for (int t = 0; t < intWorkers.length; t++) {
            intWorkers[t].join();
        }
        check(ai.get() == 16000, "atomic int " + ai.get());
        check(ai.compareAndSet(16000, 16001), "atomic int CAS");
        check(ai.decrementAndGet() == 16000, "atomic int decrement");
        check(ai.getAndAdd(5) == 16000 && ai.get() == 16005, "atomic int getAndAdd");
        check(ai.addAndGet(-5) == 16000, "atomic int addAndGet");
        check(ai.getAndIncrement() == 16000 && ai.get() == 16001, "atomic int getAndIncrement");
        ai.set(16000);
        check(ai.longValue() == 16000L && ai.intValue() == 16000, "atomic int conversions");

        final AtomicLong al = new AtomicLong();
        Thread[] longWorkers = new Thread[4];
        for (int t = 0; t < longWorkers.length; t++) {
            longWorkers[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        al.addAndGet(1L);
                    }
                }
            });
            longWorkers[t].start();
        }
        for (int t = 0; t < longWorkers.length; t++) {
            longWorkers[t].join();
        }
        check(al.get() == 4000L, "atomic long " + al.get());
        check(al.compareAndSet(4000L, 4001L) && al.decrementAndGet() == 4000L,
              "atomic long CAS");

        final AtomicBoolean ab = new AtomicBoolean();
        check(!ab.get(), "atomic boolean initial");
        check(ab.compareAndSet(false, true), "atomic boolean CAS");
        check(!ab.compareAndSet(false, true), "atomic boolean CAS mismatch");
        check(ab.getAndSet(false), "atomic boolean getAndSet");
        check(!ab.get(), "atomic boolean final");

        final AtomicReference<String> ar = new AtomicReference<String>("a");
        check("a".equals(ar.get()), "atomic reference initial");
        check(ar.compareAndSet("a", "b"), "atomic reference CAS");
        check("b".equals(ar.getAndSet("c")), "atomic reference getAndSet");
        check("c".equals(ar.get()), "atomic reference final");
        check(!ar.compareAndSet("x", "y"), "atomic reference CAS mismatch");

        final AtomicIntegerArray array = new AtomicIntegerArray(4);
        Thread[] arrayWorkers = new Thread[4];
        for (int t = 0; t < arrayWorkers.length; t++) {
            final int index = t;
            arrayWorkers[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        array.incrementAndGet(index);
                    }
                }
            });
            arrayWorkers[t].start();
        }
        for (int t = 0; t < arrayWorkers.length; t++) {
            arrayWorkers[t].join();
        }
        int arraySum = 0;
        for (int i = 0; i < array.length(); i++) {
            check(array.get(i) == 1000, "array slot " + i);
            arraySum += array.get(i);
        }
        check(arraySum == 4000, "array sum " + arraySum);
        check(array.compareAndSet(0, 1000, 7), "array CAS");
        check(array.addAndGet(0, -7) == 0, "array addAndGet");
        boolean outOfBounds = false;
        try {
            array.get(4);
        } catch (IndexOutOfBoundsException expected) {
            outOfBounds = true;
        }
        check(outOfBounds, "array bounds");

        final LongAdder adder = new LongAdder();
        Thread[] adderWorkers = new Thread[8];
        for (int t = 0; t < adderWorkers.length; t++) {
            adderWorkers[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        adder.increment();
                    }
                }
            });
            adderWorkers[t].start();
        }
        for (int t = 0; t < adderWorkers.length; t++) {
            adderWorkers[t].join();
        }
        check(adder.sum() == 8000L, "adder sum " + adder.sum());
        adder.add(3L);
        adder.decrement();
        check(adder.sum() == 8002L, "adder add/decrement");
        long beforeReset = adder.sumThenReset();
        check(beforeReset == 8002L && adder.sum() == 0L, "adder sumThenReset");

        System.out.println("atomics ok checks=" + checks
                + " ai=16000 arr=4000 adder=8000");
    }
}
