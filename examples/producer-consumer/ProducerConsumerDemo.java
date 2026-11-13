// Bounded blocking queue with multiple producers and consumers.
//
// Everything is built on the SDK's public java.lang API: Thread/Runnable,
// synchronized blocks and Object.wait/notifyAll.  There are no sleeps or
// time-based guesses: producers and consumers finish because of the queue
// protocol, and the main thread joins every worker with a finite deadline so
// a broken runtime fails the test instead of hanging CI.

public class ProducerConsumerDemo {
    static final int PRODUCERS = 3;
    static final int PER_PRODUCER = 200;
    static final int CONSUMERS = 2;
    static final int CAPACITY = 4;
    static final int TOTAL = PRODUCERS * PER_PRODUCER;
    static final long JOIN_TIMEOUT_MS = 30000;

    static final Object LOCK = new Object();
    static final int[] queue = new int[CAPACITY];
    static int head;
    static int tail;
    static int size;

    static boolean finished;
    static int produced;
    static int consumed;
    static long sumProduced;
    static long sumConsumed;
    static int duplicates;
    static int outOfRange;
    static final boolean[] seen = new boolean[TOTAL];

    static class Producer implements Runnable {
        private final int id;

        Producer(int id) {
            this.id = id;
        }

        public void run() {
            for (int k = 0; k < PER_PRODUCER; k++) {
                int value = id * PER_PRODUCER + k;
                synchronized (LOCK) {
                    while (size == CAPACITY) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException("producer " + id + " interrupted");
                        }
                    }
                    queue[tail] = value;
                    tail = (tail + 1) % CAPACITY;
                    size++;
                    produced++;
                    sumProduced += value;
                    LOCK.notifyAll();
                }
            }
        }
    }

    static class Consumer implements Runnable {
        private final int id;

        Consumer(int id) {
            this.id = id;
        }

        public void run() {
            while (true) {
                synchronized (LOCK) {
                    while (size == 0 && !finished) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException("consumer " + id + " interrupted");
                        }
                    }
                    if (size == 0 && finished) {
                        return;
                    }
                    int value = queue[head];
                    head = (head + 1) % CAPACITY;
                    size--;
                    consumed++;
                    sumConsumed += value;
                    if (value < 0 || value >= TOTAL) {
                        outOfRange++;
                    } else if (seen[value]) {
                        duplicates++;
                    } else {
                        seen[value] = true;
                    }
                    LOCK.notifyAll();
                }
            }
        }
    }

    static void joinAll(Thread[] threads, String kind) throws InterruptedException {
        for (int i = 0; i < threads.length; i++) {
            threads[i].join(JOIN_TIMEOUT_MS);
            if (threads[i].isAlive()) {
                throw new RuntimeException(kind + " " + i + " did not finish within "
                    + JOIN_TIMEOUT_MS + " ms (produced=" + produced + " consumed="
                    + consumed + " size=" + size + ")");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("producer-consumer demo: producers=" + PRODUCERS + " x "
            + PER_PRODUCER + " consumers=" + CONSUMERS + " capacity=" + CAPACITY);

        Thread[] producers = new Thread[PRODUCERS];
        Thread[] consumers = new Thread[CONSUMERS];
        for (int i = 0; i < PRODUCERS; i++) {
            producers[i] = new Thread(new Producer(i), "producer-" + i);
            producers[i].start();
        }
        for (int i = 0; i < CONSUMERS; i++) {
            consumers[i] = new Thread(new Consumer(i), "consumer-" + i);
            consumers[i].start();
        }

        joinAll(producers, "producer");

        synchronized (LOCK) {
            finished = true;
            LOCK.notifyAll();
        }

        joinAll(consumers, "consumer");

        int missing = 0;
        for (int i = 0; i < seen.length; i++) {
            if (!seen[i]) {
                missing++;
            }
        }
        long expectedSum = (long) TOTAL * (TOTAL - 1) / 2;
        boolean empty = size == 0 && head == tail;

        System.out.println("produced=" + produced + " consumed=" + consumed
            + " unique=" + (TOTAL - missing) + " duplicates=" + duplicates
            + " missing=" + missing + " outOfRange=" + outOfRange + " left=" + size);
        System.out.println("sumProduced=" + sumProduced + " sumConsumed=" + sumConsumed
            + " expectedSum=" + expectedSum);
        System.out.println("queueEmpty=" + empty);

        boolean ok = produced == TOTAL
            && consumed == TOTAL
            && duplicates == 0
            && missing == 0
            && outOfRange == 0
            && sumProduced == expectedSum
            && sumConsumed == expectedSum
            && empty;
        if (!ok) {
            throw new AssertionError("producer-consumer validation failed");
        }
        System.out.println("producer-consumer ok");
    }
}
