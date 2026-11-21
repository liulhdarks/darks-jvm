import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// BlockingQueue behaviour: FIFO order, bounded capacity, timed operations,
// interruption and handoff.
public class BlockingQueueTest {
    static int checks;

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new RuntimeException("blocking-queue failed: " + what);
        }
    }

    public static void main(String[] args) throws Exception {
        final ArrayBlockingQueue<Integer> abq = new ArrayBlockingQueue<Integer>(2);
        check(abq.offer(Integer.valueOf(1)), "abq offer 1");
        check(abq.offer(Integer.valueOf(2)), "abq offer 2");
        check(!abq.offer(Integer.valueOf(3)), "abq offer when full");
        check(abq.remainingCapacity() == 0, "abq remaining capacity");
        check(abq.take().intValue() == 1, "abq take 1");
        check(abq.peek().intValue() == 2, "abq peek");
        long timeoutStart = System.currentTimeMillis();
        check(abq.poll(40, TimeUnit.MILLISECONDS) != null, "abq poll value");
        check(abq.poll(40, TimeUnit.MILLISECONDS) == null, "abq poll timeout");
        check(System.currentTimeMillis() - timeoutStart >= 30, "abq poll waited");

        check(abq.offer(Integer.valueOf(10)) && abq.offer(Integer.valueOf(11)),
              "abq refill");
        long offerStart = System.currentTimeMillis();
        check(!abq.offer(Integer.valueOf(12), 40, TimeUnit.MILLISECONDS),
              "abq timed offer full");
        check(System.currentTimeMillis() - offerStart >= 30, "abq offer waited");
        check(abq.size() == 2, "abq size after timed offer");
        abq.clear();
        check(abq.isEmpty(), "abq clear");

        final ArrayBlockingQueue<Integer> transfer = new ArrayBlockingQueue<Integer>(2);
        final AtomicInteger producerFailure = new AtomicInteger();
        Thread producer = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int i = 0; i < 5; i++) {
                        transfer.put(Integer.valueOf(i));
                    }
                } catch (InterruptedException interrupted) {
                    producerFailure.incrementAndGet();
                }
            }
        });
        producer.start();
        StringBuilder order = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            order.append(transfer.take().intValue());
            if (i < 4) {
                order.append(',');
            }
        }
        producer.join(5000);
        check(producerFailure.get() == 0, "producer interrupted");
        check("0,1,2,3,4".equals(order.toString()), "abq FIFO order " + order);
        check(transfer.poll() == null, "transfer drained");

        final ArrayBlockingQueue<Integer> interruptQueue = new ArrayBlockingQueue<Integer>(1);
        final AtomicInteger takeInterrupted = new AtomicInteger();
        Thread blockedTaker = new Thread(new Runnable() {
            public void run() {
                try {
                    interruptQueue.take();
                } catch (InterruptedException expected) {
                    takeInterrupted.incrementAndGet();
                }
            }
        });
        blockedTaker.start();
        Thread.sleep(40);
        blockedTaker.interrupt();
        blockedTaker.join(5000);
        check(takeInterrupted.get() == 1, "take interruption");

        final LinkedBlockingQueue<Integer> lbq = new LinkedBlockingQueue<Integer>(3);
        check(lbq.remainingCapacity() == 3, "lbq initial capacity");
        Thread lbqProducer = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        lbq.put(Integer.valueOf(i));
                    }
                } catch (InterruptedException ignored) {
                }
            }
        });
        lbqProducer.start();
        StringBuilder lbqOrder = new StringBuilder();
        boolean lbqFifo = true;
        for (int i = 0; i < 10; i++) {
            Integer value = lbq.take();
            if (value == null || value.intValue() != i) {
                lbqFifo = false;
            }
            lbqOrder.append(value);
            if (i < 9) {
                lbqOrder.append(',');
            }
        }
        lbqProducer.join(5000);
        check(lbqFifo, "lbq FIFO " + lbqOrder);
        lbq.offer(Integer.valueOf(1));
        lbq.offer(Integer.valueOf(2));
        ArrayList<Integer> drained = new ArrayList<Integer>();
        int moved = lbq.drainTo(drained);
        check(moved == 2 && drained.size() == 2 && lbq.isEmpty(), "lbq drainTo");
        check(lbq.poll(20, TimeUnit.MILLISECONDS) == null, "lbq poll timeout");

        final SynchronousQueue<Integer> sync = new SynchronousQueue<Integer>();
        check(!sync.offer(Integer.valueOf(1)), "sync offer without consumer");
        check(sync.remainingCapacity() == 1, "sync capacity");
        final AtomicInteger syncValue = new AtomicInteger(-1);
        final CountDownLatch ready = new CountDownLatch(1);
        final AtomicInteger syncInterrupted = new AtomicInteger();
        Thread syncConsumer = new Thread(new Runnable() {
            public void run() {
                ready.countDown();
                try {
                    syncValue.set(sync.take().intValue());
                } catch (InterruptedException interrupted) {
                    syncInterrupted.incrementAndGet();
                }
            }
        });
        syncConsumer.start();
        ready.await();
        boolean handedOff = false;
        long handoffDeadline = System.currentTimeMillis() + 2000;
        while (!handedOff && System.currentTimeMillis() < handoffDeadline) {
            handedOff = sync.offer(Integer.valueOf(42));
            if (!handedOff) {
                Thread.sleep(5);
            }
        }
        check(handedOff, "sync handoff");
        syncConsumer.join(5000);
        check(syncInterrupted.get() == 0, "sync consumer interrupted");
        check(syncValue.get() == 42, "sync value " + syncValue.get());

        final SynchronousQueue<Integer> syncInterrupt = new SynchronousQueue<Integer>();
        final CountDownLatch interruptReady = new CountDownLatch(1);
        final AtomicInteger syncTakeInterrupted = new AtomicInteger();
        Thread syncBlocked = new Thread(new Runnable() {
            public void run() {
                interruptReady.countDown();
                try {
                    syncInterrupt.take();
                } catch (InterruptedException expected) {
                    syncTakeInterrupted.incrementAndGet();
                }
            }
        });
        syncBlocked.start();
        interruptReady.await();
        Thread.sleep(40);
        syncBlocked.interrupt();
        syncBlocked.join(5000);
        check(syncTakeInterrupted.get() == 1, "sync take interruption");

        System.out.println("blocking-queue ok checks=" + checks
                + " abq=0,1,2,3,4 lbq=true sync=42");
    }
}
